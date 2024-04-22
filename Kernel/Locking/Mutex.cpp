/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SetOnce.h>
#include <Kernel/Debug.h>
#include <Kernel/KSyms.h>
#include <Kernel/Locking/LockLocation.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Tasks/Thread.h>

extern SetOnce g_not_in_early_boot;

namespace Kernel {

void Mutex::lock(Mode mode, [[maybe_unused]] LockLocation const& location)
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    VERIFY(!Processor::current_in_irq());
    if constexpr (LOCK_IN_CRITICAL_DEBUG) {
        // There are no interrupts enabled in early boot.
        if (g_not_in_early_boot.was_set())
            VERIFY_INTERRUPTS_ENABLED();
    }
    VERIFY(mode != Mode::Unlocked);
    auto* current_thread = Thread::current();

    SpinlockLocker lock(m_lock);
    bool did_block = false;
    Mode current_mode = m_mode;
    switch (current_mode) {
    case Mode::Unlocked: {
        dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ ({}) {}: acquire {}, currently unlocked", this, m_name, mode_to_string(mode));
        m_mode = mode;
        VERIFY(!m_holder);
        VERIFY(m_shared_holders == 0);
        if (mode == Mode::Exclusive) {
            m_holder = bit_cast<uintptr_t>(current_thread);
        } else {
            VERIFY(mode == Mode::Shared);
            ++m_shared_holders;
#if LOCK_SHARED_UPGRADE_DEBUG
            m_shared_holders_map.set(bit_cast<uintptr_t>(current_thread), 1);
#endif
        }
        VERIFY(m_times_locked == 0);
        m_times_locked++;

#if LOCK_DEBUG
        if (current_thread) {
            current_thread->holding_lock(*this, 1, location);
        }
#endif
        return;
    }
    case Mode::Exclusive: {
        VERIFY(m_holder);
        if (m_holder != bit_cast<uintptr_t>(current_thread)) {
            block(*current_thread, mode, lock, 1);
            did_block = true;
            // If we blocked then m_mode should have been updated to what we requested
            VERIFY(m_mode == mode);
        }

        if (m_mode == Mode::Exclusive) {
            VERIFY(m_holder == bit_cast<uintptr_t>(current_thread));
            VERIFY(m_shared_holders == 0);
        } else if (did_block && mode == Mode::Shared) {
            // Only if we blocked trying to acquire a shared lock the lock would have been converted
            VERIFY(!m_holder);
            VERIFY(m_shared_holders > 0);
        }

        if constexpr (LOCK_TRACE_DEBUG) {
            if (mode == Mode::Exclusive)
                dbgln("Mutex::lock @ {} ({}): acquire {}, currently exclusive, holding: {}", this, m_name, mode_to_string(mode), m_times_locked);
            else
                dbgln("Mutex::lock @ {} ({}): acquire exclusive (requested {}), currently exclusive, holding: {}", this, m_name, mode_to_string(mode), m_times_locked);
        }

        VERIFY(m_times_locked > 0);
        if (!did_block) {
            // if we didn't block we must still be an exclusive lock
            VERIFY(m_mode == Mode::Exclusive);
            m_times_locked++;
        }

#if LOCK_DEBUG
        current_thread->holding_lock(*this, 1, location);
#endif
        return;
    }
    case Mode::Shared: {
        VERIFY(m_behavior == MutexBehavior::Regular);
        VERIFY(!m_holder);
        if (mode == Mode::Exclusive) {
            dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ {} ({}): blocking for exclusive access, currently shared, locks held {}", this, m_name, m_times_locked);
#if LOCK_SHARED_UPGRADE_DEBUG
            VERIFY(m_shared_holders_map.size() != 1 || m_shared_holders_map.begin()->key != bit_cast<uintptr_t>(current_thread));
#endif
            // WARNING: The following block will deadlock if the current thread is the only shared locker of this Mutex
            // and is asking to upgrade the lock to be exclusive without first releasing the shared lock. We have no
            // allocation-free way to detect such a scenario, so if you suspect that this is the cause of your deadlock,
            // try turning on LOCK_SHARED_UPGRADE_DEBUG.
            block(*current_thread, mode, lock, 1);
            did_block = true;
            VERIFY(m_mode == mode);
        }

        dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ {} ({}): acquire {}, currently shared, locks held {}", this, m_name, mode_to_string(mode), m_times_locked);

        VERIFY(m_times_locked > 0);
        if (m_mode == Mode::Shared) {
            VERIFY(!m_holder);
            VERIFY(!did_block);
        } else if (did_block) {
            VERIFY(mode == Mode::Exclusive);
            VERIFY(m_holder == bit_cast<uintptr_t>(current_thread));
            VERIFY(m_shared_holders == 0);
        }

        if (!did_block) {
            // if we didn't block we must still be a shared lock
            VERIFY(m_mode == Mode::Shared);
            m_times_locked++;
            VERIFY(m_shared_holders > 0);
            ++m_shared_holders;
#if LOCK_SHARED_UPGRADE_DEBUG
            m_shared_holders_map.ensure(bit_cast<uintptr_t>(current_thread), [] { return 0; })++;
#endif
        }

#if LOCK_DEBUG
        current_thread->holding_lock(*this, 1, location);
#endif
        return;
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

void Mutex::unlock()
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    VERIFY(!Processor::current_in_irq());
    if constexpr (LOCK_IN_CRITICAL_DEBUG) {
        // There are no interrupts enabled in early boot.
        if (g_not_in_early_boot.was_set())
            VERIFY_INTERRUPTS_ENABLED();
    }
    auto* current_thread = Thread::current();
    SpinlockLocker lock(m_lock);
    Mode current_mode = m_mode;
    if constexpr (LOCK_TRACE_DEBUG) {
        if (current_mode == Mode::Shared)
            dbgln("Mutex::unlock @ {} ({}): release {}, locks held: {}", this, m_name, mode_to_string(current_mode), m_times_locked);
        else
            dbgln("Mutex::unlock @ {} ({}): release {}, holding: {}", this, m_name, mode_to_string(current_mode), m_times_locked);
    }

    VERIFY(current_mode != Mode::Unlocked);

    VERIFY(m_times_locked > 0);
    m_times_locked--;

    switch (current_mode) {
    case Mode::Exclusive:
        VERIFY(m_holder == bit_cast<uintptr_t>(current_thread));
        VERIFY(m_shared_holders == 0);
        if (m_times_locked == 0)
            m_holder = 0;
        break;
    case Mode::Shared: {
        VERIFY(!m_holder);
        VERIFY(m_shared_holders > 0);
        --m_shared_holders;
#if LOCK_SHARED_UPGRADE_DEBUG
        auto it = m_shared_holders_map.find(bit_cast<uintptr_t>(current_thread));
        if (it->value > 1)
            it->value--;
        else
            m_shared_holders_map.remove(it);
#endif
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

#if LOCK_DEBUG
    if (current_thread) {
        current_thread->holding_lock(*this, -1, {});
    }
#endif

    if (m_times_locked == 0) {
        VERIFY(current_mode == Mode::Exclusive ? !m_holder : m_shared_holders == 0);

        m_mode = Mode::Unlocked;
        unblock_waiters(current_mode);
    }
}

void Mutex::block(Thread& current_thread, Mode mode, SpinlockLocker<Spinlock<LockRank::None>>& lock, u32 requested_locks)
{
    if constexpr (LOCK_IN_CRITICAL_DEBUG) {
        // There are no interrupts enabled in early boot.
        if (g_not_in_early_boot.was_set())
            VERIFY_INTERRUPTS_ENABLED();
    }
    m_blocked_thread_lists.with([&](auto& lists) {
        auto append_to_list = [&]<typename L>(L& list) {
            VERIFY(!list.contains(current_thread));
            list.append(current_thread);
        };

        if (m_behavior == MutexBehavior::BigLock)
            append_to_list(lists.exclusive_big_lock);
        else
            append_to_list(lists.list_for_mode(mode));
    });

    dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ {} ({}) waiting...", this, m_name);
    current_thread.block(*this, lock, requested_locks);
    dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ {} ({}) waited", this, m_name);

    m_blocked_thread_lists.with([&](auto& lists) {
        auto remove_from_list = [&]<typename L>(L& list) {
            VERIFY(list.contains(current_thread));
            list.remove(current_thread);
        };

        if (m_behavior == MutexBehavior::BigLock)
            remove_from_list(lists.exclusive_big_lock);
        else
            remove_from_list(lists.list_for_mode(mode));
    });
}

void Mutex::unblock_waiters(Mode previous_mode)
{
    VERIFY(m_times_locked == 0);
    VERIFY(m_mode == Mode::Unlocked);

    m_blocked_thread_lists.with([&](auto& lists) {
        auto unblock_shared = [&]() {
            if (lists.shared.is_empty())
                return false;
            VERIFY(m_behavior == MutexBehavior::Regular);
            m_mode = Mode::Shared;
            for (auto& thread : lists.shared) {
                auto requested_locks = thread.unblock_from_mutex(*this);
                m_shared_holders += requested_locks;
#if LOCK_SHARED_UPGRADE_DEBUG
                auto set_result = m_shared_holders_map.set(bit_cast<uintptr_t>(&thread), requested_locks);
                VERIFY(set_result == AK::HashSetResult::InsertedNewEntry);
#endif
                m_times_locked += requested_locks;
            }
            return true;
        };
        auto unblock_exclusive = [&]<typename L>(L& list) {
            if (auto* next_exclusive_thread = list.first()) {
                m_mode = Mode::Exclusive;
                m_times_locked = next_exclusive_thread->unblock_from_mutex(*this);
                m_holder = bit_cast<uintptr_t>(next_exclusive_thread);
                return true;
            }
            return false;
        };

        if (m_behavior == MutexBehavior::BigLock) {
            unblock_exclusive(lists.exclusive_big_lock);
        } else if (previous_mode == Mode::Exclusive) {
            if (!unblock_shared())
                unblock_exclusive(lists.exclusive);
        } else {
            if (!unblock_exclusive(lists.exclusive))
                unblock_shared();
        }
    });
}

auto Mutex::force_unlock_exclusive_if_locked(u32& lock_count_to_restore) -> Mode
{
    VERIFY(m_behavior == MutexBehavior::BigLock);
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    VERIFY(!Processor::current_in_irq());

    auto* current_thread = Thread::current();
    SpinlockLocker lock(m_lock);
    auto current_mode = m_mode;
    switch (current_mode) {
    case Mode::Exclusive: {
        if (m_holder != bit_cast<uintptr_t>(current_thread)) {
            lock_count_to_restore = 0;
            return Mode::Unlocked;
        }

        dbgln_if(LOCK_RESTORE_DEBUG, "Mutex::force_unlock_exclusive_if_locked @ {}: unlocking exclusive with lock count: {}", this, m_times_locked);
#if LOCK_DEBUG
        current_thread->holding_lock(*this, -(int)m_times_locked, {});
#endif
        m_holder = 0;
        VERIFY(m_times_locked > 0);
        lock_count_to_restore = m_times_locked;
        m_times_locked = 0;
        m_mode = Mode::Unlocked;
        unblock_waiters(Mode::Exclusive);
        break;
    }
    case Mode::Unlocked: {
        lock_count_to_restore = 0;
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
    return current_mode;
}

void Mutex::restore_exclusive_lock(u32 lock_count, [[maybe_unused]] LockLocation const& location)
{
    VERIFY(m_behavior == MutexBehavior::BigLock);
    VERIFY(lock_count > 0);
    VERIFY(!Processor::current_in_irq());

    auto* current_thread = Thread::current();
    bool did_block = false;
    SpinlockLocker lock(m_lock);
    [[maybe_unused]] auto previous_mode = m_mode;
    if (m_mode == Mode::Exclusive && m_holder != bit_cast<uintptr_t>(current_thread)) {
        block(*current_thread, Mode::Exclusive, lock, lock_count);
        did_block = true;
        // If we blocked then m_mode should have been updated to what we requested
        VERIFY(m_mode == Mode::Exclusive);
    }

    dbgln_if(LOCK_RESTORE_DEBUG, "Mutex::restore_exclusive_lock @ {}: restoring exclusive with lock count {}, was {}", this, lock_count, mode_to_string(previous_mode));

    VERIFY(m_mode != Mode::Shared);
    VERIFY(m_shared_holders == 0);
    if (did_block) {
        VERIFY(m_times_locked > 0);
        VERIFY(m_holder == bit_cast<uintptr_t>(current_thread));
    } else {
        if (m_mode == Mode::Unlocked) {
            m_mode = Mode::Exclusive;
            VERIFY(m_times_locked == 0);
            m_times_locked = lock_count;
            VERIFY(!m_holder);
            m_holder = bit_cast<uintptr_t>(current_thread);
        } else {
            VERIFY(m_mode == Mode::Exclusive);
            VERIFY(m_holder == bit_cast<uintptr_t>(current_thread));
            VERIFY(m_times_locked > 0);
            m_times_locked += lock_count;
        }
    }

#if LOCK_DEBUG
    current_thread->holding_lock(*this, (int)lock_count, location);
#endif
}

}
