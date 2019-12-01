#include <AK/Demangle.h>
#include <AK/StringBuilder.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibC/signal_numbers.h>
#include <LibELF/ELFLoader.h>

//#define SIGNAL_DEBUG

u16 thread_specific_selector()
{
    static u16 selector;
    if (!selector) {
        selector = gdt_alloc_entry();
        auto& descriptor = get_gdt_entry(selector);
        descriptor.dpl = 3;
        descriptor.segment_present = 1;
        descriptor.granularity = 0;
        descriptor.zero = 0;
        descriptor.operation_size = 1;
        descriptor.descriptor_type = 1;
        descriptor.type = 2;
    }
    return selector;
}

Descriptor& thread_specific_descriptor()
{
    return get_gdt_entry(thread_specific_selector());
}

HashTable<Thread*>& thread_table()
{
    ASSERT_INTERRUPTS_DISABLED();
    static HashTable<Thread*>* table;
    if (!table)
        table = new HashTable<Thread*>;
    return *table;
}

Thread::Thread(Process& process)
    : m_process(process)
    , m_tid(process.m_next_tid++)
{
    dbgprintf("Thread{%p}: New thread TID=%u in %s(%u)\n", this, m_tid, process.name().characters(), process.pid());
    set_default_signal_dispositions();
    m_fpu_state = (FPUState*)kmalloc_aligned(sizeof(FPUState), 16);
    memset(m_fpu_state, 0, sizeof(FPUState));
    memset(&m_tss, 0, sizeof(m_tss));

    // Only IF is set when a process boots.
    m_tss.eflags = 0x0202;
    u16 cs, ds, ss, gs;

    if (m_process.is_ring0()) {
        cs = 0x08;
        ds = 0x10;
        ss = 0x10;
        gs = 0;
    } else {
        cs = 0x1b;
        ds = 0x23;
        ss = 0x23;
        gs = thread_specific_selector() | 3;
    }

    m_tss.ds = ds;
    m_tss.es = ds;
    m_tss.fs = ds;
    m_tss.gs = gs;
    m_tss.ss = ss;
    m_tss.cs = cs;

    m_tss.cr3 = m_process.page_directory().cr3();

    if (m_process.is_ring0()) {
        // FIXME: This memory is leaked.
        // But uh, there's also no kernel process termination, so I guess it's not technically leaked...
        m_kernel_stack_base = (u32)kmalloc_eternal(default_kernel_stack_size);
        m_kernel_stack_top = (m_kernel_stack_base + default_kernel_stack_size) & 0xfffffff8u;
        m_tss.esp = m_kernel_stack_top;

    } else {
        // Ring3 processes need a separate stack for Ring0.
        m_kernel_stack_region = MM.allocate_kernel_region(default_kernel_stack_size, String::format("Kernel Stack (Thread %d)", m_tid));
        m_kernel_stack_base = m_kernel_stack_region->vaddr().get();
        m_kernel_stack_top = m_kernel_stack_region->vaddr().offset(default_kernel_stack_size).get() & 0xfffffff8u;
        m_tss.ss0 = 0x10;
        m_tss.esp0 = m_kernel_stack_top;
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

void Thread::set_should_die()
{
    if (m_should_die)
        return;
    InterruptDisabler disabler;

    // Remember that we should die instead of returning to
    // the userspace.
    m_should_die = true;

    if (is_blocked()) {
        ASSERT(in_kernel());
        ASSERT(m_blocker != nullptr);
        // We're blocked in the kernel. Pretend to have
        // been interrupted by a signal (perhaps that is
        // what has actually killed us).
        m_blocker->set_interrupted_by_signal();
        unblock();
    } else if (!in_kernel()) {
        // We're executing in userspace (and we're clearly
        // not the current thread). No need to unwind, so
        // set the state to dying right away. This also
        // makes sure we won't be scheduled anymore.
        set_state(Thread::State::Dying);
    }
}

void Thread::die_if_needed()
{
    ASSERT(current == this);

    if (!m_should_die)
        return;

    InterruptDisabler disabler;
    set_state(Thread::State::Dying);
    if (!Scheduler::is_active())
        Scheduler::pick_next_and_switch_now();
}

void Thread::yield_without_holding_big_lock()
{
    bool did_unlock = process().big_lock().unlock_if_locked();
    Scheduler::yield();
    if (did_unlock)
        process().big_lock().lock();
}

bool Thread::unlock_process_if_locked()
{
    return process().big_lock().unlock_if_locked();
}

void Thread::relock_process()
{
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

u64 Thread::sleep_until(u64 wakeup_time)
{
    ASSERT(state() == Thread::Running);
    auto ret = current->block<Thread::SleepBlocker>(wakeup_time);
    if (wakeup_time > g_uptime)
        ASSERT(ret == Thread::BlockResult::InterruptedBySignal);
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
    case Thread::Queued:
        return "Queued";
    case Thread::Blocked:
        ASSERT(m_blocker != nullptr);
        return m_blocker->state_string();
    }
    kprintf("Thread::state_string(): Invalid state: %u\n", state());
    ASSERT_NOT_REACHED();
    return nullptr;
}

void Thread::finalize()
{
    ASSERT(current == g_finalizer);

    dbgprintf("Finalizing Thread %u in %s(%u)\n", tid(), m_process.name().characters(), pid());
    set_state(Thread::State::Dead);

    if (m_joiner) {
        ASSERT(m_joiner->m_joinee == this);
        static_cast<JoinBlocker*>(m_joiner->m_blocker)->set_joinee_exit_value(m_exit_value);
        m_joiner->m_joinee = nullptr;
        // NOTE: We clear the joiner pointer here as well, to be tidy.
        m_joiner = nullptr;
    }

    if (m_dump_backtrace_on_finalization)
        dbg() << backtrace_impl();

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

// Certain exceptions, such as SIGSEGV and SIGILL, put a
// thread into a state where the signal handler must be
// invoked immediately, otherwise it will continue to fault.
// This function should be used in an exception handler to
// ensure that when the thread resumes, it's executing in
// the appropriate signal handler.
void Thread::send_urgent_signal_to_self(u8 signal)
{
    // FIXME: because of a bug in dispatch_signal we can't
    // setup a signal while we are the current thread. Because of
    // this we use a work-around where we send the signal and then
    // block, allowing the scheduler to properly dispatch the signal
    // before the thread is next run.
    send_signal(signal, &process());
    (void)block<SemiPermanentBlocker>(SemiPermanentBlocker::Reason::Signal);
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

bool Thread::has_signal_handler(u8 signal) const
{
    ASSERT(signal < 32);
    auto& action = m_signal_action_data[signal];
    return !action.handler_or_sigaction.is_null();
}

static void push_value_on_user_stack(u32* stack, u32 data)
{
    *stack -= 4;
    *(u32*)*stack = data;
}

ShouldUnblockThread Thread::dispatch_signal(u8 signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal > 0 && signal <= 32);
    ASSERT(!process().is_ring0());

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
        case DefaultSignalAction::DumpCore:
            process().for_each_thread([](auto& thread) {
                thread.set_dump_backtrace_on_finalization();
                return IterationDecision::Continue;
            });
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

    ProcessPagingScope paging_scope(m_process);

    u32 old_signal_mask = m_signal_mask;
    u32 new_signal_mask = action.mask;
    if (action.flags & SA_NODEFER)
        new_signal_mask &= ~(1 << (signal - 1));
    else
        new_signal_mask |= 1 << (signal - 1);

    m_signal_mask |= new_signal_mask;

    auto setup_stack = [&]<typename ThreadState>(ThreadState state, u32 * stack)
    {
        u32 old_esp = *stack;
        u32 ret_eip = state.eip;
        u32 ret_eflags = state.eflags;

        // Align the stack to 16 bytes.
        // Note that we push 56 bytes (4 * 14) on to the stack,
        // so we need to account for this here.
        u32 stack_alignment = (*stack - 56) % 16;
        *stack -= stack_alignment;

        push_value_on_user_stack(stack, ret_eflags);

        push_value_on_user_stack(stack, ret_eip);
        push_value_on_user_stack(stack, state.eax);
        push_value_on_user_stack(stack, state.ecx);
        push_value_on_user_stack(stack, state.edx);
        push_value_on_user_stack(stack, state.ebx);
        push_value_on_user_stack(stack, old_esp);
        push_value_on_user_stack(stack, state.ebp);
        push_value_on_user_stack(stack, state.esi);
        push_value_on_user_stack(stack, state.edi);

        // PUSH old_signal_mask
        push_value_on_user_stack(stack, old_signal_mask);

        push_value_on_user_stack(stack, signal);
        push_value_on_user_stack(stack, handler_vaddr.get());
        push_value_on_user_stack(stack, 0); //push fake return address

        ASSERT((*stack % 16) == 0);
    };

    // We now place the thread state on the userspace stack.
    // Note that when we are in the kernel (ie. blocking) we cannot use the
    // tss, as that will contain kernel state; instead, we use a RegisterDump.
    // Conversely, when the thread isn't blocking the RegisterDump may not be
    // valid (fork, exec etc) but the tss will, so we use that instead.
    if (!in_kernel()) {
        u32* stack = &m_tss.esp;
        setup_stack(m_tss, stack);

        Scheduler::prepare_to_modify_tss(*this);
        m_tss.cs = 0x1b;
        m_tss.ds = 0x23;
        m_tss.es = 0x23;
        m_tss.fs = 0x23;
        m_tss.gs = thread_specific_selector() | 3;
        m_tss.eip = g_return_to_ring3_from_signal_trampoline.get();
        // FIXME: This state is such a hack. It avoids trouble if 'current' is the process receiving a signal.
        set_state(Skip1SchedulerPass);
    } else {
        auto& regs = get_RegisterDump_from_stack();
        u32* stack = &regs.esp_if_crossRing;
        setup_stack(regs, stack);
        regs.eip = g_return_to_ring3_from_signal_trampoline.get();
    }

#ifdef SIGNAL_DEBUG
    kprintf("signal: Okay, %s(%u) {%s} has been primed with signal handler %w:%x\n", process().name().characters(), pid(), state_string(), m_tss.cs, m_tss.eip);
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

RegisterDump& Thread::get_RegisterDump_from_stack()
{
    // The userspace registers should be stored at the top of the stack
    // We have to subtract 2 because the processor decrements the kernel
    // stack before pushing the args.
    return *(RegisterDump*)(kernel_stack_top() - sizeof(RegisterDump) - 2);
}

void Thread::make_userspace_stack_for_main_thread(Vector<String> arguments, Vector<String> environment)
{
    auto* region = m_process.allocate_region(VirtualAddress(), default_userspace_stack_size, "Stack (Main thread)", PROT_READ | PROT_WRITE, false);
    ASSERT(region);
    region->set_stack(true);
    m_tss.esp = region->vaddr().offset(default_userspace_stack_size).get();

    char* stack_base = (char*)region->vaddr().get();
    int argc = arguments.size();
    char** argv = (char**)stack_base;
    char** env = argv + arguments.size() + 1;
    char* bufptr = stack_base + (sizeof(char*) * (arguments.size() + 1)) + (sizeof(char*) * (environment.size() + 1));

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

Thread* Thread::clone(Process& process)
{
    auto* clone = new Thread(process);
    memcpy(clone->m_signal_action_data, m_signal_action_data, sizeof(m_signal_action_data));
    clone->m_signal_mask = m_signal_mask;
    memcpy(clone->m_fpu_state, m_fpu_state, sizeof(FPUState));
    clone->m_has_used_fpu = m_has_used_fpu;
    clone->m_thread_specific_data = m_thread_specific_data;
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
    if (new_state == m_state)
        return;

    if (new_state == Blocked) {
        // we should always have a Blocker while blocked
        ASSERT(m_blocker != nullptr);
    }

    m_state = new_state;
    if (m_process.pid() != 0) {
        Scheduler::update_state_for_thread(*this);
    }
}

String Thread::backtrace(ProcessInspectionHandle&) const
{
    return backtrace_impl();
}

String Thread::backtrace_impl() const
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
            if (!Scheduler::is_active() && process.elf_loader() && process.elf_loader()->has_symbols())
                builder.appendf("%p  %s\n", symbol.address, process.elf_loader()->symbolicate(symbol.address).characters());
            else
                builder.appendf("%p\n", symbol.address);
            continue;
        }
        unsigned offset = symbol.address - symbol.ksym->address;
        if (symbol.ksym->address == ksym_highest_address && offset > 4096)
            builder.appendf("%p\n", symbol.address);
        else
            builder.appendf("%p  %s +%u\n", symbol.address, demangle(symbol.ksym->name).characters(), offset);
    }
    return builder.to_string();
}

void Thread::make_thread_specific_region(Badge<Process>)
{
    size_t thread_specific_region_alignment = max(process().m_master_tls_alignment, alignof(ThreadSpecificData));
    size_t thread_specific_region_size = align_up_to(process().m_master_tls_size, thread_specific_region_alignment) + sizeof(ThreadSpecificData);
    auto* region = process().allocate_region({}, thread_specific_region_size, "Thread-specific", PROT_READ | PROT_WRITE, true);
    auto* thread_specific_data = (ThreadSpecificData*)region->vaddr().offset(align_up_to(process().m_master_tls_size, thread_specific_region_alignment)).as_ptr();
    auto* thread_local_storage = (u8*)((u8*)thread_specific_data) - align_up_to(process().m_master_tls_size, process().m_master_tls_alignment);
    m_thread_specific_data = VirtualAddress((u32)thread_specific_data);
    thread_specific_data->self = thread_specific_data;
    if (process().m_master_tls_size)
        memcpy(thread_local_storage, process().m_master_tls_region->vaddr().as_ptr(), process().m_master_tls_size);
}

const LogStream& operator<<(const LogStream& stream, const Thread& value)
{
    return stream << value.process().name() << "(" << value.pid() << ":" << value.tid() << ")";
}

const char* to_string(ThreadPriority priority)
{
    switch (priority) {
    case ThreadPriority::Idle:
        return "Idle";
    case ThreadPriority::Low:
        return "Low";
    case ThreadPriority::Normal:
        return "Normal";
    case ThreadPriority::High:
        return "High";
    }
    dbg() << "to_string(ThreadPriority): Invalid priority: " << (u32)priority;
    ASSERT_NOT_REACHED();
    return nullptr;
}
