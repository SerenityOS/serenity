/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/BitmapView.h>
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
    virtual ssize_t read_bytes(off_t, ssize_t, UserOrKernelBuffer& buffer, FileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual KResult traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)>) const override;
    virtual RefPtr<Inode> lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual ssize_t write_bytes(off_t, ssize_t, const UserOrKernelBuffer& data, FileDescription*) override;
    virtual KResultOr<NonnullRefPtr<Inode>> create_child(const String& name, mode_t, dev_t, uid_t, gid_t) override;
    virtual KResult add_child(Inode& child, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual int set_atime(time_t) override;
    virtual int set_ctime(time_t) override;
    virtual int set_mtime(time_t) override;
    virtual KResult increment_link_count() override;
    virtual KResult decrement_link_count() override;
    virtual KResultOr<size_t> directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult truncate(u64) override;
    virtual KResultOr<int> get_block_address(int) override;

    KResult write_directory(const Vector<Ext2FSDirectoryEntry>&);
    bool populate_lookup_cache() const;
    KResult resize(u64);
    KResult write_indirect_block(BlockBasedFS::BlockIndex, Span<BlockBasedFS::BlockIndex>);
    KResult grow_doubly_indirect_block(BlockBasedFS::BlockIndex, size_t, Span<BlockBasedFS::BlockIndex>, Vector<BlockBasedFS::BlockIndex>&, unsigned&);
    KResult shrink_doubly_indirect_block(BlockBasedFS::BlockIndex, size_t, size_t, unsigned&);
    KResult grow_triply_indirect_block(BlockBasedFS::BlockIndex, size_t, Span<BlockBasedFS::BlockIndex>, Vector<BlockBasedFS::BlockIndex>&, unsigned&);
    KResult shrink_triply_indirect_block(BlockBasedFS::BlockIndex, size_t, size_t, unsigned&);
    KResult flush_block_list();
    Vector<BlockBasedFS::BlockIndex> compute_block_list() const;
    Vector<BlockBasedFS::BlockIndex> compute_block_list_with_meta_blocks() const;
    Vector<BlockBasedFS::BlockIndex> compute_block_list_impl(bool include_block_list_blocks) const;
    Vector<BlockBasedFS::BlockIndex> compute_block_list_impl_internal(const ext2_inode& e2inode, bool include_block_list_blocks) const;

    Ext2FS& fs();
    const Ext2FS& fs() const;
    Ext2FSInode(Ext2FS&, InodeIndex);

    mutable Vector<BlockBasedFS::BlockIndex> m_block_list;
    mutable HashMap<String, InodeIndex> m_lookup_cache;
    ext2_inode m_raw_inode;
};

class Ext2FS final : public BlockBasedFS {
    friend class Ext2FSInode;

public:
    enum class FeaturesReadOnly : u32 {
        None = 0,
        FileSize64bits = 1 << 1,
    };

    static NonnullRefPtr<Ext2FS> create(FileDescription&);

    virtual ~Ext2FS() override;
    virtual bool initialize() override;

    virtual unsigned total_block_count() const override;
    virtual unsigned free_block_count() const override;
    virtual unsigned total_inode_count() const override;
    virtual unsigned free_inode_count() const override;

    virtual KResult prepare_to_unmount() const override;

    virtual bool supports_watchers() const override { return true; }

    virtual u8 internal_file_type_to_directory_entry_type(const DirectoryEntryView& entry) const override;

    FeaturesReadOnly get_features_readonly() const;

private:
    TYPEDEF_DISTINCT_ORDERED_ID(unsigned, GroupIndex);

    explicit Ext2FS(FileDescription&);

    const ext2_super_block& super_block() const { return m_super_block; }
    const ext2_group_desc& group_descriptor(GroupIndex) const;
    ext2_group_desc* block_group_descriptors() { return (ext2_group_desc*)m_cached_group_descriptor_table->data(); }
    const ext2_group_desc* block_group_descriptors() const { return (const ext2_group_desc*)m_cached_group_descriptor_table->data(); }
    void flush_block_group_descriptor_table();
    unsigned inodes_per_block() const;
    unsigned inodes_per_group() const;
    unsigned blocks_per_group() const;
    unsigned inode_size() const;

    bool write_ext2_inode(InodeIndex, const ext2_inode&);
    bool find_block_containing_inode(InodeIndex, BlockIndex& block_index, unsigned& offset) const;

    bool flush_super_block();

    virtual const char* class_name() const override;
    virtual NonnullRefPtr<Inode> root_inode() const override;
    RefPtr<Inode> get_inode(InodeIdentifier) const;
    KResultOr<NonnullRefPtr<Inode>> create_inode(Ext2FSInode& parent_inode, const String& name, mode_t, dev_t, uid_t, gid_t);
    KResult create_directory(Ext2FSInode& parent_inode, const String& name, mode_t, uid_t, gid_t);
    virtual void flush_writes() override;

    BlockIndex first_block_index() const;
    KResultOr<InodeIndex> allocate_inode(GroupIndex preferred_group = 0);
    KResultOr<Vector<BlockIndex>> allocate_blocks(GroupIndex preferred_group_index, size_t count);
    GroupIndex group_index_from_inode(InodeIndex) const;
    GroupIndex group_index_from_block_index(BlockIndex) const;

    KResultOr<bool> get_inode_allocation_state(InodeIndex) const;
    KResult set_inode_allocation_state(InodeIndex, bool);
    KResult set_block_allocation_state(BlockIndex, bool);

    void uncache_inode(InodeIndex);
    void free_inode(Ext2FSInode&);

    struct BlockListShape {
        unsigned direct_blocks { 0 };
        unsigned indirect_blocks { 0 };
        unsigned doubly_indirect_blocks { 0 };
        unsigned triply_indirect_blocks { 0 };
        unsigned meta_blocks { 0 };
    };

    BlockListShape compute_block_list_shape(unsigned blocks) const;

    unsigned m_block_group_count { 0 };

    mutable ext2_super_block m_super_block;
    mutable OwnPtr<KBuffer> m_cached_group_descriptor_table;

    mutable HashMap<InodeIndex, RefPtr<Ext2FSInode>> m_inode_cache;

    bool m_super_block_dirty { false };
    bool m_block_group_descriptors_dirty { false };

    struct CachedBitmap {
        CachedBitmap(BlockIndex bi, KBuffer&& buf)
            : bitmap_block_index(bi)
            , buffer(move(buf))
        {
        }
        BlockIndex bitmap_block_index { 0 };
        bool dirty { false };
        KBuffer buffer;
        BitmapView bitmap(u32 blocks_per_group) { return BitmapView { buffer.data(), blocks_per_group }; }
    };

    KResultOr<CachedBitmap*> get_bitmap_block(BlockIndex);
    KResult update_bitmap_block(BlockIndex bitmap_block, size_t bit_index, bool new_state, u32& super_block_counter, u16& group_descriptor_counter);

    Vector<OwnPtr<CachedBitmap>> m_cached_bitmaps;
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
