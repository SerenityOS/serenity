#include "Ext2FileSystem.h"
#include "ext2_fs.h"
#include "UnixTypes.h"
#include <AK/Bitmap.h>
#include <AK/StdLib.h>
#include <AK/kmalloc.h>
#include <AK/ktime.h>
#include <AK/kstdio.h>
#include <AK/BufferStream.h>
#include "sys-errno.h"

//#define EXT2_DEBUG

class Ext2FileSystem::CachedExt2InodeImpl : public Retainable<CachedExt2InodeImpl> {
public:
    CachedExt2InodeImpl(OwnPtr<ext2_inode>&& e2i) : e2inode(move(e2i)) { }
    ~CachedExt2InodeImpl() { }
    OwnPtr<ext2_inode> e2inode;
};

class Ext2FileSystem::CachedExt2Inode {
public:
    const ext2_inode* operator->() const { return ptr->e2inode.ptr(); }
    const ext2_inode& operator*() const { return *ptr->e2inode; }
    ext2_inode* operator->() { return ptr->e2inode.ptr(); }
    ext2_inode& operator*() { return *ptr->e2inode; }
    bool operator!() const { return !ptr; }
    operator bool() const { return !!ptr; }
    CachedExt2Inode() { }
    explicit CachedExt2Inode(OwnPtr<ext2_inode>&& e2inode)
        : ptr(adopt(*new CachedExt2InodeImpl(move(e2inode))))
    { }
    explicit CachedExt2Inode(RetainPtr<CachedExt2InodeImpl> p)
        : ptr(p)
    { }
    RetainPtr<CachedExt2InodeImpl> ptr;
};

RetainPtr<Ext2FileSystem> Ext2FileSystem::create(RetainPtr<DiskDevice>&& device)
{
    return adopt(*new Ext2FileSystem(move(device)));
}

Ext2FileSystem::Ext2FileSystem(RetainPtr<DiskDevice>&& device)
    : DiskBackedFileSystem(move(device))
{
}

Ext2FileSystem::~Ext2FileSystem()
{
}

ByteBuffer Ext2FileSystem::readSuperBlock() const
{
    auto buffer = ByteBuffer::createUninitialized(1024);
    device().readBlock(2, buffer.pointer());
    device().readBlock(3, buffer.offsetPointer(512));
    return buffer;
}

bool Ext2FileSystem::writeSuperBlock(const ext2_super_block& sb)
{
    const byte* raw = (const byte*)&sb;
    bool success;
    success = device().writeBlock(2, raw);
    ASSERT(success);
    success = device().writeBlock(3, raw + 512);
    ASSERT(success);
    // FIXME: This is an ugly way to refresh the superblock cache. :-|
    superBlock();
    return true;
}

unsigned Ext2FileSystem::firstBlockOfGroup(unsigned groupIndex) const
{
    return superBlock().s_first_data_block + (groupIndex * superBlock().s_blocks_per_group);
}

const ext2_super_block& Ext2FileSystem::superBlock() const
{
    if (!m_cachedSuperBlock)
        m_cachedSuperBlock = readSuperBlock();
    return *reinterpret_cast<ext2_super_block*>(m_cachedSuperBlock.pointer());
}

const ext2_group_desc& Ext2FileSystem::blockGroupDescriptor(unsigned groupIndex) const
{
    // FIXME: Should this fail gracefully somehow?
    ASSERT(groupIndex <= m_blockGroupCount);

    if (!m_cachedBlockGroupDescriptorTable) {
        unsigned blocksToRead = ceilDiv(m_blockGroupCount * (unsigned)sizeof(ext2_group_desc), blockSize());
        unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
#ifdef EXT2_DEBUG
        kprintf("ext2fs: block group count: %u, blocks-to-read: %u\n", m_blockGroupCount, blocksToRead);
        kprintf("ext2fs: first block of BGDT: %u\n", firstBlockOfBGDT);
#endif
        m_cachedBlockGroupDescriptorTable = readBlocks(firstBlockOfBGDT, blocksToRead);
    }
    return reinterpret_cast<ext2_group_desc*>(m_cachedBlockGroupDescriptorTable.pointer())[groupIndex - 1];
}

