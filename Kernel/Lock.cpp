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

#include <AK/Debug.h>
#include <AK/TemporaryChange.h>
#include <Kernel/KSyms.h>
#include <Kernel/Lock.h>
#include <Kernel/Thread.h>

namespace Kernel {

#if LOCK_DEBUG
void Lock::lock(Mode mode)
{
    lock("unknown", 0, mode);
}

void Lock::lock(const char* file, int line, Mode mode)
#else
void Lock::lock(Mode mode)
#endif
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    ASSERT(!Processor::current().in_irq());
    ASSERT(mode != Mode::Unlocked);
    auto current_thread = Thread::current();
    ScopedCritical critical; // in case we're not in a critical section already
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) == false) {
            do {
                // FIXME: Do not add new readers if writers are queued.
                Mode current_mode = m_mode;
                switch (current_mode) {
                case Mode::Unlocked: {
                    dbgln<LOCK_TRACE_DEBUG>("Lock::lock @ {}: acquire {}, currently unlocked", this, mode_to_string(mode));
                    m_mode = mode;
                    ASSERT(!m_holder);
                    ASSERT(m_shared_holders.is_empty());
                    if (mode == Mode::Exclusive) {
                        m_holder = current_thread;
                    } else {
                        ASSERT(mode == Mode::Shared);
                        m_shared_holders.set(current_thread, 1);
                    }
                    ASSERT(m_times_locked == 0);
                    m_times_locked++;
#if LOCK_DEBUG
                    current_thread->holding_lock(*this, 1, file, line);
#endif
                    m_lock.store(false, AK::memory_order_release);
                    return;
                }
                case Mode::Exclusive: {
                    ASSERT(m_holder);
                    if (m_holder != current_thread)
                        break;
                    ASSERT(m_shared_holders.is_empty());

                    if constexpr (LOCK_TRACE_DEBUG) {
                        if (mode == Mode::Exclusive)
                            dbgln("Lock::lock @ {}: acquire {}, currently exclusive, holding: {}", this, mode_to_string(mode), m_times_locked);
                        else
                            dbgln("Lock::lock @ {}: acquire exclusive (requested {}), currently exclusive, holding: {}", this, mode_to_string(mode), m_times_locked);
                    }

                    ASSERT(mode == Mode::Exclusive || mode == Mode::Shared);
                    ASSERT(m_times_locked > 0);
                    m_times_locked++;
#if LOCK_DEBUG
                    current_thread->holding_lock(*this, 1, file, line);
#endif
                    m_lock.store(false, AK::memory_order_release);
                    return;
                }
                case Mode::Shared: {
                    ASSERT(!m_holder);
                    if (mode != Mode::Shared)
                        break;

                    dbgln<LOCK_TRACE_DEBUG>("Lock::lock @ {}: acquire {}, currently shared, locks held {}", this, mode_to_string(mode), m_times_locked);

                    ASSERT(m_times_locked > 0);
                    m_times_locked++;
                    ASSERT(!m_shared_holders.is_empty());
                    auto it = m_shared_holders.find(current_thread);
                    if (it != m_shared_holders.end())
                        it->value++;
                    else
                        m_shared_holders.set(current_thread, 1);
#if LOCK_DEBUG
                    current_thread->holding_lock(*this, 1, file, line);
#endif
                    m_lock.store(false, AK::memory_order_release);
                    return;
                }
                default:
                    ASSERT_NOT_REACHED();
                }
                m_lock.store(false, AK::memory_order_release);
            } while (m_queue.wait_on({}, m_name) == Thread::BlockResult::NotBlocked);
        } else {
            // I don't know *who* is using "m_lock", so just yield.
            Scheduler::yield_from_critical();
        }
    }
}

void Lock::unlock()
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    ASSERT(!Processor::current().in_irq());
    auto current_thread = Thread::current();
    ScopedCritical critical; // in case we're not in a critical section already
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) == false) {
            Mode current_mode = m_mode;
            if constexpr (LOCK_TRACE_DEBUG) {
                if (current_mode == Mode::Shared)
                    dbgln("Lock::unlock @ {}: release {}, locks held: {}", this, mode_to_string(current_mode), m_times_locked);
                else
                    dbgln("Lock::unlock @ {}: release {}, holding: {}", this, mode_to_string(current_mode), m_times_locked);
            }

            ASSERT(current_mode != Mode::Unlocked);

            ASSERT(m_times_locked > 0);
            m_times_locked--;

            switch (current_mode) {
            case Mode::Exclusive:
                ASSERT(m_holder == current_thread);
                ASSERT(m_shared_holders.is_empty());
                if (m_times_locked == 0)
                    m_holder = nullptr;
                break;
            case Mode::Shared: {
                ASSERT(!m_holder);
                auto it = m_shared_holders.find(current_thread);
                ASSERT(it != m_shared_holders.end());
                if (it->value > 1) {
                    it->value--;
                } else {
                    ASSERT(it->value > 0);
                    m_shared_holders.remove(it);
                }
                break;
            }
            default:
                ASSERT_NOT_REACHED();
            }

            if (m_times_locked == 0) {
                ASSERT(current_mode == Mode::Exclusive ? !m_holder : m_shared_holders.is_empty());
                m_mode = Mode::Unlocked;
            }

#if LOCK_DEBUG
            current_thread->holding_lock(*this, -1);
#endif

            m_lock.store(false, AK::memory_order_release);
            m_queue.wake_one();
            return;
        }
        // I don't know *who* is using "m_lock", so just yield.
        Scheduler::yield_from_critical();
    }
}

