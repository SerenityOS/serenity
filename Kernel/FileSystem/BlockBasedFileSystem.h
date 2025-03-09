/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/FileBackedFileSystem.h>
#include <Kernel/Locking/MutexProtected.h>

namespace Kernel {

class BlockBasedFileSystem : public FileBackedFileSystem {
public:
    AK_TYPEDEF_DISTINCT_ORDERED_ID(u64, BlockIndex);

    virtual ~BlockBasedFileSystem() override;

    u64 device_block_size() const { return m_device_block_size; }

    virtual ErrorOr<void> flush_writes() override;
    void flush_writes_impl();

protected:
    explicit BlockBasedFileSystem(OpenFileDescription&);

    virtual ErrorOr<void> initialize_while_locked() override;

    ErrorOr<void> read_block(BlockIndex, UserOrKernelBuffer*, size_t count, u64 offset = 0, bool allow_cache = true) const;
    ErrorOr<void> read_blocks(BlockIndex, unsigned count, UserOrKernelBuffer&, bool allow_cache = true) const;

    ErrorOr<void> raw_read(BlockIndex, UserOrKernelBuffer&);
    ErrorOr<void> raw_write(BlockIndex, UserOrKernelBuffer const&);

    ErrorOr<void> raw_read_blocks(BlockIndex index, size_t count, UserOrKernelBuffer&);
    ErrorOr<void> raw_write_blocks(BlockIndex index, size_t count, UserOrKernelBuffer const&);

    ErrorOr<void> write_block(BlockIndex, UserOrKernelBuffer const&, size_t count, u64 offset = 0, bool allow_cache = true);
    ErrorOr<void> write_blocks(BlockIndex, unsigned count, UserOrKernelBuffer const&, bool allow_cache = true);

    u64 m_device_block_size { 512 };

private:
    void flush_specific_block_if_needed(BlockIndex index);

    mutable MutexProtected<OwnPtr<DiskCache>> m_cache;
};

}