bool Ext2FileSystem::initialize()
{
    auto& superBlock = this->superBlock();
#ifdef EXT2_DEBUG
    kprintf("ext2fs: super block magic: %x (super block size: %u)\n", superBlock.s_magic, sizeof(ext2_super_block));
#endif
    if (superBlock.s_magic != EXT2_SUPER_MAGIC)
        return false;

#ifdef EXT2_DEBUG
    kprintf("ext2fs: %u inodes, %u blocks\n", superBlock.s_inodes_count, superBlock.s_blocks_count);
    kprintf("ext2fs: block size = %u\n", EXT2_BLOCK_SIZE(&superBlock));
    kprintf("ext2fs: first data block = %u\n", superBlock.s_first_data_block);
    kprintf("ext2fs: inodes per block = %u\n", inodesPerBlock());
    kprintf("ext2fs: inodes per group = %u\n", inodesPerGroup());
    kprintf("ext2fs: free inodes = %u\n", superBlock.s_free_inodes_count);
    kprintf("ext2fs: desc per block = %u\n", EXT2_DESC_PER_BLOCK(&superBlock));
    kprintf("ext2fs: desc size = %u\n", EXT2_DESC_SIZE(&superBlock));
#endif

    setBlockSize(EXT2_BLOCK_SIZE(&superBlock));

    m_blockGroupCount = ceilDiv(superBlock.s_blocks_count, superBlock.s_blocks_per_group);

    if (m_blockGroupCount == 0) {
        kprintf("ext2fs: no block groups :(\n");
        return false;
    }

#ifdef EXT2_DEBUG
    for (unsigned i = 1; i <= m_blockGroupCount; ++i) {
        auto& group = blockGroupDescriptor(i);
        kprintf("ext2fs: group[%u] { block_bitmap: %u, inode_bitmap: %u, inode_table: %u }\n",
            i,
            group.bg_block_bitmap,
            group.bg_inode_bitmap,
            group.bg_inode_table);
    }
#endif

    return true;
}

const char* Ext2FileSystem::className() const
{
    return "ext2fs";
}

InodeIdentifier Ext2FileSystem::rootInode() const
{
    return { id(), EXT2_ROOT_INO };
}

#ifdef EXT2_DEBUG
static void dumpExt2Inode(const ext2_inode& inode)
{
    kprintf("Dump of ext2_inode:\n");
    kprintf("  i_size: %u\n", inode.i_size);
    kprintf("  i_mode: %u\n", inode.i_mode);
    kprintf("  i_blocks: %u\n", inode.i_blocks);
    kprintf("  i_uid: %u\n", inode.i_uid);
    kprintf("  i_gid: %u\n", inode.i_gid);
}
#endif

ByteBuffer Ext2FileSystem::readBlockContainingInode(unsigned inode, unsigned& blockIndex, unsigned& offset) const
{
    auto& superBlock = this->superBlock();

    if (inode != EXT2_ROOT_INO && inode < EXT2_FIRST_INO(&superBlock))
        return { };

    if (inode > superBlock.s_inodes_count)
        return { };

    auto& bgd = blockGroupDescriptor(groupIndexFromInode(inode));

    offset = ((inode - 1) % inodesPerGroup()) * inodeSize();
    blockIndex = bgd.bg_inode_table + (offset >> EXT2_BLOCK_SIZE_BITS(&superBlock));
    offset &= blockSize() - 1;

    return readBlock(blockIndex);
}

auto Ext2FileSystem::lookupExt2Inode(unsigned inode) const -> CachedExt2Inode
{
    {
        LOCKER(m_inodeCacheLock);
        auto it = m_inodeCache.find(inode);
        if (it != m_inodeCache.end()) {
            return CachedExt2Inode{ (*it).value };
        }
    }

    unsigned blockIndex;
    unsigned offset;
    auto block = readBlockContainingInode(inode, blockIndex, offset);

    if (!block)
        return { };

    auto* e2inode = reinterpret_cast<ext2_inode*>(kmalloc(inodeSize()));
    memcpy(e2inode, reinterpret_cast<ext2_inode*>(block.offsetPointer(offset)), inodeSize());
#ifdef EXT2_DEBUG
    dumpExt2Inode(*e2inode);
#endif

    LOCKER(m_inodeCacheLock);
    if (m_inodeCache.size() >= 64)
        m_inodeCache.removeOneRandomly();
    auto cachedInode = adopt(*new CachedExt2InodeImpl(OwnPtr<ext2_inode>(e2inode)));
    m_inodeCache.set(inode, cachedInode.copyRef());
    return CachedExt2Inode{ cachedInode };
}

InodeMetadata Ext2FileSystem::inodeMetadata(InodeIdentifier inode) const
{
    ASSERT(inode.fileSystemID() == id());

    auto e2inode = lookupExt2Inode(inode.index());
    if (!e2inode)
        return InodeMetadata();

    InodeMetadata metadata;
    metadata.inode = inode;
    metadata.size = e2inode->i_size;
    metadata.mode = e2inode->i_mode;
    metadata.uid = e2inode->i_uid;
    metadata.gid = e2inode->i_gid;
    metadata.linkCount = e2inode->i_links_count;
    metadata.atime = e2inode->i_atime;
    metadata.ctime = e2inode->i_ctime;
    metadata.mtime = e2inode->i_mtime;
    metadata.dtime = e2inode->i_dtime;
    metadata.blockSize = blockSize();
    metadata.blockCount = e2inode->i_blocks;

    if (isBlockDevice(e2inode->i_mode) || isCharacterDevice(e2inode->i_mode)) {
        unsigned dev = e2inode->i_block[0];
        metadata.majorDevice = (dev & 0xfff00) >> 8;
        metadata.minorDevice= (dev & 0xff) | ((dev >> 12) & 0xfff00);
    }

    return metadata;
}

