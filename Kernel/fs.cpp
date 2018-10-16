#include "Disk.h"
#include "Task.h"
#include "VGA.h"
#include "kmalloc.h"
#include "Ext2FileSystem.h"
#include "i386.h"
#include "StdLib.h"
#include "OwnPtr.h"
#include "FileSystem.h"
#include "String.h"

//#define FS_DEBUG

Ext2FileSystem::~Ext2FileSystem()
{
    kprintf("fs: kill Ext2FileSystem\n");
    ASSERT(false);
    kfree(m_groupTable);
    m_groupTable = nullptr;
    if (m_inodeTables) {
        for (DWORD i = 0; i < m_blockGroupCount; ++i)
            delete [] m_inodeTables[i];
    }
    delete [] m_inodeTables;
    m_inodeTables = nullptr;
}

static Ext2FileSystem* fileSystem;

void Ext2FileSystem::readDiskSector(DWORD sectorIndex, BYTE* buffer)
{
    Task::checkSanity("Ext2FileSystem::readDiskSector");
    bool success = Disk::readSectors(sectorIndex, 1, buffer);
    (void) success;
}

RefPtr<DataBuffer> Ext2FileSystem::readBlocks(DWORD blockIndex, BYTE count)
{
    Task::checkSanity("readBlocks");
    if (!m_superBlock) {
        kprintf("fs: Attempt to read blocks without superblock!\n");
        HANG;
    }

#ifdef FS_DEBUG
    kprintf("Read %u block(s) starting at %u\n", count, blockIndex);
#endif
    // FIXME: This is broken up into 1-sector reads because the disk task can't handle multi-sector reads yet.
    auto buffer = DataBuffer::createUninitialized(count * sectorsPerBlock() * bytesPerSector);
    BYTE* bufptr = (BYTE*)buffer->data();
    for (DWORD i = 0; i < count; ++i) {
        readDiskSector(((blockIndex + i) * sectorsPerBlock()) + 0, bufptr);
        readDiskSector(((blockIndex + i) * sectorsPerBlock()) + 1, bufptr + bytesPerSector);
        bufptr += bytesPerSector * 2;
    }
    return buffer;
}

void Ext2FileSystem::readSuperBlock()
{
    ASSERT(!m_superBlock);
    ASSERT(!m_groupTable);

    m_superBlock = make<ext2_super_block>();
    readDiskSector(2, (BYTE*)m_superBlock.ptr());

    if (m_superBlock->s_magic != EXT2_MAGIC) {
        kprintf("fs: PANIC! No ext2 filesystem found\n");
        HANG;
    }

    kprintf("fs: ext2 filesystem found -- %u inodes, %u blocks\n",
        m_superBlock->s_inodes_count,
        m_superBlock->s_blocks_count);

    const BYTE* u = m_superBlock->s_uuid;
    kprintf("fs: UUID: %b%b%b%b-%b%b-%b%b-%b%b-%b%b%b%b%b%b\n",
            u[0], u[1], u[2], u[3], u[4], u[5], u[6], u[7], u[8],
            u[9], u[10], u[11], u[12], u[13], u[14], u[15], u[16]);

#ifdef FS_DEBUG
    kprintf("fs: Block size is %u bytes\n", 1024 << m_superBlock->s_log_frag_size);
    kprintf("fs: Blocks per group: %u\n", m_superBlock->s_blocks_per_group);
#endif

    m_blockGroupCount = m_superBlock->s_blocks_count / m_superBlock->s_blocks_per_group;
    if ((m_superBlock->s_blocks_count % m_superBlock->s_blocks_per_group) != 0)
        ++m_blockGroupCount;

    m_inodeTables = new ext2_inode*[m_blockGroupCount];
    memset(m_inodeTables, 0, sizeof(ext2_inode*) * m_blockGroupCount);
}

