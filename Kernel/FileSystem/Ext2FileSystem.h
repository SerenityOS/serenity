/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/HashMap.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/ext2_fs.h>
#include <Kernel/KBuffer.h>
#include <Kernel/UnixTypes.h>

struct ext2_group_desc;
struct ext2_inode;
struct ext2_super_block;

namespace Kernel {

class Ext2FS;
struct Ext2FSDirectoryEntry;

class Ext2FSInode final : public Inode {
    friend class Ext2FS;

public:
    virtual ~Ext2FSInode() override;

    u64 size() const;
    bool is_symlink() const { return Kernel::is_symlink(m_raw_inode.i_mode); }
    bool is_directory() const { return Kernel::is_directory(m_raw_inode.i_mode); }

    // ^Inode (RefCounted magic)
    virtual void one_ref_left() override;

private:
    // ^Inode
    virtual ErrorOr<size_t> read_bytes(off_t, size_t, UserOrKernelBuffer& buffer, OpenFileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual ErrorOr<void> traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)>) const override;
    virtual ErrorOr<NonnullRefPtr<Inode>> lookup(StringView name) override;
    virtual ErrorOr<void> flush_metadata() override;
    virtual ErrorOr<size_t> write_bytes(off_t, size_t, const UserOrKernelBuffer& data, OpenFileDescription*) override;
    virtual ErrorOr<NonnullRefPtr<Inode>> create_child(StringView name, mode_t, dev_t, UserID, GroupID) override;
    virtual ErrorOr<void> add_child(Inode& child, StringView name, mode_t) override;
    virtual ErrorOr<void> remove_child(StringView name) override;
    virtual ErrorOr<void> set_atime(time_t) override;
    virtual ErrorOr<void> set_ctime(time_t) override;
    virtual ErrorOr<void> set_mtime(time_t) override;
    virtual ErrorOr<void> increment_link_count() override;
    virtual ErrorOr<void> decrement_link_count() override;
    virtual ErrorOr<void> chmod(mode_t) override;
    virtual ErrorOr<void> chown(UserID, GroupID) override;
    virtual ErrorOr<void> truncate(u64) override;
    virtual ErrorOr<int> get_block_address(int) override;

    ErrorOr<void> write_directory(Vector<Ext2FSDirectoryEntry>&);
    ErrorOr<void> populate_lookup_cache() const;
    ErrorOr<void> resize(u64);
    ErrorOr<void> write_indirect_block(BlockBasedFileSystem::BlockIndex, Span<BlockBasedFileSystem::BlockIndex>);
    ErrorOr<void> grow_doubly_indirect_block(BlockBasedFileSystem::BlockIndex, size_t, Span<BlockBasedFileSystem::BlockIndex>, Vector<BlockBasedFileSystem::BlockIndex>&, unsigned&);
    ErrorOr<void> shrink_doubly_indirect_block(BlockBasedFileSystem::BlockIndex, size_t, size_t, unsigned&);
    ErrorOr<void> grow_triply_indirect_block(BlockBasedFileSystem::BlockIndex, size_t, Span<BlockBasedFileSystem::BlockIndex>, Vector<BlockBasedFileSystem::BlockIndex>&, unsigned&);
    ErrorOr<void> shrink_triply_indirect_block(BlockBasedFileSystem::BlockIndex, size_t, size_t, unsigned&);
    ErrorOr<void> flush_block_list();
    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list() const;
    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list_with_meta_blocks() const;
    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list_impl(bool include_block_list_blocks) const;
    ErrorOr<Vector<BlockBasedFileSystem::BlockIndex>> compute_block_list_impl_internal(ext2_inode const&, bool include_block_list_blocks) const;

    Ext2FS& fs();
    const Ext2FS& fs() const;
    Ext2FSInode(Ext2FS&, InodeIndex);

    mutable Vector<BlockBasedFileSystem::BlockIndex> m_block_list;
    mutable HashMap<String, InodeIndex> m_lookup_cache;
    ext2_inode m_raw_inode;
};

class Ext2FS final : public BlockBasedFileSystem {
    friend class Ext2FSInode;

public:
    enum class FeaturesReadOnly : u32 {
        None = 0,
        FileSize64bits = 1 << 1,
    };

    static ErrorOr<NonnullRefPtr<Ext2FS>> try_create(OpenFileDescription&);

    virtual ~Ext2FS() override;
    virtual ErrorOr<void> initialize() override;

    virtual unsigned total_block_count() const override;
    virtual unsigned free_block_count() const override;
    virtual unsigned total_inode_count() const override;
    virtual unsigned free_inode_count() const override;

