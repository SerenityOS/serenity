/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ScopeGuard.h>
#include <AK/Singleton.h>
#include <AK/StringBuilder.h>
#include <AK/TemporaryChange.h>
#include <AK/Time.h>
#include <Kernel/API/POSIX/signal_numbers.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/Arch/TrapFrame.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/KSyms.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/ScopedAddressSpaceSwitcher.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/PerformanceEventBuffer.h>
#include <Kernel/Tasks/PowerStateSwitchTask.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/ScopedProcessList.h>
#include <Kernel/Tasks/Thread.h>
#include <Kernel/Tasks/ThreadTracer.h>
#include <Kernel/Time/TimerQueue.h>
#include <Kernel/kstdio.h>

namespace Kernel {

static Singleton<SpinlockProtected<Thread::GlobalList, LockRank::None>> s_list;

SpinlockProtected<Thread::GlobalList, LockRank::None>& Thread::all_instances()
{
    return *s_list;
}

ErrorOr<NonnullRefPtr<Thread>> Thread::create(NonnullRefPtr<Process> process)
{
    auto kernel_stack_region = TRY(MM.allocate_kernel_region(default_kernel_stack_size, {}, Memory::Region::Access::ReadWrite, AllocationStrategy::AllocateNow));
    kernel_stack_region->set_stack(true);

    auto block_timer = TRY(try_make_ref_counted<Timer>());

    return adopt_nonnull_ref_or_enomem(new (nothrow) Thread(move(process), move(kernel_stack_region), move(block_timer)));
}

Thread::Thread(NonnullRefPtr<Process> process, NonnullOwnPtr<Memory::Region> kernel_stack_region, NonnullRefPtr<Timer> block_timer)
    : m_process(move(process))
    , m_kernel_stack_region(move(kernel_stack_region))
    , m_block_timer(move(block_timer))
{
    m_process->name().with([this](auto& process_name) {
        set_name(process_name.representable_view());
    });

    bool is_first_thread = m_process->add_thread(*this);
    if (is_first_thread) {
        // First thread gets TID == PID
        m_tid = m_process->pid().value();
    } else {
        m_tid = Process::allocate_pid().value();
    }

    // FIXME: Handle KString allocation failure.
    m_kernel_stack_region->set_name(MUST(KString::formatted("Kernel stack (thread {})", m_tid.value())));

    Thread::all_instances().with([&](auto& list) {
        list.append(*this);
    });

    if constexpr (THREAD_DEBUG) {
        m_process->name().with([&](auto& process_name) {
            dbgln("Created new thread {}({}:{})", process_name.representable_view(), m_process->pid().value(), m_tid.value());
        });
    }

    reset_fpu_state();

    m_kernel_stack_base = m_kernel_stack_region->vaddr().get();
    m_kernel_stack_top = m_kernel_stack_region->vaddr().offset(default_kernel_stack_size).get() & ~(FlatPtr)0x7u;

    m_process->address_space().with([&](auto& space) {
        m_regs.set_initial_state(m_process->is_kernel_process(), *space, m_kernel_stack_top);
    });

    // We need to add another reference if we could successfully create
    // all the resources needed for this thread. The reason for this is that
    // we don't want to delete this thread after dropping the reference,
    // it may still be running or scheduled to be run.
    // The finalizer is responsible for dropping this reference once this
    // thread is ready to be cleaned up.
    ref();
}

Thread::~Thread()
{
    VERIFY(!m_process_thread_list_node.is_in_list());

    // We shouldn't be queued
    VERIFY(m_runnable_priority < 0);
}

Thread::BlockResult Thread::block_impl(BlockTimeout const& timeout, Blocker& blocker)
{
    VERIFY(!Processor::current_in_irq());
    VERIFY(this == Thread::current());
    ScopedCritical critical;

    SpinlockLocker scheduler_lock(g_scheduler_lock);

    SpinlockLocker block_lock(m_block_lock);
    // We need to hold m_block_lock so that nobody can unblock a blocker as soon
    // as it is constructed and registered elsewhere

    ScopeGuard finalize_guard([&] {
        blocker.finalize();
    });

    if (!blocker.setup_blocker()) {
        blocker.will_unblock_immediately_without_blocking(Blocker::UnblockImmediatelyReason::UnblockConditionAlreadyMet);
        return BlockResult::NotBlocked;
    }

    // Relaxed semantics are fine for timeout_unblocked because we
    // synchronize on the spin locks already.
    Atomic<bool, AK::MemoryOrder::memory_order_relaxed> timeout_unblocked(false);
    bool timer_was_added = false;

    switch (state()) {
    case Thread::State::Stopped:
        // It's possible that we were requested to be stopped!
        break;
    case Thread::State::Running:
        VERIFY(m_blocker == nullptr);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    m_blocker = &blocker;

    if (auto& block_timeout = blocker.override_timeout(timeout); !block_timeout.is_infinite()) {
        // Process::kill_all_threads may be called at any time, which will mark all
        // threads to die. In that case
        timer_was_added = TimerQueue::the().add_timer_without_id(*m_block_timer, block_timeout.clock_id(), block_timeout.absolute_time(), [&]() {
            VERIFY(!Processor::current_in_irq());
            VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
            VERIFY(!m_block_lock.is_locked_by_current_processor());
            // NOTE: this may execute on the same or any other processor!
            SpinlockLocker scheduler_lock(g_scheduler_lock);
            SpinlockLocker block_lock(m_block_lock);
            if (m_blocker && !timeout_unblocked.exchange(true))
                unblock();
        });
        if (!timer_was_added) {
            // Timeout is already in the past
            blocker.will_unblock_immediately_without_blocking(Blocker::UnblockImmediatelyReason::TimeoutInThePast);
            m_blocker = nullptr;
            return BlockResult::InterruptedByTimeout;
        }
    }

    blocker.begin_blocking({});

    set_state(Thread::State::Blocked);

    block_lock.unlock();
    scheduler_lock.unlock();

    dbgln_if(THREAD_DEBUG, "Thread {} blocking on {} ({}) -->", *this, &blocker, blocker.state_string());
    bool did_timeout = false;
    u32 lock_count_to_restore = 0;
    auto previous_locked = unlock_process_if_locked(lock_count_to_restore);
    for (;;) {
        // Yield to the scheduler, and wait for us to resume unblocked.
        VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
        VERIFY(Processor::in_critical());
        yield_without_releasing_big_lock();
        VERIFY(Processor::in_critical());

        SpinlockLocker block_lock2(m_block_lock);
        if (m_blocker && !m_blocker->can_be_interrupted() && !m_should_die) {
            block_lock2.unlock();
            dbgln("Thread should not be unblocking, current state: {}", state_string());
            set_state(Thread::State::Blocked);
            continue;
        }
        // Prevent the timeout from unblocking this thread if it happens to
        // be in the process of firing already
        did_timeout |= timeout_unblocked.exchange(true);
        if (m_blocker) {
            // Remove ourselves...
            VERIFY(m_blocker == &blocker);
            m_blocker = nullptr;
        }
        dbgln_if(THREAD_DEBUG, "<-- Thread {} unblocked from {} ({})", *this, &blocker, blocker.state_string());
        break;
    }

    // Notify the blocker that we are no longer blocking. It may need
    // to clean up now while we're still holding m_lock
    auto result = blocker.end_blocking({}, did_timeout); // calls was_unblocked internally

    if (timer_was_added && !did_timeout) {
        // Cancel the timer while not holding any locks. This allows
        // the timer function to complete before we remove it
        // (e.g. if it's on another processor)
        TimerQueue::the().cancel_timer(*m_block_timer);
    }
    if (previous_locked != LockMode::Unlocked) {
        // NOTE: This may trigger another call to Thread::block().
        relock_process(previous_locked, lock_count_to_restore);
    }
    return result;
}

void Thread::block(Kernel::Mutex& lock, SpinlockLocker<Spinlock<LockRank::None>>& lock_lock, u32 lock_count)
{
    VERIFY(!Processor::current_in_irq());
    VERIFY(this == Thread::current());
    ScopedCritical critical;

    SpinlockLocker scheduler_lock(g_scheduler_lock);
    SpinlockLocker block_lock(m_block_lock);

    switch (state()) {
    case Thread::State::Stopped:
        // It's possible that we were requested to be stopped!
        break;
    case Thread::State::Running:
        VERIFY(m_blocker == nullptr);
        break;
    default:
        dbgln("Error: Attempting to block with invalid thread state - {}", state_string());
        VERIFY_NOT_REACHED();
    }

    // If we're blocking on the big-lock we may actually be in the process
    // of unblocking from another lock. If that's the case m_blocking_mutex
    // is already set
    auto& big_lock = process().big_lock();
    VERIFY((&lock == &big_lock && m_blocking_mutex != &big_lock) || !m_blocking_mutex);

    auto* previous_blocking_mutex = m_blocking_mutex;
    m_blocking_mutex = &lock;
    m_lock_requested_count = lock_count;

    set_state(Thread::State::Blocked);

    block_lock.unlock();
    scheduler_lock.unlock();

    lock_lock.unlock();

    dbgln_if(THREAD_DEBUG, "Thread {} blocking on Mutex {}", *this, &lock);

    for (;;) {
        // Yield to the scheduler, and wait for us to resume unblocked.
        VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
        VERIFY(Processor::in_critical());
        if (&lock != &big_lock && big_lock.is_exclusively_locked_by_current_thread()) {
            // We're locking another lock and already hold the big lock...
            // We need to release the big lock
            yield_and_release_relock_big_lock();
        } else {
            // By the time we've reached this another thread might have
            // marked us as holding the big lock, so this call must not
            // verify that we're not holding it.
            yield_without_releasing_big_lock(VerifyLockNotHeld::No);
        }
        VERIFY(Processor::in_critical());

        SpinlockLocker block_lock2(m_block_lock);
        VERIFY(!m_blocking_mutex);
        m_blocking_mutex = previous_blocking_mutex;
        break;
    }

    lock_lock.lock();
}

u32 Thread::unblock_from_mutex(Kernel::Mutex& mutex)
{
    SpinlockLocker scheduler_lock(g_scheduler_lock);
    SpinlockLocker block_lock(m_block_lock);

    VERIFY(!Processor::current_in_irq());
    VERIFY(m_blocking_mutex == &mutex);

    dbgln_if(THREAD_DEBUG, "Thread {} unblocked from Mutex {}", *this, &mutex);

    auto requested_count = m_lock_requested_count;

    m_blocking_mutex = nullptr;
    if (Thread::current() == this) {
        set_state(Thread::State::Running);
        return requested_count;
    }
    VERIFY(m_state != Thread::State::Runnable && m_state != Thread::State::Running);
    set_state(Thread::State::Runnable);
    return requested_count;
}

void Thread::unblock_from_blocker(Blocker& blocker)
{
    auto do_unblock = [&]() {
        SpinlockLocker scheduler_lock(g_scheduler_lock);
        SpinlockLocker block_lock(m_block_lock);
        if (m_blocker != &blocker)
            return;
        VERIFY(!is_stopped());
        unblock();
    };
    if (Processor::current_in_irq() != 0) {
        Processor::deferred_call_queue([do_unblock = move(do_unblock), self = try_make_weak_ptr().release_value_but_fixme_should_propagate_errors()]() {
            if (auto this_thread = self.strong_ref())
                do_unblock();
        });
    } else {
        do_unblock();
    }
}

void Thread::unblock(u8 signal)
{
    VERIFY(!Processor::current_in_irq());
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());
    VERIFY(m_block_lock.is_locked_by_current_processor());
    if (m_state != Thread::State::Blocked)
        return;
    if (m_blocking_mutex)
        return;
    VERIFY(m_blocker);
    if (signal != 0) {
        if (is_handling_page_fault()) {
            // Don't let signals unblock threads that are blocked inside a page fault handler.
            // This prevents threads from EINTR'ing the inode read in an inode page fault.
            // FIXME: There's probably a better way to solve this.
            return;
        }
        if (!m_blocker->can_be_interrupted() && !m_should_die)
            return;
        m_blocker->set_interrupted_by_signal(signal);
    }
    m_blocker = nullptr;
    if (Thread::current() == this) {
        set_state(Thread::State::Running);
        return;
    }
    VERIFY(m_state != Thread::State::Runnable && m_state != Thread::State::Running);
    set_state(Thread::State::Runnable);
}

void Thread::set_should_die()
{
    if (m_should_die) {
        dbgln("{} Should already die", *this);
        return;
    }
    ScopedCritical critical;

    // Remember that we should die instead of returning to
    // the userspace.
    SpinlockLocker lock(g_scheduler_lock);
    m_should_die = true;

    // NOTE: Even the current thread can technically be in "Stopped"
    // state! This is the case when another thread sent a SIGSTOP to
    // it while it was running and it calls e.g. exit() before
    // the scheduler gets involved again.
    if (is_stopped()) {
        // If we were stopped, we need to briefly resume so that
        // the kernel stacks can clean up. We won't ever return back
        // to user mode, though
        VERIFY(!process().is_stopped());
        resume_from_stopped();
    }
    if (is_blocked()) {
        SpinlockLocker block_lock(m_block_lock);
        if (m_blocker) {
            // We're blocked in the kernel.
            m_blocker->set_interrupted_by_death();
            unblock();
        }
    }
}

void Thread::die_if_needed()
{
    VERIFY(Thread::current() == this);

    if (!m_should_die)
        return;

    u32 unlock_count;
    [[maybe_unused]] auto rc = unlock_process_if_locked(unlock_count);

    dbgln_if(THREAD_DEBUG, "Thread {} is dying", *this);

    {
        SpinlockLocker lock(g_scheduler_lock);
        // It's possible that we don't reach the code after this block if the
        // scheduler is invoked and FinalizerTask cleans up this thread, however
        // that doesn't matter because we're trying to invoke the scheduler anyway
        set_state(Thread::State::Dying);
    }

    ScopedCritical critical;

    // Flag a context switch. Because we're in a critical section,
    // Scheduler::yield will actually only mark a pending context switch
    // Simply leaving the critical section would not necessarily trigger
    // a switch.
    Scheduler::yield();

    // Now leave the critical section so that we can also trigger the
    // actual context switch
    Processor::clear_critical();
    dbgln("die_if_needed returned from clear_critical!!! in irq: {}", Processor::current_in_irq());
    // We should never get here, but the scoped scheduler lock
    // will be released by Scheduler::context_switch again
    VERIFY_NOT_REACHED();
}

void Thread::exit(void* exit_value)
{
    VERIFY(Thread::current() == this);
    m_join_blocker_set.thread_did_exit(exit_value);
    set_should_die();
    u32 unlock_count;
    [[maybe_unused]] auto rc = unlock_process_if_locked(unlock_count);
    die_if_needed();
}

void Thread::yield_without_releasing_big_lock(VerifyLockNotHeld verify_lock_not_held)
{
    VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
    VERIFY(verify_lock_not_held == VerifyLockNotHeld::No || !process().big_lock().is_exclusively_locked_by_current_thread());
    // Disable interrupts here. This ensures we don't accidentally switch contexts twice
    InterruptDisabler disable;
    Scheduler::yield(); // flag a switch
    u32 prev_critical = Processor::clear_critical();
    // NOTE: We may be on a different CPU now!
    Processor::restore_critical(prev_critical);
}

void Thread::yield_and_release_relock_big_lock()
{
    VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
    // Disable interrupts here. This ensures we don't accidentally switch contexts twice
    InterruptDisabler disable;
    Scheduler::yield(); // flag a switch
    u32 lock_count_to_restore = 0;
    auto previous_locked = unlock_process_if_locked(lock_count_to_restore);
    // NOTE: Even though we call Scheduler::yield here, unless we happen
    // to be outside of a critical section, the yield will be postponed
    // until leaving it in relock_process.
    relock_process(previous_locked, lock_count_to_restore);
}

LockMode Thread::unlock_process_if_locked(u32& lock_count_to_restore)
{
    return process().big_lock().force_unlock_exclusive_if_locked(lock_count_to_restore);
}

void Thread::relock_process(LockMode previous_locked, u32 lock_count_to_restore)
{
    // Clearing the critical section may trigger the context switch
    // flagged by calling Scheduler::yield above.
    // We have to do it this way because we intentionally
    // leave the critical section here to be able to switch contexts.
    u32 prev_critical = Processor::clear_critical();

    // CONTEXT SWITCH HAPPENS HERE!

    // NOTE: We may be on a different CPU now!
    Processor::restore_critical(prev_critical);

    if (previous_locked != LockMode::Unlocked) {
        // We've unblocked, relock the process if needed and carry on.
        process().big_lock().restore_exclusive_lock(lock_count_to_restore);
    }
}

// NOLINTNEXTLINE(readability-make-member-function-const) False positive; We call block<SleepBlocker> which is not const
auto Thread::sleep(clockid_t clock_id, Duration const& duration, Duration* remaining_time) -> BlockResult
{
    VERIFY(state() == Thread::State::Running);
    return Thread::current()->block<Thread::SleepBlocker>({}, Thread::BlockTimeout(false, &duration, nullptr, clock_id), remaining_time);
}

// NOLINTNEXTLINE(readability-make-member-function-const) False positive; We call block<SleepBlocker> which is not const
auto Thread::sleep_until(clockid_t clock_id, Duration const& deadline) -> BlockResult
{
    VERIFY(state() == Thread::State::Running);
    return Thread::current()->block<Thread::SleepBlocker>({}, Thread::BlockTimeout(true, &deadline, nullptr, clock_id));
}

StringView Thread::state_string() const
{
    switch (state()) {
    case Thread::State::Invalid:
        return "Invalid"sv;
    case Thread::State::Runnable:
        return "Runnable"sv;
    case Thread::State::Running:
        return "Running"sv;
    case Thread::State::Dying:
        return "Dying"sv;
    case Thread::State::Dead:
        return "Dead"sv;
    case Thread::State::Stopped:
        return "Stopped"sv;
    case Thread::State::Blocked: {
        SpinlockLocker block_lock(m_block_lock);
        if (m_blocking_mutex)
            return "Mutex"sv;
        if (m_blocker)
            return m_blocker->state_string();
        VERIFY_NOT_REACHED();
    }
    }
    PANIC("Thread::state_string(): Invalid state: {}", (int)state());
}

void Thread::finalize()
{
    if (!g_in_system_shutdown)
        VERIFY(Thread::current() == g_finalizer);
    VERIFY(Thread::current() != this);

#if LOCK_DEBUG
    VERIFY(!m_lock.is_locked_by_current_processor());
    if (lock_count() > 0) {
        dbgln("Thread {} leaking {} Locks!", *this, lock_count());
        SpinlockLocker list_lock(m_holding_locks_lock);
        for (auto& info : m_holding_locks_list) {
            auto const& location = info.lock_location;
            dbgln(" - Mutex: \"{}\" @ {} locked in function \"{}\" at \"{}:{}\" with a count of: {}", info.lock->name(), info.lock, location.function_name(), location.filename(), location.line_number(), info.count);
        }
        VERIFY_NOT_REACHED();
    }
#endif

    {
        SpinlockLocker lock(g_scheduler_lock);
        dbgln_if(THREAD_DEBUG, "Finalizing thread {}", *this);
        set_state(Thread::State::Dead);
        m_join_blocker_set.thread_finalizing();
    }

    if (m_dump_backtrace_on_finalization) {
        print_backtrace();
    }

    drop_thread_count();
}

void Thread::drop_thread_count()
{
    bool is_last = process().remove_thread(*this);
    if (is_last)
        process().finalize();
}

void Thread::finalize_dying_threads()
{
    VERIFY(Thread::current() == g_finalizer);
    Vector<Thread*, 32> dying_threads;
    {
        SpinlockLocker lock(g_scheduler_lock);
        for_each_in_state_ignoring_process_lists(Thread::State::Dying, [&](Thread& thread) {
            if (!thread.is_finalizable())
                return;
            auto result = dying_threads.try_append(&thread);
            // We ignore allocation failures above the first 32 guaranteed thread slots, and
            // just flag our future-selves to finalize these threads at a later point
            if (result.is_error())
                g_finalizer_has_work.store(true, AK::MemoryOrder::memory_order_release);
        });
    }
    for (auto* thread : dying_threads) {
        RefPtr<Process> const process = thread->process();
        dbgln_if(PROCESS_DEBUG, "Before finalization, {} has {} refs and its process has {}",
            *thread, thread->ref_count(), thread->process().ref_count());
        thread->finalize();
        dbgln_if(PROCESS_DEBUG, "After finalization, {} has {} refs and its process has {}",
            *thread, thread->ref_count(), thread->process().ref_count());
        // This thread will never execute again, drop the running reference
        // NOTE: This may not necessarily drop the last reference if anything
        //       else is still holding onto this thread!
        thread->unref();
    }
}

void Thread::update_time_scheduled(u64 current_scheduler_time, bool is_kernel, bool no_longer_running)
{
    if (m_last_time_scheduled.has_value()) {
        u64 delta;
        if (current_scheduler_time >= m_last_time_scheduled.value())
            delta = current_scheduler_time - m_last_time_scheduled.value();
        else
            delta = m_last_time_scheduled.value() - current_scheduler_time; // the unlikely event that the clock wrapped
        if (delta != 0) {
            // Add it to the global total *before* updating the thread's value!
            Scheduler::add_time_scheduled(delta, is_kernel);

            auto& total_time = is_kernel ? m_total_time_scheduled_kernel : m_total_time_scheduled_user;
            total_time.fetch_add(delta, AK::memory_order_relaxed);
        }
    }
    if (no_longer_running)
        m_last_time_scheduled = {};
    else
        m_last_time_scheduled = current_scheduler_time;
}

bool Thread::tick()
{
    if (previous_mode() == ExecutionMode::Kernel) {
        ++m_process->m_ticks_in_kernel;
        ++m_ticks_in_kernel;
    } else {
        ++m_process->m_ticks_in_user;
        ++m_ticks_in_user;
    }
    --m_ticks_left;
    return m_ticks_left != 0;
}

void Thread::check_dispatch_pending_signal()
{
    auto result = DispatchSignalResult::Continue;
    {
        SpinlockLocker scheduler_lock(g_scheduler_lock);
        if (pending_signals_for_state() != 0) {
            result = dispatch_one_pending_signal();
        }
    }

    if (result == DispatchSignalResult::Yield) {
        yield_without_releasing_big_lock();
    }
}

u32 Thread::pending_signals() const
{
    SpinlockLocker lock(g_scheduler_lock);
    return pending_signals_for_state();
}

u32 Thread::pending_signals_for_state() const
{
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());
    constexpr u32 stopped_signal_mask = (1 << (SIGCONT - 1)) | (1 << (SIGKILL - 1)) | (1 << (SIGTRAP - 1));
    if (is_handling_page_fault())
        return 0;
    return m_state != State::Stopped ? m_pending_signals : m_pending_signals & stopped_signal_mask;
}

