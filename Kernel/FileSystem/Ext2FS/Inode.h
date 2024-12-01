/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <Kernel/FileSystem/Ext2FS/BlockView.h>
#include <Kernel/FileSystem/Ext2FS/Definitions.h>
#include <Kernel/FileSystem/Ext2FS/DirectoryEntry.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class Ext2FSInode final : public Inode {
    friend class Ext2FS;
    friend class Ext2FSBlockView;

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
    virtual ErrorOr<void> update_timestamps(Optional<UnixDateTime> atime, Optional<UnixDateTime> ctime, Optional<UnixDateTime> mtime) override;
    virtual ErrorOr<void> increment_link_count() override;
    virtual ErrorOr<void> decrement_link_count() override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate_locked(u64) override;
    virtual ErrorOr<int> get_block_address(int) override;

    bool is_within_inode_bounds(FlatPtr base, FlatPtr value_offset, size_t value_size) const;

    static u8 to_ext2_file_type(mode_t mode);

    static time_t decode_seconds_with_extra(i32 seconds, u32 extra) { return (extra & EXT4_EPOCH_MASK) ? static_cast<time_t>(seconds) + (static_cast<time_t>(extra & EXT4_EPOCH_MASK) << 32) : static_cast<time_t>(seconds); }
    static u32 decode_nanoseconds_from_extra(u32 extra) { return (extra & EXT4_NSEC_MASK) >> EXT4_EPOCH_BITS; }
    static u32 encode_time_to_extra(time_t seconds, u32 nanoseconds) { return (((static_cast<time_t>(seconds) - static_cast<i32>(seconds)) >> 32) & EXT4_EPOCH_MASK) | (nanoseconds << EXT4_EPOCH_BITS); }

    ErrorOr<BlockBasedFileSystem::BlockIndex> allocate_block(BlockBasedFileSystem::BlockIndex, bool zero_newly_allocated_block, bool allow_cache);
    ErrorOr<u32> allocate_and_zero_block();

    enum class RemoveDotEntries {
        Yes,
        No,
    };

    ErrorOr<void> remove_child_impl(StringView name, RemoveDotEntries);
    ErrorOr<void> write_directory(Vector<Ext2FSDirectoryEntry>&);
    ErrorOr<void> populate_lookup_cache();
    ErrorOr<void> resize(u64);
    ErrorOr<void> write_singly_indirect_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index);
    ErrorOr<void> write_doubly_indirect_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index);
    ErrorOr<void> write_triply_indirect_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index);
    ErrorOr<void> write_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index);

    ErrorOr<Ext2FS::BlockList> compute_block_list(BlockBasedFileSystem::BlockIndex, BlockBasedFileSystem::BlockIndex) const;

    ErrorOr<void> free_all_blocks();

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

    mutable Ext2FSBlockView m_block_view;
    HashMap<NonnullOwnPtr<KString>, InodeIndex> m_lookup_cache;
    ext2_inode_large m_raw_inode {};
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
