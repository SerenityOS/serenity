#include <AK/ELF/ELFLoader.h>
#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/signal_numbers.h>

//#define SIGNAL_DEBUG

HashTable<Thread*>& thread_table()
{
    ASSERT_INTERRUPTS_DISABLED();
    static HashTable<Thread*>* table;
    if (!table)
        table = new HashTable<Thread*>;
    return *table;
}

static const u32 default_kernel_stack_size = 65536;
static const u32 default_userspace_stack_size = 65536;

Thread::Thread(Process& process)
    : m_process(process)
    , m_tid(process.m_next_tid++)
{
    dbgprintf("Thread{%p}: New thread TID=%u in %s(%u)\n", this, m_tid, process.name().characters(), process.pid());
    set_default_signal_dispositions();
    m_fpu_state = (FPUState*)kmalloc_aligned(sizeof(FPUState), 16);
    memset(&m_tss, 0, sizeof(m_tss));

    // Only IF is set when a process boots.
    m_tss.eflags = 0x0202;
    u16 cs, ds, ss;

    if (m_process.is_ring0()) {
        cs = 0x08;
        ds = 0x10;
        ss = 0x10;
    } else {
        cs = 0x1b;
        ds = 0x23;
        ss = 0x23;
    }

    m_tss.ds = ds;
    m_tss.es = ds;
    m_tss.fs = ds;
    m_tss.gs = ds;
    m_tss.ss = ss;
    m_tss.cs = cs;

    m_tss.cr3 = m_process.page_directory().cr3();

    if (m_process.is_ring0()) {
        // FIXME: This memory is leaked.
        // But uh, there's also no kernel process termination, so I guess it's not technically leaked...
        m_kernel_stack_base = (u32)kmalloc_eternal(default_kernel_stack_size);
        m_tss.esp = (m_kernel_stack_base + default_kernel_stack_size) & 0xfffffff8u;

    } else {
        // Ring3 processes need a separate stack for Ring0.
        m_kernel_stack_region = MM.allocate_kernel_region(default_kernel_stack_size, String::format("Kernel Stack (Thread %d)", m_tid));
        m_kernel_stack_base = m_kernel_stack_region->vaddr().get();
        m_tss.ss0 = 0x10;
        m_tss.esp0 = m_kernel_stack_region->vaddr().offset(default_kernel_stack_size).get() & 0xfffffff8u;
    }

    // HACK: Ring2 SS in the TSS is the current PID.
    m_tss.ss2 = m_process.pid();
    m_far_ptr.offset = 0x98765432;

    if (m_process.pid() != 0) {
        InterruptDisabler disabler;
        thread_table().set(this);
        Scheduler::init_thread(*this);
    }
}

Thread::~Thread()
{
    dbgprintf("~Thread{%p}\n", this);
    kfree_aligned(m_fpu_state);
    {
        InterruptDisabler disabler;
        thread_table().remove(this);
    }

    if (g_last_fpu_thread == this)
        g_last_fpu_thread = nullptr;

    if (selector())
        gdt_free_entry(selector());

    if (m_userspace_stack_region)
        m_process.deallocate_region(*m_userspace_stack_region);

    if (m_kernel_stack_region)
        m_process.deallocate_region(*m_kernel_stack_region);

    if (m_kernel_stack_for_signal_handler_region)
        m_process.deallocate_region(*m_kernel_stack_for_signal_handler_region);
}

void Thread::unblock()
{
    if (current == this) {
        set_state(Thread::Running);
        return;
    }
    ASSERT(m_state != Thread::Runnable && m_state != Thread::Running);
    set_state(Thread::Runnable);
}

void Thread::block_helper()
{
    // This function mostly exists to avoid circular header dependencies. If
    // anything needs adding, think carefully about whether it belongs in
    // block() instead. Remember that we're unlocking here, so be very careful
    // about altering any state once we're unlocked!
    bool did_unlock = process().big_lock().unlock_if_locked();
    Scheduler::yield();
    if (did_unlock)
        process().big_lock().lock();
}

