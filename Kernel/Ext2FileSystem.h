#pragma once

#include "ext2fs.h"
#include "OwnPtr.h"
#include "DataBuffer.h"
#include "FileSystem.h"

static const size_t bytesPerSector = 512;

class Ext2VirtualNode;

class Ext2FileSystem {
public:
    Ext2FileSystem() { }
    ~Ext2FileSystem();

    void initialize();
    RefPtr<DataBuffer> loadFile(ext2_dir_entry*);

    ext2_inode* findInode(DWORD index);
    ext2_inode* findPath(const String& path, DWORD& inodeIndex);

private:
    friend class Ext2VirtualNode;

    void readSuperBlock();
    void readBlockGroup(DWORD);
    void readInodeTable(DWORD);
    void dumpDirectory(ext2_inode&);
    void dumpFile(ext2_inode&);

    template<typename F> void forEachBlockIn(ext2_inode&, F func);
    template<typename F> void traverseDirectory(ext2_dir_entry&, DWORD blockCount, F);
    template<typename F> void traverseDirectory(ext2_inode&, F);
    RefPtr<DataBuffer> readFile(ext2_inode&);

    RefPtr<DataBuffer> readBlocks(DWORD blockIndex, BYTE count);
    void readDiskSector(DWORD sectorIndex, BYTE* buffer);

    size_t blockSize() const { return 1024 << m_superBlock->s_log_frag_size; }
    size_t sectorsPerBlock() const { return blockSize() / bytesPerSector; }
    DWORD blockGroupForInode(DWORD inode) const;
    DWORD toInodeTableIndex(DWORD inode) const;

    ext2_super_block& superBlock() { ASSERT(m_superBlock); return *m_superBlock; }

    OwnPtr<ext2_super_block> m_superBlock;
    ext2_inode* m_root { nullptr }; // raw pointer into one of the m_inodeTables

    size_t m_blockGroupCount { 0 };
    ext2_group_descriptor* m_groupTable { nullptr };
    ext2_inode** m_inodeTables { nullptr };
};

class Ext2VirtualNode final : public FileSystem::VirtualNode {
public:
    static RefPtr<Ext2VirtualNode> create(DWORD index, String&& path, Ext2FileSystem&, DWORD inodeNumber);

    virtual ~Ext2VirtualNode();

    virtual size_t size() const override { return m_inode.i_size; }
    virtual uid_t uid() const override { return m_inode.i_uid; }
    virtual gid_t gid() const override { return m_inode.i_gid; }
    virtual size_t mode() const override { return m_inode.i_mode; }

    virtual size_t read(BYTE* outbuf, size_t start, size_t maxLength) override;

private:
    Ext2VirtualNode(DWORD index, String&& path, Ext2FileSystem&, ext2_inode&, DWORD inodeNumber);

    Ext2FileSystem& m_fileSystem;
    ext2_inode& m_inode;
    DWORD m_inodeNumber { 0 };
};
