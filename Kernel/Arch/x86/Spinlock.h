/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/Processor.h>
#include <Kernel/Locking/LockRank.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

class Spinlock {
    AK_MAKE_NONCOPYABLE(Spinlock);
    AK_MAKE_NONMOVABLE(Spinlock);

public:
    Spinlock(LockRank rank = LockRank::None)
        : m_rank(rank)
    {
    }

    ALWAYS_INLINE u32 lock()
    {
        u32 prev_flags = cpu_flags();
        Processor::enter_critical();
        cli();
        while (m_lock.exchange(1, AK::memory_order_acquire) != 0) {
            Processor::wait_check();
        }
        track_lock_acquire(m_rank);
        return prev_flags;
    }

    ALWAYS_INLINE void unlock(u32 prev_flags)
    {
        VERIFY(is_locked());
        track_lock_release(m_rank);
        m_lock.store(0, AK::memory_order_release);
        if ((prev_flags & 0x200) != 0)
            sti();
        else
            cli();

        Processor::leave_critical();
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return m_lock.load(AK::memory_order_relaxed) != 0;
    }

    ALWAYS_INLINE void initialize()
    {
        m_lock.store(0, AK::memory_order_relaxed);
    }

private:
    Atomic<u8> m_lock { 0 };
    const LockRank m_rank;
};

class RecursiveSpinlock {
    AK_MAKE_NONCOPYABLE(RecursiveSpinlock);
    AK_MAKE_NONMOVABLE(RecursiveSpinlock);

public:
    RecursiveSpinlock(LockRank rank = LockRank::None)
        : m_rank(rank)
    {
    }

    ALWAYS_INLINE u32 lock()
    {
        u32 prev_flags = cpu_flags();
        cli();
        Processor::enter_critical();
        auto& proc = Processor::current();
        FlatPtr cpu = FlatPtr(&proc);
        FlatPtr expected = 0;
        while (!m_lock.compare_exchange_strong(expected, cpu, AK::memory_order_acq_rel)) {
            if (expected == cpu)
                break;
            Processor::wait_check();
            expected = 0;
        }
        if (m_recursions == 0)
            track_lock_acquire(m_rank);
        m_recursions++;
        return prev_flags;
    }

    ALWAYS_INLINE void unlock(u32 prev_flags)
    {
        VERIFY(m_recursions > 0);
        VERIFY(m_lock.load(AK::memory_order_relaxed) == FlatPtr(&Processor::current()));
        if (--m_recursions == 0) {
            track_lock_release(m_rank);
            m_lock.store(0, AK::memory_order_release);
        }
        if ((prev_flags & 0x200) != 0)
            sti();
        else
            cli();

        Processor::leave_critical();
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return m_lock.load(AK::memory_order_relaxed) != 0;
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked_by_current_processor() const
    {
        return m_lock.load(AK::memory_order_relaxed) == FlatPtr(&Processor::current());
    }

    ALWAYS_INLINE void initialize()
    {
        m_lock.store(0, AK::memory_order_relaxed);
    }

private:
    Atomic<FlatPtr> m_lock { 0 };
    u32 m_recursions { 0 };
    const LockRank m_rank;
};

}
