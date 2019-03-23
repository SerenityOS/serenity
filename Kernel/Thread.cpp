#include <Kernel/Thread.h>
#include <Kernel/Scheduler.h>
#include <Kernel/system.h>
#include <Kernel/Process.h>
#include <Kernel/MemoryManager.h>
#include <LibC/signal_numbers.h>

InlineLinkedList<Thread>* g_threads;
static const dword default_kernel_stack_size = 16384;
static const dword default_userspace_stack_size = 65536;

Thread::Thread(Process& process)
    : m_process(process)
    , m_tid(process.m_next_tid++)
{
    dbgprintf("Thread: New thread TID=%u in %s(%u)\n", m_tid, process.name().characters(), process.pid());
    set_default_signal_dispositions();
    memset(&m_fpu_state, 0, sizeof(FPUState));
    memset(&m_tss, 0, sizeof(m_tss));

    // Only IF is set when a process boots.
    m_tss.eflags = 0x0202;
    word cs, ds, ss;

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
        dword stack_bottom = (dword)kmalloc_eternal(default_kernel_stack_size);
        m_stack_top0 = (stack_bottom + default_kernel_stack_size) & 0xffffff8;
        m_tss.esp = m_stack_top0;
    } else {
        // Ring3 processes need a separate stack for Ring0.
        m_kernel_stack = kmalloc(default_kernel_stack_size);
        m_stack_top0 = ((dword)m_kernel_stack + default_kernel_stack_size) & 0xffffff8;
        m_tss.ss0 = 0x10;
        m_tss.esp0 = m_stack_top0;
    }

    // HACK: Ring2 SS in the TSS is the current PID.
    m_tss.ss2 = m_process.pid();
    m_far_ptr.offset = 0x98765432;

    InterruptDisabler disabler;
    g_threads->prepend(this);
}

Thread::~Thread()
{
    dbgprintf("~Thread{%p}\n", this);
    {
        InterruptDisabler disabler;
        g_threads->remove(this);
    }

    if (g_last_fpu_thread == this)
        g_last_fpu_thread = nullptr;

    if (selector())
        gdt_free_entry(selector());

    if (m_kernel_stack) {
        kfree(m_kernel_stack);
        m_kernel_stack = nullptr;
    }

    if (m_kernel_stack_for_signal_handler) {
        kfree(m_kernel_stack_for_signal_handler);
        m_kernel_stack_for_signal_handler = nullptr;
    }
}

void Thread::unblock()
{
    if (current == this) {
        system.nblocked--;
        m_state = Thread::Running;
        return;
    }
    ASSERT(m_state != Thread::Runnable && m_state != Thread::Running);
    system.nblocked--;
    m_state = Thread::Runnable;
}

void Thread::snooze_until(Alarm& alarm)
{
    m_snoozing_alarm = &alarm;
    block(Thread::BlockedSnoozing);
    Scheduler::yield();
}

void Thread::block(Thread::State new_state)
{
    if (state() != Thread::Running) {
        kprintf("Thread::block: %s(%u) block(%u/%s) with state=%u/%s\n", process().name().characters(), process().pid(), new_state, to_string(new_state), state(), to_string(state()));
    }
    ASSERT(state() == Thread::Running);
    system.nblocked++;
    m_was_interrupted_while_blocked = false;
    set_state(new_state);
}

void block(Thread::State state)
{
    current->block(state);
    Scheduler::yield();
}

void sleep(dword ticks)
{
    ASSERT(current->state() == Thread::Running);
    current->set_wakeup_time(system.uptime + ticks);
    current->block(Thread::BlockedSleep);
    Scheduler::yield();
}

const char* to_string(Thread::State state)
{
    switch (state) {
    case Thread::Invalid: return "Invalid";
    case Thread::Runnable: return "Runnable";
    case Thread::Running: return "Running";
    case Thread::Dying: return "Dying";
    case Thread::Dead: return "Dead";
    case Thread::Stopped: return "Stopped";
    case Thread::Skip1SchedulerPass: return "Skip1";
    case Thread::Skip0SchedulerPasses: return "Skip0";
    case Thread::BlockedSleep: return "Sleep";
    case Thread::BlockedWait: return "Wait";
    case Thread::BlockedRead: return "Read";
    case Thread::BlockedWrite: return "Write";
    case Thread::BlockedSignal: return "Signal";
    case Thread::BlockedSelect: return "Select";
    case Thread::BlockedLurking: return "Lurking";
    case Thread::BlockedConnect: return "Connect";
    case Thread::BlockedReceive: return "Receive";
    case Thread::BlockedSnoozing: return "Snoozing";
    }
    kprintf("to_string(Thread::State): Invalid state: %u\n", state);
    ASSERT_NOT_REACHED();
    return nullptr;
}

