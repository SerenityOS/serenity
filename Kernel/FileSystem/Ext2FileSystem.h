#pragma once

#include <Kernel/FileSystem/DiskBackedFileSystem.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/FileSystem/ext2_fs.h>
#include <Kernel/UnixTypes.h>

struct ext2_group_desc;
struct ext2_inode;
struct ext2_super_block;

class Ext2FS;

class Ext2FSInode final : public Inode {
    friend class Ext2FS;

public:
    virtual ~Ext2FSInode() override;

    size_t size() const { return m_raw_inode.i_size; }
    bool is_symlink() const { return ::is_symlink(m_raw_inode.i_mode); }

    // ^Inode (RefCounted magic)
    virtual void one_ref_left() override;

private:
    // ^Inode
    virtual ssize_t read_bytes(off_t, ssize_t, u8* buffer, FileDescription*) const override;
    virtual InodeMetadata metadata() const override;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) const override;
    virtual InodeIdentifier lookup(StringView name) override;
    virtual void flush_metadata() override;
    virtual ssize_t write_bytes(off_t, ssize_t, const u8* data, FileDescription*) override;
    virtual KResult add_child(InodeIdentifier child_id, const StringView& name, mode_t) override;
    virtual KResult remove_child(const StringView& name) override;
    virtual int set_atime(time_t) override;
    virtual int set_ctime(time_t) override;
    virtual int set_mtime(time_t) override;
    virtual int increment_link_count() override;
    virtual int decrement_link_count() override;
    virtual size_t directory_entry_count() const override;
    virtual KResult chmod(mode_t) override;
    virtual KResult chown(uid_t, gid_t) override;
    virtual KResult truncate(off_t) override;

    bool write_directory(const Vector<FS::DirectoryEntry>&);
    void populate_lookup_cache() const;
    bool resize(u64);

    Ext2FS& fs();
    const Ext2FS& fs() const;
    Ext2FSInode(Ext2FS&, unsigned index);

    mutable Vector<unsigned> m_block_list;
    mutable HashMap<String, unsigned> m_lookup_cache;
    ext2_inode m_raw_inode;
};

class Ext2FS final : public DiskBackedFS {
    friend class Ext2FSInode;

public:
    static NonnullRefPtr<Ext2FS> create(NonnullRefPtr<DiskDevice>);
    virtual ~Ext2FS() override;
    virtual bool initialize() override;

    virtual unsigned total_block_count() const override;
    virtual unsigned free_block_count() const override;
    virtual unsigned total_inode_count() const override;
    virtual unsigned free_inode_count() const override;

private:
    typedef unsigned BlockIndex;
    typedef unsigned GroupIndex;
    typedef unsigned InodeIndex;
    explicit Ext2FS(NonnullRefPtr<DiskDevice>&&);

    const ext2_super_block& super_block() const;
    const ext2_group_desc& group_descriptor(unsigned groupIndex) const;
    void flush_block_group_descriptor_table();
    unsigned first_block_of_group(unsigned groupIndex) const;
    unsigned inodes_per_block() const;
    unsigned inodes_per_group() const;
    unsigned blocks_per_group() const;
    unsigned inode_size() const;

    bool write_ext2_inode(InodeIndex, const ext2_inode&);
    ByteBuffer read_block_containing_inode(InodeIndex inode, BlockIndex& block_index, unsigned& offset) const;

    ByteBuffer read_super_block() const;
    bool write_super_block(const ext2_super_block&);

    virtual const char* class_name() const override;
    virtual InodeIdentifier root_inode() const override;
    virtual RefPtr<Inode> create_inode(InodeIdentifier parentInode, const String& name, mode_t, off_t size, dev_t, int& error) override;
    virtual RefPtr<Inode> create_directory(InodeIdentifier parentInode, const String& name, mode_t, int& error) override;
    virtual RefPtr<Inode> get_inode(InodeIdentifier) const override;

    InodeIndex allocate_inode(GroupIndex preferred_group, off_t expected_size);
    Vector<BlockIndex> allocate_blocks(GroupIndex, int count);
    GroupIndex group_index_from_inode(InodeIndex) const;
    GroupIndex group_index_from_block_index(BlockIndex) const;

    Vector<BlockIndex> block_list_for_inode(const ext2_inode&, bool include_block_list_blocks = false) const;
    bool write_block_list_for_inode(InodeIndex, ext2_inode&, const Vector<BlockIndex>&);

    bool get_inode_allocation_state(InodeIndex) const;
    bool set_inode_allocation_state(InodeIndex, bool);
    bool set_block_allocation_state(BlockIndex, bool);

    void uncache_inode(InodeIndex);
    void free_inode(Ext2FSInode&);

    struct BlockListShape {
        unsigned direct_blocks { 0 };
        unsigned indirect_blocks { 0 };
        unsigned doubly_indirect_blocks { 0 };
        unsigned triply_indirect_blocks { 0 };
        unsigned meta_blocks { 0 };
    };

    BlockListShape compute_block_list_shape(unsigned blocks);

    unsigned m_block_group_count { 0 };

    mutable ByteBuffer m_cached_super_block;
    mutable ByteBuffer m_cached_group_descriptor_table;

    mutable HashMap<BlockIndex, RefPtr<Ext2FSInode>> m_inode_cache;
};

inline Ext2FS& Ext2FSInode::fs()
{
    return static_cast<Ext2FS&>(Inode::fs());
}

inline const Ext2FS& Ext2FSInode::fs() const
{
    return static_cast<const Ext2FS&>(Inode::fs());
}
