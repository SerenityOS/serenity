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
#include <Kernel/KSyms.h>
#include <Kernel/Lock.h>

namespace Kernel {

#ifdef LOCK_DEBUG
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
    ScopedCritical critical;
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) == false) {
            do {
                // FIXME: Do not add new readers if writers are queued.
                bool can_lock;
                switch (m_mode) {
                case Lock::Mode::Unlocked:
                    can_lock = true;
                    break;
                case Lock::Mode::Shared:
                    can_lock = (mode == Lock::Mode::Shared);
                    break;
                case Lock::Mode::Exclusive:
                    can_lock = (m_holder == current_thread);
                    break;
                }
                if (can_lock) {
                    // We got the lock!
                    if (m_mode == Lock::Mode::Unlocked) {
                        m_mode = mode;
                        ASSERT(m_times_locked == 0);
                        if (mode == Mode::Exclusive)
                            m_holder = current_thread;
                    }
#ifdef LOCK_DEBUG
                    current_thread->holding_lock(*this, true, file, line);
#endif
                    m_times_locked++;
                    m_lock.store(false, AK::memory_order_release);
                    return;
                }
                m_lock.store(false, AK::memory_order_release);
            } while (m_queue.wait_on(nullptr, m_name) == Thread::BlockResult::NotBlocked);
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
    ScopedCritical critical;
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) == false) {
            ASSERT(m_times_locked);
            --m_times_locked;

            ASSERT(m_mode != Mode::Unlocked);

            if (m_mode == Mode::Exclusive) {
                ASSERT(m_holder == current_thread);
                if (m_times_locked == 0)
                    m_holder = nullptr;
            }
#ifdef LOCK_DEBUG
            current_thread->holding_lock(*this, false);
#endif

            if (m_times_locked > 0) {
                m_lock.store(false, AK::memory_order_release);
                return;
            }
            m_mode = Mode::Unlocked;
            m_lock.store(false, AK::memory_order_release);
            m_queue.wake_one();
            return;
        }
        // I don't know *who* is using "m_lock", so just yield.
        Scheduler::yield_from_critical();
    }
}

bool Lock::force_unlock_if_locked()
{
    // NOTE: This may be called from an interrupt handler (not an IRQ handler)
    // and also from within critical sections!
    ASSERT(!Processor::current().in_irq());
    ScopedCritical critical;
    for (;;) {
        if (m_lock.exchange(true, AK::memory_order_acq_rel) == false) {
            if (m_holder != Thread::current()) {
                m_lock.store(false, AK::MemoryOrder::memory_order_release);
                return false;
            }
            ASSERT(m_mode != Mode::Shared);
            ASSERT(m_times_locked == 1);
#ifdef LOCK_DEBUG
            m_holder->holding_lock(*this, false);
#endif
            m_holder = nullptr;
            m_mode = Mode::Unlocked;
            m_times_locked = 0;
            m_lock.store(false, AK::memory_order_release);
            m_queue.wake_one();
            break;
        }
        // I don't know *who* is using "m_lock", so just yield.
        Scheduler::yield_from_critical();
    }
    return true;
}

void Lock::clear_waiters()
{
    ASSERT(m_mode != Mode::Shared);
    m_queue.wake_all();
}

}