void Thread::send_signal(u8 signal, [[maybe_unused]] Process* sender)
{
    VERIFY(signal < NSIG);
    VERIFY(process().is_user_process());
    SpinlockLocker scheduler_lock(g_scheduler_lock);

    // FIXME: Figure out what to do for masked signals. Should we also ignore them here?
    if (should_ignore_signal(signal)) {
        dbgln_if(SIGNAL_DEBUG, "Signal {} was ignored by {}", signal, process());
        return;
    }

    if constexpr (SIGNAL_DEBUG) {
        if (sender)
            dbgln("Signal: {} sent {} to {}", *sender, signal, process());
        else
            dbgln("Signal: Kernel send {} to {}", signal, process());
    }

    m_pending_signals |= 1 << (signal - 1);
    m_signal_senders[signal] = sender ? sender->pid() : pid();
    m_have_any_unmasked_pending_signals.store((pending_signals_for_state() & ~m_signal_mask) != 0, AK::memory_order_release);
    m_signal_blocker_set.unblock_all_blockers_whose_conditions_are_met();

    if (!has_unmasked_pending_signals())
        return;

    if (m_state == Thread::State::Stopped) {
        if (pending_signals_for_state() != 0) {
            dbgln_if(SIGNAL_DEBUG, "Signal: Resuming stopped {} to deliver signal {}", *this, signal);
            resume_from_stopped();
        }
    } else {
        SpinlockLocker block_lock(m_block_lock);
        dbgln_if(SIGNAL_DEBUG, "Signal: Unblocking {} to deliver signal {}", *this, signal);
        unblock(signal);
    }
}

