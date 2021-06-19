/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/TemporaryChange.h>
#include <AK/Time.h>
#include <Kernel/Debug.h>
#include <Kernel/Panic.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/RTC.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Time/TimeManagement.h>
#include <Kernel/TimerQueue.h>

// Remove this once SMP is stable and can be enabled by default
#define SCHEDULE_ON_ALL_PROCESSORS 0

namespace Kernel {

class SchedulerPerProcessorData {
    AK_MAKE_NONCOPYABLE(SchedulerPerProcessorData);
    AK_MAKE_NONMOVABLE(SchedulerPerProcessorData);

public:
    SchedulerPerProcessorData() = default;

    WeakPtr<Thread> m_pending_beneficiary;
    const char* m_pending_donate_reason { nullptr };
    bool m_in_scheduler { true };
};

RecursiveSpinLock g_scheduler_lock;

static u32 time_slice_for(const Thread& thread)
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
    IntrusiveList<Thread, RawPtr<Thread>, &Thread::m_ready_queue_node> thread_list;
};
static SpinLock<u8> g_ready_queues_lock;
static u32 g_ready_queues_mask;
static constexpr u32 g_ready_queue_buckets = sizeof(g_ready_queues_mask) * 8;
READONLY_AFTER_INIT static ThreadReadyQueue* g_ready_queues; // g_ready_queue_buckets entries
static void dump_thread_list();

static inline u32 thread_priority_to_priority_index(u32 thread_priority)
{
    // Converts the priority in the range of THREAD_PRIORITY_MIN...THREAD_PRIORITY_MAX
    // to a index into g_ready_queues where 0 is the highest priority bucket
    VERIFY(thread_priority >= THREAD_PRIORITY_MIN && thread_priority <= THREAD_PRIORITY_MAX);
    constexpr u32 thread_priority_count = THREAD_PRIORITY_MAX - THREAD_PRIORITY_MIN + 1;
    static_assert(thread_priority_count > 0);
    auto priority_bucket = ((thread_priority_count - (thread_priority - THREAD_PRIORITY_MIN)) / thread_priority_count) * (g_ready_queue_buckets - 1);
    VERIFY(priority_bucket < g_ready_queue_buckets);
    return priority_bucket;
}