    virtual ErrorOr<void> prepare_to_unmount() override;

    virtual bool supports_watchers() const override { return true; }

    virtual u8 internal_file_type_to_directory_entry_type(const DirectoryEntryView& entry) const override;

    FeaturesReadOnly get_features_readonly() const;

private:
    TYPEDEF_DISTINCT_ORDERED_ID(unsigned, GroupIndex);

    explicit Ext2FS(OpenFileDescription&);

    const ext2_super_block& super_block() const { return m_super_block; }
    const ext2_group_desc& group_descriptor(GroupIndex) const;
    ext2_group_desc* block_group_descriptors() { return (ext2_group_desc*)m_cached_group_descriptor_table->data(); }
    const ext2_group_desc* block_group_descriptors() const { return (const ext2_group_desc*)m_cached_group_descriptor_table->data(); }
    void flush_block_group_descriptor_table();
    u64 inodes_per_block() const;
    u64 inodes_per_group() const;
    u64 blocks_per_group() const;
    u64 inode_size() const;

    ErrorOr<void> write_ext2_inode(InodeIndex, ext2_inode const&);
    bool find_block_containing_inode(InodeIndex, BlockIndex& block_index, unsigned& offset) const;

    bool flush_super_block();

    virtual StringView class_name() const override { return "Ext2FS"sv; }
    virtual Ext2FSInode& root_inode() override;
    ErrorOr<NonnullRefPtr<Inode>> get_inode(InodeIdentifier) const;
    ErrorOr<NonnullRefPtr<Inode>> create_inode(Ext2FSInode& parent_inode, StringView name, mode_t, dev_t, UserID, GroupID);
    ErrorOr<NonnullRefPtr<Inode>> create_directory(Ext2FSInode& parent_inode, StringView name, mode_t, UserID, GroupID);
    virtual void flush_writes() override;

    BlockIndex first_block_index() const;
    ErrorOr<InodeIndex> allocate_inode(GroupIndex preferred_group = 0);
    ErrorOr<Vector<BlockIndex>> allocate_blocks(GroupIndex preferred_group_index, size_t count);
    GroupIndex group_index_from_inode(InodeIndex) const;
    GroupIndex group_index_from_block_index(BlockIndex) const;

    ErrorOr<bool> get_inode_allocation_state(InodeIndex) const;
    ErrorOr<void> set_inode_allocation_state(InodeIndex, bool);
    ErrorOr<void> set_block_allocation_state(BlockIndex, bool);

    void uncache_inode(InodeIndex);
    ErrorOr<void> free_inode(Ext2FSInode&);

    struct BlockListShape {
        unsigned direct_blocks { 0 };
        unsigned indirect_blocks { 0 };
        unsigned doubly_indirect_blocks { 0 };
        unsigned triply_indirect_blocks { 0 };
        unsigned meta_blocks { 0 };
    };

    BlockListShape compute_block_list_shape(unsigned blocks) const;

    u64 m_block_group_count { 0 };

    mutable ext2_super_block m_super_block;
    mutable OwnPtr<KBuffer> m_cached_group_descriptor_table;

    mutable HashMap<InodeIndex, RefPtr<Ext2FSInode>> m_inode_cache;

    bool m_super_block_dirty { false };
    bool m_block_group_descriptors_dirty { false };

    struct CachedBitmap {
        CachedBitmap(BlockIndex bi, NonnullOwnPtr<KBuffer> buf)
            : bitmap_block_index(bi)
            , buffer(move(buf))
        {
        }
        BlockIndex bitmap_block_index { 0 };
        bool dirty { false };
        NonnullOwnPtr<KBuffer> buffer;
        Bitmap bitmap(u32 blocks_per_group) { return Bitmap { buffer->data(), blocks_per_group }; }
    };

    ErrorOr<CachedBitmap*> get_bitmap_block(BlockIndex);
    ErrorOr<void> update_bitmap_block(BlockIndex bitmap_block, size_t bit_index, bool new_state, u32& super_block_counter, u16& group_descriptor_counter);

    Vector<OwnPtr<CachedBitmap>> m_cached_bitmaps;
    RefPtr<Ext2FSInode> m_root_inode;
};

inline Ext2FS& Ext2FSInode::fs()
{
    return static_cast<Ext2FS&>(Inode::fs());
}

inline const Ext2FS& Ext2FSInode::fs() const
{
    return static_cast<const Ext2FS&>(Inode::fs());
}

}
