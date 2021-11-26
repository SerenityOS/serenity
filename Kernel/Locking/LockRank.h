/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>

namespace Kernel {
// To catch bugs where locks are taken out of order, we annotate all locks
// in the kernel with a rank. The rank describes the order in which locks
// are allowed to be taken. If a lock is acquired, and it is of an incompatible
// rank with the lock held by the executing thread then the system can detect
// the lock order violation and respond appropriately (crash with error).
//
// A thread holding a lower ranked lock cannot acquire a lock of a greater or equal rank.
enum class LockRank : int {
    // Special marker for locks which haven't been annotated yet.
    // Note: This should be removed once all locks are annotated.
    None = 0x000,

    // We need to be able to handle page faults from anywhere, so
    // memory manager locks are our lowest rank lock.
    MemoryManager = 0x001,

    Interrupts = 0x002,

    FileSystem = 0x004,

    Thread = 0x008,

    // Process locks are the highest rank, as they normally are taken
    // first thing when processing syscalls.
    Process = 0x010,
};

AK_ENUM_BITWISE_OPERATORS(LockRank);

void track_lock_acquire(LockRank);
void track_lock_release(LockRank);
}