Thread& Scheduler::pull_next_runnable_thread()
{
    auto affinity_mask = 1u << Processor::current().id();

    ScopedSpinLock lock(g_ready_queues_lock);
    auto priority_mask = g_ready_queues_mask;
    while (priority_mask != 0) {
        auto priority = __builtin_ffsl(priority_mask);
        VERIFY(priority > 0);
        auto& ready_queue = g_ready_queues[--priority];
        for (auto& thread : ready_queue.thread_list) {
            VERIFY(thread.m_runnable_priority == (int)priority);
            if (thread.is_active())
                continue;
            if (!(thread.affinity() & affinity_mask))
                continue;
            thread.m_runnable_priority = -1;
            ready_queue.thread_list.remove(thread);
            if (ready_queue.thread_list.is_empty())
                g_ready_queues_mask &= ~(1u << priority);
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
    return *Processor::idle_thread();
}

bool Scheduler::dequeue_runnable_thread(Thread& thread, bool check_affinity)
{
    if (thread.is_idle_thread())
        return true;
    ScopedSpinLock lock(g_ready_queues_lock);
    auto priority = thread.m_runnable_priority;
    if (priority < 0) {
        VERIFY(!thread.m_ready_queue_node.is_in_list());
        return false;
    }

    if (check_affinity && !(thread.affinity() & (1 << Processor::current().id())))
        return false;

    VERIFY(g_ready_queues_mask & (1u << priority));
    auto& ready_queue = g_ready_queues[priority];
    thread.m_runnable_priority = -1;
    ready_queue.thread_list.remove(thread);
    if (ready_queue.thread_list.is_empty())
        g_ready_queues_mask &= ~(1u << priority);
    return true;
}

void Scheduler::queue_runnable_thread(Thread& thread)
{
    VERIFY(g_scheduler_lock.own_lock());
    if (thread.is_idle_thread())
        return;
    auto priority = thread_priority_to_priority_index(thread.priority());

    ScopedSpinLock lock(g_ready_queues_lock);
    VERIFY(thread.m_runnable_priority < 0);
    thread.m_runnable_priority = (int)priority;
    VERIFY(!thread.m_ready_queue_node.is_in_list());
    auto& ready_queue = g_ready_queues[priority];
    bool was_empty = ready_queue.thread_list.is_empty();
    ready_queue.thread_list.append(thread);
    if (was_empty)
        g_ready_queues_mask |= (1u << priority);
}

UNMAP_AFTER_INIT void Scheduler::start()
{
    VERIFY_INTERRUPTS_DISABLED();

    // We need to acquire our scheduler lock, which will be released
    // by the idle thread once control transferred there
    g_scheduler_lock.lock();

    auto& processor = Processor::current();
    processor.set_scheduler_data(*new SchedulerPerProcessorData());
    VERIFY(processor.is_initialized());
    auto& idle_thread = *Processor::idle_thread();
    VERIFY(processor.current_thread() == &idle_thread);
    idle_thread.set_ticks_left(time_slice_for(idle_thread));
    idle_thread.did_schedule();
    idle_thread.set_initialized(true);
    processor.init_context(idle_thread, false);
    idle_thread.set_state(Thread::Running);
    VERIFY(idle_thread.affinity() == (1u << processor.get_id()));
    processor.initialize_context_switching(idle_thread);
    VERIFY_NOT_REACHED();
}

bool Scheduler::pick_next()
{
    VERIFY_INTERRUPTS_DISABLED();

    // Set the m_in_scheduler flag before acquiring the spinlock. This
    // prevents a recursive call into Scheduler::invoke_async upon
    // leaving the scheduler lock.
    ScopedCritical critical;
    auto& scheduler_data = Processor::current().get_scheduler_data();
    scheduler_data.m_in_scheduler = true;
    ScopeGuard guard(
        []() {
            // We may be on a different processor after we got switched
            // back to this thread!
            auto& scheduler_data = Processor::current().get_scheduler_data();
            VERIFY(scheduler_data.m_in_scheduler);
            scheduler_data.m_in_scheduler = false;
        });

    ScopedSpinLock lock(g_scheduler_lock);

    auto current_thread = Thread::current();
    if (current_thread->should_die() && current_thread->may_die_immediately()) {
        // Ordinarily the thread would die on syscall exit, however if the thread
        // doesn't perform any syscalls we still need to mark it for termination here.
        current_thread->set_state(Thread::Dying);
    }

    if constexpr (SCHEDULER_RUNNABLE_DEBUG) {
        dump_thread_list();
    }

    auto pending_beneficiary = scheduler_data.m_pending_beneficiary.strong_ref();
    if (pending_beneficiary && dequeue_runnable_thread(*pending_beneficiary, true)) {
        // The thread we're supposed to donate to still exists and we can
        const char* reason = scheduler_data.m_pending_donate_reason;
        scheduler_data.m_pending_beneficiary = nullptr;
        scheduler_data.m_pending_donate_reason = nullptr;

        // We need to leave our first critical section before switching context,
        // but since we're still holding the scheduler lock we're still in a critical section
        critical.leave();

        dbgln_if(SCHEDULER_DEBUG, "Processing pending donate to {} reason={}", *pending_beneficiary, reason);
        return donate_to_and_switch(pending_beneficiary.ptr(), reason);
    }

    // Either we're not donating or the beneficiary disappeared.
    // Either way clear any pending information
    scheduler_data.m_pending_beneficiary = nullptr;
    scheduler_data.m_pending_donate_reason = nullptr;

    auto& thread_to_schedule = pull_next_runnable_thread();
    if constexpr (SCHEDULER_DEBUG) {
        dbgln("Scheduler[{}]: Switch to {} @ {:04x}:{:08x}",
            Processor::id(),
            thread_to_schedule,
            thread_to_schedule.tss().cs, thread_to_schedule.tss().eip);
    }

    // We need to leave our first critical section before switching context,
    // but since we're still holding the scheduler lock we're still in a critical section
    critical.leave();

    thread_to_schedule.set_ticks_left(time_slice_for(thread_to_schedule));
    return context_switch(&thread_to_schedule);
}

bool Scheduler::yield()
{
    InterruptDisabler disabler;
    auto& proc = Processor::current();
    auto& scheduler_data = proc.get_scheduler_data();

    // Clear any pending beneficiary
    scheduler_data.m_pending_beneficiary = nullptr;
    scheduler_data.m_pending_donate_reason = nullptr;

    auto current_thread = Thread::current();
    dbgln_if(SCHEDULER_DEBUG, "Scheduler[{}]: yielding thread {} in_irq={}", proc.get_id(), *current_thread, proc.in_irq());
    VERIFY(current_thread != nullptr);
    if (proc.in_irq() || proc.in_critical()) {
        // If we're handling an IRQ we can't switch context, or we're in
        // a critical section where we don't want to switch contexts, then
        // delay until exiting the trap or critical section
        proc.invoke_scheduler_async();
        return false;
    }

    if (!Scheduler::pick_next())
        return false;

    if constexpr (SCHEDULER_DEBUG)
        dbgln("Scheduler[{}]: yield returns to thread {} in_irq={}", Processor::id(), *current_thread, Processor::current().in_irq());
    return true;
}

bool Scheduler::donate_to_and_switch(Thread* beneficiary, [[maybe_unused]] const char* reason)
{
    VERIFY(g_scheduler_lock.own_lock());

    auto& proc = Processor::current();
    VERIFY(proc.in_critical() == 1);

    unsigned ticks_left = Thread::current()->ticks_left();
    if (!beneficiary || beneficiary->state() != Thread::Runnable || ticks_left <= 1)
        return Scheduler::yield();

    unsigned ticks_to_donate = min(ticks_left - 1, time_slice_for(*beneficiary));
    dbgln_if(SCHEDULER_DEBUG, "Scheduler[{}]: Donating {} ticks to {}, reason={}", proc.get_id(), ticks_to_donate, *beneficiary, reason);
    beneficiary->set_ticks_left(ticks_to_donate);

    return Scheduler::context_switch(beneficiary);
}

bool Scheduler::donate_to(RefPtr<Thread>& beneficiary, const char* reason)
{
    VERIFY(beneficiary);

    if (beneficiary == Thread::current())
        return Scheduler::yield();

    // Set the m_in_scheduler flag before acquiring the spinlock. This
    // prevents a recursive call into Scheduler::invoke_async upon
    // leaving the scheduler lock.
    ScopedCritical critical;
    auto& proc = Processor::current();
    auto& scheduler_data = proc.get_scheduler_data();
    scheduler_data.m_in_scheduler = true;
    ScopeGuard guard(
        []() {
            // We may be on a different processor after we got switched
            // back to this thread!
            auto& scheduler_data = Processor::current().get_scheduler_data();
            VERIFY(scheduler_data.m_in_scheduler);
            scheduler_data.m_in_scheduler = false;
        });

    VERIFY(!proc.in_irq());

    if (proc.in_critical() > 1) {
        scheduler_data.m_pending_beneficiary = beneficiary; // Save the beneficiary
        scheduler_data.m_pending_donate_reason = reason;
        proc.invoke_scheduler_async();
        return false;
    }

    ScopedSpinLock lock(g_scheduler_lock);

    // "Leave" the critical section before switching context. Since we
    // still hold the scheduler lock, we're not actually leaving it.
    // Processor::switch_context expects Processor::in_critical() to be 1
    critical.leave();
    donate_to_and_switch(beneficiary, reason);
    return false;
}

bool Scheduler::context_switch(Thread* thread)
{
    if (s_mm_lock.own_lock()) {
        PANIC("In context switch while holding s_mm_lock");
    }

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
        dbgln("Scheduler[{}]: {} -> {} [prio={}] {:04x}:{:08x}", Processor::id(), from_thread->tid().value(), thread->tid().value(), thread->priority(), thread->tss().cs, thread->tss().eip);
#endif
    }

    auto& proc = Processor::current();
    if (!thread->is_initialized()) {
        proc.init_context(*thread, false);
        thread->set_initialized(true);
    }
    thread->set_state(Thread::Running);

    PerformanceManager::add_context_switch_perf_event(*from_thread, *thread);

    proc.switch_context(from_thread, thread);

    // NOTE: from_thread at this point reflects the thread we were
    // switched from, and thread reflects Thread::current()
    enter_current(*from_thread, false);
    VERIFY(thread == Thread::current());

#if ARCH(I386)
    if (thread->process().is_user_process()) {
        auto iopl = get_iopl_from_eflags(Thread::current()->get_register_dump_from_stack().eflags);
        if (iopl != 0) {
            PANIC("Switched to thread {} with non-zero IOPL={}", Thread::current()->tid().value(), iopl);
        }
    }
#endif

    return true;
}

void Scheduler::enter_current(Thread& prev_thread, bool is_first)
{
    VERIFY(g_scheduler_lock.own_lock());
    prev_thread.set_active(false);
    if (prev_thread.state() == Thread::Dying) {
        // If the thread we switched from is marked as dying, then notify
        // the finalizer. Note that as soon as we leave the scheduler lock
        // the finalizer may free from_thread!
        notify_finalizer();
    } else if (!is_first) {
        // Check if we have any signals we should deliver (even if we don't
        // end up switching to another thread).
        auto current_thread = Thread::current();
        if (!current_thread->is_in_block() && current_thread->previous_mode() != Thread::PreviousMode::KernelMode) {
            ScopedSpinLock lock(current_thread->get_lock());
            if (current_thread->state() == Thread::Running && current_thread->pending_signals_for_state()) {
                current_thread->dispatch_one_pending_signal();
            }
        }
    }
}

void Scheduler::leave_on_first_switch(u32 flags)
{
    // This is called when a thread is switched into for the first time.
    // At this point, enter_current has already be called, but because
    // Scheduler::context_switch is not in the call stack we need to
    // clean up and release locks manually here
    g_scheduler_lock.unlock(flags);
    auto& scheduler_data = Processor::current().get_scheduler_data();
    VERIFY(scheduler_data.m_in_scheduler);
    scheduler_data.m_in_scheduler = false;
}

void Scheduler::prepare_after_exec()
{
    // This is called after exec() when doing a context "switch" into
    // the new process. This is called from Processor::assume_context
    VERIFY(g_scheduler_lock.own_lock());
    auto& scheduler_data = Processor::current().get_scheduler_data();
    VERIFY(!scheduler_data.m_in_scheduler);
    scheduler_data.m_in_scheduler = true;
}

void Scheduler::prepare_for_idle_loop()
{
    // This is called when the CPU finished setting up the idle loop
    // and is about to run it. We need to acquire he scheduler lock
    VERIFY(!g_scheduler_lock.own_lock());
    g_scheduler_lock.lock();
    auto& scheduler_data = Processor::current().get_scheduler_data();
    VERIFY(!scheduler_data.m_in_scheduler);
    scheduler_data.m_in_scheduler = true;
}

Process* Scheduler::colonel()
{
    VERIFY(s_colonel_process);
    return s_colonel_process;
}

UNMAP_AFTER_INIT void Scheduler::initialize()
{
    VERIFY(&Processor::current() != nullptr); // sanity check

    RefPtr<Thread> idle_thread;
    g_finalizer_wait_queue = new WaitQueue;
    g_ready_queues = new ThreadReadyQueue[g_ready_queue_buckets];

    g_finalizer_has_work.store(false, AK::MemoryOrder::memory_order_release);
    s_colonel_process = Process::create_kernel_process(idle_thread, "colonel", idle_loop, nullptr, 1).leak_ref();
    VERIFY(s_colonel_process);
    VERIFY(idle_thread);
    idle_thread->set_priority(THREAD_PRIORITY_MIN);
    idle_thread->set_name(StringView("idle thread #0"));

    set_idle_thread(idle_thread);
}

UNMAP_AFTER_INIT void Scheduler::set_idle_thread(Thread* idle_thread)
{
    idle_thread->set_idle_thread();
    Processor::current().set_idle_thread(*idle_thread);
    Processor::current().set_current_thread(*idle_thread);
}

UNMAP_AFTER_INIT Thread* Scheduler::create_ap_idle_thread(u32 cpu)
{
    VERIFY(cpu != 0);
    // This function is called on the bsp, but creates an idle thread for another AP
    VERIFY(Processor::is_bootstrap_processor());

    VERIFY(s_colonel_process);
    Thread* idle_thread = s_colonel_process->create_kernel_thread(idle_loop, nullptr, THREAD_PRIORITY_MIN, String::formatted("idle thread #{}", cpu), 1 << cpu, false);
    VERIFY(idle_thread);
    return idle_thread;
}

void Scheduler::timer_tick(const RegisterState& regs)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(Processor::current().in_irq());

    auto current_thread = Processor::current_thread();
    if (!current_thread)
        return;

    // Sanity checks
    VERIFY(current_thread->current_trap());
    VERIFY(current_thread->current_trap()->regs == &regs);

#if !SCHEDULE_ON_ALL_PROCESSORS
    if (!Processor::is_bootstrap_processor())
        return; // TODO: This prevents scheduling on other CPUs!
#endif

    if (current_thread->tick())
        return;

    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(Processor::current().in_irq());
    Processor::current().invoke_scheduler_async();
}

