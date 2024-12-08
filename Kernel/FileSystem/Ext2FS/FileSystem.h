/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Bitmap.h>
#include <AK/HashMap.h>
#include <Kernel/FileSystem/BlockBasedFileSystem.h>
#include <Kernel/FileSystem/Ext2FS/Definitions.h>
#include <Kernel/FileSystem/FileSystemSpecificOption.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Library/KBuffer.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class Ext2FSInode;

class Ext2FS final : public BlockBasedFileSystem {
    friend class Ext2FSInode;

public:
    // s_feature_compat
    enum class FeaturesOptional : u32 {
        None = 0,
        ExtendedAttributes = EXT2_FEATURE_COMPAT_EXT_ATTR,
    };
    AK_ENUM_BITWISE_FRIEND_OPERATORS(FeaturesOptional);

    // s_feature_ro_compat
    enum class FeaturesReadOnly : u32 {
        None = 0,
        SparseSuperblock = EXT2_FEATURE_RO_COMPAT_SPARSE_SUPER,
        FileSize64bits = EXT2_FEATURE_RO_COMPAT_LARGE_FILE,
    };
    AK_ENUM_BITWISE_FRIEND_OPERATORS(FeaturesReadOnly);

    static ErrorOr<NonnullRefPtr<FileSystem>> try_create(OpenFileDescription&, FileSystemSpecificOptions const&);

    virtual ~Ext2FS() override;

    virtual unsigned total_block_count() const override;
    virtual unsigned free_block_count() const override;
    virtual unsigned total_inode_count() const override;
    virtual unsigned free_inode_count() const override;

    virtual bool supports_watchers() const override { return true; }
    virtual bool supports_backing_loop_devices() const override { return true; }

    virtual ErrorOr<void> rename(Inode& old_parent_inode, StringView old_basename, Inode& new_parent_inode, StringView new_basename) override;

    virtual u8 internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const override;

    FeaturesOptional get_features_optional() const;
    FeaturesReadOnly get_features_readonly() const;

    u32 i_blocks_increment() { return m_i_blocks_increment; }

    virtual StringView class_name() const override { return "Ext2FS"sv; }
    virtual Inode& root_inode() override;

    using BlockList = HashMap<BlockBasedFileSystem::BlockIndex, BlockBasedFileSystem::BlockIndex>;

private:
    AK_TYPEDEF_DISTINCT_ORDERED_ID(unsigned, GroupIndex);

    explicit Ext2FS(OpenFileDescription&);

    ext2_super_block const& super_block() const { return m_super_block; }
    ext2_group_desc const& group_descriptor(GroupIndex) const;
    ext2_group_desc* block_group_descriptors() { return (ext2_group_desc*)m_cached_group_descriptor_table->data(); }
    ext2_group_desc const* block_group_descriptors() const { return (ext2_group_desc const*)m_cached_group_descriptor_table->data(); }
    void flush_block_group_descriptor_table();
    u64 inodes_per_block() const;
    u64 inodes_per_group() const;
    u64 blocks_per_group() const;
    u64 inode_size() const;

    ErrorOr<NonnullRefPtr<Ext2FSInode>> build_root_inode() const;

    // NOTE: The large Ext2 inode structure is strictly superset of the classic 128-byte inode structure,
    // so the this function simply ignores all the extra data if the filesystem doesn't support large inodes.
    ErrorOr<void> write_ext2_inode(InodeIndex, ext2_inode_large const&);

    bool find_block_containing_inode(InodeIndex, BlockIndex& block_index, unsigned& offset) const;

    ErrorOr<void> flush_super_block();

    virtual ErrorOr<void> initialize_while_locked() override;
    virtual bool is_initialized_while_locked() override;

    virtual ErrorOr<void> prepare_to_clear_last_mount(Inode& mount_guest_inode) override;
    ErrorOr<NonnullRefPtr<Inode>> get_inode(InodeIdentifier) const;
    ErrorOr<NonnullRefPtr<Inode>> create_inode(Ext2FSInode& parent_inode, StringView name, mode_t, dev_t, UserID, GroupID);
    ErrorOr<NonnullRefPtr<Inode>> create_directory(Ext2FSInode& parent_inode, StringView name, mode_t, UserID, GroupID);
    virtual ErrorOr<void> flush_writes() override;

    BlockIndex first_block_index() const;
    BlockIndex first_block_of_block_group_descriptors() const;
    ErrorOr<InodeIndex> allocate_inode(GroupIndex preferred_group = 0);
    ErrorOr<Vector<BlockIndex>> allocate_blocks(GroupIndex preferred_group_index, size_t count);
    GroupIndex group_index_from_inode(InodeIndex) const;
    GroupIndex group_index_from_block_index(BlockIndex) const;
    BlockIndex first_block_of_group(GroupIndex) const;

    ErrorOr<bool> get_inode_allocation_state(InodeIndex) const;
    ErrorOr<void> set_inode_allocation_state(InodeIndex, bool);
    ErrorOr<void> set_block_allocation_state(BlockIndex, bool);

    void uncache_inode(InodeIndex);
    ErrorOr<void> free_inode(Ext2FSInode&);

    u64 m_block_group_count { 0 };
    u32 m_i_blocks_increment { 0 };

    mutable ext2_super_block m_super_block {};
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

}
