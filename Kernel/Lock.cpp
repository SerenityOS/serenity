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

#include <AK/TemporaryChange.h>
#include <Kernel/Debug.h>
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
    VERIFY(!Processor::current().in_irq());
    VERIFY(mode != Mode::Unlocked);
    auto current_thread = Thread::current();
    ScopedCritical critical; // in case we're not in a critical section already
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) != false) {
            // I don't know *who* is using "m_lock", so just yield.
            Scheduler::yield_from_critical();
            continue;
        }

        // FIXME: Do not add new readers if writers are queued.
        Mode current_mode = m_mode;
        switch (current_mode) {
        case Mode::Unlocked: {
            dbgln_if(LOCK_TRACE_DEBUG, "Lock::lock @ ({}) {}: acquire {}, currently unlocked", this, m_name, mode_to_string(mode));
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
            current_thread->holding_lock(*this, 1, file, line);
#endif
            m_queue.should_block(true);
            m_lock.store(false, AK::memory_order_release);
            return;
        }
        case Mode::Exclusive: {
            VERIFY(m_holder);
            if (m_holder != current_thread)
                break;
            VERIFY(m_shared_holders.is_empty());

            if constexpr (LOCK_TRACE_DEBUG) {
                if (mode == Mode::Exclusive)
                    dbgln("Lock::lock @ {} ({}): acquire {}, currently exclusive, holding: {}", this, m_name, mode_to_string(mode), m_times_locked);
                else
                    dbgln("Lock::lock @ {} ({}): acquire exclusive (requested {}), currently exclusive, holding: {}", this, m_name, mode_to_string(mode), m_times_locked);
            }

            VERIFY(mode == Mode::Exclusive || mode == Mode::Shared);
            VERIFY(m_times_locked > 0);
            m_times_locked++;
#if LOCK_DEBUG
            current_thread->holding_lock(*this, 1, file, line);
#endif
            m_lock.store(false, AK::memory_order_release);
            return;
        }
        case Mode::Shared: {
            VERIFY(!m_holder);
            if (mode != Mode::Shared)
                break;

            dbgln_if(LOCK_TRACE_DEBUG, "Lock::lock @ {} ({}): acquire {}, currently shared, locks held {}", this, m_name, mode_to_string(mode), m_times_locked);

            VERIFY(m_times_locked > 0);
            m_times_locked++;
            VERIFY(!m_shared_holders.is_empty());
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
            VERIFY_NOT_REACHED();
        }
        m_lock.store(false, AK::memory_order_release);
        dbgln_if(LOCK_TRACE_DEBUG, "Lock::lock @ {} ({}) waiting...", this, m_name);
        m_queue.wait_forever(m_name);
        dbgln_if(LOCK_TRACE_DEBUG, "Lock::lock @ {} ({}) waited", this, m_name);
    }
}

