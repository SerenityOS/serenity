#include <AK/TemporaryChange.h>
#include <Kernel/Arch/i386/PIT.h>
#include <Kernel/Devices/PCSpeaker.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/RTC.h>
#include <Kernel/Scheduler.h>

struct SchedulerData {
    typedef IntrusiveList<Thread, &Thread::m_runnable_list_node> ThreadList;

    ThreadList m_runnable_threads;
    ThreadList m_nonrunnable_threads;

    ThreadList& thread_list_for_state(Thread::State state)
    {
        if (Thread::is_runnable_state(state))
            return m_runnable_threads;
        return m_nonrunnable_threads;
    }
};

static SchedulerData* g_scheduler_data;

void Scheduler::init_thread(Thread& thread)
{
    g_scheduler_data->m_nonrunnable_threads.append(thread);
}

void Scheduler::update_state_for_thread(Thread& thread)
{
    auto& list = g_scheduler_data->thread_list_for_state(thread.state());

    if (list.contains(thread))
        return;

    list.append(thread);
}

IterationDecision Scheduler::for_each_runnable_func(Function<IterationDecision(Thread&)>&& callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    auto& tl = g_scheduler_data->m_runnable_threads;
    for (auto it = tl.begin(); it != tl.end();) {
        auto thread = *it;
        it = ++it;
        if (callback(*thread) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    return IterationDecision::Continue;
}

IterationDecision Scheduler::for_each_nonrunnable_func(Function<IterationDecision(Thread&)>&& callback)
{
    ASSERT_INTERRUPTS_DISABLED();
    auto& tl = g_scheduler_data->m_nonrunnable_threads;
    for (auto it = tl.begin(); it != tl.end();) {
        auto thread = *it;
        it = ++it;
        if (callback(*thread) == IterationDecision::Break)
            return IterationDecision::Break;
    }

    return IterationDecision::Continue;
}

//#define LOG_EVERY_CONTEXT_SWITCH
//#define SCHEDULER_DEBUG
//#define SCHEDULER_RUNNABLE_DEBUG

static u32 time_slice_for(Process::Priority priority)
{
    // One time slice unit == 1ms
    switch (priority) {
    case Process::HighPriority:
        return 50;
    case Process::NormalPriority:
        return 15;
    case Process::LowPriority:
        return 5;
    case Process::IdlePriority:
        return 1;
    }
    ASSERT_NOT_REACHED();
}

Thread* current;
Thread* g_last_fpu_thread;
Thread* g_finalizer;
static Process* s_colonel_process;
u64 g_uptime;
static u64 s_beep_timeout;

struct TaskRedirectionData {
    u16 selector;
    TSS32 tss;
};
static TaskRedirectionData s_redirection;
static bool s_active;

bool Scheduler::is_active()
{
    return s_active;
}

void Scheduler::beep()
{
    PCSpeaker::tone_on(440);
    s_beep_timeout = g_uptime + 100;
}

Thread::FileDescriptionBlocker::FileDescriptionBlocker(const FileDescription& description)
    : m_blocked_description(description)
{}

const FileDescription& Thread::FileDescriptionBlocker::blocked_description() const
{
    return m_blocked_description;
}

Thread::AcceptBlocker::AcceptBlocker(const FileDescription& description)
    : FileDescriptionBlocker(description)
{
}

bool Thread::AcceptBlocker::should_unblock(Thread&, time_t, long)
{
    auto& socket = *blocked_description().socket();
    return socket.can_accept();
}

Thread::ReceiveBlocker::ReceiveBlocker(const FileDescription& description)
    : FileDescriptionBlocker(description)
{
}

bool Thread::ReceiveBlocker::should_unblock(Thread&, time_t now_sec, long now_usec)
{
    auto& socket = *blocked_description().socket();
    // FIXME: Block until the amount of data wanted is available.
    bool timed_out = now_sec > socket.receive_deadline().tv_sec || (now_sec == socket.receive_deadline().tv_sec && now_usec >= socket.receive_deadline().tv_usec);
    if (timed_out || blocked_description().can_read())
        return true;
    return false;
}

Thread::ConnectBlocker::ConnectBlocker(const FileDescription& description)
    : FileDescriptionBlocker(description)
{
}

bool Thread::ConnectBlocker::should_unblock(Thread&, time_t, long)
{
    auto& socket = *blocked_description().socket();
    return socket.is_connected();
}

Thread::WriteBlocker::WriteBlocker(const FileDescription& description)
    : FileDescriptionBlocker(description)
{
}

bool Thread::WriteBlocker::should_unblock(Thread&, time_t, long)
{
    return blocked_description().can_write();
}

Thread::ReadBlocker::ReadBlocker(const FileDescription& description)
    : FileDescriptionBlocker(description)
{
}

bool Thread::ReadBlocker::should_unblock(Thread&, time_t, long)
{
    // FIXME: Block until the amount of data wanted is available.
    return blocked_description().can_read();
}

Thread::ConditionBlocker::ConditionBlocker(const char* state_string, Function<bool()>&& condition)
    : m_block_until_condition(move(condition))
    , m_state_string(state_string)
{
    ASSERT(m_block_until_condition);
}

bool Thread::ConditionBlocker::should_unblock(Thread&, time_t, long)
{
    return m_block_until_condition();
}

Thread::SleepBlocker::SleepBlocker(u64 wakeup_time)
    : m_wakeup_time(wakeup_time)
{
}

bool Thread::SleepBlocker::should_unblock(Thread&, time_t, long)
{
    return m_wakeup_time <= g_uptime;
}

Thread::SelectBlocker::SelectBlocker(const timeval& tv, bool select_has_timeout, const FDVector& read_fds, const FDVector& write_fds, const FDVector& except_fds)
    : m_select_timeout(tv)
    , m_select_has_timeout(select_has_timeout)
    , m_select_read_fds(read_fds)
    , m_select_write_fds(write_fds)
    , m_select_exceptional_fds(except_fds)
{
}

bool Thread::SelectBlocker::should_unblock(Thread& thread, time_t now_sec, long now_usec)
{
    if (m_select_has_timeout) {
        if (now_sec > m_select_timeout.tv_sec || (now_sec == m_select_timeout.tv_sec && now_usec >= m_select_timeout.tv_usec))
            return true;
    }

    auto& process = thread.process();
    for (int fd : m_select_read_fds) {
        if (process.m_fds[fd].description->can_read())
            return true;
    }
    for (int fd : m_select_write_fds) {
        if (process.m_fds[fd].description->can_write())
            return true;
    }

    return false;
}

Thread::WaitBlocker::WaitBlocker(int wait_options, pid_t& waitee_pid)
    : m_wait_options(wait_options)
    , m_waitee_pid(waitee_pid)
{
}

bool Thread::WaitBlocker::should_unblock(Thread& thread, time_t, long)
{
    bool should_unblock = false;
    thread.process().for_each_child([&](Process& child) {
        if (m_waitee_pid != -1 && m_waitee_pid != child.pid())
            return IterationDecision::Continue;

        bool child_exited = child.is_dead();
        bool child_stopped = child.main_thread().state() == Thread::State::Stopped;

        bool wait_finished = ((m_wait_options & WEXITED) && child_exited)
            || ((m_wait_options & WSTOPPED) && child_stopped);

        if (!wait_finished)
            return IterationDecision::Continue;

        m_waitee_pid = child.pid();
        should_unblock = true;
        return IterationDecision::Break;
    });
    return should_unblock;
}

Thread::SemiPermanentBlocker::SemiPermanentBlocker(Reason reason)
    : m_reason(reason)
{}

bool Thread::SemiPermanentBlocker::should_unblock(Thread&, time_t, long)
{
    // someone else has to unblock us
    return false;
}

// Called by the scheduler on threads that are blocked for some reason.
// Make a decision as to whether to unblock them or not.
void Thread::consider_unblock(time_t now_sec, long now_usec)
{
    switch (state()) {
    case Thread::Invalid:
    case Thread::Runnable:
    case Thread::Running:
    case Thread::Dead:
    case Thread::Stopped:
        /* don't know, don't care */
        return;
    case Thread::Blocked:
        ASSERT(!m_blockers.is_empty());
        if (m_blockers.first()->should_unblock(*this, now_sec, now_usec))
            unblock();
        return;
    case Thread::Skip1SchedulerPass:
        set_state(Thread::Skip0SchedulerPasses);
        return;
    case Thread::Skip0SchedulerPasses:
        set_state(Thread::Runnable);
        return;
    case Thread::Dying:
        ASSERT(g_finalizer);
        if (g_finalizer->is_blocked())
            g_finalizer->unblock();
        return;
    }
}

bool Scheduler::pick_next()
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!s_active);

    TemporaryChange<bool> change(s_active, true);

    ASSERT(s_active);

    if (!current) {
        // XXX: The first ever context_switch() goes to the idle process.
        //      This to setup a reliable place we can return to.
        return context_switch(s_colonel_process->main_thread());
    }

    struct timeval now;
    kgettimeofday(now);

    auto now_sec = now.tv_sec;
    auto now_usec = now.tv_usec;

    // Check and unblock threads whose wait conditions have been met.
    Scheduler::for_each_nonrunnable([&](Thread& thread) {
        thread.consider_unblock(now_sec, now_usec);
        return IterationDecision::Continue;
    });

    Process::for_each([&](Process& process) {
        if (process.is_dead()) {
            if (current != &process.main_thread() && (!process.ppid() || !Process::from_pid(process.ppid()))) {
                auto name = process.name();
                auto pid = process.pid();
                auto exit_status = Process::reap(process);
                dbgprintf("reaped unparented process %s(%u), exit status: %u\n", name.characters(), pid, exit_status);
            }
            return IterationDecision::Continue;
        }
        if (process.m_alarm_deadline && g_uptime > process.m_alarm_deadline) {
            process.m_alarm_deadline = 0;
            process.send_signal(SIGALRM, nullptr);
        }
        return IterationDecision::Continue;
    });

    // Dispatch any pending signals.
    // FIXME: Do we really need this to be a separate pass over the process list?
    Thread::for_each_living([](Thread& thread) -> IterationDecision {
        if (!thread.has_unmasked_pending_signals())
            return IterationDecision::Continue;
        // FIXME: It would be nice if the Scheduler didn't have to worry about who is "current"
        //        For now, avoid dispatching signals to "current" and do it in a scheduling pass
        //        while some other process is interrupted. Otherwise a mess will be made.
        if (&thread == current)
            return IterationDecision::Continue;
        // We know how to interrupt blocked processes, but if they are just executing
        // at some random point in the kernel, let them continue. They'll be in userspace
        // sooner or later and we can deliver the signal then.
        // FIXME: Maybe we could check when returning from a syscall if there's a pending
        //        signal and dispatch it then and there? Would that be doable without the
        //        syscall effectively being "interrupted" despite having completed?
        if (thread.in_kernel() && !thread.is_blocked() && !thread.is_stopped())
            return IterationDecision::Continue;
        // NOTE: dispatch_one_pending_signal() may unblock the process.
        bool was_blocked = thread.is_blocked();
        if (thread.dispatch_one_pending_signal() == ShouldUnblockThread::No)
            return IterationDecision::Continue;
        if (was_blocked) {
            dbgprintf("Unblock %s(%u) due to signal\n", thread.process().name().characters(), thread.pid());
            ASSERT(!thread.m_blockers.is_empty());
            thread.m_blockers.first()->set_interrupted_by_signal();
            thread.unblock();
        }
        return IterationDecision::Continue;
    });

#ifdef SCHEDULER_RUNNABLE_DEBUG
    dbgprintf("Non-runnables:\n");
    Scheduler::for_each_nonrunnable([](Thread& thread) -> IterationDecision {
        auto& process = thread.process();
        dbgprintf("[K%x] %-12s %s(%u:%u) @ %w:%x\n", &process, thread.state_string(), process.name().characters(), process.pid(), thread.tid(), thread.tss().cs, thread.tss().eip);
        return IterationDecision::Continue;
    });

    dbgprintf("Runnables:\n");
    Scheduler::for_each_runnable([](Thread& thread) -> IterationDecision {
        auto& process = thread.process();
        dbgprintf("[K%x] %-12s %s(%u:%u) @ %w:%x\n", &process, thread.state_string(), process.name().characters(), process.pid(), thread.tid(), thread.tss().cs, thread.tss().eip);
        return IterationDecision::Continue;
    });
#endif

    auto& runnable_list = g_scheduler_data->m_runnable_threads;
    if (runnable_list.is_empty())
        return context_switch(s_colonel_process->main_thread());

    auto* previous_head = runnable_list.first();
    for (;;) {
        // Move head to tail.
        runnable_list.append(*runnable_list.first());
        auto* thread = runnable_list.first();

        if (!thread->process().is_being_inspected() && (thread->state() == Thread::Runnable || thread->state() == Thread::Running)) {
#ifdef SCHEDULER_DEBUG
            dbgprintf("switch to %s(%u:%u) @ %w:%x\n", thread->process().name().characters(), thread->process().pid(), thread->tid(), thread->tss().cs, thread->tss().eip);
#endif
            return context_switch(*thread);
        }

        if (thread == previous_head) {
            // Back at process_head, nothing wants to run. Send in the colonel!
            return context_switch(s_colonel_process->main_thread());
        }
    }
}