u32 Thread::update_signal_mask(u32 signal_mask)
{
    SpinlockLocker lock(g_scheduler_lock);
    auto previous_signal_mask = m_signal_mask;
    m_signal_mask = signal_mask;
    m_have_any_unmasked_pending_signals.store((pending_signals_for_state() & ~m_signal_mask) != 0, AK::memory_order_release);
    return previous_signal_mask;
}

u32 Thread::signal_mask() const
{
    SpinlockLocker lock(g_scheduler_lock);
    return m_signal_mask;
}

u32 Thread::signal_mask_block(sigset_t signal_set, bool block)
{
    SpinlockLocker lock(g_scheduler_lock);
    auto previous_signal_mask = m_signal_mask;
    if (block)
        m_signal_mask |= signal_set;
    else
        m_signal_mask &= ~signal_set;
    m_have_any_unmasked_pending_signals.store((pending_signals_for_state() & ~m_signal_mask) != 0, AK::memory_order_release);
    return previous_signal_mask;
}

void Thread::reset_signals_for_exec()
{
    SpinlockLocker lock(g_scheduler_lock);
    // The signal mask is preserved across execve(2).
    // The pending signal set is preserved across an execve(2).
    m_have_any_unmasked_pending_signals.store(false, AK::memory_order_release);
    m_signal_action_masks.fill({});
    // A successful call to execve(2) removes any existing alternate signal stack
    m_alternative_signal_stack.clear();
}

