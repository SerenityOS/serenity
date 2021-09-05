/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/KSyms.h>
#include <Kernel/Locking/LockLocation.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Thread.h>

namespace Kernel {

void Mutex::lock(Mode mode, [[maybe_unused]] LockLocation const& location)
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    VERIFY(!Processor::current_in_irq());
    if constexpr (LOCK_IN_CRITICAL_DEBUG)
        VERIFY_INTERRUPTS_ENABLED();
    VERIFY(mode != Mode::Unlocked);
    auto current_thread = Thread::current();

    SpinlockLocker lock(m_lock);
    bool did_block = false;
    Mode current_mode = m_mode;
    switch (current_mode) {
    case Mode::Unlocked: {
        dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ ({}) {}: acquire {}, currently unlocked", this, m_name, mode_to_string(mode));
        m_mode = mode;
        VERIFY(!m_holder);
        VERIFY(m_shared_holders.is_empty());
        if (mode == Mode::Exclusive) {
            m_holder = current_thread;
        } else {
            VERIFY(mode == Mode::Shared);
            m_shared_holders.set(current_thread, 1);
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
        if (m_holder != current_thread) {
            block(*current_thread, mode, lock, 1);
            did_block = true;
            // If we blocked then m_mode should have been updated to what we requested
            VERIFY(m_mode == mode);
        }

        if (m_mode == Mode::Exclusive) {
            VERIFY(m_holder == current_thread);
            VERIFY(m_shared_holders.is_empty());
        } else if (did_block && mode == Mode::Shared) {
            // Only if we blocked trying to acquire a shared lock the lock would have been converted
            VERIFY(!m_holder);
            VERIFY(!m_shared_holders.is_empty());
            VERIFY(m_shared_holders.find(current_thread) != m_shared_holders.end());
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
        VERIFY(!m_holder);
        if (mode == Mode::Exclusive) {
            if (m_shared_holders.size() == 1) {
                auto it = m_shared_holders.begin();
                if (it->key == current_thread) {
                    it->value++;
                    m_times_locked++;
                    m_mode = Mode::Exclusive;
                    m_holder = current_thread;
                    m_shared_holders.clear();
                    dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ {} ({}): acquire {}, converted shared to exclusive lock, locks held {}", this, m_name, mode_to_string(mode), m_times_locked);
                    return;
                }
            }

            block(*current_thread, mode, lock, 1);
            did_block = true;
            VERIFY(m_mode == mode);
        }

        dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ {} ({}): acquire {}, currently shared, locks held {}", this, m_name, mode_to_string(mode), m_times_locked);

        VERIFY(m_times_locked > 0);
        if (m_mode == Mode::Shared) {
            VERIFY(!m_holder);
            VERIFY(!did_block || m_shared_holders.contains(current_thread));
        } else if (did_block) {
            VERIFY(mode == Mode::Exclusive);
            VERIFY(m_holder == current_thread);
            VERIFY(m_shared_holders.is_empty());
        }

        if (!did_block) {
            // if we didn't block we must still be a shared lock
            VERIFY(m_mode == Mode::Shared);
            m_times_locked++;
            VERIFY(!m_shared_holders.is_empty());
            auto it = m_shared_holders.find(current_thread);
            if (it != m_shared_holders.end())
                it->value++;
            else
                m_shared_holders.set(current_thread, 1);
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
    if constexpr (LOCK_IN_CRITICAL_DEBUG)
        VERIFY_INTERRUPTS_ENABLED();
    VERIFY(!Processor::current_in_irq());
    auto current_thread = Thread::current();
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

#if LOCK_DEBUG
    if (current_thread) {
        current_thread->holding_lock(*this, -1, {});
    }
#endif

    if (m_times_locked == 0) {
        VERIFY(current_mode == Mode::Exclusive ? !m_holder : m_shared_holders.is_empty());

        m_mode = Mode::Unlocked;
        unblock_waiters(current_mode);
    }
}

void Mutex::block(Thread& current_thread, Mode mode, SpinlockLocker<Spinlock>& lock, u32 requested_locks)
{
    if constexpr (LOCK_IN_CRITICAL_DEBUG)
        VERIFY_INTERRUPTS_ENABLED();
    auto& blocked_thread_list = thread_list_for_mode(mode);
    VERIFY(!blocked_thread_list.contains(current_thread));
    blocked_thread_list.append(current_thread);

    dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ {} ({}) waiting...", this, m_name);
    current_thread.block(*this, lock, requested_locks);
    dbgln_if(LOCK_TRACE_DEBUG, "Mutex::lock @ {} ({}) waited", this, m_name);

    VERIFY(blocked_thread_list.contains(current_thread));
    blocked_thread_list.remove(current_thread);
}

void Mutex::unblock_waiters(Mode previous_mode)
{
    VERIFY(m_times_locked == 0);
    VERIFY(m_mode == Mode::Unlocked);

    if (m_blocked_threads_list_exclusive.is_empty() && m_blocked_threads_list_shared.is_empty())
        return;

    auto unblock_shared = [&]() {
        if (m_blocked_threads_list_shared.is_empty())
            return false;
        m_mode = Mode::Shared;
        for (auto& thread : m_blocked_threads_list_shared) {
            auto requested_locks = thread.unblock_from_lock(*this);
            auto set_result = m_shared_holders.set(&thread, requested_locks);
            VERIFY(set_result == AK::HashSetResult::InsertedNewEntry);
            m_times_locked += requested_locks;
        }
        return true;
    };
    auto unblock_exclusive = [&]() {
        if (auto* next_exclusive_thread = m_blocked_threads_list_exclusive.first()) {
            m_mode = Mode::Exclusive;
            m_times_locked = next_exclusive_thread->unblock_from_lock(*this);
            m_holder = next_exclusive_thread;
            return true;
        }
        return false;
    };

    if (previous_mode == Mode::Exclusive) {
        if (!unblock_shared())
            unblock_exclusive();
    } else {
        if (!unblock_exclusive())
            unblock_shared();
    }
}

auto Mutex::force_unlock_if_locked(u32& lock_count_to_restore) -> Mode
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    VERIFY(!Processor::current_in_irq());
    auto current_thread = Thread::current();
    SpinlockLocker lock(m_lock);
    auto current_mode = m_mode;
    switch (current_mode) {
    case Mode::Exclusive: {
        if (m_holder != current_thread) {
            lock_count_to_restore = 0;
            return Mode::Unlocked;
        }

        dbgln_if(LOCK_RESTORE_DEBUG, "Mutex::force_unlock_if_locked @ {}: unlocking exclusive with lock count: {}", this, m_times_locked);
#if LOCK_DEBUG
        m_holder->holding_lock(*this, -(int)m_times_locked, {});
#endif
        m_holder = nullptr;
        VERIFY(m_times_locked > 0);
        lock_count_to_restore = m_times_locked;
        m_times_locked = 0;
        m_mode = Mode::Unlocked;
        unblock_waiters(Mode::Exclusive);
        break;
    }
    case Mode::Shared: {
        VERIFY(!m_holder);
        auto it = m_shared_holders.find(current_thread);
        if (it == m_shared_holders.end()) {
            lock_count_to_restore = 0;
            return Mode::Unlocked;
        }

        dbgln_if(LOCK_RESTORE_DEBUG, "Mutex::force_unlock_if_locked @ {}: unlocking exclusive with lock count: {}, total locks: {}",
            this, it->value, m_times_locked);

        VERIFY(it->value > 0);
        lock_count_to_restore = it->value;
        VERIFY(lock_count_to_restore > 0);
#if LOCK_DEBUG
        m_holder->holding_lock(*this, -(int)lock_count_to_restore, {});
#endif
        m_shared_holders.remove(it);
        VERIFY(m_times_locked >= lock_count_to_restore);
        m_times_locked -= lock_count_to_restore;
        if (m_times_locked == 0) {
            m_mode = Mode::Unlocked;
            unblock_waiters(Mode::Shared);
        }
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

void Mutex::restore_lock(Mode mode, u32 lock_count, [[maybe_unused]] LockLocation const& location)
{
    VERIFY(mode != Mode::Unlocked);
    VERIFY(lock_count > 0);
    VERIFY(!Processor::current_in_irq());
    auto current_thread = Thread::current();
    bool did_block = false;
    SpinlockLocker lock(m_lock);
    switch (mode) {
    case Mode::Exclusive: {
        auto previous_mode = m_mode;
        bool need_to_block = false;
        if (m_mode == Mode::Exclusive && m_holder != current_thread)
            need_to_block = true;
        else if (m_mode == Mode::Shared && (m_shared_holders.size() != 1 || !m_shared_holders.contains(current_thread)))
            need_to_block = true;
        if (need_to_block) {
            block(*current_thread, Mode::Exclusive, lock, lock_count);
            did_block = true;
            // If we blocked then m_mode should have been updated to what we requested
            VERIFY(m_mode == Mode::Exclusive);
        }

        dbgln_if(LOCK_RESTORE_DEBUG, "Mutex::restore_lock @ {}: restoring {} with lock count {}, was {}", this, mode_to_string(mode), lock_count, mode_to_string(previous_mode));

        VERIFY(m_mode != Mode::Shared);
        VERIFY(m_shared_holders.is_empty());
        if (did_block) {
            VERIFY(m_times_locked > 0);
            VERIFY(m_holder == current_thread);
        } else {
            if (m_mode == Mode::Unlocked) {
                m_mode = Mode::Exclusive;
                VERIFY(m_times_locked == 0);
                m_times_locked = lock_count;
                VERIFY(!m_holder);
                m_holder = current_thread;
            } else if (m_mode == Mode::Shared) {
                // Upgrade the shared lock to an exclusive lock
                VERIFY(!m_holder);
                VERIFY(m_shared_holders.size() == 1);
                VERIFY(m_shared_holders.contains(current_thread));
                m_mode = Mode::Exclusive;
                m_holder = current_thread;
                m_shared_holders.clear();
            } else {
                VERIFY(m_mode == Mode::Exclusive);
                VERIFY(m_holder == current_thread);
                VERIFY(m_times_locked > 0);
                m_times_locked += lock_count;
            }
        }

#if LOCK_DEBUG
        m_holder->holding_lock(*this, (int)lock_count, location);
#endif
        return;
    }
    case Mode::Shared: {
        auto previous_mode = m_mode;
        if (m_mode == Mode::Exclusive && m_holder != current_thread) {
            block(*current_thread, Mode::Shared, lock, lock_count);
            did_block = true;
            // If we blocked then m_mode should have been updated to what we requested
            VERIFY(m_mode == Mode::Shared);
        }

        dbgln_if(LOCK_RESTORE_DEBUG, "Mutex::restore_lock @ {}: restoring {} with lock count {}, was {}",
            this, mode_to_string(mode), lock_count, mode_to_string(previous_mode));

        VERIFY(!m_holder);
        if (did_block) {
            VERIFY(m_times_locked > 0);
            VERIFY(m_shared_holders.contains(current_thread));
        } else {
            if (m_mode == Mode::Unlocked) {
                m_mode = Mode::Shared;
                m_times_locked += lock_count;
                auto set_result = m_shared_holders.set(current_thread, lock_count);
                // There may be other shared lock holders already, but we should not have an entry yet
                VERIFY(set_result == AK::HashSetResult::InsertedNewEntry);
            } else if (m_mode == Mode::Shared) {
                m_times_locked += lock_count;
                if (auto it = m_shared_holders.find(current_thread); it != m_shared_holders.end()) {
                    it->value += lock_count;
                } else {
                    auto set_result = m_shared_holders.set(current_thread, lock_count);
                    // There may be other shared lock holders already, but we should not have an entry yet
                    VERIFY(set_result == AK::HashSetResult::InsertedNewEntry);
                }
            } else {
                VERIFY(m_mode == Mode::Exclusive);
                VERIFY(m_holder == current_thread);
                m_times_locked += lock_count;
            }
        }

#if LOCK_DEBUG
        m_holder->holding_lock(*this, (int)lock_count, location);
#endif
        return;
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

}
