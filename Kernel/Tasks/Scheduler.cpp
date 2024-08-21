/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <AK/ScopeGuard.h>
#include <AK/Singleton.h>
#include <AK/Time.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Debug.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/kstdio.h>

namespace Kernel {

RecursiveSpinlock<LockRank::None> g_scheduler_lock {};

static u32 time_slice_for(Thread const& thread)
{
    // One time slice unit == 4ms (assuming 250 ticks/second)
    if (thread.is_idle_thread())
        return 1;
    return 2;
}

READONLY_AFTER_INIT Thread* g_finalizer;
READONLY_AFTER_INIT WaitQueue* g_finalizer_wait_queue;
Atomic<bool> g_finalizer_has_work { false };
READONLY_AFTER_INIT static Process* s_colonel_process;

struct ThreadReadyQueue {
    IntrusiveList<&Thread::m_ready_queue_node> thread_list;
};

struct ThreadReadyQueues {
    u32 mask {};
    static constexpr size_t count = sizeof(mask) * 8;
    Array<ThreadReadyQueue, count> queues;
};

static Singleton<SpinlockProtected<ThreadReadyQueues, LockRank::None>> g_ready_queues;

static SpinlockProtected<TotalTimeScheduled, LockRank::None> g_total_time_scheduled {};

static void dump_thread_list(bool = false);

static inline u32 thread_priority_to_priority_index(u32 thread_priority)
{
    // Converts the priority in the range of THREAD_PRIORITY_MIN...THREAD_PRIORITY_MAX
    // to a index into g_ready_queues where 0 is the highest priority bucket
    VERIFY(thread_priority >= THREAD_PRIORITY_MIN && thread_priority <= THREAD_PRIORITY_MAX);
    constexpr u32 thread_priority_count = THREAD_PRIORITY_MAX - THREAD_PRIORITY_MIN + 1;
    static_assert(thread_priority_count > 0);
    auto priority_bucket = ((thread_priority_count - (thread_priority - THREAD_PRIORITY_MIN)) / thread_priority_count) * (ThreadReadyQueues::count - 1);
    VERIFY(priority_bucket < ThreadReadyQueues::count);
    return priority_bucket;
}

Thread& Scheduler::pull_next_runnable_thread()
{
    auto affinity_mask = 1u << Processor::current_id();

    return g_ready_queues->with([&](auto& ready_queues) -> Thread& {
        auto priority_mask = ready_queues.mask;
        while (priority_mask != 0) {
            auto priority = bit_scan_forward(priority_mask);
            VERIFY(priority > 0);
            auto& ready_queue = ready_queues.queues[--priority];
            for (auto& thread : ready_queue.thread_list) {
                VERIFY(thread.m_runnable_priority == (int)priority);
                if (thread.is_active())
                    continue;
                if (!(thread.affinity() & affinity_mask))
                    continue;
                thread.m_runnable_priority = -1;
                ready_queue.thread_list.remove(thread);
                if (ready_queue.thread_list.is_empty())
                    ready_queues.mask &= ~(1u << priority);
                // Mark it as active because we are using this thread. This is similar
                // to comparing it with Processor::current_thread, but when there are
                // multiple processors there's no easy way to check whether the thread
                // is actually still needed. This prevents accidental finalization when
                // a thread is no longer in Running state, but running on another core.

                // We need to mark it active here so that this thread won't be
                // scheduled on another core if it were to be queued before actually
                // switching to it.
                // FIXME: Figure out a better way maybe?
                thread.set_active(true);
                return thread;
            }
            priority_mask &= ~(1u << priority);
        }

        auto* idle_thread = Processor::idle_thread();
        idle_thread->set_active(true);
        return *idle_thread;
    });
}

Thread* Scheduler::peek_next_runnable_thread()
{
    auto affinity_mask = 1u << Processor::current_id();

    return g_ready_queues->with([&](auto& ready_queues) -> Thread* {
        auto priority_mask = ready_queues.mask;
        while (priority_mask != 0) {
            auto priority = bit_scan_forward(priority_mask);
            VERIFY(priority > 0);
            auto& ready_queue = ready_queues.queues[--priority];
            for (auto& thread : ready_queue.thread_list) {
                VERIFY(thread.m_runnable_priority == (int)priority);
                if (thread.is_active())
                    continue;
                if (!(thread.affinity() & affinity_mask))
                    continue;
                return &thread;
            }
            priority_mask &= ~(1u << priority);
        }

        // Unlike in pull_next_runnable_thread() we don't want to fall back to
        // the idle thread. We just want to see if we have any other thread ready
        // to be scheduled.
        return nullptr;
    });
}

bool Scheduler::dequeue_runnable_thread(Thread& thread, bool check_affinity)
{
    if (thread.is_idle_thread())
        return true;

    return g_ready_queues->with([&](auto& ready_queues) {
        auto priority = thread.m_runnable_priority;
        if (priority < 0) {
            VERIFY(!thread.m_ready_queue_node.is_in_list());
            return false;
        }

        if (check_affinity && !(thread.affinity() & (1 << Processor::current_id())))
            return false;

        VERIFY(ready_queues.mask & (1u << priority));
        auto& ready_queue = ready_queues.queues[priority];
        thread.m_runnable_priority = -1;
        ready_queue.thread_list.remove(thread);
        if (ready_queue.thread_list.is_empty())
            ready_queues.mask &= ~(1u << priority);
        return true;
    });
}

void Scheduler::enqueue_runnable_thread(Thread& thread)
{
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());
    if (thread.is_idle_thread())
        return;
    auto priority = thread_priority_to_priority_index(thread.priority());