u64 Thread::sleep(u32 ticks)
{
    ASSERT(state() == Thread::Running);
    u64 wakeup_time = g_uptime + ticks;
    auto ret = current->block<Thread::SleepBlocker>(wakeup_time);
    if (wakeup_time > g_uptime) {
        ASSERT(ret == Thread::BlockResult::InterruptedBySignal);
    }
    return wakeup_time;
}

const char* Thread::state_string() const
{
    switch (state()) {
    case Thread::Invalid:
        return "Invalid";
    case Thread::Runnable:
        return "Runnable";
    case Thread::Running:
        return "Running";
    case Thread::Dying:
        return "Dying";
    case Thread::Dead:
        return "Dead";
    case Thread::Stopped:
        return "Stopped";
    case Thread::Skip1SchedulerPass:
        return "Skip1";
    case Thread::Skip0SchedulerPasses:
        return "Skip0";
    case Thread::Blocked:
        ASSERT(!m_blockers.is_empty());
        return m_blockers.first()->state_string();
    }
    kprintf("to_string(Thread::State): Invalid state: %u\n", state());
    ASSERT_NOT_REACHED();
    return nullptr;
}

void Thread::finalize()
{
    ASSERT(current == g_finalizer);

    dbgprintf("Finalizing Thread %u in %s(%u)\n", tid(), m_process.name().characters(), pid());
    set_state(Thread::State::Dead);

    if (this == &m_process.main_thread()) {
        m_process.finalize();
        return;
    }

    delete this;
}

void Thread::finalize_dying_threads()
{
    ASSERT(current == g_finalizer);
    Vector<Thread*, 32> dying_threads;
    {
        InterruptDisabler disabler;
        for_each_in_state(Thread::State::Dying, [&](Thread& thread) {
            dying_threads.append(&thread);
            return IterationDecision::Continue;
        });
    }
    for (auto* thread : dying_threads)
        thread->finalize();
}

bool Thread::tick()
{
    ++m_ticks;
    if (tss().cs & 3)
        ++m_process.m_ticks_in_user;
    else
        ++m_process.m_ticks_in_kernel;
    return --m_ticks_left;
}

void Thread::send_signal(u8 signal, Process* sender)
{
    ASSERT(signal < 32);
    InterruptDisabler disabler;

    // FIXME: Figure out what to do for masked signals. Should we also ignore them here?
    if (should_ignore_signal(signal)) {
        dbg() << "signal " << signal << " was ignored by " << process();
        return;
    }

    if (sender)
        dbgprintf("signal: %s(%u) sent %d to %s(%u)\n", sender->name().characters(), sender->pid(), signal, process().name().characters(), pid());
    else
        dbgprintf("signal: kernel sent %d to %s(%u)\n", signal, process().name().characters(), pid());

    m_pending_signals |= 1 << (signal - 1);
}

bool Thread::has_unmasked_pending_signals() const
{
    return m_pending_signals & ~m_signal_mask;
}

ShouldUnblockThread Thread::dispatch_one_pending_signal()
{
    ASSERT_INTERRUPTS_DISABLED();
    u32 signal_candidates = m_pending_signals & ~m_signal_mask;
    ASSERT(signal_candidates);

    u8 signal = 1;
    for (; signal < 32; ++signal) {
        if (signal_candidates & (1 << (signal - 1))) {
            break;
        }
    }
    return dispatch_signal(signal);
}

enum class DefaultSignalAction {
    Terminate,
    Ignore,
    DumpCore,
    Stop,
    Continue,
};

DefaultSignalAction default_signal_action(u8 signal)
{
    ASSERT(signal && signal < NSIG);

    switch (signal) {
    case SIGHUP:
    case SIGINT:
    case SIGKILL:
    case SIGPIPE:
    case SIGALRM:
    case SIGUSR1:
    case SIGUSR2:
    case SIGVTALRM:
    case SIGSTKFLT:
    case SIGIO:
    case SIGPROF:
    case SIGTERM:
    case SIGPWR:
        return DefaultSignalAction::Terminate;
    case SIGCHLD:
    case SIGURG:
    case SIGWINCH:
        return DefaultSignalAction::Ignore;
    case SIGQUIT:
    case SIGILL:
    case SIGTRAP:
    case SIGABRT:
    case SIGBUS:
    case SIGFPE:
    case SIGSEGV:
    case SIGXCPU:
    case SIGXFSZ:
    case SIGSYS:
        return DefaultSignalAction::DumpCore;
    case SIGCONT:
        return DefaultSignalAction::Continue;
    case SIGSTOP:
    case SIGTSTP:
    case SIGTTIN:
    case SIGTTOU:
        return DefaultSignalAction::Stop;
    }
    ASSERT_NOT_REACHED();
}