bool Scheduler::donate_to(Thread* beneficiary, const char* reason)
{
    InterruptDisabler disabler;
    if (!Thread::is_thread(beneficiary))
        return false;

    (void)reason;
    unsigned ticks_left = current->ticks_left();
    if (!beneficiary || beneficiary->state() != Thread::Runnable || ticks_left <= 1)
        return yield();

    unsigned ticks_to_donate = min(ticks_left - 1, time_slice_for(beneficiary->process().priority()));
#ifdef SCHEDULER_DEBUG
    dbgprintf("%s(%u:%u) donating %u ticks to %s(%u:%u), reason=%s\n", current->process().name().characters(), current->pid(), current->tid(), ticks_to_donate, beneficiary->process().name().characters(), beneficiary->pid(), beneficiary->tid(), reason);
#endif
    context_switch(*beneficiary);
    beneficiary->set_ticks_left(ticks_to_donate);
    switch_now();
    return false;
}

bool Scheduler::yield()
{
    InterruptDisabler disabler;
    ASSERT(current);
    //    dbgprintf("%s(%u:%u) yield()\n", current->process().name().characters(), current->pid(), current->tid());

    if (!pick_next())
        return false;

    //    dbgprintf("yield() jumping to new process: sel=%x, %s(%u:%u)\n", current->far_ptr().selector, current->process().name().characters(), current->pid(), current->tid());
    switch_now();
    return true;
}

