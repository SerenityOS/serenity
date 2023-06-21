/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Locking/LockRank.h>
#include <Kernel/Tasks/Thread.h>

// Note: These stubs can't be in LockRank.h as that would create
// a cyclic dependency in the header include graph of the Kernel.

namespace Kernel {

void track_lock_acquire(LockRank rank)
{
    if constexpr (LOCK_RANK_ENFORCEMENT) {
        auto* thread = Thread::current();
        if (thread && !thread->is_crashing())
            thread->track_lock_acquire(rank);
    }
}

void track_lock_release(LockRank rank)
{
    if constexpr (LOCK_RANK_ENFORCEMENT) {
        auto* thread = Thread::current();
        if (thread && !thread->is_crashing())
            thread->track_lock_release(rank);
    }
}

}