void Scheduler::invoke_async()
{
    VERIFY_INTERRUPTS_DISABLED();
    auto& proc = Processor::current();
    VERIFY(!proc.in_irq());

    // Since this function is called when leaving critical sections (such
    // as a SpinLock), we need to check if we're not already doing this
    // to prevent recursion
    if (!proc.get_scheduler_data().m_in_scheduler)
        pick_next();
}

void Scheduler::yield_from_critical()
{
    auto& proc = Processor::current();
    VERIFY(proc.in_critical());
    VERIFY(!proc.in_irq());

    yield(); // Flag a context switch

    u32 prev_flags;
    u32 prev_crit = Processor::current().clear_critical(prev_flags, false);

    // Note, we may now be on a different CPU!
    Processor::current().restore_critical(prev_crit, prev_flags);
}

void Scheduler::notify_finalizer()
{
    if (g_finalizer_has_work.exchange(true, AK::MemoryOrder::memory_order_acq_rel) == false)
        g_finalizer_wait_queue->wake_all();
}

void Scheduler::idle_loop(void*)
{
    auto& proc = Processor::current();
    dbgln("Scheduler[{}]: idle loop running", proc.get_id());
    VERIFY(are_interrupts_enabled());

    for (;;) {
        proc.idle_begin();
        asm("hlt");

        proc.idle_end();
        VERIFY_INTERRUPTS_ENABLED();
#if SCHEDULE_ON_ALL_PROCESSORS
        yield();
#else
        if (Processor::current().id() == 0)
            yield();
#endif
    }
}

void Scheduler::dump_scheduler_state()
{
    dump_thread_list();
}

void dump_thread_list()
{
    dbgln("Scheduler thread list for processor {}:", Processor::id());

    auto get_cs = [](Thread& thread) -> u16 {
        if (!thread.current_trap())
            return thread.tss().cs;
        return thread.get_register_dump_from_stack().cs;
    };

    auto get_eip = [](Thread& thread) -> u32 {
        if (!thread.current_trap())
            return thread.tss().eip;
        return thread.get_register_dump_from_stack().eip;
    };

    Thread::for_each([&](Thread& thread) {
        switch (thread.state()) {
        case Thread::Dying:
            dmesgln("  {:14} {:30} @ {:04x}:{:08x} Finalizable: {}, (nsched: {})",
                thread.state_string(),
                thread,
                get_cs(thread),
                get_eip(thread),
                thread.is_finalizable(),
                thread.times_scheduled());
            break;
        default:
            dmesgln("  {:14} Pr:{:2} {:30} @ {:04x}:{:08x} (nsched: {})",
                thread.state_string(),
                thread.priority(),
                thread,
                get_cs(thread),
                get_eip(thread),
                thread.times_scheduled());
            break;
        }
    });
}

}