void Ext2FileSystem::readBlockGroup(DWORD index)
{
    Task::checkSanity("readBlockGroup");
    DWORD superBlockBI = m_superBlock->s_first_data_block + (m_superBlock->s_blocks_per_group * index);
    DWORD descriptorTableBI = superBlockBI + 1;
    //DWORD blockBitmapBI = descriptorTableBI + 1;
    //DWORD inodeBitmapBI = blockBitmapBI + 1;
    //DWORD inodeTableBI = inodeBitmapBI + 1;

    auto buffer = readBlocks(descriptorTableBI, 1);

#ifdef FS_DEBUG
    kprintf("Inodes per group = %u\n", superBlock().s_inodes_per_group);
    kprintf("First data block = %u\n", superBlock().s_first_data_block);
#endif

    m_groupTable = (ext2_group_descriptor*)kmalloc(blockSize());
    memcpy(m_groupTable, buffer->data(), buffer->length());

#ifdef FS_DEBUG
    kprintf("[%u] block bitmap: %u\n", index, m_groupTable[index].bg_block_bitmap);
    kprintf("[%u] inode bitmap: %u\n", index, m_groupTable[index].bg_inode_bitmap);
    kprintf("[%u] inode table:  %u\n", index, m_groupTable[index].bg_inode_table);
#endif
}

template<typename F>
void Ext2FileSystem::traverseDirectory(ext2_dir_entry& firstEntry, DWORD blockCount, F func)
{
    Task::checkSanity("traverseDirectory1");
    auto* entry = &firstEntry;
    char* name = new char[EXT2_NAME_LEN + 1];
    auto* end = (ext2_dir_entry*)((BYTE*)entry + blockCount * blockSize());

    while (entry < end) {
        if (entry->d_inode != 0) {
            memcpy(name, entry->d_name, entry->d_name_len);
            name[entry->d_name_len] = 0;
            func(name, *entry);
        }
        entry = (ext2_dir_entry*)((BYTE *)entry + entry->d_rec_len);
    }

    delete [] name;
}

void Ext2FileSystem::readInodeTable(DWORD blockGroup)
{
    Task::checkSanity("readInodeTable");
    ext2_inode*& inodeTable = m_inodeTables[blockGroup];

    if (!inodeTable)
        inodeTable = new ext2_inode[m_superBlock->s_inodes_per_group];

    DWORD inodeTableBlocks = (m_superBlock->s_inodes_per_group * sizeof(ext2_inode)) / blockSize();
//    kprintf("inode table blocks: %u\n", inodeTableBlocks);

    auto buffer = readBlocks(m_groupTable[blockGroup].bg_inode_table, inodeTableBlocks);
    memcpy(inodeTable, buffer->data(), m_superBlock->s_inodes_per_group * sizeof(ext2_inode));
    m_root = &inodeTable[1];

#ifdef FS_DEBUG
    kprintf("Root directory inode:\n");
    kprintf("sizeof(ext2_inode): %u\n", sizeof(ext2_inode));
    kprintf("sizeof(ext2_dir_entry): %u\n", sizeof(ext2_dir_entry));
    kprintf("Mode: %u, Owner: %u/%u, Size: %u\n", m_root->i_mode, m_root->i_uid, m_root->i_gid, m_root->i_size);

    kprintf("Directory blocks: { ");
    for (DWORD i = 0; i < 12; ++i) {
        kprintf( "%u ", m_root->i_block[i] );
    }
    kprintf("}\n");
#endif
}

template<typename F>
void Ext2FileSystem::forEachBlockIn(ext2_inode& inode, F func)
{
    Task::checkSanity("forEachBlockIn");
    DWORD blockCount = inode.i_blocks / (2 << m_superBlock->s_log_block_size);
    // FIXME: Support indirect blocks
    for (DWORD i = 0; i < blockCount; ++i) {
        //kprintf(" [blk %u]\n", inode.i_block[i]);
        auto buffer = readBlocks(inode.i_block[i], 1);
        func(move(buffer));
    }
}

DWORD Ext2FileSystem::blockGroupForInode(DWORD inode) const
{
    // FIXME: Implement
    (void)inode;
    return 0;
}

DWORD Ext2FileSystem::toInodeTableIndex(DWORD inode) const
{
    // FIXME: Implement
    return inode - 1;
}

ext2_inode* Ext2FileSystem::findInode(DWORD index)
{
    if (index >= m_superBlock->s_inodes_count)
        return nullptr;
    return &m_inodeTables[blockGroupForInode(index)][toInodeTableIndex(index)];
}