// Certain exceptions, such as SIGSEGV and SIGILL, put a
// thread into a state where the signal handler must be
// invoked immediately, otherwise it will continue to fault.
// This function should be used in an exception handler to
// ensure that when the thread resumes, it's executing in
// the appropriate signal handler.
void Thread::send_urgent_signal_to_self(u8 signal)
{
    VERIFY(Thread::current() == this);
    DispatchSignalResult result;
    {
        SpinlockLocker lock(g_scheduler_lock);
        result = dispatch_signal(signal);
    }
    if (result == DispatchSignalResult::Terminate) {
        Thread::current()->die_if_needed();
        VERIFY_NOT_REACHED(); // dispatch_signal will request termination of the thread, so the above call should never return
    }
    if (result == DispatchSignalResult::Yield)
        yield_and_release_relock_big_lock();
}

DispatchSignalResult Thread::dispatch_one_pending_signal()
{
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());
    u32 signal_candidates = pending_signals_for_state() & ~m_signal_mask;
    if (signal_candidates == 0)
        return DispatchSignalResult::Continue;

    u8 signal = 1;
    for (; signal < NSIG; ++signal) {
        if ((signal_candidates & (1 << (signal - 1))) != 0) {
            break;
        }
    }
    return dispatch_signal(signal);
}