Vector<unsigned> Ext2FileSystem::blockListForInode(const ext2_inode& e2inode) const
{
    unsigned entriesPerBlock = EXT2_ADDR_PER_BLOCK(&superBlock());

    // NOTE: i_blocks is number of 512-byte blocks, not number of fs-blocks.
    unsigned blockCount = e2inode.i_blocks / (blockSize() / 512);
    unsigned blocksRemaining = blockCount;
    Vector<unsigned> list;
    list.ensureCapacity(blocksRemaining);

    unsigned directCount = min(blockCount, (unsigned)EXT2_NDIR_BLOCKS);
    for (unsigned i = 0; i < directCount; ++i) {
        list.append(e2inode.i_block[i]);
        --blocksRemaining;
    }

    if (!blocksRemaining)
        return list;

    auto processBlockArray = [&] (unsigned arrayBlockIndex, Function<void(unsigned)> callback) {
        auto arrayBlock = readBlock(arrayBlockIndex);
        ASSERT(arrayBlock);
        auto* array = reinterpret_cast<const __u32*>(arrayBlock.pointer());
        unsigned count = min(blocksRemaining, entriesPerBlock);
        for (unsigned i = 0; i < count; ++i) {
            if (!array[i]) {
                blocksRemaining = 0;
                return;
            }
            callback(array[i]);
            --blocksRemaining;
        }
    };

    processBlockArray(e2inode.i_block[EXT2_IND_BLOCK], [&] (unsigned entry) {
        list.append(entry);
    });

    if (!blocksRemaining)
        return list;

    processBlockArray(e2inode.i_block[EXT2_DIND_BLOCK], [&] (unsigned entry) {
        processBlockArray(entry, [&] (unsigned entry) {
            list.append(entry);
        });
    });

    if (!blocksRemaining)
        return list;

    processBlockArray(e2inode.i_block[EXT2_TIND_BLOCK], [&] (unsigned entry) {
        processBlockArray(entry, [&] (unsigned entry) {
            processBlockArray(entry, [&] (unsigned entry) {
                list.append(entry);
            });
        });
    });

    return list;
}

Unix::ssize_t Ext2FileSystem::readInodeBytes(InodeIdentifier inode, Unix::off_t offset, Unix::size_t count, byte* buffer, FileHandle*) const
{
    ASSERT(offset >= 0);
    ASSERT(inode.fileSystemID() == id());

    auto e2inode = lookupExt2Inode(inode.index());
    if (!e2inode) {
        kprintf("ext2fs: readInodeBytes: metadata lookup for inode %u failed\n", inode.index());
        return -EIO;
    }

#if 0
    // FIXME: We can't fail here while the directory traversal depends on this function. :]
    if (isDirectory(e2inode->i_mode))
        return -EISDIR;
#endif

    if (e2inode->i_size == 0)
        return 0;

    // Symbolic links shorter than 60 characters are store inline inside the i_block array.
    // This avoids wasting an entire block on short links. (Most links are short.)
    static const unsigned maxInlineSymlinkLength = 60;
    if (isSymbolicLink(e2inode->i_mode) && e2inode->i_size < maxInlineSymlinkLength) {
        Unix::ssize_t nread = min((Unix::off_t)e2inode->i_size - offset, static_cast<Unix::off_t>(count));
        memcpy(buffer, e2inode->i_block + offset, nread);
        return nread;
    }

    // FIXME: It's grossly inefficient to fetch the blocklist on every call to readInodeBytes().
    //        It needs to be cached!
    auto list = blockListForInode(*e2inode);
    if (list.isEmpty()) {
        kprintf("ext2fs: readInodeBytes: empty block list for inode %u\n", inode.index());
        return -EIO;
    }

    dword firstBlockLogicalIndex = offset / blockSize();
    dword lastBlockLogicalIndex = (offset + count) / blockSize();
    if (lastBlockLogicalIndex >= list.size())
        lastBlockLogicalIndex = list.size() - 1;

    dword offsetIntoFirstBlock = offset % blockSize();

    Unix::ssize_t nread = 0;
    Unix::size_t remainingCount = min((Unix::off_t)count, (Unix::off_t)e2inode->i_size - offset);
    byte* out = buffer;

#ifdef EXT2_DEBUG
    kprintf("ok let's do it, read(%llu, %u) -> blocks %u thru %u, oifb: %u\n", offset, count, firstBlockLogicalIndex, lastBlockLogicalIndex, offsetIntoFirstBlock);
#endif

    for (dword bi = firstBlockLogicalIndex; bi <= lastBlockLogicalIndex; ++bi) {
        auto block = readBlock(list[bi]);
        if (!block) {
            kprintf("ext2fs: readInodeBytes: readBlock(%u) failed (lbi: %u)\n", list[bi], bi);
            return -EIO;
        }

        dword offsetIntoBlock;

        if (bi == firstBlockLogicalIndex)
            offsetIntoBlock = offsetIntoFirstBlock;
        else
            offsetIntoBlock = 0;

        dword numBytesToCopy = min(blockSize() - offsetIntoBlock, remainingCount);
        memcpy(out, block.pointer() + offsetIntoBlock, numBytesToCopy);
        remainingCount -= numBytesToCopy;
        nread += numBytesToCopy;
        out += numBytesToCopy;
    }

    return nread;
}

