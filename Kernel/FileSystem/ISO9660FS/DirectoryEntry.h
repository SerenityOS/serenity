/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Library/KBuffer.h>

namespace Kernel {

struct ISO9660FSDirectoryEntry final : public AtomicRefCounted<ISO9660FSDirectoryEntry> {
    u32 extent { 0 };
    u32 length { 0 };

    // NOTE: This can never be empty if we read the directory successfully.
    //       We need it as an OwnPtr to default-construct this struct.
    OwnPtr<KBuffer> blocks;

    static ErrorOr<NonnullLockRefPtr<ISO9660FSDirectoryEntry>> try_create(u32 extent, u32 length, OwnPtr<KBuffer> blocks)
    {
        return adopt_nonnull_lock_ref_or_enomem(new (nothrow) ISO9660FSDirectoryEntry(extent, length, move(blocks)));
    }

private:
    ISO9660FSDirectoryEntry(u32 extent, u32 length, OwnPtr<KBuffer> blocks)
        : extent(extent)
        , length(length)
        , blocks(move(blocks))
    {
    }
};

struct ISO9660FSDirectoryState {
    LockRefPtr<ISO9660FSDirectoryEntry> entry;
    u32 offset { 0 };
};

}