DispatchSignalResult Thread::try_dispatch_one_pending_signal(u8 signal)
{
    VERIFY(signal != 0);
    SpinlockLocker scheduler_lock(g_scheduler_lock);
    u32 signal_candidates = pending_signals_for_state() & ~m_signal_mask;
    if ((signal_candidates & (1 << (signal - 1))) == 0)
        return DispatchSignalResult::Continue;
    return dispatch_signal(signal);
}

enum class DefaultSignalAction {
    Terminate,
    Ignore,
    DumpCore,
    Stop,
    Continue,
};

static DefaultSignalAction default_signal_action(u8 signal)
{
    VERIFY(signal && signal < NSIG);

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
    case SIGCANCEL:
        return DefaultSignalAction::Terminate;
    case SIGCHLD:
    case SIGURG:
    case SIGWINCH:
    case SIGINFO:
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
    default:
        VERIFY_NOT_REACHED();
    }
}

bool Thread::should_ignore_signal(u8 signal) const
{
    VERIFY(signal < NSIG);
    auto const& action = m_process->m_signal_action_data[signal];
    if (action.handler_or_sigaction.is_null())
        return default_signal_action(signal) == DefaultSignalAction::Ignore;
    return ((sighandler_t)action.handler_or_sigaction.get() == SIG_IGN);
}

bool Thread::has_signal_handler(u8 signal) const
{
    VERIFY(signal < NSIG);
    auto const& action = m_process->m_signal_action_data[signal];
    return !action.handler_or_sigaction.is_null();
}

bool Thread::is_signal_masked(u8 signal) const
{
    VERIFY(signal < NSIG);
    return (1 << (signal - 1)) & m_signal_mask;
}

bool Thread::is_in_alternative_signal_stack() const
{
    auto sp = get_register_dump_from_stack().userspace_sp();
    if (!m_alternative_signal_stack.has_value())
        return false;
    return m_alternative_signal_stack->contains(VirtualAddress(sp));
}

static ErrorOr<void> push_value_on_user_stack(FlatPtr& stack, FlatPtr data)
{
    stack -= sizeof(FlatPtr);
    return copy_to_user((FlatPtr*)stack, &data);
}

template<typename T>
static ErrorOr<void> copy_value_on_user_stack(FlatPtr& stack, T const& data)
{
    stack -= sizeof(data);
    return copy_to_user((RemoveCVReference<T>*)stack, &data);
}

void Thread::resume_from_stopped()
{
    VERIFY(is_stopped());
    VERIFY(m_stop_state != State::Invalid);
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());
    if (m_stop_state == Thread::State::Blocked) {
        SpinlockLocker block_lock(m_block_lock);
        if (m_blocker || m_blocking_mutex) {
            // Hasn't been unblocked yet
            set_state(Thread::State::Blocked, 0);
        } else {
            // Was unblocked while stopped
            set_state(Thread::State::Runnable);
        }
    } else {
        set_state(m_stop_state, 0);
    }
}