bool Ext2FileSystem::writeInode(InodeIdentifier inode, const ByteBuffer& data)
{
    ASSERT(inode.fileSystemID() == id());

    auto e2inode = lookupExt2Inode(inode.index());
    if (!e2inode) {
        kprintf("ext2fs: writeInode: metadata lookup for inode %u failed\n", inode.index());
        return false;
    }

    // FIXME: Support writing to symlink inodes.
    ASSERT(!isSymbolicLink(e2inode->i_mode));

    unsigned blocksNeededBefore = ceilDiv(e2inode->i_size, blockSize());
    unsigned blocksNeededAfter = ceilDiv((unsigned)data.size(), blockSize());

    // FIXME: Support growing or shrinking the block list.
    ASSERT(blocksNeededBefore == blocksNeededAfter);

    auto list = blockListForInode(*e2inode);
    if (list.isEmpty()) {
        kprintf("ext2fs: writeInode: empty block list for inode %u\n", inode.index());
        return false;
    }

    for (unsigned i = 0; i < list.size(); ++i) {
        auto section = data.slice(i * blockSize(), blockSize());
        kprintf("section = %p (%u)\n", section.pointer(), section.size());
        bool success = writeBlock(list[i], section);
        ASSERT(success);
    }

    return true;
}

bool Ext2FileSystem::enumerateDirectoryInode(InodeIdentifier inode, Function<bool(const DirectoryEntry&)> callback) const
{
    ASSERT(inode.fileSystemID() == id());
    ASSERT(isDirectoryInode(inode.index()));

#ifdef EXT2_DEBUG
    kprintf("ext2fs: Enumerating directory contents of inode %u:\n", inode.index());
#endif

    auto buffer = readEntireInode(inode);
    ASSERT(buffer);
    auto* entry = reinterpret_cast<ext2_dir_entry_2*>(buffer.pointer());

    char namebuf[EXT2_NAME_LEN + 1];

    while (entry < buffer.endPointer()) {
        if (entry->inode != 0) {
            memcpy(namebuf, entry->name, entry->name_len);
            namebuf[entry->name_len] = 0;
#ifdef EXT2_DEBUG
            kprintf("inode: %u, name_len: %u, rec_len: %u, file_type: %u, name: %s\n", entry->inode, entry->name_len, entry->rec_len, entry->file_type, namebuf);
#endif
            if (!callback({ namebuf, { id(), entry->inode }, entry->file_type }))
                break;
        }
        entry = (ext2_dir_entry_2*)((char*)entry + entry->rec_len);
    }
    return true;
}

bool Ext2FileSystem::addInodeToDirectory(unsigned directoryInode, unsigned inode, const String& name, byte fileType)
{
    auto e2inodeForDirectory = lookupExt2Inode(directoryInode);
    ASSERT(e2inodeForDirectory);
    ASSERT(isDirectory(e2inodeForDirectory->i_mode));

//#ifdef EXT2_DEBUG
    kprintf("ext2fs: Adding inode %u with name '%s' to directory %u\n", inode, name.characters(), directoryInode);
//#endif

    Vector<DirectoryEntry> entries;
    bool nameAlreadyExists = false;
    enumerateDirectoryInode({ id(), directoryInode }, [&] (const DirectoryEntry& entry) {
        if (entry.name == name) {
            nameAlreadyExists = true;
            return false;
        }
        entries.append(entry);
        return true;
    });
    if (nameAlreadyExists) {
        kprintf("ext2fs: Name '%s' already exists in directory inode %u\n", name.characters(), directoryInode);
        return false;
    }

    entries.append({ name, { id(), inode }, fileType });

    return writeDirectoryInode(directoryInode, move(entries));
}