    g_ready_queues->with([&](auto& ready_queues) {
        VERIFY(thread.m_runnable_priority < 0);
        thread.m_runnable_priority = (int)priority;
        VERIFY(!thread.m_ready_queue_node.is_in_list());
        auto& ready_queue = ready_queues.queues[priority];
        bool was_empty = ready_queue.thread_list.is_empty();
        ready_queue.thread_list.append(thread);
        if (was_empty)
            ready_queues.mask |= (1u << priority);
    });
}

UNMAP_AFTER_INIT void Scheduler::start()
{
    VERIFY_INTERRUPTS_DISABLED();

    // We need to acquire our scheduler lock, which will be released
    // by the idle thread once control transferred there
    g_scheduler_lock.lock();

    auto& processor = Processor::current();
    VERIFY(processor.is_initialized());
    auto& idle_thread = *Processor::idle_thread();
    VERIFY(processor.current_thread() == &idle_thread);
    idle_thread.set_ticks_left(time_slice_for(idle_thread));
    idle_thread.did_schedule();
    idle_thread.set_initialized(true);
    processor.init_context(idle_thread, false);
    idle_thread.set_state(Thread::State::Running);
    VERIFY(idle_thread.affinity() == (1u << processor.id()));
    processor.initialize_context_switching(idle_thread);
    VERIFY_NOT_REACHED();
}

void Scheduler::pick_next()
{
    VERIFY_INTERRUPTS_DISABLED();

    // Set the in_scheduler flag before acquiring the spinlock. This
    // prevents a recursive call into Scheduler::invoke_async upon
    // leaving the scheduler lock.
    ScopedCritical critical;
    Processor::set_current_in_scheduler(true);
    ScopeGuard guard(
        []() {
            // We may be on a different processor after we got switched
            // back to this thread!
            VERIFY(Processor::current_in_scheduler());
            Processor::set_current_in_scheduler(false);
        });

    SpinlockLocker lock(g_scheduler_lock);

    if constexpr (SCHEDULER_RUNNABLE_DEBUG) {
        dump_thread_list();
    }

    auto& thread_to_schedule = pull_next_runnable_thread();
    if constexpr (SCHEDULER_DEBUG) {
        dbgln("Scheduler[{}]: Switch to {} @ {:p}",
            Processor::current_id(),
            thread_to_schedule,
            thread_to_schedule.regs().ip());
    }

    // We need to leave our first critical section before switching context,
    // but since we're still holding the scheduler lock we're still in a critical section
    critical.leave();

    thread_to_schedule.set_ticks_left(time_slice_for(thread_to_schedule));
    context_switch(&thread_to_schedule);
}

void Scheduler::yield()
{
    InterruptDisabler disabler;

    auto const* current_thread = Thread::current();
    dbgln_if(SCHEDULER_DEBUG, "Scheduler[{}]: yielding thread {} in_irq={}", Processor::current_id(), *current_thread, Processor::current_in_irq());
    VERIFY(current_thread != nullptr);
    if (Processor::current_in_irq() || Processor::in_critical()) {
        // If we're handling an IRQ we can't switch context, or we're in
        // a critical section where we don't want to switch contexts, then
        // delay until exiting the trap or critical section
        Processor::current().invoke_scheduler_async();
        return;
    }

    Scheduler::pick_next();
}

