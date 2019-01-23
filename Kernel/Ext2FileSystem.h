#pragma once

#include "DiskBackedFileSystem.h"
#include "UnixTypes.h"
#include <AK/Buffer.h>
#include <AK/OwnPtr.h>
#include "ext2_fs.h"

struct ext2_group_desc;
struct ext2_inode;
struct ext2_super_block;

class Ext2FS;

class Ext2FSInode final : public Inode {
    friend class Ext2FS;
public:
    virtual ~Ext2FSInode() override;

    size_t size() const { return m_raw_inode.i_size; }
    bool is_symlink() const { return isSymbolicLink(m_raw_inode.i_mode); }

    // ^Inode (Retainable magic)
    virtual void one_retain_left() override;

private:
    // ^Inode
    virtual ssize_t read_bytes(Unix::off_t, size_t, byte* buffer, FileDescriptor*) override;
    virtual InodeMetadata metadata() const override;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) override;
    virtual InodeIdentifier lookup(const String& name) override;
    virtual String reverse_lookup(InodeIdentifier) override;
    virtual void flush_metadata() override;
    virtual ssize_t write_bytes(Unix::off_t, size_t, const byte* data, FileDescriptor*) override;
    virtual bool add_child(InodeIdentifier child_id, const String& name, byte file_type, int& error) override;
    virtual bool remove_child(const String& name, int& error) override;
    virtual RetainPtr<Inode> parent() const override;
    virtual int set_atime(Unix::time_t) override;
    virtual int set_ctime(Unix::time_t) override;
    virtual int set_mtime(Unix::time_t) override;
    virtual int increment_link_count() override;
    virtual int decrement_link_count() override;

    void populate_lookup_cache();

    Ext2FS& fs();
    const Ext2FS& fs() const;
    Ext2FSInode(Ext2FS&, unsigned index, const ext2_inode&);

    Vector<unsigned> m_block_list;
    HashMap<String, unsigned> m_lookup_cache;
    ext2_inode m_raw_inode;
    mutable InodeIdentifier m_parent_id;
};

class Ext2FS final : public DiskBackedFS {
    friend class Ext2FSInode;
public:
    static RetainPtr<Ext2FS> create(RetainPtr<DiskDevice>&&);
    virtual ~Ext2FS() override;
    virtual bool initialize() override;

private:
    typedef unsigned BlockIndex;
    typedef unsigned GroupIndex;
    typedef unsigned InodeIndex;
    explicit Ext2FS(RetainPtr<DiskDevice>&&);

    const ext2_super_block& super_block() const;
    const ext2_group_desc& group_descriptor(unsigned groupIndex) const;
    unsigned first_block_of_group(unsigned groupIndex) const;
    unsigned inodes_per_block() const;
    unsigned inodes_per_group() const;
    unsigned blocks_per_group() const;
    unsigned inode_size() const;

    bool write_ext2_inode(unsigned, const ext2_inode&);
    ByteBuffer read_block_containing_inode(unsigned inode, unsigned& blockIndex, unsigned& offset) const;

    ByteBuffer read_super_block() const;
    bool write_super_block(const ext2_super_block&);

    virtual const char* class_name() const override;
    virtual InodeIdentifier root_inode() const override;
    virtual RetainPtr<Inode> create_inode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size, int& error) override;
    virtual RetainPtr<Inode> create_directory(InodeIdentifier parentInode, const String& name, Unix::mode_t, int& error) override;
    virtual RetainPtr<Inode> get_inode(InodeIdentifier) const override;

    unsigned allocate_inode(unsigned preferredGroup, unsigned expectedSize);
    Vector<BlockIndex> allocate_blocks(unsigned group, unsigned count);
    unsigned group_index_from_inode(unsigned) const;

    Vector<unsigned> block_list_for_inode(const ext2_inode&, bool include_block_list_blocks = false) const;
    bool write_block_list_for_inode(InodeIndex, ext2_inode&, const Vector<BlockIndex>&);

    void dump_block_bitmap(unsigned groupIndex) const;
    void dump_inode_bitmap(unsigned groupIndex) const;

    template<typename F> void traverse_inode_bitmap(unsigned groupIndex, F) const;
    template<typename F> void traverse_block_bitmap(unsigned groupIndex, F) const;

    bool add_inode_to_directory(InodeIndex parent, InodeIndex child, const String& name, byte fileType, int& error);
    bool write_directory_inode(unsigned directoryInode, Vector<DirectoryEntry>&&);
    bool get_inode_allocation_state(InodeIndex) const;
    bool set_inode_allocation_state(unsigned inode, bool);
    bool set_block_allocation_state(GroupIndex, BlockIndex, bool);

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

    unsigned m_blockGroupCount { 0 };

    mutable ByteBuffer m_cached_super_block;
    mutable ByteBuffer m_cached_group_descriptor_table;

    mutable Lock m_inode_cache_lock;
    mutable HashMap<BlockIndex, RetainPtr<Ext2FSInode>> m_inode_cache;
};

inline Ext2FS& Ext2FSInode::fs()
{
    return static_cast<Ext2FS&>(Inode::fs());
}

inline const Ext2FS& Ext2FSInode::fs() const
{
    return static_cast<const Ext2FS&>(Inode::fs());
}