bool Ext2FileSystem::writeDirectoryInode(unsigned directoryInode, Vector<DirectoryEntry>&& entries)
{
    kprintf("ext2fs: New directory inode %u contents to write:\n", directoryInode);

    unsigned directorySize = 0;
    for (auto& entry : entries) {
        kprintf("  - %08u %s\n", entry.inode.index(), entry.name.characters());
        directorySize += EXT2_DIR_REC_LEN(entry.name.length());
    }

    unsigned blocksNeeded = ceilDiv(directorySize, blockSize());
    unsigned occupiedSize = blocksNeeded * blockSize();

    kprintf("ext2fs: directory size: %u (occupied: %u)\n", directorySize, occupiedSize);

    auto directoryData = ByteBuffer::createUninitialized(occupiedSize);

    BufferStream stream(directoryData);
    for (unsigned i = 0; i < entries.size(); ++i) {
        auto& entry = entries[i];

        unsigned recordLength = EXT2_DIR_REC_LEN(entry.name.length());
        if (i == entries.size() - 1)
            recordLength += occupiedSize - directorySize;

        kprintf("* inode: %u", entry.inode.index());
        kprintf(", name_len: %u", word(entry.name.length()));
        kprintf(", rec_len: %u", word(recordLength));
        kprintf(", file_type: %u", byte(entry.fileType));
        kprintf(", name: %s\n", entry.name.characters());

        stream << dword(entry.inode.index());
        stream << word(recordLength);
        stream << byte(entry.name.length());
        stream << byte(entry.fileType);
        stream << entry.name;

        unsigned padding = recordLength - entry.name.length() - 8;
        kprintf("  *** pad %u bytes\n", padding);
        for (unsigned j = 0; j < padding; ++j) {
            stream << byte(0);
        }
    }

    stream.fillToEnd(0);

#if 0
    kprintf("data to write (%u):\n", directoryData.size());
    for (unsigned i = 0; i < directoryData.size(); ++i) {
        kprintf("%02x ", directoryData[i]);
        if ((i + 1) % 8 == 0)
            kprintf(" ");
        if ((i + 1) % 16 == 0)
            kprintf("\n");
    }
    kprintf("\n");
#endif

    writeInode({ id(), directoryInode }, directoryData);

    return true;
}

unsigned Ext2FileSystem::inodesPerBlock() const
{
    return EXT2_INODES_PER_BLOCK(&superBlock());
}

unsigned Ext2FileSystem::inodesPerGroup() const
{
    return EXT2_INODES_PER_GROUP(&superBlock());
}

unsigned Ext2FileSystem::inodeSize() const
{
    return EXT2_INODE_SIZE(&superBlock());

}
unsigned Ext2FileSystem::blocksPerGroup() const
{
    return EXT2_BLOCKS_PER_GROUP(&superBlock());
}

void Ext2FileSystem::dumpBlockBitmap(unsigned groupIndex) const
{
    ASSERT(groupIndex <= m_blockGroupCount);
    auto& bgd = blockGroupDescriptor(groupIndex);

    unsigned blocksInGroup = min(blocksPerGroup(), superBlock().s_blocks_count);
    unsigned blockCount = ceilDiv(blocksInGroup, 8u);

    auto bitmapBlocks = readBlocks(bgd.bg_block_bitmap, blockCount);
    ASSERT(bitmapBlocks);

    kprintf("ext2fs: group[%u] block bitmap (bitmap occupies %u blocks):\n", groupIndex, blockCount);

    auto bitmap = Bitmap::wrap(bitmapBlocks.pointer(), blocksInGroup);
    for (unsigned i = 0; i < blocksInGroup; ++i) {
        kprintf("%c", bitmap.get(i) ? '1' : '0');
    }
    kprintf("\n");
}

void Ext2FileSystem::dumpInodeBitmap(unsigned groupIndex) const
{
    traverseInodeBitmap(groupIndex, [] (unsigned, const Bitmap& bitmap) {
        for (unsigned i = 0; i < bitmap.size(); ++i)
            kprintf("%c", bitmap.get(i) ? '1' : '0');
        return true;
    });
}

template<typename F>
void Ext2FileSystem::traverseInodeBitmap(unsigned groupIndex, F callback) const
{
    ASSERT(groupIndex <= m_blockGroupCount);
    auto& bgd = blockGroupDescriptor(groupIndex);

    unsigned inodesInGroup = min(inodesPerGroup(), superBlock().s_inodes_count);
    unsigned blockCount = ceilDiv(inodesInGroup, 8u);

    for (unsigned i = 0; i < blockCount; ++i) {
        auto block = readBlock(bgd.bg_inode_bitmap + i);
        ASSERT(block);
        bool shouldContinue = callback(i * (blockSize() / 8) + 1, Bitmap::wrap(block.pointer(), inodesInGroup));
        if (!shouldContinue)
            break;
    }
}

template<typename F>
void Ext2FileSystem::traverseBlockBitmap(unsigned groupIndex, F callback) const
{
    ASSERT(groupIndex <= m_blockGroupCount);
    auto& bgd = blockGroupDescriptor(groupIndex);

    unsigned blocksInGroup = min(blocksPerGroup(), superBlock().s_blocks_count);
    unsigned blockCount = ceilDiv(blocksInGroup, 8u);

    for (unsigned i = 0; i < blockCount; ++i) {
        auto block = readBlock(bgd.bg_block_bitmap + i);
        ASSERT(block);
        bool shouldContinue = callback(i * (blockSize() / 8) + 1, Bitmap::wrap(block.pointer(), blocksInGroup));
        if (!shouldContinue)
            break;
    }
}

bool Ext2FileSystem::modifyLinkCount(InodeIndex inode, int delta)
{
    ASSERT(inode);
    auto e2inode = lookupExt2Inode(inode);
    if (!e2inode)
        return false;

    auto newLinkCount = e2inode->i_links_count + delta;
    kprintf("changing inode %u link count from %u to %u\n", inode, e2inode->i_links_count, newLinkCount);
    e2inode->i_links_count = newLinkCount;

    return writeExt2Inode(inode, *e2inode);
}