void Lock::unlock()
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    VERIFY(!Processor::current().in_irq());
    auto current_thread = Thread::current();
    ScopedCritical critical; // in case we're not in a critical section already
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) == false) {
            Mode current_mode = m_mode;
            if constexpr (LOCK_TRACE_DEBUG) {
                if (current_mode == Mode::Shared)
                    dbgln("Lock::unlock @ {} ({}): release {}, locks held: {}", this, m_name, mode_to_string(current_mode), m_times_locked);
                else
                    dbgln("Lock::unlock @ {} ({}): release {}, holding: {}", this, m_name, mode_to_string(current_mode), m_times_locked);
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

            bool unlocked_last = (m_times_locked == 0);
            if (unlocked_last) {
                VERIFY(current_mode == Mode::Exclusive ? !m_holder : m_shared_holders.is_empty());
                m_mode = Mode::Unlocked;
                m_queue.should_block(false);
            }

#if LOCK_DEBUG
            current_thread->holding_lock(*this, -1);
#endif

            m_lock.store(false, AK::memory_order_release);
            if (unlocked_last) {
                u32 did_wake = m_queue.wake_one();
                dbgln_if(LOCK_TRACE_DEBUG, "Lock::unlock @ {} ({})  wake one ({})", this, m_name, did_wake);
            }
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
    VERIFY(!Processor::current().in_irq());
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

                dbgln_if(LOCK_RESTORE_DEBUG, "Lock::force_unlock_if_locked @ {}: unlocking exclusive with lock count: {}", this, m_times_locked);
#if LOCK_DEBUG
                m_holder->holding_lock(*this, -(int)lock_count_to_restore);
#endif
                m_holder = nullptr;
                VERIFY(m_times_locked > 0);
                lock_count_to_restore = m_times_locked;
                m_times_locked = 0;
                m_mode = Mode::Unlocked;
                m_queue.should_block(false);
                m_lock.store(false, AK::memory_order_release);
                previous_mode = Mode::Exclusive;
                break;
            }
            case Mode::Shared: {
                VERIFY(!m_holder);
                auto it = m_shared_holders.find(current_thread);
                if (it == m_shared_holders.end()) {
                    m_lock.store(false, AK::MemoryOrder::memory_order_release);
                    lock_count_to_restore = 0;
                    return Mode::Unlocked;
                }

                dbgln_if(LOCK_RESTORE_DEBUG, "Lock::force_unlock_if_locked @ {}: unlocking exclusive with lock count: {}, total locks: {}",
                    this, it->value, m_times_locked);

                VERIFY(it->value > 0);
                lock_count_to_restore = it->value;
                VERIFY(lock_count_to_restore > 0);
#if LOCK_DEBUG
                m_holder->holding_lock(*this, -(int)lock_count_to_restore);
#endif
                m_shared_holders.remove(it);
                VERIFY(m_times_locked >= lock_count_to_restore);
                m_times_locked -= lock_count_to_restore;
                if (m_times_locked == 0) {
                    m_mode = Mode::Unlocked;
                    m_queue.should_block(false);
                }
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
                VERIFY_NOT_REACHED();
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
    VERIFY(mode != Mode::Unlocked);
    VERIFY(lock_count > 0);
    VERIFY(!Processor::current().in_irq());
    auto current_thread = Thread::current();
    ScopedCritical critical; // in case we're not in a critical section already
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) == false) {
            switch (mode) {
            case Mode::Exclusive: {
                auto expected_mode = Mode::Unlocked;
                if (!m_mode.compare_exchange_strong(expected_mode, Mode::Exclusive))
                    break;

                dbgln_if(LOCK_RESTORE_DEBUG, "Lock::restore_lock @ {}: restoring {} with lock count {}, was unlocked", this, mode_to_string(mode), lock_count);

                VERIFY(m_times_locked == 0);
                m_times_locked = lock_count;
                VERIFY(!m_holder);
                VERIFY(m_shared_holders.is_empty());
                m_holder = current_thread;
                m_queue.should_block(true);
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

                dbgln_if(LOCK_RESTORE_DEBUG, "Lock::restore_lock @ {}: restoring {} with lock count {}, was {}",
                    this, mode_to_string(mode), lock_count, mode_to_string(expected_mode));

                VERIFY(expected_mode == Mode::Shared || m_times_locked == 0);
                m_times_locked += lock_count;
                VERIFY(!m_holder);
                VERIFY((expected_mode == Mode::Unlocked) == m_shared_holders.is_empty());
                auto set_result = m_shared_holders.set(current_thread, lock_count);
                // There may be other shared lock holders already, but we should not have an entry yet
                VERIFY(set_result == AK::HashSetResult::InsertedNewEntry);
                m_queue.should_block(true);
                m_lock.store(false, AK::memory_order_release);
#if LOCK_DEBUG
                m_holder->holding_lock(*this, (int)lock_count, file, line);
#endif
                return;
            }
            default:
                VERIFY_NOT_REACHED();
            }

            m_lock.store(false, AK::memory_order_relaxed);
        }
        // I don't know *who* is using "m_lock", so just yield.
        Scheduler::yield_from_critical();
    }
}

void Lock::clear_waiters()
{
    VERIFY(m_mode != Mode::Shared);
    m_queue.wake_all();
}

}