void Thread::finalize()
{
    dbgprintf("Finalizing Thread %u in %s(%u)\n", tid(), m_process.name().characters(), pid());
    m_blocked_socket = nullptr;
    set_state(Thread::State::Dead);

    if (this == &m_process.main_thread())
        m_process.finalize();
}

void Thread::finalize_dying_threads()
{
    Vector<Thread*> dying_threads;
    {
        InterruptDisabler disabler;
        for_each_in_state(Thread::State::Dying, [&] (Thread& thread) {
            dying_threads.append(&thread);
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

void Thread::send_signal(byte signal, Process* sender)
{
    ASSERT(signal < 32);

    if (sender)
        dbgprintf("signal: %s(%u) sent %d to %s(%u)\n", sender->name().characters(), sender->pid(), signal, process().name().characters(), pid());
    else
        dbgprintf("signal: kernel sent %d to %s(%u)\n", signal, process().name().characters(), pid());

    InterruptDisabler disabler;
    m_pending_signals |= 1 << signal;
}

bool Thread::has_unmasked_pending_signals() const
{
    return m_pending_signals & ~m_signal_mask;
}

ShouldUnblockThread Thread::dispatch_one_pending_signal()
{
    ASSERT_INTERRUPTS_DISABLED();
    dword signal_candidates = m_pending_signals & ~m_signal_mask;
    ASSERT(signal_candidates);

    byte signal = 0;
    for (; signal < 32; ++signal) {
        if (signal_candidates & (1 << signal)) {
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

DefaultSignalAction default_signal_action(byte signal)
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

ShouldUnblockThread Thread::dispatch_signal(byte signal)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(signal < 32);

#ifdef SIGNAL_DEBUG
    kprintf("dispatch_signal %s(%u) <- %u\n", name().characters(), pid(), signal);
#endif

    auto& action = m_signal_action_data[signal];
    // FIXME: Implement SA_SIGINFO signal handlers.
    ASSERT(!(action.flags & SA_SIGINFO));

    // Mark this signal as handled.
    m_pending_signals &= ~(1 << signal);

    if (signal == SIGSTOP) {
        set_state(Stopped);
        return ShouldUnblockThread::No;
    }

    if (signal == SIGCONT && state() == Stopped)
        set_state(Runnable);

    auto handler_laddr = action.handler_or_sigaction;
    if (handler_laddr.is_null()) {
        switch (default_signal_action(signal)) {
        case DefaultSignalAction::Stop:
            set_state(Stopped);
            return ShouldUnblockThread::No;
        case DefaultSignalAction::DumpCore:
        case DefaultSignalAction::Terminate:
            m_process.terminate_due_to_signal(signal);
            return ShouldUnblockThread::No;
        case DefaultSignalAction::Ignore:
            return ShouldUnblockThread::No;
        case DefaultSignalAction::Continue:
            return ShouldUnblockThread::Yes;
        }
        ASSERT_NOT_REACHED();
    }

    if (handler_laddr.as_ptr() == SIG_IGN) {
#ifdef SIGNAL_DEBUG
        kprintf("%s(%u) ignored signal %u\n", name().characters(), pid(), signal);
#endif
        return ShouldUnblockThread::Yes;
    }

    dword old_signal_mask = m_signal_mask;
    dword new_signal_mask = action.mask;
    if (action.flags & SA_NODEFER)
        new_signal_mask &= ~(1 << signal);
    else
        new_signal_mask |= 1 << signal;

    m_signal_mask |= new_signal_mask;

    Scheduler::prepare_to_modify_tss(*this);

    word ret_cs = m_tss.cs;
    dword ret_eip = m_tss.eip;
    dword ret_eflags = m_tss.eflags;
    bool interrupting_in_kernel = (ret_cs & 3) == 0;

    ProcessPagingScope paging_scope(m_process);
    m_process.create_signal_trampolines_if_needed();

    if (interrupting_in_kernel) {
#ifdef SIGNAL_DEBUG
        kprintf("dispatch_signal to %s(%u) in state=%s with return to %w:%x\n", name().characters(), pid(), to_string(state()), ret_cs, ret_eip);
#endif
        ASSERT(is_blocked());
        m_tss_to_resume_kernel = m_tss;
#ifdef SIGNAL_DEBUG
        kprintf("resume tss pc: %w:%x stack: %w:%x flags: %x cr3: %x\n", m_tss_to_resume_kernel.cs, m_tss_to_resume_kernel.eip, m_tss_to_resume_kernel.ss, m_tss_to_resume_kernel.esp, m_tss_to_resume_kernel.eflags, m_tss_to_resume_kernel.cr3);
#endif

        if (!m_signal_stack_user_region) {
            m_signal_stack_user_region = m_process.allocate_region(LinearAddress(), default_userspace_stack_size, "Signal stack (user)");
            ASSERT(m_signal_stack_user_region);
        }
        if (!m_kernel_stack_for_signal_handler) {
            m_kernel_stack_for_signal_handler = kmalloc(default_kernel_stack_size);
            ASSERT(m_kernel_stack_for_signal_handler);
        }
        m_tss.ss = 0x23;
        m_tss.esp = m_signal_stack_user_region->laddr().offset(default_userspace_stack_size).get();
        m_tss.ss0 = 0x10;
        m_tss.esp0 = (dword)m_kernel_stack_for_signal_handler + default_kernel_stack_size;

        push_value_on_stack(0);
    } else {
        push_value_on_stack(ret_eip);
        push_value_on_stack(ret_eflags);

        // PUSHA
        dword old_esp = m_tss.esp;
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
    m_tss.eip = handler_laddr.get();

    // FIXME: Should we worry about the stack being 16 byte aligned when entering a signal handler?
    push_value_on_stack(signal);

    if (interrupting_in_kernel)
        push_value_on_stack(m_process.m_return_to_ring0_from_signal_trampoline.get());
    else
        push_value_on_stack(m_process.m_return_to_ring3_from_signal_trampoline.get());

    ASSERT((m_tss.esp % 16) == 0);

    // FIXME: This state is such a hack. It avoids trouble if 'current' is the process receiving a signal.
    set_state(Skip1SchedulerPass);

#ifdef SIGNAL_DEBUG
    kprintf("signal: Okay, %s(%u) {%s} has been primed with signal handler %w:%x\n", name().characters(), pid(), to_string(state()), m_tss.cs, m_tss.eip);
#endif
    return ShouldUnblockThread::Yes;
}

void Thread::set_default_signal_dispositions()
{
    // FIXME: Set up all the right default actions. See signal(7).
    memset(&m_signal_action_data, 0, sizeof(m_signal_action_data));
    m_signal_action_data[SIGCHLD].handler_or_sigaction = LinearAddress((dword)SIG_IGN);
    m_signal_action_data[SIGWINCH].handler_or_sigaction = LinearAddress((dword)SIG_IGN);
}

void Thread::push_value_on_stack(dword value)
{
    m_tss.esp -= 4;
    dword* stack_ptr = (dword*)m_tss.esp;
    *stack_ptr = value;
}

void Thread::make_userspace_stack(Vector<String> arguments, Vector<String> environment)
{
    auto* region = m_process.allocate_region(LinearAddress(), default_userspace_stack_size, "stack");
    ASSERT(region);
    m_stack_top3 = region->laddr().offset(default_userspace_stack_size).get();
    m_tss.esp = m_stack_top3;

    char* stack_base = (char*)region->laddr().get();
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
    push_value_on_stack((dword)env);
    push_value_on_stack((dword)argv);
    push_value_on_stack((dword)argc);
    push_value_on_stack(0);
}

Thread* Thread::clone(Process& process)
{
    auto* clone = new Thread(process);
    memcpy(clone->m_signal_action_data, m_signal_action_data, sizeof(m_signal_action_data));
    clone->m_signal_mask = m_signal_mask;
    //clone->m_tss = m_tss;
    clone->m_fpu_state = m_fpu_state;
    clone->m_has_used_fpu = m_has_used_fpu;
    return clone;
}

KResult Thread::wait_for_connect(Socket& socket)
{
    if (socket.is_connected())
        return KSuccess;
    m_blocked_socket = socket;
    block(Thread::State::BlockedConnect);
    Scheduler::yield();
    m_blocked_socket = nullptr;
    if (!socket.is_connected())
        return KResult(-ECONNREFUSED);
    return KSuccess;
}

void Thread::initialize()
{
    g_threads = new InlineLinkedList<Thread>;
    Scheduler::initialize();
}