bool Ext2FileSystem::setModificationTime(InodeIdentifier inode, dword timestamp)
{
    ASSERT(inode.fileSystemID() == id());

    auto e2inode = lookupExt2Inode(inode.index());
    if (!e2inode)
        return false;

    kprintf("changing inode %u mtime from %u to %u\n", inode.index(), e2inode->i_mtime, timestamp);
    e2inode->i_mtime = timestamp;

    return writeExt2Inode(inode.index(), *e2inode);
}

bool Ext2FileSystem::writeExt2Inode(unsigned inode, const ext2_inode& e2inode)
{
    unsigned blockIndex;
    unsigned offset;
    auto block = readBlockContainingInode(inode, blockIndex, offset);
    if (!block)
        return false;
    memcpy(reinterpret_cast<ext2_inode*>(block.offsetPointer(offset)), &e2inode, inodeSize());
    writeBlock(blockIndex, block);
    return true;
}

bool Ext2FileSystem::isDirectoryInode(unsigned inode) const
{
    if (auto e2inode = lookupExt2Inode(inode))
        return isDirectory(e2inode->i_mode);
    return false;
}

Vector<Ext2FileSystem::BlockIndex> Ext2FileSystem::allocateBlocks(unsigned group, unsigned count)
{
    kprintf("ext2fs: allocateBlocks(group: %u, count: %u)\n", group, count);

    auto& bgd = blockGroupDescriptor(group);
    if (bgd.bg_free_blocks_count < count) {
        kprintf("ext2fs: allocateBlocks can't allocate out of group %u, wanted %u but only %u available\n", group, count, bgd.bg_free_blocks_count);
        return { };
    }

    // FIXME: Implement a scan that finds consecutive blocks if possible.
    Vector<BlockIndex> blocks;
    traverseBlockBitmap(group, [&blocks, count] (unsigned firstBlockInBitmap, const Bitmap& bitmap) {
        for (unsigned i = 0; i < bitmap.size(); ++i) {
            if (!bitmap.get(i)) {
                blocks.append(firstBlockInBitmap + i);
                if (blocks.size() == count)
                    return false;
            }
        }
        return true;
    });
    kprintf("ext2fs: allocateBlock found these blocks:\n");
    for (auto& bi : blocks) {
        kprintf("  > %u\n", bi);
    }

    return blocks;
}

unsigned Ext2FileSystem::allocateInode(unsigned preferredGroup, unsigned expectedSize)
{
    kprintf("ext2fs: allocateInode(preferredGroup: %u, expectedSize: %u)\n", preferredGroup, expectedSize);

    unsigned neededBlocks = ceilDiv(expectedSize, blockSize());

    kprintf("ext2fs: minimum needed blocks: %u\n", neededBlocks);

    unsigned groupIndex = 0;

    auto isSuitableGroup = [this, neededBlocks] (unsigned groupIndex) {
        auto& bgd = blockGroupDescriptor(groupIndex);
        return bgd.bg_free_inodes_count && bgd.bg_free_blocks_count >= neededBlocks;
    };

    if (preferredGroup && isSuitableGroup(preferredGroup)) {
        groupIndex = preferredGroup;
    } else {
        for (unsigned i = 1; i <= m_blockGroupCount; ++i) {
            if (isSuitableGroup(i))
                groupIndex = i;
        }
    }

    if (!groupIndex) {
        kprintf("ext2fs: allocateInode: no suitable group found for new inode with %u blocks needed :(\n", neededBlocks);
        return 0;
    }

    kprintf("ext2fs: allocateInode: found suitable group [%u] for new inode with %u blocks needed :^)\n", groupIndex, neededBlocks);

    unsigned firstFreeInodeInGroup = 0;
    traverseInodeBitmap(groupIndex, [&firstFreeInodeInGroup] (unsigned firstInodeInBitmap, const Bitmap& bitmap) {
        for (unsigned i = 0; i < bitmap.size(); ++i) {
            if (!bitmap.get(i)) {
                firstFreeInodeInGroup = firstInodeInBitmap + i;
                return false;
            }
        }
        return true;
    });

    if (!firstFreeInodeInGroup) {
        kprintf("ext2fs: firstFreeInodeInGroup returned no inode, despite bgd claiming there are inodes :(\n");
        return 0;
    }

    unsigned inode = firstFreeInodeInGroup;
    kprintf("ext2fs: found suitable inode %u\n", inode);

    // FIXME: allocate blocks if needed!

    return inode;
}

unsigned Ext2FileSystem::groupIndexFromInode(unsigned inode) const
{
    if (!inode)
        return 0;
    return (inode - 1) / inodesPerGroup() + 1;
}

