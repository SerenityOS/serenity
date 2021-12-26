/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/QuickSort.h>
#include <AK/TemporaryChange.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Net/Socket.h>
#include <Kernel/Process.h>
#include <Kernel/Profiling.h>
#include <Kernel/RTC.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/TimerQueue.h>

//#define LOG_EVERY_CONTEXT_SWITCH
//#define SCHEDULER_DEBUG
//#define SCHEDULER_RUNNABLE_DEBUG

namespace Kernel {

SchedulerData* g_scheduler_data;
timeval g_timeofday;
RecursiveSpinLock g_scheduler_lock;

void Scheduler::init_thread(Thread& thread)
{
    ASSERT(g_scheduler_data);
    g_scheduler_data->m_nonrunnable_threads.append(thread);
}

void Scheduler::update_state_for_thread(Thread& thread)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(g_scheduler_data);
    auto& list = g_scheduler_data->thread_list_for_state(thread.state());

    if (list.contains(thread))
        return;

    list.append(thread);
}

static u32 time_slice_for(const Thread& thread)
{
    // One time slice unit == 1ms
    if (&thread == Processor::current().idle_thread())
        return 1;
    return 10;
}

timeval Scheduler::time_since_boot()
{
    return { TimeManagement::the().seconds_since_boot(), (suseconds_t)TimeManagement::the().ticks_this_second() * 1000 };
}

Thread* g_finalizer;
WaitQueue* g_finalizer_wait_queue;
Atomic<bool> g_finalizer_has_work { false };
static Process* s_colonel_process;
u64 g_uptime;

Thread::JoinBlocker::JoinBlocker(Thread& joinee, void*& joinee_exit_value)
    : m_joinee(joinee)
    , m_joinee_exit_value(joinee_exit_value)
{
    ASSERT(m_joinee.m_joiner == nullptr);
    auto current_thread = Thread::current();
    m_joinee.m_joiner = current_thread;
    current_thread->m_joinee = &joinee;
}

bool Thread::JoinBlocker::should_unblock(Thread& joiner, time_t, long)
{
    return !joiner.m_joinee;
}

Thread::FileDescriptionBlocker::FileDescriptionBlocker(const FileDescription& description)
    : m_blocked_description(description)
{
}

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

Thread::ConnectBlocker::ConnectBlocker(const FileDescription& description)
    : FileDescriptionBlocker(description)
{
}

bool Thread::ConnectBlocker::should_unblock(Thread&, time_t, long)
{
    auto& socket = *blocked_description().socket();
    return socket.setup_state() == Socket::SetupState::Completed;
}

Thread::WriteBlocker::WriteBlocker(const FileDescription& description)
    : FileDescriptionBlocker(description)
{
    if (description.is_socket()) {
        auto& socket = *description.socket();
        if (socket.has_send_timeout()) {
            timeval deadline = Scheduler::time_since_boot();
            deadline.tv_sec += socket.send_timeout().tv_sec;
            deadline.tv_usec += socket.send_timeout().tv_usec;
            deadline.tv_sec += (socket.send_timeout().tv_usec / 1000000) * 1;
            deadline.tv_usec %= 1000000;
            m_deadline = deadline;
        }
    }
}

bool Thread::WriteBlocker::should_unblock(Thread&, time_t now_sec, long now_usec)
{
    if (m_deadline.has_value()) {
        bool timed_out = now_sec > m_deadline.value().tv_sec || (now_sec == m_deadline.value().tv_sec && now_usec >= m_deadline.value().tv_usec);
        return timed_out || blocked_description().can_write();
    }
    return blocked_description().can_write();
}

Thread::ReadBlocker::ReadBlocker(const FileDescription& description)
    : FileDescriptionBlocker(description)
{
    if (description.is_socket()) {
        auto& socket = *description.socket();
        if (socket.has_receive_timeout()) {
            timeval deadline = Scheduler::time_since_boot();
            deadline.tv_sec += socket.receive_timeout().tv_sec;
            deadline.tv_usec += socket.receive_timeout().tv_usec;
            deadline.tv_sec += (socket.receive_timeout().tv_usec / 1000000) * 1;
            deadline.tv_usec %= 1000000;
            m_deadline = deadline;
        }
    }
}