void Scheduler::pick_next_and_switch_now()
{
    bool someone_wants_to_run = pick_next();
    ASSERT(someone_wants_to_run);
    switch_now();
}

void Scheduler::switch_now()
{
    Descriptor& descriptor = get_gdt_entry(current->selector());
    descriptor.type = 9;
    flush_gdt();
    asm("sti\n"
        "ljmp *(%%eax)\n" ::"a"(&current->far_ptr()));
}

bool Scheduler::context_switch(Thread& thread)
{
    thread.set_ticks_left(time_slice_for(thread.process().priority()));
    thread.did_schedule();

    if (current == &thread)
        return false;

    if (current) {
        // If the last process hasn't blocked (still marked as running),
        // mark it as runnable for the next round.
        if (current->state() == Thread::Running)
            current->set_state(Thread::Runnable);

#ifdef LOG_EVERY_CONTEXT_SWITCH
        dbgprintf("Scheduler: %s(%u:%u) -> %s(%u:%u) %w:%x\n",
            current->process().name().characters(), current->process().pid(), current->tid(),
            thread.process().name().characters(), thread.process().pid(), thread.tid(),
            thread.tss().cs, thread.tss().eip);
#endif
    }

    current = &thread;
    thread.set_state(Thread::Running);

    if (!thread.selector()) {
        thread.set_selector(gdt_alloc_entry());
        auto& descriptor = get_gdt_entry(thread.selector());
        descriptor.set_base(&thread.tss());
        descriptor.set_limit(0xffff);
        descriptor.dpl = 0;
        descriptor.segment_present = 1;
        descriptor.granularity = 1;
        descriptor.zero = 0;
        descriptor.operation_size = 1;
        descriptor.descriptor_type = 0;
    }

    auto& descriptor = get_gdt_entry(thread.selector());
    descriptor.type = 11; // Busy TSS
    flush_gdt();
    return true;
}

