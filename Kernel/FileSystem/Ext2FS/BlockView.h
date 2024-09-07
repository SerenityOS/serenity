/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/SetOnce.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>

namespace Kernel {

class Ext2FSInode;

class Ext2FSBlockView {
public:
    Ext2FSBlockView(Ext2FSInode&);
    ErrorOr<BlockBasedFileSystem::BlockIndex> get_block(BlockBasedFileSystem::BlockIndex);
    ErrorOr<BlockBasedFileSystem::BlockIndex> get_or_allocate_block(BlockBasedFileSystem::BlockIndex, bool zero_newly_allocated_block, bool allow_cache);
    ErrorOr<void> write_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index);

private:
    ErrorOr<void> ensure_block(BlockBasedFileSystem::BlockIndex);

    Ext2FSInode& m_inode;
    Ext2FS::BlockList m_block_list;
    BlockBasedFileSystem::BlockIndex m_first_block = 0;
    BlockBasedFileSystem::BlockIndex m_last_block = 0;
    SetOnce m_block_list_initialized;

    Mutex m_block_list_lock { "BlockList"sv };
};

}
