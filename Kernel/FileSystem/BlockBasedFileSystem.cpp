/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntrusiveList.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

BlockBasedFileSystem::BlockBasedFileSystem(OpenFileDescription& file_description)
    : FileBackedFileSystem(file_description)
{
    VERIFY(file_description.file().is_seekable());
}

BlockBasedFileSystem::~BlockBasedFileSystem() = default;

ErrorOr<void> BlockBasedFileSystem::initialize_while_locked()
{
    VERIFY(m_lock.is_locked());
    VERIFY(!is_initialized_while_locked());
    return {};
}

ErrorOr<void> BlockBasedFileSystem::write_block(BlockIndex index, UserOrKernelBuffer const& data, size_t count, u64 offset)
{
    VERIFY(m_logical_block_size);
    VERIFY(offset + count <= block_size());
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::write_block {}, size={}", index, count);

    u64 base_offset = index.value() * block_size() + offset;
    auto nwritten = TRY(file_description().write(base_offset, data, count));
    VERIFY(nwritten == count);
    return {};
}

ErrorOr<void> BlockBasedFileSystem::raw_read(BlockIndex index, UserOrKernelBuffer& buffer)
{
    auto base_offset = index.value() * m_logical_block_size;
    auto nread = TRY(file_description().read(buffer, base_offset, m_logical_block_size));
    VERIFY(nread == m_logical_block_size);
    return {};
}

ErrorOr<void> BlockBasedFileSystem::raw_write(BlockIndex index, UserOrKernelBuffer const& buffer)
{
    auto base_offset = index.value() * m_logical_block_size;
    auto nwritten = TRY(file_description().write(base_offset, buffer, m_logical_block_size));
    VERIFY(nwritten == m_logical_block_size);
    return {};
}

ErrorOr<void> BlockBasedFileSystem::raw_read_blocks(BlockIndex index, size_t count, UserOrKernelBuffer& buffer)
{
    auto current = buffer;
    for (auto block = index.value(); block < (index.value() + count); block++) {
        TRY(raw_read(BlockIndex { block }, current));
        current = current.offset(logical_block_size());
    }
    return {};
}

ErrorOr<void> BlockBasedFileSystem::raw_write_blocks(BlockIndex index, size_t count, UserOrKernelBuffer const& buffer)
{
    auto current = buffer;
    for (auto block = index.value(); block < (index.value() + count); block++) {
        TRY(raw_write(block, current));
        current = current.offset(logical_block_size());
    }
    return {};
}

ErrorOr<void> BlockBasedFileSystem::write_blocks(BlockIndex index, unsigned count, UserOrKernelBuffer const& data)
{
    VERIFY(m_logical_block_size);
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::write_blocks {}, count={}", index, count);
    for (unsigned i = 0; i < count; ++i) {
        TRY(write_block(BlockIndex { index.value() + i }, data.offset(i * block_size()), block_size(), 0));
    }
    return {};
}

ErrorOr<void> BlockBasedFileSystem::read_block(BlockIndex index, UserOrKernelBuffer* buffer, size_t count, u64 offset) const
{
    VERIFY(m_logical_block_size);
    VERIFY(offset + count <= block_size());
    dbgln_if(BBFS_DEBUG, "BlockBasedFileSystem::read_block {}", index);

    u64 base_offset = index.value() * block_size() + offset;
    auto nread = TRY(file_description().read(*buffer, base_offset, count));
    VERIFY(nread == count);
    return {};
}

ErrorOr<void> BlockBasedFileSystem::read_blocks(BlockIndex index, unsigned count, UserOrKernelBuffer& buffer) const
{
    VERIFY(m_logical_block_size);
    if (!count)
        return EINVAL;
    if (count == 1)
        return read_block(index, &buffer, block_size(), 0);
    auto out = buffer;
    for (unsigned i = 0; i < count; ++i) {
        TRY(read_block(BlockIndex { index.value() + i }, &out, block_size(), 0));
        out = out.offset(block_size());
    }

    return {};
}

}
