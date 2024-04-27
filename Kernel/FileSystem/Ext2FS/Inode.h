/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <Kernel/FileSystem/Ext2FS/Definitions.h>
#include <Kernel/FileSystem/Ext2FS/DirectoryEntry.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class Ext2FSInode final : public Inode {
    friend class Ext2FS;

public:
    virtual ~Ext2FSInode() override;

    u64 size() const;
    bool is_symlink() const { return Kernel::is_symlink(m_raw_inode.i_mode); }
    bool is_directory() const { return Kernel::is_directory(m_raw_inode.i_mode); }

private:
    // ^Inode
    virtual ErrorOr<size_t> read_bytes_locked(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<size_t> write_bytes_locked(off_t, size_t, UserOrKernelBuffer const& data, OpenFileDescription*) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode& child, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> replace_child(StringView name, Inode& child) override;
    virtual ErrorOr<void> update_timestamps(Optional<UnixDateTime> atime, Optional<UnixDateTime> ctime, Optional<UnixDateTime> mtime) override;
    virtual ErrorOr<void> increment_link_count() override;
    virtual ErrorOr<void> decrement_link_count() override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate_locked(u64) override;
    virtual ErrorOr<int> get_block_address(int) override;

    ErrorOr<BlockBasedFileSystem::BlockIndex> get_or_allocate_block(BlockBasedFileSystem::BlockIndex, bool zero_newly_allocated_block, bool allow_cache);
    BlockBasedFileSystem::BlockIndex get_block(BlockBasedFileSystem::BlockIndex) const;
    ErrorOr<u32> allocate_and_zero_block();

    ErrorOr<void> write_directory(Vector<Ext2FSDirectoryEntry>&);
    ErrorOr<void> populate_lookup_cache();
    ErrorOr<void> resize(u64);
    ErrorOr<void> write_singly_indirect_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index);
    ErrorOr<void> write_doubly_indirect_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index);
    ErrorOr<void> write_triply_indirect_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index);
    ErrorOr<void> write_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index);
    ErrorOr<void> flush_block_list(Ext2FS::BlockList const& old_block_list);

    ErrorOr<void> compute_block_list_with_exclusive_locking();
    ErrorOr<Ext2FS::BlockList> compute_block_list() const;
    ErrorOr<Ext2FS::BlockList> compute_block_list_impl(Vector<Ext2FS::BlockIndex>* meta_blocks = nullptr) const;
    ErrorOr<Vector<Ext2FS::BlockIndex>> compute_meta_blocks() const;

    u64 singly_indirect_block_capacity() const
    {
        auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
        return EXT2_NDIR_BLOCKS + entries_per_block;
    }

    u64 doubly_indirect_block_capacity() const
    {
        auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
        return singly_indirect_block_capacity() + entries_per_block * entries_per_block;
    }

    u64 triply_indirect_block_capacity() const
    {
        auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
        return doubly_indirect_block_capacity() + entries_per_block * entries_per_block * entries_per_block;
    }

    Ext2FS& fs();
    Ext2FS const& fs() const;
    Ext2FSInode(Ext2FS&, InodeIndex);

    Ext2FS::BlockList m_block_list;
    HashMap<NonnullOwnPtr<KString>, InodeIndex> m_lookup_cache;
    ext2_inode m_raw_inode {};

    Mutex m_block_list_lock { "BlockList"sv };
};

inline Ext2FS& Ext2FSInode::fs()
{
    return static_cast<Ext2FS&>(Inode::fs());
}

inline Ext2FS const& Ext2FSInode::fs() const
{
    return static_cast<Ext2FS const&>(Inode::fs());
}

}
