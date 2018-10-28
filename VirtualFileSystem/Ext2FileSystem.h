#pragma once

#include "DiskBackedFileSystem.h"
#include "UnixTypes.h"
#include <AK/Buffer.h>
#include <AK/OwnPtr.h>

struct ext2_group_desc;
struct ext2_inode;
struct ext2_super_block;

class Ext2FileSystem final : public DiskBackedFileSystem {
public:
    static RetainPtr<Ext2FileSystem> create(RetainPtr<DiskDevice>&&);
    virtual ~Ext2FileSystem() override;
    virtual bool initialize() override;

private:
    typedef unsigned BlockIndex;
    typedef unsigned GroupIndex;
    typedef unsigned InodeIndex;

    explicit Ext2FileSystem(RetainPtr<DiskDevice>&&);

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

    virtual const char* className() const override;
    virtual InodeIdentifier rootInode() const override;
    virtual bool writeInode(InodeIdentifier, const ByteBuffer&) override;
    virtual bool enumerateDirectoryInode(InodeIdentifier, Function<bool(const DirectoryEntry&)>) const override;
    virtual InodeMetadata inodeMetadata(InodeIdentifier) const override;
    virtual bool setModificationTime(InodeIdentifier, dword timestamp) override;
    virtual InodeIdentifier createInode(InodeIdentifier parentInode, const String& name, Unix::mode_t, unsigned size) override;
    virtual Unix::ssize_t readInodeBytes(InodeIdentifier, Unix::off_t offset, Unix::size_t count, byte* buffer, FileHandle*) const override;
    virtual InodeIdentifier makeDirectory(InodeIdentifier parentInode, const String& name, Unix::mode_t) override;
    virtual InodeIdentifier findParentOfInode(InodeIdentifier) const override;

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
};

