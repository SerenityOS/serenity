/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
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

    u32 lock();
    void unlock(u32 prev_flags);

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

    u32 lock();
    void unlock(u32 prev_flags);

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