static void initialize_redirection()
{
    auto& descriptor = get_gdt_entry(s_redirection.selector);
    descriptor.set_base(&s_redirection.tss);
    descriptor.set_limit(0xffff);
    descriptor.dpl = 0;
    descriptor.segment_present = 1;
    descriptor.granularity = 1;
    descriptor.zero = 0;
    descriptor.operation_size = 1;
    descriptor.descriptor_type = 0;
    descriptor.type = 9;
    flush_gdt();
}

void Scheduler::prepare_for_iret_to_new_process()
{
    auto& descriptor = get_gdt_entry(s_redirection.selector);
    descriptor.type = 9;
    s_redirection.tss.backlink = current->selector();
    load_task_register(s_redirection.selector);
}

void Scheduler::prepare_to_modify_tss(Thread& thread)
{
    // This ensures that a currently running process modifying its own TSS
    // in order to yield() and end up somewhere else doesn't just end up
    // right after the yield().
    if (current == &thread)
        load_task_register(s_redirection.selector);
}

Process* Scheduler::colonel()
{
    return s_colonel_process;
}

void Scheduler::initialize()
{
    g_scheduler_data = new SchedulerData;
    s_redirection.selector = gdt_alloc_entry();
    initialize_redirection();
    s_colonel_process = Process::create_kernel_process("colonel", nullptr);
    // Make sure the colonel uses a smallish time slice.
    s_colonel_process->set_priority(Process::IdlePriority);
    load_task_register(s_redirection.selector);
}

