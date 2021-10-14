/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/Types.h>
#include <Kernel/Locking/LockRank.h>

namespace Kernel {

class Spinlock {
    AK_MAKE_NONCOPYABLE(Spinlock);
    AK_MAKE_NONMOVABLE(Spinlock);

public:
    Spinlock(LockRank rank = LockRank::None)
    {
        (void)rank;
    }

    ALWAYS_INLINE u32 lock()
    {
        return 0;
    }

    ALWAYS_INLINE void unlock(u32 /*prev_flags*/)
    {
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return false;
    }

    ALWAYS_INLINE void initialize()
    {
    }
};

class RecursiveSpinlock {
    AK_MAKE_NONCOPYABLE(RecursiveSpinlock);
    AK_MAKE_NONMOVABLE(RecursiveSpinlock);

public:
    RecursiveSpinlock(LockRank rank = LockRank::None)
    {
        (void)rank;
    }

    ALWAYS_INLINE u32 lock()
    {
        return 0;
    }

    ALWAYS_INLINE void unlock(u32 /*prev_flags*/)
    {
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked() const
    {
        return false;
    }

    [[nodiscard]] ALWAYS_INLINE bool is_locked_by_current_processor() const
    {
        return false;
    }

    ALWAYS_INLINE void initialize()
    {
    }
};

}
