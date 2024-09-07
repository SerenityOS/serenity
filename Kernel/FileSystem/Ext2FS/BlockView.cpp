/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/Ext2FS/BlockView.h>
#include <Kernel/FileSystem/Ext2FS/Inode.h>

namespace Kernel {

static constexpr size_t max_blocks_in_view = 16384; // 2^14

Ext2FSBlockView::Ext2FSBlockView(Ext2FSInode& inode)
    : m_inode(inode) {};

ErrorOr<void> Ext2FSBlockView::ensure_block(BlockBasedFileSystem::BlockIndex block)
{
    VERIFY(m_block_list_lock.is_locked());
    if (block >= m_first_block && block <= m_last_block && m_block_list_initialized.was_set())
        return {};

    BlockBasedFileSystem::BlockIndex new_first_block = (block.value() / max_blocks_in_view) * max_blocks_in_view;
    BlockBasedFileSystem::BlockIndex new_last_block = new_first_block.value() + max_blocks_in_view - 1;

    m_block_list = TRY(m_inode.compute_block_list(new_first_block, new_last_block));

    m_first_block = new_first_block;
    m_last_block = new_last_block;

    m_block_list_initialized.set();

    return {};
}

ErrorOr<BlockBasedFileSystem::BlockIndex> Ext2FSBlockView::get_block(BlockBasedFileSystem::BlockIndex block)
{
    MutexLocker block_list_locker(m_block_list_lock);
    TRY(ensure_block(block));

    auto it = m_block_list.find(block);
    if (it == m_block_list.end())
        return BlockBasedFileSystem::BlockIndex { 0 };

    auto on_disk_block = (*it).value;
    VERIFY(on_disk_block != 0);
    return on_disk_block;
}

ErrorOr<BlockBasedFileSystem::BlockIndex> Ext2FSBlockView::get_or_allocate_block(BlockBasedFileSystem::BlockIndex block, bool zero_newly_allocated_block, bool allow_cache)
{
    MutexLocker block_list_locker(m_block_list_lock);
    TRY(ensure_block(block));

    auto it = m_block_list.find(block);
    if (it != m_block_list.end()) {
        auto on_disk_block = (*it).value;
        VERIFY(on_disk_block != 0);
        return on_disk_block;
    }

    auto on_disk_block = TRY(m_inode.allocate_block(block, zero_newly_allocated_block, allow_cache));
    TRY(m_block_list.try_set(block, on_disk_block));

    return on_disk_block;
}

ErrorOr<void> Ext2FSBlockView::write_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index)
{
    MutexLocker block_list_locker(m_block_list_lock);

    TRY(m_inode.write_block_pointer(logical_block_index, on_disk_index));

    if (on_disk_index == 0)
        m_block_list.remove(logical_block_index);
    else
        TRY(m_block_list.try_set(logical_block_index, on_disk_index));

    return {};
}

}