DispatchSignalResult Thread::dispatch_signal(u8 signal)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());
    VERIFY(signal > 0 && signal <= NSIG);
    VERIFY(process().is_user_process());
    VERIFY(this == Thread::current());

    dbgln_if(SIGNAL_DEBUG, "Dispatch signal {} to {}, state: {}", signal, *this, state_string());

    if (m_state == Thread::State::Invalid || !is_initialized()) {
        // Thread has barely been created, we need to wait until it is
        // at least in Runnable state and is_initialized() returns true,
        // which indicates that it is fully set up an we actually have
        // a register state on the stack that we can modify
        return DispatchSignalResult::Deferred;
    }

    auto& action = m_process->m_signal_action_data[signal];
    auto sender_pid = m_signal_senders[signal];
    auto sender = Process::from_pid_ignoring_process_lists(sender_pid);

    if (!current_trap() && !action.handler_or_sigaction.is_null()) {
        // We're trying dispatch a handled signal to a user process that was scheduled
        // after a yielding/blocking kernel thread, we don't have a register capture of
        // the thread, so just defer processing the signal to later.
        return DispatchSignalResult::Deferred;
    }

    // Mark this signal as handled.
    m_pending_signals &= ~(1 << (signal - 1));
    m_have_any_unmasked_pending_signals.store((m_pending_signals & ~m_signal_mask) != 0, AK::memory_order_release);

    auto& process = this->process();
    auto* tracer = process.tracer();
    if (signal == SIGSTOP || (tracer && default_signal_action(signal) == DefaultSignalAction::DumpCore)) {
        dbgln_if(SIGNAL_DEBUG, "Signal {} stopping this thread", signal);
        if (tracer)
            tracer->set_regs(get_register_dump_from_stack());
        set_state(Thread::State::Stopped, signal);
        return DispatchSignalResult::Yield;
    }

    if (signal == SIGCONT) {
        dbgln_if(SIGNAL_DEBUG, "signal: SIGCONT resuming {}", *this);
    } else {
        if (tracer) {
            // when a thread is traced, it should be stopped whenever it receives a signal
            // the tracer is notified of this by using waitpid()
            // only "pending signals" from the tracer are sent to the tracee
            if (!tracer->has_pending_signal(signal)) {
                dbgln_if(SIGNAL_DEBUG, "signal: {} stopping {} for tracer", signal, *this);
                set_state(Thread::State::Stopped, signal);
                return DispatchSignalResult::Yield;
            }
            tracer->unset_signal(signal);
        }
    }

    auto handler_vaddr = action.handler_or_sigaction;
    if (handler_vaddr.is_null()) {
        switch (default_signal_action(signal)) {
        case DefaultSignalAction::Stop:
            set_state(Thread::State::Stopped, signal);
            return DispatchSignalResult::Yield;
        case DefaultSignalAction::DumpCore:
            process.set_should_generate_coredump(true);
            process.for_each_thread([](auto& thread) {
                thread.set_dump_backtrace_on_finalization();
            });
            [[fallthrough]];
        case DefaultSignalAction::Terminate:
            m_process->terminate_due_to_signal(signal);
            return DispatchSignalResult::Terminate;
        case DefaultSignalAction::Ignore:
            VERIFY_NOT_REACHED();
        case DefaultSignalAction::Continue:
            return DispatchSignalResult::Continue;
        }
        VERIFY_NOT_REACHED();
    }

    if ((sighandler_t)handler_vaddr.as_ptr() == SIG_IGN) {
        dbgln_if(SIGNAL_DEBUG, "Ignored signal {}", signal);
        return DispatchSignalResult::Continue;
    }

    ScopedAddressSpaceSwitcher switcher(m_process);

    m_currently_handled_signal = signal;

    u32 old_signal_mask = m_signal_mask;
    u32 new_signal_mask = m_signal_action_masks[signal].value_or(action.mask);
    if ((action.flags & SA_NODEFER) == SA_NODEFER)
        new_signal_mask &= ~(1 << (signal - 1));
    else
        new_signal_mask |= 1 << (signal - 1);

    m_signal_mask |= new_signal_mask;
    m_have_any_unmasked_pending_signals.store((m_pending_signals & ~m_signal_mask) != 0, AK::memory_order_release);

    bool use_alternative_stack = ((action.flags & SA_ONSTACK) != 0) && m_alternative_signal_stack.has_value() && !is_in_alternative_signal_stack();

    auto setup_stack = [&](RegisterState& state) -> ErrorOr<void> {
        FlatPtr stack;
        if (use_alternative_stack)
            stack = m_alternative_signal_stack->end().get();
        else
            stack = state.userspace_sp();

        dbgln_if(SIGNAL_DEBUG, "Setting up user stack to return to IP {:p}, SP {:p}", state.ip(), state.userspace_sp());

        __ucontext ucontext {
            .uc_link = nullptr,
            .uc_sigmask = old_signal_mask,
            .uc_stack = {
                .ss_sp = bit_cast<void*>(stack),
                .ss_flags = action.flags & SA_ONSTACK,
                .ss_size = use_alternative_stack ? m_alternative_signal_stack->size() : 0,
            },
            .uc_mcontext = {},
        };
        copy_kernel_registers_into_ptrace_registers(static_cast<PtraceRegisters&>(ucontext.uc_mcontext), state);

        auto fill_signal_info_for_signal = [&](siginfo& signal_info) {
            if (signal == SIGCHLD) {
                if (!sender) {
                    signal_info.si_code = CLD_EXITED;
                    return;
                }
                auto const* thread = sender->thread_list().with([](auto& list) { return list.is_empty() ? nullptr : list.first(); });
                if (!thread) {
                    signal_info.si_code = CLD_EXITED;
                    return;
                }

                switch (thread->m_state) {
                case State::Dead:
                    if (sender->should_generate_coredump() && sender->is_dumpable()) {
                        signal_info.si_code = CLD_DUMPED;
                        signal_info.si_status = sender->termination_signal();
                        return;
                    }
                    [[fallthrough]];
                case State::Dying:
                    if (sender->termination_signal() == 0) {
                        signal_info.si_code = CLD_EXITED;
                        signal_info.si_status = sender->termination_status();
                        return;
                    }
                    signal_info.si_code = CLD_KILLED;
                    signal_info.si_status = sender->termination_signal();
                    return;
                case State::Runnable:
                case State::Running:
                case State::Blocked:
                    signal_info.si_code = CLD_CONTINUED;
                    return;
                case State::Stopped:
                    signal_info.si_code = CLD_STOPPED;
                    return;
                case State::Invalid:
                    // Something is wrong, but we're just an observer.
                    break;
                }
            }

            signal_info.si_code = SI_NOINFO;
        };

        siginfo signal_info {
            .si_signo = signal,
            // Filled in below by fill_signal_info_for_signal.
            .si_code = 0,
            // Set for SI_TIMER, we don't have the data here.
            .si_errno = 0,
            .si_pid = sender_pid.value(),
            .si_uid = sender ? sender->credentials()->uid().value() : 0,
            // Set for SIGILL, SIGFPE, SIGSEGV and SIGBUS
            // FIXME: We don't generate these signals in a way that can be handled.
            .si_addr = 0,
            // Set for SIGCHLD.
            .si_status = 0,
            // Set for SIGPOLL, we don't have SIGPOLL.
            .si_band = 0,
            // Set for SI_QUEUE, SI_TIMER, SI_ASYNCIO and SI_MESGQ
            // We do not generate any of these.
            .si_value = {
                .sival_int = 0,
            },
        };

        if (action.flags & SA_SIGINFO)
            fill_signal_info_for_signal(signal_info);

        // Align the stack to 16 bytes.
        // Note that we push some elements on to the stack before the return address,
        // so we need to account for this here.
        constexpr static FlatPtr elements_pushed_on_stack_before_handler_address = 1; // one slot for a saved register
        FlatPtr const extra_bytes_pushed_on_stack_before_handler_address = sizeof(ucontext) + sizeof(signal_info);
        FlatPtr stack_alignment = (stack - elements_pushed_on_stack_before_handler_address * sizeof(FlatPtr) + extra_bytes_pushed_on_stack_before_handler_address) % 16;
        stack -= stack_alignment;

#if ARCH(X86_64)
        // Also note that we have to skip the thread red-zone (if needed), so do that here.
        constexpr static FlatPtr thread_red_zone_size = 128;
        stack -= thread_red_zone_size;
#endif

        auto start_of_stack = stack;

        TRY(push_value_on_user_stack(stack, 0)); // syscall return value slot

        TRY(copy_value_on_user_stack(stack, ucontext));
        auto pointer_to_ucontext = stack;

        TRY(copy_value_on_user_stack(stack, signal_info));
        auto pointer_to_signal_info = stack;

        // Make sure we actually pushed as many elements as we claimed to have pushed.
        if (start_of_stack - stack != elements_pushed_on_stack_before_handler_address * sizeof(FlatPtr) + extra_bytes_pushed_on_stack_before_handler_address) {
            PANIC("Stack in invalid state after signal trampoline, expected {:x} but got {:x}",
                start_of_stack - elements_pushed_on_stack_before_handler_address * sizeof(FlatPtr) - extra_bytes_pushed_on_stack_before_handler_address, stack);
        }

        VERIFY(stack % 16 == 0);

        // Save the FPU/SSE state
        TRY(copy_value_on_user_stack(stack, fpu_state()));

        TRY(push_value_on_user_stack(stack, pointer_to_ucontext));
        TRY(push_value_on_user_stack(stack, pointer_to_signal_info));
        TRY(push_value_on_user_stack(stack, signal));

        TRY(push_value_on_user_stack(stack, handler_vaddr.get()));

        // We write back the adjusted stack value into the register state.
        // We have to do this because we can't just pass around a reference to a packed field, as it's UB.
        state.set_userspace_sp(stack);

        return {};
    };

    // We now place the thread state on the userspace stack.
    // Note that we use a RegisterState.
    // Conversely, when the thread isn't blocking the RegisterState may not be
    // valid (fork, exec etc) but the tss will, so we use that instead.
    auto& regs = get_register_dump_from_stack();

    auto result = setup_stack(regs);
    if (result.is_error()) {
        dbgln("Invalid stack pointer: {}", regs.userspace_sp());
        process.set_should_generate_coredump(true);
        process.for_each_thread([](auto& thread) {
            thread.set_dump_backtrace_on_finalization();
        });
        m_process->terminate_due_to_signal(signal);
        return DispatchSignalResult::Terminate;
    }

    auto signal_trampoline_addr = process.signal_trampoline().get();
    regs.set_ip(signal_trampoline_addr);