bool Thread::should_ignore_signal(u8 signal) const
{
    ASSERT(signal < 32);
    auto& action = m_signal_action_data[signal];
    if (action.handler_or_sigaction.is_null())
        return default_signal_action(signal) == DefaultSignalAction::Ignore;
    if (action.handler_or_sigaction.as_ptr() == SIG_IGN)
        return true;
    return false;
}

ShouldUnblockThread Thread::dispatch_signal(u8 signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal > 0 && signal <= 32);

#ifdef SIGNAL_DEBUG
    kprintf("dispatch_signal %s(%u) <- %u\n", process().name().characters(), pid(), signal);
#endif

    auto& action = m_signal_action_data[signal];
    // FIXME: Implement SA_SIGINFO signal handlers.
    ASSERT(!(action.flags & SA_SIGINFO));

    // Mark this signal as handled.
    m_pending_signals &= ~(1 << (signal - 1));

    if (signal == SIGSTOP) {
        set_state(Stopped);
        return ShouldUnblockThread::No;
    }

    if (signal == SIGCONT && state() == Stopped)
        set_state(Runnable);

    auto handler_vaddr = action.handler_or_sigaction;
    if (handler_vaddr.is_null()) {
        switch (default_signal_action(signal)) {
        case DefaultSignalAction::Stop:
            set_state(Stopped);
            return ShouldUnblockThread::No;
        case DefaultSignalAction::DumpCore: {
            ProcessInspectionHandle handle(process());
            dbg() << "Dumping \"Core\" for " << process();
            dbg() << process().backtrace(handle);
        }
            [[fallthrough]];
        case DefaultSignalAction::Terminate:
            m_process.terminate_due_to_signal(signal);
            return ShouldUnblockThread::No;
        case DefaultSignalAction::Ignore:
            ASSERT_NOT_REACHED();
        case DefaultSignalAction::Continue:
            return ShouldUnblockThread::Yes;
        }
        ASSERT_NOT_REACHED();
    }

    if (handler_vaddr.as_ptr() == SIG_IGN) {
#ifdef SIGNAL_DEBUG
        kprintf("%s(%u) ignored signal %u\n", process().name().characters(), pid(), signal);
#endif
        return ShouldUnblockThread::Yes;
    }

    u32 old_signal_mask = m_signal_mask;
    u32 new_signal_mask = action.mask;
    if (action.flags & SA_NODEFER)
        new_signal_mask &= ~(1 << (signal - 1));
    else
        new_signal_mask |= 1 << (signal - 1);

    m_signal_mask |= new_signal_mask;

    Scheduler::prepare_to_modify_tss(*this);

    u16 ret_cs = m_tss.cs;
    u32 ret_eip = m_tss.eip;
    u32 ret_eflags = m_tss.eflags;
    bool interrupting_in_kernel = (ret_cs & 3) == 0;

    ProcessPagingScope paging_scope(m_process);

    if (interrupting_in_kernel) {
#ifdef SIGNAL_DEBUG
        kprintf("dispatch_signal to %s(%u) in state=%s with return to %w:%x\n", process().name().characters(), pid(), to_string(state()), ret_cs, ret_eip);
#endif
        ASSERT(is_blocked());
        m_tss_to_resume_kernel = make<TSS32>(m_tss);
#ifdef SIGNAL_DEBUG
        kprintf("resume tss pc: %w:%x stack: %w:%x flags: %x cr3: %x\n", m_tss_to_resume_kernel->cs, m_tss_to_resume_kernel->eip, m_tss_to_resume_kernel->ss, m_tss_to_resume_kernel->esp, m_tss_to_resume_kernel->eflags, m_tss_to_resume_kernel->cr3);
#endif

        if (!m_signal_stack_user_region) {
            m_signal_stack_user_region = m_process.allocate_region(VirtualAddress(), default_userspace_stack_size, String::format("User Signal Stack (Thread %d)", m_tid));
            ASSERT(m_signal_stack_user_region);
        }
        if (!m_kernel_stack_for_signal_handler_region)
            m_kernel_stack_for_signal_handler_region = MM.allocate_kernel_region(default_kernel_stack_size, String::format("Kernel Signal Stack (Thread %d)", m_tid));
        m_tss.ss = 0x23;
        m_tss.esp = m_signal_stack_user_region->vaddr().offset(default_userspace_stack_size).get();
        m_tss.ss0 = 0x10;
        m_tss.esp0 = m_kernel_stack_for_signal_handler_region->vaddr().offset(default_kernel_stack_size).get();

        push_value_on_stack(0);
    } else {
        push_value_on_stack(ret_eip);
        push_value_on_stack(ret_eflags);

        // PUSHA
        u32 old_esp = m_tss.esp;
        push_value_on_stack(m_tss.eax);
        push_value_on_stack(m_tss.ecx);
        push_value_on_stack(m_tss.edx);
        push_value_on_stack(m_tss.ebx);
        push_value_on_stack(old_esp);
        push_value_on_stack(m_tss.ebp);
        push_value_on_stack(m_tss.esi);
        push_value_on_stack(m_tss.edi);

        // Align the stack.
        m_tss.esp -= 12;
    }

    // PUSH old_signal_mask
    push_value_on_stack(old_signal_mask);

    m_tss.cs = 0x1b;
    m_tss.ds = 0x23;
    m_tss.es = 0x23;
    m_tss.fs = 0x23;
    m_tss.gs = 0x23;
    m_tss.eip = handler_vaddr.get();

    // FIXME: Should we worry about the stack being 16 byte aligned when entering a signal handler?
    push_value_on_stack(signal);

    if (interrupting_in_kernel)
        push_value_on_stack(g_return_to_ring0_from_signal_trampoline.get());
    else
        push_value_on_stack(g_return_to_ring3_from_signal_trampoline.get());

    ASSERT((m_tss.esp % 16) == 0);

    // FIXME: This state is such a hack. It avoids trouble if 'current' is the process receiving a signal.
    set_state(Skip1SchedulerPass);

