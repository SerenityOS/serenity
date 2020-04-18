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

#include <Kernel/KSyms.h>
#include <Kernel/Lock.h>
#include <Kernel/Thread.h>

namespace Kernel {

static bool modes_conflict(Lock::Mode mode1, Lock::Mode mode2)
{
    if (mode1 == Lock::Mode::Unlocked || mode2 == Lock::Mode::Unlocked)
        return false;

    if (mode1 == Lock::Mode::Shared && mode2 == Lock::Mode::Shared)
        return false;

    return true;
}

void Lock::lock(Mode mode)
{
    ASSERT(mode != Mode::Unlocked);
    ASSERT(!Scheduler::is_active());
    if (!are_interrupts_enabled()) {
        klog() << "Interrupts disabled when trying to take Lock{" << m_name << "}";
        dump_backtrace();
        hang();
    }
    for (;;) {
        bool expected = false;
        if (m_lock.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            // FIXME: Do not add new readers if writers are queued.
            bool modes_dont_conflict = !modes_conflict(m_mode, mode);
            bool already_hold_exclusive_lock = m_mode == Mode::Exclusive && m_holder == Thread::current;
            if (modes_dont_conflict || already_hold_exclusive_lock) {
                // We got the lock!
                if (!already_hold_exclusive_lock)
                    m_mode = mode;
                m_holder = Thread::current;
                m_times_locked++;
                m_lock.store(false, AK::memory_order_release);
                return;
            }
            Thread::current->wait_on(m_queue, &m_lock, m_holder, m_name);
        }
    }
}

void Lock::unlock()
{
    for (;;) {
        bool expected = false;
        if (m_lock.compare_exchange_strong(expected, true, AK::memory_order_acq_rel)) {
            ASSERT(m_times_locked);
            --m_times_locked;

            ASSERT(m_mode != Mode::Unlocked);
            if (m_mode == Mode::Exclusive)
                ASSERT(m_holder == Thread::current);
            if (m_holder == Thread::current && (m_mode == Mode::Shared || m_times_locked == 0))
                m_holder = nullptr;

            if (m_times_locked > 0) {
                m_lock.store(false, AK::memory_order_release);
                return;
            }
            m_mode = Mode::Unlocked;
            m_queue.wake_one(&m_lock);
            return;
        }
        // I don't know *who* is using "m_lock", so just yield.
        Scheduler::yield();
    }
}

bool Lock::force_unlock_if_locked()
{
    ASSERT(m_mode != Mode::Shared);
    InterruptDisabler disabler;
    if (m_holder != Thread::current)
        return false;
    ASSERT(m_times_locked == 1);
    m_holder = nullptr;
    m_mode = Mode::Unlocked;
    m_times_locked = 0;
    m_queue.wake_one();
    return true;
}

void Lock::clear_waiters()
{
    ASSERT(m_mode != Mode::Shared);
    InterruptDisabler disabler;
    m_queue.clear();
}

}