#if ARCH(X86_64)
    // Userspace flags might be invalid for function entry, according to SYSV ABI (section 3.2.1).
    // Set them to a known-good value to avoid weird handler misbehavior.
    // Only IF (and the reserved bit 1) are set.
    regs.set_flags(2 | (regs.rflags & ~safe_eflags_mask));
#endif

    dbgln_if(SIGNAL_DEBUG, "Thread in state '{}' has been primed with signal handler {:p} to deliver {}", state_string(), regs.ip(), signal);

    return DispatchSignalResult::Continue;
}

RegisterState& Thread::get_register_dump_from_stack()
{
    auto* trap = current_trap();

    // We should *always* have a trap. If we don't we're probably a kernel
    // thread that hasn't been preempted. If we want to support this, we
    // need to capture the registers probably into m_regs and return it
    VERIFY(trap);

    while (trap) {
        if (!trap->next_trap)
            break;
        trap = trap->next_trap;
    }
    return *trap->regs;
}

ErrorOr<NonnullRefPtr<Thread>> Thread::clone(NonnullRefPtr<Process> process)
{
    auto clone = TRY(Thread::create(move(process)));
    m_signal_action_masks.span().copy_to(clone->m_signal_action_masks);
    clone->m_signal_mask = m_signal_mask;
    clone->m_fpu_state = m_fpu_state;
    clone->m_arch_specific_data = m_arch_specific_data;
    return clone;
}

void Thread::set_state(State new_state, u8 stop_signal)
{
    State previous_state;
    VERIFY(g_scheduler_lock.is_locked_by_current_processor());
    if (new_state == m_state)
        return;

    {
        previous_state = m_state;
        if (previous_state == Thread::State::Invalid) {
            // If we were *just* created, we may have already pending signals
            if (has_unmasked_pending_signals()) {
                dbgln_if(THREAD_DEBUG, "Dispatch pending signals to new thread {}", *this);
                dispatch_one_pending_signal();
            }
        }

        m_state = new_state;
        dbgln_if(THREAD_DEBUG, "Set thread {} state to {}", *this, state_string());
    }

    if (previous_state == Thread::State::Runnable) {
        Scheduler::dequeue_runnable_thread(*this);
    } else if (previous_state == Thread::State::Stopped) {
        m_stop_state = State::Invalid;
        auto& process = this->process();
        if (process.set_stopped(false)) {
            process.for_each_thread([&](auto& thread) {
                if (&thread == this)
                    return;
                if (!thread.is_stopped())
                    return;
                dbgln_if(THREAD_DEBUG, "Resuming peer thread {}", thread);
                thread.resume_from_stopped();
            });
            process.unblock_waiters(Thread::WaitBlocker::UnblockFlags::Continued);
            // Tell the parent process (if any) about this change.
            if (auto parent = Process::from_pid_ignoring_process_lists(process.ppid())) {
                [[maybe_unused]] auto result = parent->send_signal(SIGCHLD, &process);
            }
        }
    }

    if (m_state == Thread::State::Runnable) {
        Scheduler::enqueue_runnable_thread(*this);
        Processor::smp_wake_n_idle_processors(1);
    } else if (m_state == Thread::State::Stopped) {
        // We don't want to restore to Running state, only Runnable!
        m_stop_state = previous_state != Thread::State::Running ? previous_state : Thread::State::Runnable;
        auto& process = this->process();
        if (!process.set_stopped(true)) {
            // Note that we don't explicitly stop peer threads, we let them stop on their own the next time they
            // enter/exit a syscall, or once their current time slice runs out.
            process.unblock_waiters(Thread::WaitBlocker::UnblockFlags::Stopped, stop_signal);
            // Tell the parent process (if any) about this change.
            if (auto parent = Process::from_pid_ignoring_process_lists(process.ppid())) {
                [[maybe_unused]] auto result = parent->send_signal(SIGCHLD, &process);
            }
        }
    } else if (m_state == Thread::State::Dying) {
        VERIFY(previous_state != Thread::State::Blocked);
        if (this != Thread::current() && is_finalizable()) {
            // Some other thread set this thread to Dying, notify the
            // finalizer right away as it can be cleaned up now
            Scheduler::notify_finalizer();
        }
    }
}

struct RecognizedSymbol {
    FlatPtr address;
    KernelSymbol const* symbol { nullptr };
};

