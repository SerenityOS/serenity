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

    ErrorOr<void> write_directory(Vector<Ext2FSDirectoryEntry>&);
    ErrorOr<void> populate_lookup_cache();
    ErrorOr<void> resize(u64);
    ErrorOr<void> write_indirect_block(BlockBasedFileSystem::BlockIndex, Span<BlockBasedFileSystem::BlockIndex>);
    ErrorOr<void> grow_doubly_indirect_block(BlockBasedFileSystem::BlockIndex, size_t, Span<BlockBasedFileSystem::BlockIndex>, Vector<BlockBasedFileSystem::BlockIndex>&, unsigned&);
    ErrorOr<void> shrink_doubly_indirect_block(BlockBasedFileSystem::BlockIndex, size_t, size_t, unsigned&);
    ErrorOr<void> grow_triply_indirect_block(BlockBasedFileSystem::BlockIndex, size_t, Span<BlockBasedFileSystem::BlockIndex>, Vector<BlockBasedFileSystem::BlockIndex>&, unsigned&);
    ErrorOr<void> shrink_triply_indirect_block(BlockBasedFileSystem::BlockIndex, size_t, size_t, unsigned&);
    ErrorOr<void> flush_block_list();

    ErrorOr<void> compute_block_list_with_exclusive_locking();
    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list() const;
    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list_with_meta_blocks() const;
    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list_impl(bool include_block_list_blocks) const;
    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list_impl_internal(ext2_inode const&, bool include_block_list_blocks) const;

    Ext2FS& fs();
    Ext2FS const& fs() const;
    Ext2FSInode(Ext2FS&, InodeIndex);

    Vector<BlockBasedFileSystem::BlockIndex> m_block_list;
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