auto Lock::force_unlock_if_locked(u32& lock_count_to_restore) -> Mode
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    ASSERT(!Processor::current().in_irq());
    auto current_thread = Thread::current();
    ScopedCritical critical; // in case we're not in a critical section already
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) == false) {
            Mode previous_mode;
            auto current_mode = m_mode.load(AK::MemoryOrder::memory_order_relaxed);
            switch (current_mode) {
            case Mode::Exclusive: {
                if (m_holder != current_thread) {
                    m_lock.store(false, AK::MemoryOrder::memory_order_release);
                    lock_count_to_restore = 0;
                    return Mode::Unlocked;
                }

                dbgln<LOCK_RESTORE_DEBUG>("Lock::force_unlock_if_locked @ {}: unlocking exclusive with lock count: {}", this, m_times_locked);

                m_holder = nullptr;
                ASSERT(m_times_locked > 0);
                lock_count_to_restore = m_times_locked;
                m_times_locked = 0;
                m_mode = Mode::Unlocked;
                m_lock.store(false, AK::memory_order_release);
#if LOCK_DEBUG
                m_holder->holding_lock(*this, -(int)lock_count_to_restore);
#endif
                previous_mode = Mode::Exclusive;
                break;
            }
            case Mode::Shared: {
                ASSERT(!m_holder);
                auto it = m_shared_holders.find(current_thread);
                if (it == m_shared_holders.end()) {
                    m_lock.store(false, AK::MemoryOrder::memory_order_release);
                    lock_count_to_restore = 0;
                    return Mode::Unlocked;
                }

                dbgln<LOCK_RESTORE_DEBUG>("Lock::force_unlock_if_locked @ {}: unlocking exclusive with lock count: {}, total locks: {}",
                    this, it->value, m_times_locked);

                ASSERT(it->value > 0);
                lock_count_to_restore = it->value;
                ASSERT(lock_count_to_restore > 0);
#if LOCK_DEBUG
                m_holder->holding_lock(*this, -(int)lock_count_to_restore);
#endif
                m_shared_holders.remove(it);
                ASSERT(m_times_locked >= lock_count_to_restore);
                m_times_locked -= lock_count_to_restore;
                if (m_times_locked == 0)
                    m_mode = Mode::Unlocked;
                m_lock.store(false, AK::memory_order_release);
                previous_mode = Mode::Shared;
                break;
            }
            case Mode::Unlocked: {
                m_lock.store(false, AK::memory_order_relaxed);
                lock_count_to_restore = 0;
                previous_mode = Mode::Unlocked;
                break;
            }
            default:
                ASSERT_NOT_REACHED();
            }
            m_queue.wake_one();
            return previous_mode;
        }
        // I don't know *who* is using "m_lock", so just yield.
        Scheduler::yield_from_critical();
    }
}

#if LOCK_DEBUG
void Lock::restore_lock(Mode mode, u32 lock_count)
{
    return restore_lock("unknown", 0, mode, lock_count);
}

void Lock::restore_lock(const char* file, int line, Mode mode, u32 lock_count)
#else
void Lock::restore_lock(Mode mode, u32 lock_count)
#endif
{
    ASSERT(mode != Mode::Unlocked);
    ASSERT(lock_count > 0);
    ASSERT(!Processor::current().in_irq());
    auto current_thread = Thread::current();
    ScopedCritical critical; // in case we're not in a critical section already
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) == false) {
            switch (mode) {
            case Mode::Exclusive: {
                auto expected_mode = Mode::Unlocked;
                if (!m_mode.compare_exchange_strong(expected_mode, Mode::Exclusive))
                    break;

                dbgln<LOCK_RESTORE_DEBUG>("Lock::restore_lock @ {}: restoring {} with lock count {}, was unlocked", this, mode_to_string(mode), lock_count);

                ASSERT(m_times_locked == 0);
                m_times_locked = lock_count;
                ASSERT(!m_holder);
                ASSERT(m_shared_holders.is_empty());
                m_holder = current_thread;
                m_lock.store(false, AK::memory_order_release);
#if LOCK_DEBUG
                m_holder->holding_lock(*this, (int)lock_count, file, line);
#endif
                return;
            }
            case Mode::Shared: {
                auto expected_mode = Mode::Unlocked;
                if (!m_mode.compare_exchange_strong(expected_mode, Mode::Shared) && expected_mode != Mode::Shared)
                    break;

                dbgln<LOCK_RESTORE_DEBUG>("Lock::restore_lock @ {}: restoring {} with lock count {}, was {}",
                    this, mode_to_string(mode), lock_count, mode_to_string(expected_mode));

                ASSERT(expected_mode == Mode::Shared || m_times_locked == 0);
                m_times_locked += lock_count;
                ASSERT(!m_holder);
                ASSERT((expected_mode == Mode::Unlocked) == m_shared_holders.is_empty());
                auto set_result = m_shared_holders.set(current_thread, lock_count);
                // There may be other shared lock holders already, but we should not have an entry yet
                ASSERT(set_result == AK::HashSetResult::InsertedNewEntry);
                m_lock.store(false, AK::memory_order_release);
#if LOCK_DEBUG
                m_holder->holding_lock(*this, (int)lock_count, file, line);
#endif
                return;
            }
            default:
                ASSERT_NOT_REACHED();
            }

            m_lock.store(false, AK::memory_order_relaxed);
        }
        // I don't know *who* is using "m_lock", so just yield.
        Scheduler::yield_from_critical();
    }
}

void Lock::clear_waiters()
{
    ASSERT(m_mode != Mode::Shared);
    m_queue.wake_all();
}

}