ext2_inode* Ext2FileSystem::findPath(const String& path, DWORD& inodeIndex)
{
    Task::checkSanity("findPath entry");
    ASSERT(m_root);
    Task::checkSanity("findPath entry2");

    if (path.isEmpty())
        return nullptr;
    if (path[0] != '/')
        return nullptr;

    ext2_inode* dir = m_root;

    Task::checkSanity("findPath pre-vector");
    Vector<String> pathParts = path.split('/');
    Task::checkSanity("findPath post-split");

    for (size_t i = 0; i < pathParts.size(); ++i) {
        //kprintf("[%u] %s\n", i, pathParts[i].characters());
        auto& part = pathParts[i];
        bool foundPart = false;
        //kprintf("looking for part '%s' in inode{%p}\n", part.characters(), dir);
        traverseDirectory(*dir, [&] (const char* name, ext2_dir_entry& entry) {
            //kprintf("  ?= %s\n", name);
            if (String(name) == part) {
                foundPart = true;
                //kprintf("found part ==> inode %u (type %b)\n", entry.d_inode, entry.d_file_type);
                dir = findInode(entry.d_inode);
                inodeIndex = entry.d_inode;
                // FIXME: don't try to traverse files as if they're directories
                // FIXME: need a way to skip the remaining traverseDirectory() callbacks
            }
        });
        if (!foundPart)
            return nullptr;
    }

    return dir;
}

template<typename F>
void Ext2FileSystem::traverseDirectory(ext2_inode& inode, F func)
{
    Task::checkSanity("traverseDirectory2");
    //kprintf("in traverseDir\n");
    forEachBlockIn(inode, [this, &func] (RefPtr<DataBuffer>&& data) {
        auto* directory = (ext2_dir_entry*)data->data();
        traverseDirectory<F>(*directory, 1, func);
    });
    //kprintf("out traverseDir\n");
}

RefPtr<DataBuffer> Ext2FileSystem::readFile(ext2_inode& inode)
{
    auto buffer = DataBuffer::createUninitialized(inode.i_size + 1);
    BYTE* bufptr = buffer->data();
    size_t dataRemaining = inode.i_size;
    forEachBlockIn(inode, [this, &bufptr, &dataRemaining] (RefPtr<DataBuffer>&& data) {
        memcpy(bufptr, data->data(), min(dataRemaining, data->length()));
        dataRemaining -= blockSize();
        bufptr += blockSize();
    });
    // HACK: This is silly, but let's just null terminate here for comfort.
    buffer->data()[buffer->length() - 1] = '\0';
    return buffer;
}

void Ext2FileSystem::dumpFile(ext2_inode& inode)
{
    auto buffer = readFile(inode);
    kprintf("%s", buffer->data());
}

void Ext2FileSystem::dumpDirectory(ext2_inode& inode)
{
    traverseDirectory(inode, [this] (const char* name, ext2_dir_entry& entry) {
        bool isDirectory = entry.d_file_type == EXT2_FT_DIR;
        ext2_inode& inode = m_inodeTables[blockGroupForInode(entry.d_inode)][toInodeTableIndex(entry.d_inode)];
        kprintf("i:%x %b %u:%u %x %s%s\n",
                entry.d_inode,
                entry.d_file_type,
                inode.i_uid,
                inode.i_gid,
                inode.i_size,
                name,
                isDirectory ? "/" : "");
    });
}

#if 0
void Ext2FileSystem::readRoot()
{
    auto buffer = readBlocks(m_root->i_block[0], 1);
    auto* dir_block = (ext2_dir_entry*)buffer.data();

    traverseDirectory(dir_block, [this] (const char* name, ext2_dir_entry* entry) {
        if (!strcmp(name, "test2")) {
            auto test2_entry = loadFile(entry);
            new Task((void (*)())test2_entry.data(), "test2", IPC::Handle::Any, Task::Ring3);
            // HACK: Don't delete the code we just started running :)
            test2_entry.leak();
        } else if (!strcmp( name, "motd.txt")) {
            auto motd_txt = loadFile(entry);
            kprintf("===============================================\n\n");
            vga_set_attr(0x03);
            kprintf("%s\n", motd_txt.data());
            vga_set_attr(0x07);
            kprintf("===============================================\n");
        }
    });
}
#endif

void Ext2FileSystem::initialize()
{
    readSuperBlock();
    readBlockGroup(0);
    readInodeTable(0);

#ifdef FS_DEBUG
    dumpDirectory(*m_root);
#endif

    DWORD inodeIndex;
    auto* file = findPath("/motd.txt", inodeIndex);
    dumpFile(*file);
}