void Scheduler::context_switch(Thread* thread)
{
    thread->did_schedule();

    auto* from_thread = Thread::current();
    VERIFY(from_thread);

    if (from_thread == thread)
        return;

    // If the last process hasn't blocked (still marked as running),
    // mark it as runnable for the next round, unless it's supposed
    // to be stopped, in which case just mark it as such.
    if (from_thread->state() == Thread::State::Running) {
        if (from_thread->should_be_stopped())
            from_thread->set_state(Thread::State::Stopped);
        else
            from_thread->set_state(Thread::State::Runnable);
    }

#ifdef LOG_EVERY_CONTEXT_SWITCH
    auto const msg = "Scheduler[{}]: {} -> {} [prio={}] {:p}";

    dbgln(msg,
        Processor::current_id(), from_thread->tid().value(),
        thread->tid().value(), thread->priority(), thread->regs().ip());
#endif

    auto& proc = Processor::current();
    if (!thread->is_initialized()) {
        proc.init_context(*thread, false);
        thread->set_initialized(true);
    }
    thread->set_state(Thread::State::Running);

    PerformanceManager::add_context_switch_perf_event(*from_thread, *thread);

    proc.switch_context(from_thread, thread);

    // NOTE: from_thread at this point reflects the thread we were
    // switched from, and thread reflects Thread::current()
    enter_current(*from_thread);
    VERIFY(thread == Thread::current());

    {
        SpinlockLocker lock(thread->get_lock());
        thread->dispatch_one_pending_signal();
    }
}

void Scheduler::enter_current(Thread& prev_thread)
{
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());

    // We already recorded the scheduled time when entering the trap, so this merely accounts for the kernel time since then
    auto scheduler_time = TimeManagement::scheduler_current_time();
    prev_thread.update_time_scheduled(scheduler_time, true, true);
    auto* current_thread = Thread::current();
    current_thread->update_time_scheduled(scheduler_time, true, false);

    // NOTE: When doing an exec(), we will context switch from and to the same thread!
    //       In that case, we must not mark the previous thread as inactive.
    if (&prev_thread != current_thread)
        prev_thread.set_active(false);

    if (prev_thread.state() == Thread::State::Dying) {
        // If the thread we switched from is marked as dying, then notify
        // the finalizer. Note that as soon as we leave the scheduler lock
        // the finalizer may free from_thread!
        notify_finalizer();
    }
}

void Scheduler::leave_on_first_switch(InterruptsState previous_interrupts_state)
{
    // This is called when a thread is switched into for the first time.
    // At this point, enter_current has already be called, but because
    // Scheduler::context_switch is not in the call stack we need to
    // clean up and release locks manually here
    g_scheduler_lock.unlock(previous_interrupts_state);

    VERIFY(Processor::current_in_scheduler());
    Processor::set_current_in_scheduler(false);
}

void Scheduler::prepare_after_exec()
{
    // This is called after exec() when doing a context "switch" into
    // the new process. This is called from Processor::assume_context
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());

    VERIFY(!Processor::current_in_scheduler());
    Processor::set_current_in_scheduler(true);
}

void Scheduler::prepare_for_idle_loop()
{
    // This is called when the CPU finished setting up the idle loop
    // and is about to run it. We need to acquire the scheduler lock
    VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
    g_scheduler_lock.lock();

    VERIFY(!Processor::current_in_scheduler());
    Processor::set_current_in_scheduler(true);
}

Process* Scheduler::colonel()
{
    VERIFY(s_colonel_process);
    return s_colonel_process;
}

UNMAP_AFTER_INIT void Scheduler::initialize()
{
    VERIFY(Processor::is_initialized()); // sanity check
    VERIFY(TimeManagement::is_initialized());

    g_finalizer_wait_queue = new WaitQueue;

    g_finalizer_has_work.store(false, AK::MemoryOrder::memory_order_release);
    auto [colonel_process, idle_thread] = MUST(Process::create_kernel_process("colonel"sv, idle_loop, nullptr, 1, Process::RegisterProcess::No));
    s_colonel_process = &colonel_process.leak_ref();
    idle_thread->set_priority(THREAD_PRIORITY_MIN);
    idle_thread->set_name("Idle Task #0"sv);

    set_idle_thread(idle_thread);
}

UNMAP_AFTER_INIT void Scheduler::set_idle_thread(Thread* idle_thread)
{
    idle_thread->set_idle_thread();
    Processor::current().set_idle_thread(*idle_thread);
    Processor::set_current_thread(*idle_thread);
}

UNMAP_AFTER_INIT Thread* Scheduler::create_ap_idle_thread(u32 cpu)
{
    VERIFY(cpu != 0);
    // This function is called on the bsp, but creates an idle thread for another AP
    VERIFY(Processor::is_bootstrap_processor());

    VERIFY(s_colonel_process);
    Thread* idle_thread = MUST(s_colonel_process->create_kernel_thread(idle_loop, nullptr, THREAD_PRIORITY_MIN, MUST(KString::formatted("idle thread #{}", cpu))->view(), 1 << cpu, false));
    VERIFY(idle_thread);
    return idle_thread;
}

void Scheduler::add_time_scheduled(u64 time_to_add, bool is_kernel)
{
    g_total_time_scheduled.with([&](auto& total_time_scheduled) {
        total_time_scheduled.total += time_to_add;
        if (is_kernel)
            total_time_scheduled.total_kernel += time_to_add;
    });
}

