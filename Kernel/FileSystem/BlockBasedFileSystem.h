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
    TYPEDEF_DISTINCT_ORDERED_ID(u64, BlockIndex);

    virtual ~BlockBasedFileSystem() override;
    virtual KResult initialize() override;

    u64 logical_block_size() const { return m_logical_block_size; };

    virtual void flush_writes() override;
    void flush_writes_impl();

protected:
    explicit BlockBasedFileSystem(FileDescription&);

    KResult read_block(BlockIndex, UserOrKernelBuffer*, size_t count, size_t offset = 0, bool allow_cache = true) const;
    KResult read_blocks(BlockIndex, unsigned count, UserOrKernelBuffer&, bool allow_cache = true) const;

    bool raw_read(BlockIndex, UserOrKernelBuffer&);
    bool raw_write(BlockIndex, const UserOrKernelBuffer&);

    bool raw_read_blocks(BlockIndex index, size_t count, UserOrKernelBuffer&);
    bool raw_write_blocks(BlockIndex index, size_t count, const UserOrKernelBuffer&);

    KResult write_block(BlockIndex, const UserOrKernelBuffer&, size_t count, size_t offset = 0, bool allow_cache = true);
    KResult write_blocks(BlockIndex, unsigned count, const UserOrKernelBuffer&, bool allow_cache = true);

    u64 m_logical_block_size { 512 };

private:
    DiskCache& cache() const;
    void flush_specific_block_if_needed(BlockIndex index);

    mutable MutexProtected<OwnPtr<DiskCache>> m_cache;
};

}

template<>
struct YAK::Formatter<Kernel::BlockBasedFileSystem::BlockIndex> : YAK::Formatter<FormatString> {
    void format(FormatBuilder& builder, Kernel::BlockBasedFileSystem::BlockIndex value)
    {
        return YAK::Formatter<FormatString>::format(builder, "{}", value.value());
    }
};