RefPtr<DataBuffer> Ext2FileSystem::loadFile(ext2_dir_entry* dirent)
{
    Task::checkSanity("loadFile");
    DWORD inode_group = (dirent->d_inode - 1) / superBlock().s_inodes_per_group;

#ifdef FS_DEBUG
    kprintf("inode: %u (group %u)\n", dirent->d_inode, inode_group);
    kprintf("inode table at block %u\n", m_groupTable[inode_group].bg_inode_table);
#endif

    // Calculate interesting offset into inode block.
    DWORD inode_index = (dirent->d_inode - 1) % superBlock().s_inodes_per_group;

    // Load the relevant inode block.
    auto buffer = readBlocks(m_groupTable[inode_group].bg_inode_table, 4);
    auto* inode_table = (ext2_inode*)buffer->data();

#ifdef FS_DEBUG
    kprintf("inode index: %u\n", inode_index);
#endif

    ext2_inode* inode = &inode_table[inode_index];

#ifdef FS_DEBUG
    kprintf("Mode: %u UID: %u GID: %u Size: %u Block0: %u\n", inode->i_mode, inode->i_uid, inode->i_gid, inode->i_size, inode->i_block[0]);
#endif

    auto fileContents = readBlocks(inode->i_block[0], 1);

#ifdef FS_DEBUG
    kprintf("File @ %p\n", fileContents->data());
    kprintf("File contents: %b %b %b %b %b\n",
            (*fileContents)[0],
            (*fileContents)[1],
            (*fileContents)[2],
            (*fileContents)[3],
            (*fileContents)[4]);
#endif

    return fileContents;
}

RefPtr<Ext2VirtualNode> Ext2VirtualNode::create(DWORD index, String&& path, Ext2FileSystem& fs, DWORD inodeNumber)
{
    Task::checkSanity("enter E2VN::create");
    ext2_inode* inode = fs.findInode(inodeNumber);
    Task::checkSanity("post findInode");
    if (!inode)
        return nullptr;
    auto* v = new Ext2VirtualNode(index, move(path), fs, *inode, inodeNumber);
    kprintf("v=%p\n", v);
    auto r = adoptRef(v);
    kprintf("adopted(v)=%p\n", r.ptr());
    return r;
}

Ext2VirtualNode::Ext2VirtualNode(DWORD index, String&& path, Ext2FileSystem& fs, ext2_inode& inode, DWORD inodeNumber)
    : VirtualNode(index, move(path))
    , m_fileSystem(fs)
    , m_inode(inode)
    , m_inodeNumber(inodeNumber)
{
    Task::checkSanity("Ext2VirtualNode::Ext2VirtualNode");
}

Ext2VirtualNode::~Ext2VirtualNode()
{
    Task::checkSanity("Ext2VirtualNode::~Ext2VirtualNode");
}

size_t Ext2VirtualNode::read(BYTE* outbuf, size_t start, size_t maxLength)
{
    Task::checkSanity("Ext2VirtualNode::read");
    kprintf("Ext2VirtualNode::read\n");
    if (start >= size())
        return 0;

    auto fileContents = m_fileSystem.readFile(m_inode);
    if (!fileContents)
        return 0;
    ASSERT(start < fileContents->length());
    size_t nread = min(maxLength, fileContents->length() - start);
    memcpy(outbuf, fileContents->data(), nread);
    return nread;
}

namespace FileSystem {

static DWORD nextVNodeIndex;

void initialize()
{
    nextVNodeIndex = 0;
    fileSystem = new Ext2FileSystem;
    fileSystem->initialize();
}

VirtualNode::VirtualNode(DWORD index, String&& path)
    : m_index(index)
    , m_path(move(path))
{
}

VirtualNode::~VirtualNode()
{
}

RefPtr<VirtualNode> createVirtualNode(String&& path)
{
    Task::checkSanity("createVirtualNode");
    DWORD inodeIndex = 0x12345678;
    Task::checkSanity("pre-findPath");
    kprintf("path: '%s'\n", path.characters());
    auto* inode = fileSystem->findPath(path, inodeIndex);
    Task::checkSanity("post-findPath");
    if (!inode)
        return nullptr;
    kprintf("creating e2vn\n");
    return Ext2VirtualNode::create(nextVNodeIndex++, move(path), *fileSystem, inodeIndex);
}

}