bool Ext2FileSystem::setInodeAllocationState(unsigned inode, bool newState)
{
    auto& bgd = blockGroupDescriptor(groupIndexFromInode(inode));

    // Update inode bitmap
    unsigned inodesPerBitmapBlock = blockSize() * 8;
    unsigned bitmapBlockIndex = (inode - 1) / inodesPerBitmapBlock;
    unsigned bitIndex = (inode - 1) % inodesPerBitmapBlock;
    auto block = readBlock(bgd.bg_inode_bitmap + bitmapBlockIndex);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), block.size());
    bool currentState = bitmap.get(bitIndex);
    kprintf("ext2fs: setInodeAllocationState(%u) %u -> %u\n", inode, currentState, newState);

    if (currentState == newState)
        return true;

    bitmap.set(bitIndex, newState);
    writeBlock(bgd.bg_inode_bitmap + bitmapBlockIndex, block);

    // Update superblock
    auto& sb = *reinterpret_cast<ext2_super_block*>(m_cachedSuperBlock.pointer());
    kprintf("ext2fs: superblock free inode count %u -> %u\n", sb.s_free_inodes_count, sb.s_free_inodes_count - 1);
    if (newState)
        --sb.s_free_inodes_count;
    else
        ++sb.s_free_inodes_count;
    writeSuperBlock(sb);

    // Update BGD
    auto& mutableBGD = const_cast<ext2_group_desc&>(bgd);
    if (newState)
        --mutableBGD.bg_free_inodes_count;
    else
        ++mutableBGD.bg_free_inodes_count;
    kprintf("ext2fs: group free inode count %u -> %u\n", bgd.bg_free_inodes_count, bgd.bg_free_inodes_count - 1);

    unsigned blocksToWrite = ceilDiv(m_blockGroupCount * (unsigned)sizeof(ext2_group_desc), blockSize());
    unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
    writeBlocks(firstBlockOfBGDT, blocksToWrite, m_cachedBlockGroupDescriptorTable);
    
    return true;
}

bool Ext2FileSystem::setBlockAllocationState(GroupIndex group, BlockIndex bi, bool newState)
{
    auto& bgd = blockGroupDescriptor(group);

    // Update block bitmap
    unsigned blocksPerBitmapBlock = blockSize() * 8;
    unsigned bitmapBlockIndex = (bi - 1) / blocksPerBitmapBlock;
    unsigned bitIndex = (bi - 1) % blocksPerBitmapBlock;
    auto block = readBlock(bgd.bg_block_bitmap + bitmapBlockIndex);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), block.size());
    bool currentState = bitmap.get(bitIndex);
    kprintf("ext2fs: setBlockAllocationState(%u) %u -> %u\n", block, currentState, newState);

    if (currentState == newState)
        return true;

    bitmap.set(bitIndex, newState);
    writeBlock(bgd.bg_block_bitmap + bitmapBlockIndex, block);

    // Update superblock
    auto& sb = *reinterpret_cast<ext2_super_block*>(m_cachedSuperBlock.pointer());
    kprintf("ext2fs: superblock free block count %u -> %u\n", sb.s_free_blocks_count, sb.s_free_blocks_count - 1);
    if (newState)
        --sb.s_free_blocks_count;
    else
        ++sb.s_free_blocks_count;
    writeSuperBlock(sb);

    // Update BGD
    auto& mutableBGD = const_cast<ext2_group_desc&>(bgd);
    if (newState)
        --mutableBGD.bg_free_blocks_count;
    else
        ++mutableBGD.bg_free_blocks_count;
    kprintf("ext2fs: group free block count %u -> %u\n", bgd.bg_free_blocks_count, bgd.bg_free_blocks_count - 1);

    unsigned blocksToWrite = ceilDiv(m_blockGroupCount * (unsigned)sizeof(ext2_group_desc), blockSize());
    unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
    writeBlocks(firstBlockOfBGDT, blocksToWrite, m_cachedBlockGroupDescriptorTable);
    
    return true;
}

InodeIdentifier Ext2FileSystem::makeDirectory(InodeIdentifier parentInode, const String& name, Unix::mode_t mode)
{
    ASSERT(parentInode.fileSystemID() == id());
    ASSERT(isDirectoryInode(parentInode.index()));

    // Fix up the mode to definitely be a directory.
    // FIXME: This is a bit on the hackish side.
    mode &= ~0170000;
    mode |= 0040000;

    // NOTE: When creating a new directory, make the size 1 block.
    //       There's probably a better strategy here, but this works for now.
    auto inode = createInode(parentInode, name, mode, blockSize());
    if (!inode.isValid())
        return { };

    kprintf("ext2fs: makeDirectory: created new directory named '%s' with inode %u\n", name.characters(), inode.index());

    Vector<DirectoryEntry> entries;
    entries.append({ ".", inode, EXT2_FT_DIR });
    entries.append({ "..", parentInode, EXT2_FT_DIR });

    bool success = writeDirectoryInode(inode.index(), move(entries));
    ASSERT(success);

    success = modifyLinkCount(parentInode.index(), 1);
    ASSERT(success);

    auto& bgd = const_cast<ext2_group_desc&>(blockGroupDescriptor(groupIndexFromInode(inode.index())));
    ++bgd.bg_used_dirs_count;
    kprintf("ext2fs: incremented bg_used_dirs_count %u -> %u\n", bgd.bg_used_dirs_count - 1, bgd.bg_used_dirs_count);

    unsigned blocksToWrite = ceilDiv(m_blockGroupCount * (unsigned)sizeof(ext2_group_desc), blockSize());
    unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
    writeBlocks(firstBlockOfBGDT, blocksToWrite, m_cachedBlockGroupDescriptorTable);

    return inode;
}

