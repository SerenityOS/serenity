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

class Ext2FSInode final : public CoreInode {
    friend class Ext2FS;
public:
    virtual ~Ext2FSInode() override;

    size_t size() const { return m_raw_inode.i_size; }
    bool is_symlink() const { return isSymbolicLink(m_raw_inode.i_mode); }

private:
    // ^CoreInode
    virtual ssize_t read_bytes(Unix::off_t, size_t, byte* buffer, FileDescriptor*) override;
    virtual void populate_metadata() const override;
    virtual bool traverse_as_directory(Function<bool(const FS::DirectoryEntry&)>) override;
    virtual InodeIdentifier lookup(const String& name) override;
    virtual String reverse_lookup(InodeIdentifier) override;

    void populate_lookup_cache();

    Ext2FS& fs();
    const Ext2FS& fs() const;
    Ext2FSInode(Ext2FS&, unsigned index, const ext2_inode&);

    SpinLock m_lock;
    Vector<unsigned> m_block_list;
    HashMap<String, unsigned> m_lookup_cache;
    ext2_inode m_raw_inode;
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

    const ext2_super_block& superBlock() const;
    const ext2_group_desc& blockGroupDescriptor(unsigned groupIndex) const;
    unsigned firstBlockOfGroup(unsigned groupIndex) const;
    unsigned inodesPerBlock() const;
    unsigned inodesPerGroup() const;
    unsigned blocksPerGroup() const;
    unsigned inodeSize() const;

    OwnPtr<ext2_inode> lookupExt2Inode(unsigned) const;
    bool writeExt2Inode(unsigned, const ext2_inode&);
    ByteBuffer readBlockContainingInode(unsigned inode, unsigned& blockIndex, unsigned& offset) const;

    ByteBuffer readSuperBlock() const;
    bool writeSuperBlock(const ext2_super_block&);

    virtual const char* class_name() const override;
    virtual InodeIdentifier rootInode() const override;
    virtual bool writeInode(InodeIdentifier, const ByteBuffer&) override;
    virtual InodeMetadata inodeMetadata(InodeIdentifier) const override;
    virtual bool set_mtime(InodeIdentifier, dword timestamp) override;
    virtual InodeIdentifier create_inode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size, int& error) override;
    virtual ssize_t read_inode_bytes(InodeIdentifier, Unix::off_t offset, size_t count, byte* buffer, FileDescriptor*) const override;
    virtual InodeIdentifier create_directory(InodeIdentifier parentInode, const String& name, Unix::mode_t, int& error) override;
    virtual InodeIdentifier find_parent_of_inode(InodeIdentifier) const override;
    virtual RetainPtr<CoreInode> get_inode(InodeIdentifier) const override;

    bool isDirectoryInode(unsigned) const;
    unsigned allocateInode(unsigned preferredGroup, unsigned expectedSize);
    Vector<BlockIndex> allocateBlocks(unsigned group, unsigned count);
    unsigned groupIndexFromInode(unsigned) const;

    Vector<unsigned> blockListForInode(const ext2_inode&) const;

    void dumpBlockBitmap(unsigned groupIndex) const;
    void dumpInodeBitmap(unsigned groupIndex) const;

    template<typename F> void traverseInodeBitmap(unsigned groupIndex, F) const;
    template<typename F> void traverseBlockBitmap(unsigned groupIndex, F) const;

    bool addInodeToDirectory(unsigned directoryInode, unsigned inode, const String& name, byte fileType, int& error);
    bool writeDirectoryInode(unsigned directoryInode, Vector<DirectoryEntry>&&);
    bool setInodeAllocationState(unsigned inode, bool);
    bool setBlockAllocationState(GroupIndex, BlockIndex, bool);

    bool modifyLinkCount(InodeIndex, int delta);

    unsigned m_blockGroupCount { 0 };

    bool deprecated_enumerateDirectoryInode(InodeIdentifier, Function<bool(const DirectoryEntry&)>) const;

    mutable ByteBuffer m_cachedSuperBlock;
    mutable ByteBuffer m_cachedBlockGroupDescriptorTable;

    mutable SpinLock m_inode_cache_lock;
    mutable HashMap<BlockIndex, RetainPtr<Ext2FSInode>> m_inode_cache;
};

inline Ext2FS& Ext2FSInode::fs()
{
    return static_cast<Ext2FS&>(CoreInode::fs());
}

inline const Ext2FS& Ext2FSInode::fs() const
{
    return static_cast<const Ext2FS&>(CoreInode::fs());
}
