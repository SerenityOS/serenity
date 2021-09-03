/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Atomic.h>
#include <YAK/Types.h>
#include <Kernel/Arch/x86/Processor.h>
#include <Kernel/Forward.h>

namespace Kernel {

template<typename BaseType = u32>
class Spinlock {
    YAK_MAKE_NONCOPYABLE(Spinlock);
    YAK_MAKE_NONMOVABLE(Spinlock);

public:
    Spinlock() = default;

    ALWAYS_INLINE u32 lock()
    {
        u32 prev_flags = cpu_flags();
        Processor::enter_critical();
        cli();
        while (m_lock.exchange(1, YAK::memory_order_acquire) != 0) {
            Processor::wait_check();
        }
        return prev_flags;
    }

    ALWAYS_INLINE void unlock(u32 prev_flags)
    {
        VERIFY(is_locked());
        m_lock.store(0, YAK::memory_order_release);
        if (prev_flags & 0x200)
            sti();
        else
            cli();
        Processor::leave_critical();
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return m_lock.load(YAK::memory_order_relaxed) != 0;
    }

    ALWAYS_INLINE void initialize()
    {
        m_lock.store(0, YAK::memory_order_relaxed);
    }

private:
    Atomic<BaseType> m_lock { 0 };
};

class RecursiveSpinlock {
    YAK_MAKE_NONCOPYABLE(RecursiveSpinlock);
    YAK_MAKE_NONMOVABLE(RecursiveSpinlock);

public:
    RecursiveSpinlock() = default;

    ALWAYS_INLINE u32 lock()
    {
        u32 prev_flags = cpu_flags();
        cli();
        Processor::enter_critical();
        auto& proc = Processor::current();
        FlatPtr cpu = FlatPtr(&proc);
        FlatPtr expected = 0;
        while (!m_lock.compare_exchange_strong(expected, cpu, YAK::memory_order_acq_rel)) {
            if (expected == cpu)
                break;
            Processor::wait_check();
            expected = 0;
        }
        m_recursions++;
        return prev_flags;
    }

    ALWAYS_INLINE void unlock(u32 prev_flags)
    {
        VERIFY(m_recursions > 0);
        VERIFY(m_lock.load(YAK::memory_order_relaxed) == FlatPtr(&Processor::current()));
        if (--m_recursions == 0)
            m_lock.store(0, YAK::memory_order_release);
        if (prev_flags & 0x200)
            sti();
        else
            cli();
        Processor::leave_critical();
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return m_lock.load(YAK::memory_order_relaxed) != 0;
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked_by_current_processor() const
    {
        return m_lock.load(YAK::memory_order_relaxed) == FlatPtr(&Processor::current());
    }

    ALWAYS_INLINE void initialize()
    {
        m_lock.store(0, YAK::memory_order_relaxed);
    }

private:
    Atomic<FlatPtr> m_lock { 0 };
    u32 m_recursions { 0 };
};

template<typename LockType>
class [[nodiscard]] SpinlockLocker {
    YAK_MAKE_NONCOPYABLE(SpinlockLocker);

public:
    SpinlockLocker() = delete;
    SpinlockLocker& operator=(SpinlockLocker&&) = delete;

    SpinlockLocker(LockType& lock)
        : m_lock(&lock)
    {
        VERIFY(m_lock);
        m_prev_flags = m_lock->lock();
        m_have_lock = true;
    }

    SpinlockLocker(SpinlockLocker&& from)
        : m_lock(from.m_lock)
        , m_prev_flags(from.m_prev_flags)
        , m_have_lock(from.m_have_lock)
    {
        from.m_lock = nullptr;
        from.m_prev_flags = 0;
        from.m_have_lock = false;
    }

    ~SpinlockLocker()
    {
        if (m_lock && m_have_lock) {
            m_lock->unlock(m_prev_flags);
        }
    }

    ALWAYS_INLINE void lock()
    {
        VERIFY(m_lock);
        VERIFY(!m_have_lock);
        m_prev_flags = m_lock->lock();
        m_have_lock = true;
    }

    ALWAYS_INLINE void unlock()
    {
        VERIFY(m_lock);
        VERIFY(m_have_lock);
        m_lock->unlock(m_prev_flags);
        m_prev_flags = 0;
        m_have_lock = false;
    }

    [[nodiscard]] ALWAYS_INLINE bool have_lock() const
    {
        return m_have_lock;
    }

private:
    LockType* m_lock { nullptr };
    u32 m_prev_flags { 0 };
    bool m_have_lock { false };
};

}