InodeIdentifier Ext2FileSystem::createInode(InodeIdentifier parentInode, const String& name, Unix::mode_t mode, unsigned size)
{
    ASSERT(parentInode.fileSystemID() == id());
    ASSERT(isDirectoryInode(parentInode.index()));

//#ifdef EXT2_DEBUG
    kprintf("ext2fs: Adding inode '%s' (mode %o) to parent directory %u:\n", name.characters(), mode, parentInode.index());
//#endif

    // NOTE: This doesn't commit the inode allocation just yet!
    auto inode = allocateInode(0, 0);
    if (!inode) {
        kprintf("ext2fs: createInode: allocateInode failed\n");
        return { };
    }

    auto blocks = allocateBlocks(groupIndexFromInode(inode), ceilDiv(size, blockSize()));
    if (blocks.isEmpty()) {
        kprintf("ext2fs: createInode: allocateBlocks failed\n");
        return { };
    }

    byte fileType = 0;
    if (isRegularFile(mode))
        fileType = EXT2_FT_REG_FILE;
    else if (isDirectory(mode))
        fileType = EXT2_FT_DIR;
    else if (isCharacterDevice(mode))
        fileType = EXT2_FT_CHRDEV;
    else if (isBlockDevice(mode))
        fileType = EXT2_FT_BLKDEV;
    else if (isFIFO(mode))
        fileType = EXT2_FT_FIFO;
    else if (isSocket(mode))
        fileType = EXT2_FT_SOCK;
    else if (isSymbolicLink(mode))
        fileType = EXT2_FT_SYMLINK;

    // Try adding it to the directory first, in case the name is already in use.
    bool success = addInodeToDirectory(parentInode.index(), inode, name, fileType);
    if (!success) {
        kprintf("ext2fs: failed to add inode to directory :(\n");
        return { };
    }

    // Looks like we're good, time to update the inode bitmap and group+global inode counters.
    success = setInodeAllocationState(inode, true);
    ASSERT(success);

    for (auto bi : blocks) {
        success = setBlockAllocationState(groupIndexFromInode(inode), bi, true);
        ASSERT(success);
    }

    unsigned initialLinksCount;
    if (isDirectory(mode))
        initialLinksCount = 2; // (parent directory + "." entry in self)
    else
        initialLinksCount = 1;

    auto timestamp = ktime(nullptr);
    auto e2inode = make<ext2_inode>();
    memset(e2inode.ptr(), 0, sizeof(ext2_inode));
    e2inode->i_mode = mode;
    e2inode->i_uid = 0;
    e2inode->i_size = size;
    e2inode->i_atime = timestamp;
    e2inode->i_ctime = timestamp;
    e2inode->i_mtime = timestamp;
    e2inode->i_dtime = 0;
    e2inode->i_gid = 0;
    e2inode->i_links_count = initialLinksCount;
    e2inode->i_blocks = blocks.size() * (blockSize() / 512);

    // FIXME: Implement writing out indirect blocks!
    ASSERT(blocks.size() < EXT2_NDIR_BLOCKS);

    kprintf("[XXX] writing %zu blocks to i_block array\n", min((size_t)EXT2_NDIR_BLOCKS, blocks.size()));
    for (unsigned i = 0; i < min((size_t)EXT2_NDIR_BLOCKS, blocks.size()); ++i) {
        e2inode->i_block[i] = blocks[i];
    }

    e2inode->i_flags = 0;
    success = writeExt2Inode(inode, *e2inode);
    ASSERT(success);

    return { id(), inode };
}

InodeIdentifier Ext2FileSystem::findParentOfInode(InodeIdentifier inode) const
{
    ASSERT(inode.fileSystemID() == id());
    unsigned groupIndex = groupIndexFromInode(inode.index());
    unsigned firstInodeInGroup = inodesPerGroup() * (groupIndex - 1);

    Vector<InodeIdentifier> directoriesInGroup;

    for (unsigned i = 0; i < inodesPerGroup(); ++i) {
        auto e2inode = lookupExt2Inode(firstInodeInGroup + i);
        if (!e2inode)
            continue;
        if (isDirectory(e2inode->i_mode)) {
            directoriesInGroup.append({ id(), firstInodeInGroup + i });
        }
    }

    InodeIdentifier foundParent;
    for (auto& directory : directoriesInGroup) {
        enumerateDirectoryInode(directory, [inode, directory, &foundParent] (auto& entry) {
            if (entry.inode == inode) {
                foundParent = directory;
                return false;
            }
            return true;
        });
        if (foundParent.isValid())
            break;
    }

    return foundParent;
}
