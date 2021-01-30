/*
 * Copyright (c) 2021, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SourceLocation.h>
#include <AK/TemporaryChange.h>
#include <Kernel/Debug.h>
#include <Kernel/KSyms.h>
#include <Kernel/Lock.h>
#include <Kernel/TimerQueue.h>
#include <Kernel/Thread.h>

#if MEASURE_LOCK_TIME
#define EXCESSIVE_LOCK_ACQUIRE_TIME 1000
#define EXCESSIVE_LOCK_HOLD_TIME 500
#define EXCESSIVE_LOCK_HOLD_TIME_SUBSEQUENT 10000
#endif

namespace Kernel {

constexpr bool measure_acquire_time = requires(const Thread::LockBlocker& b) {
    b.acquire_start_time();
};

#if MEASURE_LOCK_TIME
void Lock::start_locked_timer()
{
    VERIFY(!m_lock_hold_timer);
    m_lock_hold_start_time = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    set_hold_timer(m_lock_hold_start_time + Time::from_milliseconds(EXCESSIVE_LOCK_HOLD_TIME));
}

void Lock::set_hold_timer(Time deadline)
{
    m_lock_hold_timer = TimerQueue::the().add_timer_without_id(CLOCK_MONOTONIC, deadline, [this]() {
        ScopedSpinLock lock(m_lock);
        auto now = TimeManagement::the().monotonic_time(TimePrecision::Precise);
        auto duration = now - m_lock_hold_start_time;
        if (m_mode == Mode::Exclusive) {
            dbgln("Lock @ {:p} ({}) has been held excessively long ({}ms) by thread {} with {} exclusive locks", this, m_name ? m_name : "null", duration.to_milliseconds(), *m_holder, m_times_locked);
            dbgln("{}", m_holder->backtrace());
        } else {
            dbgln("Lock @ {:p} ({}) has been held excessively long ({}ms, {} shared locks by {} threads)", this, m_name ? m_name : "null", duration.to_milliseconds(), m_times_locked, m_shared_holders.size());
            for (auto& it : m_shared_holders) {
                auto& thread = *it.key;
                dbgln("  - {} has {} locks", thread, it.value);
                dbgln("{}", thread.backtrace());
            }
            do_unblock([&](Thread::Blocker& b, void* data, bool&) {
                VERIFY(data);
                VERIFY(b.blocker_type() == Thread::Blocker::Type::Lock);
                auto& thread = *(Thread*)data;
                dbgln("  - Waiting on lock: {}", thread);
                dbgln("{}", thread.backtrace());
                return false;
            });
        }
        set_hold_timer(now + Time::from_milliseconds(EXCESSIVE_LOCK_HOLD_TIME_SUBSEQUENT));
    });
}

void Lock::stop_locked_timer()
{
    if (m_lock_hold_timer)
        TimerQueue::the().cancel_timer(m_lock_hold_timer.release_nonnull());
}
#endif

bool Lock::should_add_blocker(Thread::Blocker& b, void* data)
{
    VERIFY(data != nullptr); // Thread that is requesting to be blocked
    VERIFY(m_lock.is_locked());
    VERIFY(b.blocker_type() == Thread::Blocker::Type::Lock);
    auto& blocker = static_cast<Thread::LockBlocker&>(b);
    auto& thread = *(Thread*)data;
    auto requested_mode = blocker.requested_mode();
    auto requested_locks = blocker.requested_locks();
    VERIFY(requested_locks > 0);
    switch (requested_mode) {
    case Mode::Unlocked:
        VERIFY_NOT_REACHED();
    case Mode::Exclusive:
        switch (m_mode) {
        case Mode::Unlocked:
            VERIFY(!m_holder);
            VERIFY(m_shared_holders.is_empty());
            m_mode = Mode::Exclusive;
            m_holder = thread;
            dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock (exclusive) acquired without blocking (was unlocked)", this, m_name ? m_name : "null");
            m_times_locked = requested_locks;
#if MEASURE_LOCK_TIME
            start_locked_timer();
#endif
            return false;
        case Mode::Shared:
            dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock (exclusive) blocking, currently shared by {} others ({} threads)", this, m_name ? m_name : "null", m_times_locked, m_shared_holders.size());
            if constexpr(measure_acquire_time) {
                blocker.set_acquire_start_time(TimeManagement::the().monotonic_time(TimePrecision::Precise));
            }
            return true;
        case Mode::Exclusive:
            VERIFY(m_holder);
            VERIFY(m_shared_holders.is_empty());
            if (m_holder.ptr() != &thread) {
                dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock (exclusive) blocking, currently owned exclusively by {} with {} locks", this, m_name ? m_name : "null", *m_holder, m_times_locked);
                if constexpr(measure_acquire_time) {
                    blocker.set_acquire_start_time(TimeManagement::the().monotonic_time(TimePrecision::Precise));
                }
                return true;
            }
            dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock (exclusive) acquired without blocking (already had {} locks)", this, m_name ? m_name : "null", m_times_locked);
            m_times_locked += requested_locks;
            return false;
        }
        break;
    case Mode::Shared:
        switch (m_mode) {
        case Mode::Unlocked: {
            VERIFY(!m_holder);
            VERIFY(m_shared_holders.is_empty());
            m_mode = Mode::Shared;
            auto result = m_shared_holders.set(&thread, requested_locks);
            VERIFY(result == AK::HashSetResult::InsertedNewEntry);
            dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock (shared) acquired without blocking (was unlocked)", this, m_name ? m_name : "null");
            m_times_locked = requested_locks;
#if MEASURE_LOCK_TIME
            start_locked_timer();
#endif
            return false;
        }
        case Mode::Shared: {
            VERIFY(!m_holder);
            auto it = m_shared_holders.find(&thread);
            if (it != m_shared_holders.end()) {
                dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock (shared) acquired without blocking (already had {} locks, {} locks by {} other threads)", this, m_name ? m_name : "null", it->value, m_times_locked - it->value, m_shared_holders.size() - 1);
                it->value++;
            } else {
                dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock (shared) acquired without blocking ({} locks by {} threads)", this, m_name ? m_name : "null", m_times_locked, m_shared_holders.size());
                auto result = m_shared_holders.set(&thread, requested_locks);
                VERIFY(result == AK::HashSetResult::InsertedNewEntry);
            }

            m_times_locked += requested_locks;
            return false;
        }
        case Mode::Exclusive:
            VERIFY(m_holder);
            VERIFY(m_shared_holders.is_empty());
            if (m_holder.ptr() == &thread) {
                dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock (shared->exclusive) acquired without blocking, already held exclusively with {} locks", this, m_name ? m_name : "null", m_times_locked);
                m_times_locked += requested_locks;
                return false;
            }
            dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock (shared) blocking, currently held exclusively by {} with {} locks", this, m_name ? m_name : "null", *m_holder, m_times_locked);
            if constexpr(measure_acquire_time) {
                blocker.set_acquire_start_time(TimeManagement::the().monotonic_time(TimePrecision::Precise));
            }
            return true;
        }
    }
    VERIFY_NOT_REACHED();
}

#if LOCK_DEBUG
void Lock::lock(Mode mode, const SourceLocation& location)
{
    VERIFY(!Processor::in_irq());
    dbgln_if(LOCK_TRACE_DEBUG, "{}:{}: Lock @ {:p} ({}) lock ({})...", location.filename(), location.line_number(), this, m_name ? m_name : "null", mode_to_string(mode));
    auto block_result = Thread::current()->block<Thread::LockBlocker>({}, *this, mode, 1u, nullptr, location);
    VERIFY(!block_result.was_interrupted());
    dbgln_if(LOCK_TRACE_DEBUG, "{}:{}: Lock @ {:p} ({}) lock ({}) acquired", location.filename(), location.line_number(), this, m_name ? m_name : "null", mode_to_string(mode));
}
#else
void Lock::lock(Mode mode)
{
    VERIFY(!Processor::in_irq());
    dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock ({})...", this, m_name ? m_name : "null", mode_to_string(mode));
    auto block_result = Thread::current()->block<Thread::LockBlocker>({}, *this, mode, 1u);
    
    dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) lock ({}) acquired", this, m_name ? m_name : "null", mode_to_string(mode));
    VERIFY(!block_result.was_interrupted());
}
#endif

void Lock::did_unlock()
{
    VERIFY(m_mode == Mode::Unlocked);
#if MEASURE_LOCK_TIME
    stop_locked_timer();
#endif
    if (do_unblock([&](Thread::Blocker& b, void* data, bool& stop_iterating) {
        VERIFY(data);
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Lock);
        auto& blocker = static_cast<Thread::LockBlocker&>(b);
        if (m_mode == Mode::Unlocked)
            m_mode = blocker.requested_mode();
        else if (blocker.requested_mode() != m_mode)
            return false;
        auto requested_locks = blocker.requested_locks();
        if (!blocker.unblock())
            return false;
        auto& thread = *(Thread*)data;
        if (m_mode == Mode::Exclusive) {
            m_holder = thread;
            stop_iterating = true;
        } else {
            auto result = m_shared_holders.set(&thread, requested_locks);
            VERIFY(result == AK::HashSetResult::InsertedNewEntry);
        }
        m_times_locked += requested_locks;
        if constexpr(measure_acquire_time) {
            auto elapsed_ms = (TimeManagement::the().monotonic_time(TimePrecision::Precise) - blocker.acquire_start_time()).to_milliseconds();
            if (elapsed_ms >= EXCESSIVE_LOCK_ACQUIRE_TIME)
                dbgln("Lock @ {:p} ({}) unblocking thread {} after {}ms which now has {} locks ({})", this, m_name ? m_name : "null", thread, elapsed_ms, requested_locks, mode_to_string(m_mode));
            else
                dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) unblocking thread {} after {}ms which now has {} locks ({})", this, m_name ? m_name : "null", thread, elapsed_ms, requested_locks, mode_to_string(m_mode));
        } else {
            dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) unblocking thread {} which now has {} locks ({})", this, m_name ? m_name : "null", thread, requested_locks, mode_to_string(m_mode));
        }
        return true;
    })) {
#if MEASURE_LOCK_TIME
        start_locked_timer();
#endif
    }
}

void Lock::unlock()
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    VERIFY(!Processor::in_irq());
    auto current_thread = Thread::current();

    ScopedSpinLock lock(m_lock);
    VERIFY(m_times_locked > 0);
    m_times_locked--;
    Mode current_mode = m_mode;
    switch (current_mode) {
    case Mode::Exclusive:
        VERIFY(m_holder == current_thread);
        VERIFY(m_shared_holders.is_empty());
        if (m_times_locked == 0)
            m_holder = nullptr;
        break;
    case Mode::Shared: {
        VERIFY(!m_holder);
        auto it = m_shared_holders.find(current_thread);
        VERIFY(it != m_shared_holders.end());
        if (it->value > 1) {
            it->value--;
        } else {
            VERIFY(it->value > 0);
            m_shared_holders.remove(it);
        }
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
    if (m_times_locked == 0) {
        m_mode = Mode::Unlocked;
        VERIFY(!m_holder);
        VERIFY(m_shared_holders.is_empty());
        dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) unlocked ({})", this, m_name ? m_name : "null", mode_to_string(current_mode));
        did_unlock();
    }
}

auto Lock::force_unlock_if_locked(u32& lock_count_to_restore) -> Mode
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    VERIFY(!Processor::in_irq());
    auto current_thread = Thread::current();

    ScopedSpinLock lock(m_lock);
    switch (m_mode) {
    case Mode::Unlocked:
        VERIFY(!m_holder);
        VERIFY(m_shared_holders.is_empty());
        VERIFY(m_times_locked == 0);
        lock_count_to_restore = 0;
        return Mode::Unlocked;
    case Mode::Exclusive:
        VERIFY(m_holder);
        VERIFY(m_shared_holders.is_empty());
        VERIFY(m_times_locked > 0);
        if (m_holder != current_thread) {
            lock_count_to_restore = 0;
            return Mode::Unlocked;
        }
        dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) force unlock of {} exclusive locks", this, m_name ? m_name : "null", m_times_locked);
        lock_count_to_restore = m_times_locked;
        m_holder = nullptr;
        m_times_locked = 0;
        m_mode = Mode::Unlocked;
        did_unlock();
        return Mode::Exclusive;
    case Mode::Shared: {
        VERIFY(!m_holder);
        VERIFY(!m_shared_holders.is_empty());
        VERIFY(m_times_locked > 0);
        auto it = m_shared_holders.find(current_thread);
        if (it == m_shared_holders.end()) {
            lock_count_to_restore = 0;
            return Mode::Unlocked;
        }
        VERIFY(it->value > 0);
        lock_count_to_restore = it->value;
        VERIFY(m_times_locked >= it->value);
        m_times_locked -= it->value;
        dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) force unlock of {} shared locks, remaining {} by {} threads", this, m_name ? m_name : "null", it->value, m_times_locked, m_shared_holders.size() - 1);
        m_shared_holders.remove(it);
        if (m_times_locked == 0) {
            VERIFY(m_shared_holders.is_empty());
            m_mode = Mode::Unlocked;
            did_unlock();
        } else {
            VERIFY(!m_shared_holders.is_empty());
        }
        return Mode::Shared;
    }
    }
    VERIFY_NOT_REACHED();
}

#if LOCK_DEBUG
void Lock::restore_lock(Mode mode, u32 lock_count, const SourceLocation& location)
{
    VERIFY(mode != Mode::Unlocked);
    VERIFY(lock_count > 0);
    VERIFY(!Processor::in_irq());
    dbgln_if(LOCK_TRACE_DEBUG, "{}:{}: Lock @ {:p} ({}) restore lock ({})...", location.filename(), location.line_number(), this, m_name ? m_name : "null", mode_to_string(mode));
    auto block_result = Thread::current()->block<Thread::LockBlocker>({}, *this, mode, lock_count, nullptr, location);
    dbgln_if(LOCK_TRACE_DEBUG, "{}:{}: Lock @ {:p} ({}) restore lock ({}) acquired", location.filename(), location.line_number(), this, m_name ? m_name : "null", mode_to_string(mode));
    VERIFY(!block_result.was_interrupted());
}
#else
void Lock::restore_lock(Mode mode, u32 lock_count)
{
    VERIFY(mode != Mode::Unlocked);
    VERIFY(lock_count > 0);
    VERIFY(!Processor::in_irq());

    dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) restore lock ({})...", this, m_name ? m_name : "null", mode_to_string(mode));
    auto block_result = Thread::current()->block<Thread::LockBlocker>({}, *this, mode, lock_count);
    dbgln_if(LOCK_TRACE_DEBUG, "Lock @ {:p} ({}) restore lock ({}) acquired", this, m_name ? m_name : "null", mode_to_string(mode));
    VERIFY(!block_result.was_interrupted());
}
#endif

void Lock::clear_waiters()
{
    ScopedSpinLock lock(m_lock);
    if (m_mode == Mode::Unlocked)
        return;
    VERIFY(m_mode != Mode::Shared);
    do_unblock([&](Thread::Blocker& b, void* data, bool&) {
        VERIFY(data);
        VERIFY(b.blocker_type() == Thread::Blocker::Type::Lock);
        return true;
    });
}

}
