/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/Types.h>
#include <AK/Atomic.h>
#include <Kernel/Forward.h>
#include <Kernel/Arch/i386/CPU.h>

namespace Kernel {

template <typename BaseType = u32>
class SpinLock
{
    AK::Atomic<BaseType> m_lock{0};

public:
    SpinLock() = default;
    SpinLock(const SpinLock&) = delete;
    SpinLock(SpinLock&&) = delete;

    ALWAYS_INLINE void lock()
    {
        BaseType expected;
        do {
            expected = 0;
        } while (!m_lock.compare_exchange_strong(expected, 1, AK::memory_order_acq_rel));
        
    }

    ALWAYS_INLINE void unlock()
    {
        ASSERT(is_locked());
        m_lock.store(0, AK::memory_order_release);
    }

    ALWAYS_INLINE bool is_locked() const
    {
        return m_lock.load(AK::memory_order_consume) != 0;
    }

    ALWAYS_INLINE void initialize()
    {
        m_lock.store(0, AK::memory_order_release);
    }
};

template <typename BaseType = u32, typename LockType = SpinLock<BaseType>>
class ScopedSpinLock
{
    LockType* m_lock;
    bool m_have_lock{false};
    bool m_flag{false};

public:
    ScopedSpinLock() = delete;

    ScopedSpinLock(LockType& lock):
        m_lock(&lock)
    {
        ASSERT(m_lock);
        m_flag = cli_and_save_interrupt_flag();
        m_lock->lock();
        m_have_lock = true;
    }

    ScopedSpinLock(ScopedSpinLock&& from):
        m_lock(from.m_lock),
        m_have_lock(from.m_have_lock),
        m_flag(from.m_flag)
    {
        from.m_lock = nullptr;
        from.m_have_lock = false;
        from.m_flag = false;
    }

    ScopedSpinLock(const ScopedSpinLock&) = delete;

    ~ScopedSpinLock()
    {
        if (m_lock && m_have_lock) {
            m_lock->unlock();
            restore_interrupt_flag(m_flag);
        }
    }

    ALWAYS_INLINE void lock()
    {
        ASSERT(m_lock);
        ASSERT(!m_have_lock);
        m_flag = cli_and_save_interrupt_flag();
        m_lock->lock();
        m_have_lock = true;
    }

    ALWAYS_INLINE void unlock()
    {
        ASSERT(m_lock);
        ASSERT(m_have_lock);
        m_lock->unlock();
        m_have_lock = false;
        restore_interrupt_flag(m_flag);
        m_flag = false;
    }

    ALWAYS_INLINE bool have_lock() const
    {
        return m_have_lock;
    }
};

}