bool Thread::ReadBlocker::should_unblock(Thread&, time_t now_sec, long now_usec)
{
    if (m_deadline.has_value()) {
        bool timed_out = now_sec > m_deadline.value().tv_sec || (now_sec == m_deadline.value().tv_sec && now_usec >= m_deadline.value().tv_usec);
        return timed_out || blocked_description().can_read();
    }
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

Thread::SelectBlocker::SelectBlocker(const timespec& ts, bool select_has_timeout, const FDVector& read_fds, const FDVector& write_fds, const FDVector& except_fds)
    : m_select_timeout(ts)
    , m_select_has_timeout(select_has_timeout)
    , m_select_read_fds(read_fds)
    , m_select_write_fds(write_fds)
    , m_select_exceptional_fds(except_fds)
{
}

bool Thread::SelectBlocker::should_unblock(Thread& thread, time_t now_sec, long now_usec)
{
    if (m_select_has_timeout) {
        if (now_sec > m_select_timeout.tv_sec || (now_sec == m_select_timeout.tv_sec && now_usec * 1000 >= m_select_timeout.tv_nsec))
            return true;
    }

    auto& process = thread.process();
    for (int fd : m_select_read_fds) {
        if (!process.m_fds[fd])
            continue;
        if (process.m_fds[fd].description->can_read())
            return true;
    }
    for (int fd : m_select_write_fds) {
        if (!process.m_fds[fd])
            continue;
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
    bool should_unblock = m_wait_options & WNOHANG;
    if (m_waitee_pid != -1) {
        auto* peer = Process::from_pid(m_waitee_pid);
        if (!peer)
            return true;
    }
    thread.process().for_each_child([&](Process& child) {
        if (m_waitee_pid != -1 && m_waitee_pid != child.pid())
            return IterationDecision::Continue;

        bool child_exited = child.is_dead();
        bool child_stopped = false;
        if (child.thread_count()) {
            child.for_each_thread([&](auto& child_thread) {
                if (child_thread.state() == Thread::State::Stopped && !child_thread.has_pending_signal(SIGCONT)) {
                    child_stopped = true;
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
        }

        bool fits_the_spec = ((m_wait_options & WEXITED) && child_exited)
            || ((m_wait_options & WSTOPPED) && child_stopped);

        if (!fits_the_spec)
            return IterationDecision::Continue;

        m_waitee_pid = child.pid();
        should_unblock = true;
        return IterationDecision::Break;
    });
    return should_unblock;
}

Thread::SemiPermanentBlocker::SemiPermanentBlocker(Reason reason)
    : m_reason(reason)
{
}

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
    case Thread::Queued:
    case Thread::Dying:
        /* don't know, don't care */
        return;
    case Thread::Blocked:
        ASSERT(m_blocker != nullptr);
        if (m_blocker->should_unblock(*this, now_sec, now_usec))
            unblock();
        return;
    case Thread::Skip1SchedulerPass:
        set_state(Thread::Skip0SchedulerPasses);
        return;
    case Thread::Skip0SchedulerPasses:
        set_state(Thread::Runnable);
        return;
    }
}

void Scheduler::start()
{
    ASSERT_INTERRUPTS_DISABLED();

    // We need to acquire our scheduler lock, which will be released
    // by the idle thread once control transferred there
    g_scheduler_lock.lock();

    auto& processor = Processor::current();
    ASSERT(processor.is_initialized());
    auto& idle_thread = *processor.idle_thread();
    ASSERT(processor.current_thread() == &idle_thread);
    ASSERT(processor.idle_thread() == &idle_thread);
    idle_thread.set_ticks_left(time_slice_for(idle_thread));
    idle_thread.did_schedule();
    idle_thread.set_initialized(true);
    processor.init_context(idle_thread, false);
    idle_thread.set_state(Thread::Running);
    ASSERT(idle_thread.affinity() == (1u << processor.id()));
    processor.initialize_context_switching(idle_thread);
    ASSERT_NOT_REACHED();
}

bool Scheduler::pick_next()
{
    ASSERT_INTERRUPTS_DISABLED();

    auto current_thread = Thread::current();
    auto now = time_since_boot();

    auto now_sec = now.tv_sec;
    auto now_usec = now.tv_usec;

    ScopedSpinLock lock(g_scheduler_lock);

    // Check and unblock threads whose wait conditions have been met.
    Scheduler::for_each_nonrunnable([&](Thread& thread) {
        thread.consider_unblock(now_sec, now_usec);
        return IterationDecision::Continue;
    });

    Process::for_each([&](Process& process) {
        if (process.is_dead()) {
            if (current_thread->process().pid() != process.pid() && (!process.ppid() || !Process::from_pid(process.ppid()))) {
                auto name = process.name();
                auto pid = process.pid();
                auto exit_status = Process::reap(process);
                dbg() << "Scheduler[" << Processor::current().id() << "]: Reaped unparented process " << name << "(" << pid << "), exit status: " << exit_status.si_status;
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
    Thread::for_each_living([&](Thread& thread) -> IterationDecision {
        if (!thread.has_unmasked_pending_signals())
            return IterationDecision::Continue;
        // FIXME: It would be nice if the Scheduler didn't have to worry about who is "current"
        //        For now, avoid dispatching signals to "current" and do it in a scheduling pass
        //        while some other process is interrupted. Otherwise a mess will be made.
        if (&thread == current_thread)
            return IterationDecision::Continue;
        // We know how to interrupt blocked processes, but if they are just executing
        // at some random point in the kernel, let them continue.
        // Before returning to userspace from a syscall, we will block a thread if it has any
        // pending unmasked signals, allowing it to be dispatched then.
        if (thread.in_kernel() && !thread.is_blocked() && !thread.is_stopped())
            return IterationDecision::Continue;
        // NOTE: dispatch_one_pending_signal() may unblock the process.
        bool was_blocked = thread.is_blocked();
        if (thread.dispatch_one_pending_signal() == ShouldUnblockThread::No)
            return IterationDecision::Continue;
        if (was_blocked) {
#ifdef SCHEDULER_DEBUG
            dbg() << "Scheduler[" << Processor::current().id() << "]:Unblock " << thread << " due to signal";
#endif
            ASSERT(thread.m_blocker != nullptr);
            thread.m_blocker->set_interrupted_by_signal();
            thread.unblock();
        }
        return IterationDecision::Continue;
    });

#ifdef SCHEDULER_RUNNABLE_DEBUG
    dbg() << "Non-runnables:";
    Scheduler::for_each_nonrunnable([](Thread& thread) -> IterationDecision {
        if (thread.state() == Thread::Queued)
            dbg() << "  " << String::format("%-12s", thread.state_string()) << " " << thread << " @ " << String::format("%w", thread.tss().cs) << ":" << String::format("%x", thread.tss().eip) << " Reason: " << (thread.wait_reason() ? thread.wait_reason() : "none");
        else if (thread.state() == Thread::Dying)
            dbg() << "  " << String::format("%-12s", thread.state_string()) << " " << thread << " @ " << String::format("%w", thread.tss().cs) << ":" << String::format("%x", thread.tss().eip) << " Finalizable: " << thread.is_finalizable();
        return IterationDecision::Continue;
    });

    dbg() << "Runnables:";
    Scheduler::for_each_runnable([](Thread& thread) -> IterationDecision {
        dbg() << "  " << String::format("%3u", thread.effective_priority()) << "/" << String::format("%2u", thread.priority()) << " " << String::format("%-12s", thread.state_string()) << " " << thread << " @ " << String::format("%w", thread.tss().cs) << ":" << String::format("%x", thread.tss().eip);
        return IterationDecision::Continue;
    });
#endif

    Vector<Thread*, 128> sorted_runnables;
    for_each_runnable([&sorted_runnables](auto& thread) {
        if ((thread.affinity() & (1u << Processor::current().id())) != 0)
            sorted_runnables.append(&thread);
        return IterationDecision::Continue;
    });
    quick_sort(sorted_runnables, [](auto& a, auto& b) { return a->effective_priority() >= b->effective_priority(); });

    Thread* thread_to_schedule = nullptr;

    for (auto* thread : sorted_runnables) {
        if (thread->process().is_being_inspected())
            continue;

        if (thread->process().exec_tid() && thread->process().exec_tid() != thread->tid())
            continue;

        ASSERT(thread->state() == Thread::Runnable || thread->state() == Thread::Running);

        if (!thread_to_schedule) {
            thread->m_extra_priority = 0;
            thread_to_schedule = thread;
        } else {
            thread->m_extra_priority++;
        }
    }

    if (!thread_to_schedule)
        thread_to_schedule = Processor::current().idle_thread();

#ifdef SCHEDULER_DEBUG
    dbg() << "Scheduler[" << Processor::current().id() << "]: Switch to " << *thread_to_schedule << " @ " << String::format("%04x:%08x", thread_to_schedule->tss().cs, thread_to_schedule->tss().eip);
#endif

    return context_switch(thread_to_schedule);
}

bool Scheduler::yield()
{
    InterruptDisabler disabler;
    auto& proc = Processor::current();
    auto current_thread = Thread::current();
#ifdef SCHEDULER_DEBUG
    dbg() << "Scheduler[" << proc.id() << "]: yielding thread " << *current_thread << " in_irq: " << proc.in_irq();
#endif
    ASSERT(current_thread != nullptr);
    if (proc.in_irq() || proc.in_critical()) {
        // If we're handling an IRQ we can't switch context, or we're in
        // a critical section where we don't want to switch contexts, then
        // delay until exiting the trap or critical section
        proc.invoke_scheduler_async();
        return false;
    } else if (!Scheduler::pick_next())
        return false;
#ifdef SCHEDULER_DEBUG
    dbg() << "Scheduler[" << proc.id() << "]: yield returns to thread " << *current_thread << " in_irq: " << proc.in_irq();
#endif
    return true;
}

bool Scheduler::donate_to(Thread* beneficiary, const char* reason)
{
    ScopedSpinLock lock(g_scheduler_lock);
    auto& proc = Processor::current();
    ASSERT(!proc.in_irq());
    if (!Thread::is_thread(beneficiary))
        return false;

    if (proc.in_critical()) {
        proc.invoke_scheduler_async();
        return false;
    }

    (void)reason;
    unsigned ticks_left = Thread::current()->ticks_left();
    if (!beneficiary || beneficiary->state() != Thread::Runnable || ticks_left <= 1)
        return Scheduler::yield();

    unsigned ticks_to_donate = min(ticks_left - 1, time_slice_for(*beneficiary));
#ifdef SCHEDULER_DEBUG
    dbg() << "Scheduler[" << proc.id() << "]: Donating " << ticks_to_donate << " ticks to " << *beneficiary << ", reason=" << reason;
#endif
    beneficiary->set_ticks_left(ticks_to_donate);
    Scheduler::context_switch(beneficiary);
    return false;
}

bool Scheduler::context_switch(Thread* thread)
{
    thread->set_ticks_left(time_slice_for(*thread));
    thread->did_schedule();

    auto from_thread = Thread::current();
    if (from_thread == thread)
        return false;

    if (from_thread) {
        // If the last process hasn't blocked (still marked as running),
        // mark it as runnable for the next round.
        if (from_thread->state() == Thread::Running)
            from_thread->set_state(Thread::Runnable);

#ifdef LOG_EVERY_CONTEXT_SWITCH
        dbg() << "Scheduler[" << Processor::current().id() << "]: " << *from_thread << " -> " << *thread << " [" << thread->priority() << "] " << String::format("%w", thread->tss().cs) << ":" << String::format("%x", thread->tss().eip);
#endif
    }

    auto& proc = Processor::current();
    if (!thread->is_initialized()) {
        proc.init_context(*thread, false);
        thread->set_initialized(true);
    }
    thread->set_state(Thread::Running);

    // Mark it as active because we are using this thread. This is similar
    // to comparing it with Processor::current_thread, but when there are
    // multiple processors there's no easy way to check whether the thread
    // is actually still needed. This prevents accidental finalization when
    // a thread is no longer in Running state, but running on another core.
    thread->set_active(true);

    proc.switch_context(from_thread, thread);

    // NOTE: from_thread at this point reflects the thread we were
    // switched from, and thread reflects Thread::current()
    enter_current(*from_thread);
    ASSERT(thread == Thread::current());

    return true;
}

void Scheduler::enter_current(Thread& prev_thread)
{
    ASSERT(g_scheduler_lock.is_locked());
    prev_thread.set_active(false);
    if (prev_thread.state() == Thread::Dying) {
        // If the thread we switched from is marked as dying, then notify
        // the finalizer. Note that as soon as we leave the scheduler lock
        // the finalizer may free from_thread!
        notify_finalizer();
    }
}

Process* Scheduler::colonel()
{
    ASSERT(s_colonel_process);
    return s_colonel_process;
}

void Scheduler::initialize()
{
    ASSERT(&Processor::current() != nullptr); // sanity check

    Thread* idle_thread = nullptr;
    g_scheduler_data = new SchedulerData;
    g_finalizer_wait_queue = new WaitQueue;

    g_finalizer_has_work.store(false, AK::MemoryOrder::memory_order_release);
    s_colonel_process = Process::create_kernel_process(idle_thread, "colonel", idle_loop, 1);
    ASSERT(s_colonel_process);
    ASSERT(idle_thread);
    idle_thread->set_priority(THREAD_PRIORITY_MIN);
    idle_thread->set_name("idle thread #0");

    set_idle_thread(idle_thread);
}

void Scheduler::set_idle_thread(Thread* idle_thread)
{
    Processor::current().set_idle_thread(*idle_thread);
    Processor::current().set_current_thread(*idle_thread);
}

Thread* Scheduler::create_ap_idle_thread(u32 cpu)
{
    ASSERT(cpu != 0);
    // This function is called on the bsp, but creates an idle thread for another AP
    ASSERT(Processor::current().id() == 0);

    ASSERT(s_colonel_process);
    Thread* idle_thread = s_colonel_process->create_kernel_thread(idle_loop, THREAD_PRIORITY_MIN, String::format("idle thread #%u", cpu), 1 << cpu, false);
    ASSERT(idle_thread);
    return idle_thread;
}

void Scheduler::timer_tick(const RegisterState& regs)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(Processor::current().in_irq());
    if (Processor::current().id() > 0)
        return;
    auto current_thread = Processor::current().current_thread();
    if (!current_thread)
        return;

    ++g_uptime;

    g_timeofday = TimeManagement::now_as_timeval();

    if (current_thread->process().is_profiling()) {
        SmapDisabler disabler;
        auto backtrace = current_thread->raw_backtrace(regs.ebp, regs.eip);
        auto& sample = Profiling::next_sample_slot();
        sample.pid = current_thread->process().pid();
        sample.tid = current_thread->tid();
        sample.timestamp = g_uptime;
        for (size_t i = 0; i < min(backtrace.size(), Profiling::max_stack_frame_count); ++i) {
            sample.frames[i] = backtrace[i];
        }
    }

    TimerQueue::the().fire();

    if (current_thread->tick())
        return;

    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(Processor::current().in_irq());
    Processor::current().invoke_scheduler_async();
}

void Scheduler::invoke_async()
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(!Processor::current().in_irq());
    pick_next();
}

void Scheduler::notify_finalizer()
{
    if (g_finalizer_has_work.exchange(true, AK::MemoryOrder::memory_order_acq_rel) == false)
        g_finalizer_wait_queue->wake_all();
}

void Scheduler::idle_loop()
{
    dbg() << "Scheduler[" << Processor::current().id() << "]: idle loop running";
    ASSERT(are_interrupts_enabled());

    for (;;) {
        asm("hlt");

        if (Processor::current().id() == 0)
            yield();
    }
}

}