static ErrorOr<bool> symbolicate(RecognizedSymbol const& symbol, Process& process, StringBuilder& builder)
{
    if (symbol.address == 0)
        return false;

    auto credentials = process.credentials();
    bool mask_kernel_addresses = !credentials->is_superuser();
    if (!symbol.symbol) {
        if (!Memory::is_user_address(VirtualAddress(symbol.address))) {
            TRY(builder.try_append("0xdeadc0de\n"sv));
        } else {
            TRY(process.address_space().with([&](auto& space) -> ErrorOr<void> {
                if (auto* region = space->find_region_containing({ VirtualAddress(symbol.address), sizeof(FlatPtr) })) {
                    size_t offset = symbol.address - region->vaddr().get();
                    if (auto region_name = region->name(); !region_name.is_null() && !region_name.is_empty())
                        TRY(builder.try_appendff("{:p}  {} + {:#x}\n", (void*)symbol.address, region_name, offset));
                    else
                        TRY(builder.try_appendff("{:p}  {:p} + {:#x}\n", (void*)symbol.address, region->vaddr().as_ptr(), offset));
                } else {
                    TRY(builder.try_appendff("{:p}\n", symbol.address));
                }
                return {};
            }));
        }
        return true;
    }
    unsigned offset = symbol.address - symbol.symbol->address;
    if (symbol.symbol->address == g_highest_kernel_symbol_address && offset > 4096)
        TRY(builder.try_appendff("{:p}\n", (void*)(mask_kernel_addresses ? 0xdeadc0de : symbol.address)));
    else
        TRY(builder.try_appendff("{:p}  {} + {:#x}\n", (void*)(mask_kernel_addresses ? 0xdeadc0de : symbol.address), symbol.symbol->name, offset));
    return true;
}

ErrorOr<NonnullOwnPtr<KString>> Thread::backtrace()
{
    Vector<RecognizedSymbol, 128> recognized_symbols;

    auto& process = const_cast<Process&>(this->process());
    auto stack_trace = TRY(Processor::capture_stack_trace(*this));
    VERIFY(!g_scheduler_lock.is_locked_by_current_processor());
    ScopedAddressSpaceSwitcher switcher(process);
    for (auto& frame : stack_trace) {
        if (Memory::is_user_range(VirtualAddress(frame), sizeof(FlatPtr) * 2)) {
            TRY(recognized_symbols.try_append({ frame }));
        } else {
            TRY(recognized_symbols.try_append({ frame, symbolicate_kernel_address(frame) }));
        }
    }

    StringBuilder builder;
    for (auto& symbol : recognized_symbols) {
        if (!TRY(symbolicate(symbol, process, builder)))
            break;
    }
    return KString::try_create(builder.string_view());
}

void Thread::print_backtrace()
{
    auto trace_or_error = this->backtrace();
    if (!trace_or_error.is_error()) {
        auto trace = trace_or_error.release_value();
        dbgln("Backtrace:");
        kernelputstr(trace->characters(), trace->length());
    }
}

RefPtr<Thread> Thread::from_tid_in_same_process_list(ThreadID tid)
{
    return Thread::all_instances().with([&](auto& list) -> RefPtr<Thread> {
        return Process::current().m_scoped_process_list.with([&](auto const& list_ptr) -> RefPtr<Thread> {
            if (list_ptr) {
                for (Thread& thread : list) {
                    if (thread.tid() == tid) {
                        return thread.process().m_scoped_process_list.with([list_ptr, &thread](auto const& other_thread_process_list) -> RefPtr<Thread> {
                            if (list_ptr.ptr() != other_thread_process_list.ptr())
                                return nullptr;
                            return thread;
                        });
                    }
                }
            }
            for (Thread& thread : list) {
                if (thread.tid() == tid) {
                    return thread;
                }
            }
            return {};
        });
    });
}

RefPtr<Thread> Thread::from_tid_ignoring_process_lists(ThreadID tid)
{
    return Thread::all_instances().with([&](auto& list) -> RefPtr<Thread> {
        for (Thread& thread : list) {
            if (thread.tid() == tid)
                return thread;
        }
        return nullptr;
    });
}

void Thread::reset_fpu_state()
{
    memcpy(&m_fpu_state, &Processor::clean_fpu_state(), sizeof(FPUState));
}

bool Thread::should_be_stopped() const
{
    return process().is_stopped();
}

void Thread::track_lock_acquire(LockRank rank)
{
    // Nothing to do for locks without a rank.
    if (rank == LockRank::None)
        return;

    if (m_lock_rank_mask != LockRank::None) {
        // Verify we are only attempting to take a lock of a higher rank.
        VERIFY(m_lock_rank_mask > rank);
    }

    m_lock_rank_mask |= rank;
}

void Thread::track_lock_release(LockRank rank)
{
    // Nothing to do for locks without a rank.
    if (rank == LockRank::None)
        return;

    // The rank value from the caller should only contain a single bit, otherwise
    // we are disabling the tracking for multiple locks at once which will corrupt
    // the lock tracking mask, and we will assert somewhere else.
    auto rank_is_a_single_bit = [](auto rank_enum) -> bool {
        auto rank = to_underlying(rank_enum);
        auto rank_without_least_significant_bit = rank - 1;
        return (rank & rank_without_least_significant_bit) == 0;
    };

    // We can't release locks out of order, as that would violate the ranking.
    // This is validated by toggling the least significant bit of the mask, and
    // then bit wise or-ing the rank we are trying to release with the resulting
    // mask. If the rank we are releasing is truly the highest rank then the mask
    // we get back will be equal to the current mask stored on the thread.
    auto rank_is_in_order = [](auto mask_enum, auto rank_enum) -> bool {
        auto mask = to_underlying(mask_enum);
        auto rank = to_underlying(rank_enum);
        auto mask_without_least_significant_bit = mask - 1;
        return ((mask & mask_without_least_significant_bit) | rank) == mask;
    };

    VERIFY(has_flag(m_lock_rank_mask, rank));
    VERIFY(rank_is_a_single_bit(rank));
    VERIFY(rank_is_in_order(m_lock_rank_mask, rank));

    m_lock_rank_mask ^= rank;
}

void Thread::set_name(StringView name)
{
    m_name.with([name](auto& thread_name) {
        thread_name.store_characters(name);
    });
}

}

ErrorOr<void> AK::Formatter<Kernel::Thread>::format(FormatBuilder& builder, Kernel::Thread const& value)
{
    return value.process().name().with([&](auto& thread_name) {
        return AK::Formatter<FormatString>::format(
            builder,
            "{}({}:{})"sv, thread_name.representable_view(), value.pid().value(), value.tid().value());
    });
}