#ifdef SIGNAL_DEBUG
    kprintf("signal: Okay, %s(%u) {%s} has been primed with signal handler %w:%x\n", process().name().characters(), pid(), to_string(state()), m_tss.cs, m_tss.eip);
#endif
    return ShouldUnblockThread::Yes;
}

void Thread::set_default_signal_dispositions()
{
    // FIXME: Set up all the right default actions. See signal(7).
    memset(&m_signal_action_data, 0, sizeof(m_signal_action_data));
    m_signal_action_data[SIGCHLD].handler_or_sigaction = VirtualAddress((u32)SIG_IGN);
    m_signal_action_data[SIGWINCH].handler_or_sigaction = VirtualAddress((u32)SIG_IGN);
}

void Thread::push_value_on_stack(u32 value)
{
    m_tss.esp -= 4;
    u32* stack_ptr = (u32*)m_tss.esp;
    *stack_ptr = value;
}

void Thread::make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment)
{
    auto* region = m_process.allocate_region(VirtualAddress(), default_userspace_stack_size, "Stack (Main thread)");
    ASSERT(region);
    m_tss.esp = region->vaddr().offset(default_userspace_stack_size).get();

    char* stack_base = (char*)region->vaddr().get();
    int argc = arguments.size();
    char** argv = (char**)stack_base;
    char** env = argv + arguments.size() + 1;
    char* bufptr = stack_base + (sizeof(char*) * (arguments.size() + 1)) + (sizeof(char*) * (environment.size() + 1));

    size_t total_blob_size = 0;
    for (auto& a : arguments)
        total_blob_size += a.length() + 1;
    for (auto& e : environment)
        total_blob_size += e.length() + 1;

    size_t total_meta_size = sizeof(char*) * (arguments.size() + 1) + sizeof(char*) * (environment.size() + 1);

    // FIXME: It would be better if this didn't make us panic.
    ASSERT((total_blob_size + total_meta_size) < default_userspace_stack_size);

    for (int i = 0; i < arguments.size(); ++i) {
        argv[i] = bufptr;
        memcpy(bufptr, arguments[i].characters(), arguments[i].length());
        bufptr += arguments[i].length();
        *(bufptr++) = '\0';
    }
    argv[arguments.size()] = nullptr;

    for (int i = 0; i < environment.size(); ++i) {
        env[i] = bufptr;
        memcpy(bufptr, environment[i].characters(), environment[i].length());
        bufptr += environment[i].length();
        *(bufptr++) = '\0';
    }
    env[environment.size()] = nullptr;

    // NOTE: The stack needs to be 16-byte aligned.
    push_value_on_stack((u32)env);
    push_value_on_stack((u32)argv);
    push_value_on_stack((u32)argc);
    push_value_on_stack(0);
}