void Scheduler::timer_tick()
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(Processor::current_in_irq());

    auto* current_thread = Processor::current_thread();
    if (!current_thread)
        return;

    // Sanity checks
    VERIFY(current_thread->current_trap());

    if (current_thread->process().is_kernel_process()) {
        // Because the previous mode when entering/exiting kernel threads never changes
        // we never update the time scheduled. So we need to update it manually on the
        // timer interrupt
        current_thread->update_time_scheduled(TimeManagement::scheduler_current_time(), true, false);
    }

    if (current_thread->previous_mode() == ExecutionMode::User && current_thread->should_die() && !current_thread->is_blocked()) {
        SpinlockLocker scheduler_lock(g_scheduler_lock);
        dbgln_if(SCHEDULER_DEBUG, "Scheduler[{}]: Terminating user mode thread {}", Processor::current_id(), *current_thread);
        current_thread->set_state(Thread::State::Dying);
        Processor::current().invoke_scheduler_async();
        return;
    }

    if (current_thread->tick())
        return;

    if (!current_thread->is_idle_thread() && !peek_next_runnable_thread()) {
        // If no other thread is ready to be scheduled we don't need to
        // switch to the idle thread. Just give the current thread another
        // time slice and let it run!
        current_thread->set_ticks_left(time_slice_for(*current_thread));
        current_thread->did_schedule();
        dbgln_if(SCHEDULER_DEBUG, "Scheduler[{}]: No other threads ready, give {} another timeslice", Processor::current_id(), *current_thread);
        return;
    }

    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(Processor::current_in_irq());
    Processor::current().invoke_scheduler_async();
}

void Scheduler::invoke_async()
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(!Processor::current_in_irq());

    // Since this function is called when leaving critical sections (such
    // as a Spinlock), we need to check if we're not already doing this
    // to prevent recursion
    if (!Processor::current_in_scheduler())
        pick_next();
}

void Scheduler::notify_finalizer()
{
    if (!g_finalizer_has_work.exchange(true, AK::MemoryOrder::memory_order_acq_rel))
        g_finalizer_wait_queue->wake_all();
}

void Scheduler::idle_loop(void*)
{
    auto& proc = Processor::current();
    dbgln("Scheduler[{}]: idle loop running", proc.id());
    VERIFY(Processor::are_interrupts_enabled());

    for (;;) {
        proc.idle_begin();
        proc.wait_for_interrupt();
        proc.idle_end();
        VERIFY_INTERRUPTS_ENABLED();
        yield();
    }
}

void Scheduler::dump_scheduler_state(bool with_stack_traces)
{
    dump_thread_list(with_stack_traces);
}

bool Scheduler::is_initialized()
{
    // The scheduler is initialized iff the idle thread exists
    return Processor::idle_thread() != nullptr;
}

TotalTimeScheduled Scheduler::get_total_time_scheduled()
{
    return g_total_time_scheduled.with([&](auto& total_time_scheduled) { return total_time_scheduled; });
}

void dump_thread_list(bool with_stack_traces)
{
    dbgln("Scheduler thread list for processor {}:", Processor::current_id());

    auto get_pc = [](Thread& thread) -> FlatPtr {
        if (!thread.current_trap())
            return thread.regs().ip();
        return thread.get_register_dump_from_stack().ip();
    };

    Thread::for_each_ignoring_process_lists([&](Thread& thread) {
        auto color = thread.process().is_kernel_process() ? "\x1b[34;1m"sv : "\x1b[33;1m"sv;
        switch (thread.state()) {
        case Thread::State::Dying:
            dmesgln("  {}{:30}\x1b[0m @ {:08x} is {:14} (Finalizable: {}, nsched: {})",
                color,
                thread,
                get_pc(thread),
                thread.state_string(),
                thread.is_finalizable(),
                thread.times_scheduled());
            break;
        default:
            dmesgln("  {}{:30}\x1b[0m @ {:08x} is {:14} (Pr:{:2}, nsched: {})",
                color,
                thread,
                get_pc(thread),
                thread.state_string(),
                thread.priority(),
                thread.times_scheduled());
            break;
        }
        if (thread.state() == Thread::State::Blocked && thread.blocking_mutex()) {
            dmesgln("    Blocking on Mutex {:#x} ({})", thread.blocking_mutex(), thread.blocking_mutex()->name());
        }
        if (thread.state() == Thread::State::Blocked && thread.blocker()) {
            dmesgln("    Blocking on Blocker {:#x}", thread.blocker());
        }
#if LOCK_DEBUG
        thread.for_each_held_lock([](auto const& entry) {
            dmesgln("    Holding lock {:#x} ({}) at {}", entry.lock, entry.lock->name(), entry.lock_location);
        });
#endif
        if (with_stack_traces) {
            thread.print_backtrace();
        }
        return IterationDecision::Continue;
    });
}

}
