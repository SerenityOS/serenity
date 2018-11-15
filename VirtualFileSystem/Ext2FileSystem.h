#pragma once

#include "DiskBackedFileSystem.h"
#include "UnixTypes.h"
#include <AK/Buffer.h>
#include <AK/OwnPtr.h>
#include "ext2_fs.h"

struct ext2_group_desc;
struct ext2_inode;
struct ext2_super_block;

class Ext2FileSystem;

class Ext2Inode final : public CoreInode {
    friend class Ext2FileSystem;
public:
    virtual ~Ext2Inode() override;

    size_t size() const { return m_raw_inode.i_size; }
    bool is_symlink() const { return isSymbolicLink(m_raw_inode.i_mode); }

private:
    // ^CoreInode
    virtual Unix::ssize_t read_bytes(Unix::off_t, Unix::size_t, byte* buffer, FileDescriptor*) override;
    virtual void populate_metadata() const override;
    virtual bool traverse_as_directory(Function<bool(const FileSystem::DirectoryEntry&)>) override;

    Ext2FileSystem& fs();
    const Ext2FileSystem& fs() const;
    Ext2Inode(Ext2FileSystem&, unsigned index, const ext2_inode&);

    SpinLock m_lock;
    Vector<unsigned> m_block_list;
    ext2_inode m_raw_inode;
};

class Ext2FileSystem final : public DiskBackedFileSystem {
    friend class Ext2Inode;
public:
    static RetainPtr<Ext2FileSystem> create(RetainPtr<DiskDevice>&&);
    virtual ~Ext2FileSystem() override;
    virtual bool initialize() override;

private:
    typedef unsigned BlockIndex;
    typedef unsigned GroupIndex;
    typedef unsigned InodeIndex;
    class CachedExt2Inode;
    class CachedExt2InodeImpl;

    explicit Ext2FileSystem(RetainPtr<DiskDevice>&&);

    const ext2_super_block& superBlock() const;
    const ext2_group_desc& blockGroupDescriptor(unsigned groupIndex) const;
    unsigned firstBlockOfGroup(unsigned groupIndex) const;
    unsigned inodesPerBlock() const;
    unsigned inodesPerGroup() const;
    unsigned blocksPerGroup() const;
    unsigned inodeSize() const;

    CachedExt2Inode lookupExt2Inode(unsigned) const;
    bool writeExt2Inode(unsigned, const ext2_inode&);
    ByteBuffer readBlockContainingInode(unsigned inode, unsigned& blockIndex, unsigned& offset) const;

    ByteBuffer readSuperBlock() const;
    bool writeSuperBlock(const ext2_super_block&);

    virtual const char* className() const override;
    virtual InodeIdentifier rootInode() const override;
    virtual bool writeInode(InodeIdentifier, const ByteBuffer&) override;
    virtual bool enumerateDirectoryInode(InodeIdentifier, Function<bool(const DirectoryEntry&)>) const override;
    virtual InodeMetadata inodeMetadata(InodeIdentifier) const override;
    virtual bool setModificationTime(InodeIdentifier, dword timestamp) override;
    virtual InodeIdentifier createInode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size) override;
    virtual Unix::ssize_t readInodeBytes(InodeIdentifier, Unix::off_t offset, Unix::size_t count, byte* buffer, FileDescriptor*) const override;
    virtual InodeIdentifier makeDirectory(InodeIdentifier parentInode, const String& name, Unix::mode_t) override;
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

    bool addInodeToDirectory(unsigned directoryInode, unsigned inode, const String& name, byte fileType);
    bool writeDirectoryInode(unsigned directoryInode, Vector<DirectoryEntry>&&);
    bool setInodeAllocationState(unsigned inode, bool);
    bool setBlockAllocationState(GroupIndex, BlockIndex, bool);

    bool modifyLinkCount(InodeIndex, int delta);

    unsigned m_blockGroupCount { 0 };

    mutable ByteBuffer m_cachedSuperBlock;
    mutable ByteBuffer m_cachedBlockGroupDescriptorTable;

    mutable SpinLock m_inodeCacheLock;
    mutable HashMap<unsigned, RetainPtr<CachedExt2InodeImpl>> m_inodeCache;

    mutable SpinLock m_inode_cache_lock;
    mutable HashMap<BlockIndex, RetainPtr<Ext2Inode>> m_inode_cache;
};

inline Ext2FileSystem& Ext2Inode::fs()
{
    return static_cast<Ext2FileSystem&>(CoreInode::fs());
}

inline const Ext2FileSystem& Ext2Inode::fs() const
{
    return static_cast<const Ext2FileSystem&>(CoreInode::fs());
}