void Scheduler::timer_tick(RegisterDump& regs)
{
    if (!current)
        return;

    ++g_uptime;

    if (s_beep_timeout && g_uptime > s_beep_timeout) {
        PCSpeaker::tone_off();
        s_beep_timeout = 0;
    }

    if (current->tick())
        return;

    current->tss().gs = regs.gs;
    current->tss().fs = regs.fs;
    current->tss().es = regs.es;
    current->tss().ds = regs.ds;
    current->tss().edi = regs.edi;
    current->tss().esi = regs.esi;
    current->tss().ebp = regs.ebp;
    current->tss().ebx = regs.ebx;
    current->tss().edx = regs.edx;
    current->tss().ecx = regs.ecx;
    current->tss().eax = regs.eax;
    current->tss().eip = regs.eip;
    current->tss().cs = regs.cs;
    current->tss().eflags = regs.eflags;

    // Compute process stack pointer.
    // Add 12 for CS, EIP, EFLAGS (interrupt mechanic)
    current->tss().esp = regs.esp + 12;
    current->tss().ss = regs.ss;

    if ((current->tss().cs & 3) != 0) {
        current->tss().ss = regs.ss_if_crossRing;
        current->tss().esp = regs.esp_if_crossRing;
    }

    if (!pick_next())
        return;
    prepare_for_iret_to_new_process();

    // Set the NT (nested task) flag.
    asm(
        "pushf\n"
        "orl $0x00004000, (%esp)\n"
        "popf\n");
}