void Thread::make_userspace_stack_for_secondary_thread(void* argument)
{
    m_userspace_stack_region = m_process.allocate_region(VirtualAddress(), default_userspace_stack_size, String::format("Stack (Thread %d)", tid()));
    ASSERT(m_userspace_stack_region);
    m_tss.esp = m_userspace_stack_region->vaddr().offset(default_userspace_stack_size).get();

    // NOTE: The stack needs to be 16-byte aligned.
    push_value_on_stack((u32)argument);
    push_value_on_stack(0);
}

Thread* Thread::clone(Process& process)
{
    auto* clone = new Thread(process);
    memcpy(clone->m_signal_action_data, m_signal_action_data, sizeof(m_signal_action_data));
    clone->m_signal_mask = m_signal_mask;
    clone->m_fpu_state = (FPUState*)kmalloc_aligned(sizeof(FPUState), 16);
    memcpy(clone->m_fpu_state, m_fpu_state, sizeof(FPUState));
    clone->m_has_used_fpu = m_has_used_fpu;
    return clone;
}

void Thread::initialize()
{
    Scheduler::initialize();
}

Vector<Thread*> Thread::all_threads()
{
    Vector<Thread*> threads;
    InterruptDisabler disabler;
    threads.ensure_capacity(thread_table().size());
    for (auto* thread : thread_table())
        threads.unchecked_append(thread);
    return threads;
}

bool Thread::is_thread(void* ptr)
{
    ASSERT_INTERRUPTS_DISABLED();
    return thread_table().contains((Thread*)ptr);
}

void Thread::set_state(State new_state)
{
    InterruptDisabler disabler;
    if (new_state == Blocked) {
        // we should always have a Blocker while blocked
        ASSERT(!m_blockers.is_empty());
    }

    m_state = new_state;
    if (m_process.pid() != 0) {
        Scheduler::update_state_for_thread(*this);
    }
}

String Thread::backtrace(ProcessInspectionHandle&) const
{
    auto& process = const_cast<Process&>(this->process());
    ProcessPagingScope paging_scope(process);
    struct RecognizedSymbol {
        u32 address;
        const KSym* ksym;
    };
    StringBuilder builder;
    Vector<RecognizedSymbol, 64> recognized_symbols;
    recognized_symbols.append({ tss().eip, ksymbolicate(tss().eip) });
    for (u32* stack_ptr = (u32*)frame_ptr(); process.validate_read_from_kernel(VirtualAddress((u32)stack_ptr)); stack_ptr = (u32*)*stack_ptr) {
        u32 retaddr = stack_ptr[1];
        recognized_symbols.append({ retaddr, ksymbolicate(retaddr) });
    }

    for (auto& symbol : recognized_symbols) {
        if (!symbol.address)
            break;
        if (!symbol.ksym) {
#ifdef EXPENSIVE_USERSPACE_STACKS
            if (!Scheduler::is_active() && process.elf_loader() && process.elf_loader()->has_symbols())
                builder.appendf("%p  %s\n", symbol.address, process.elf_loader()->symbolicate(symbol.address).characters());
            else
#endif
                builder.appendf("%p\n", symbol.address);
            continue;
        }
        unsigned offset = symbol.address - symbol.ksym->address;
        if (symbol.ksym->address == ksym_highest_address && offset > 4096)
            builder.appendf("%p\n", symbol.address);
        else
            builder.appendf("%p  %s +%u\n", symbol.address, symbol.ksym->name, offset);
    }
    return builder.to_string();
}
