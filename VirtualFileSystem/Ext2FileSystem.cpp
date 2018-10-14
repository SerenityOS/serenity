#include "Ext2FileSystem.h"
#include "ext2_fs.h"
#include "UnixTypes.h"
#include <AK/Bitmap.h>
#include <AK/StdLib.h>
#include <cstdio>
#include <cstring>
#include <AK/kmalloc.h>
#include "sys-errno.h"

//#define EXT2_DEBUG

RetainPtr<Ext2FileSystem> Ext2FileSystem::create(RetainPtr<BlockDevice> device)
{
    return adopt(*new Ext2FileSystem(std::move(device)));
}

Ext2FileSystem::Ext2FileSystem(RetainPtr<BlockDevice> device)
    : DeviceBackedFileSystem(std::move(device))
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
        unsigned blocksToRead = ceilDiv(m_blockGroupCount * sizeof(ext2_group_desc), blockSize());
        printf("[ext2fs] block group count: %u, blocks-to-read: %u\n", m_blockGroupCount, blocksToRead);
        unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
        printf("[ext2fs] first block of BGDT: %u\n", firstBlockOfBGDT);
        m_cachedBlockGroupDescriptorTable = readBlocks(firstBlockOfBGDT, blocksToRead);
    }
    return reinterpret_cast<ext2_group_desc*>(m_cachedBlockGroupDescriptorTable.pointer())[groupIndex - 1];
}

bool Ext2FileSystem::initialize()
{
    auto& superBlock = this->superBlock();
    printf("[ext2fs] super block magic: %04x (super block size: %u)\n", superBlock.s_magic, sizeof(ext2_super_block));
    if (superBlock.s_magic != EXT2_SUPER_MAGIC)
        return false;

    printf("[ext2fs] %u inodes, %u blocks\n", superBlock.s_inodes_count, superBlock.s_blocks_count);
    printf("[ext2fs] block size = %u\n", EXT2_BLOCK_SIZE(&superBlock));
    printf("[ext2fs] first data block = %u\n", superBlock.s_first_data_block);
    printf("[ext2fs] inodes per block = %u\n", inodesPerBlock());
    printf("[ext2fs] inodes per group = %u\n", inodesPerGroup());
    printf("[ext2fs] free inodes = %u\n", superBlock.s_free_inodes_count);

    printf("[ext2fs] desc per block = %u\n", EXT2_DESC_PER_BLOCK(&superBlock));
    printf("[ext2fs] desc size = %u\n", EXT2_DESC_SIZE(&superBlock));

    setBlockSize(EXT2_BLOCK_SIZE(&superBlock));

    m_blockGroupCount = ceilDiv(superBlock.s_blocks_count, superBlock.s_blocks_per_group);

    if (m_blockGroupCount == 0) {
        printf("[ext2fs] no block groups :(\n");
        return false;
    }

    for (unsigned i = 1; i <= m_blockGroupCount; ++i) {
        auto& group = blockGroupDescriptor(i);
        printf("[ext2fs] group[%u] { block_bitmap: %u, inode_bitmap: %u, inode_table: %u }\n",
            i,
            group.bg_block_bitmap,
            group.bg_inode_bitmap,
            group.bg_inode_table);
    }

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
    printf("Dump of ext2_inode:\n");
    printf("  i_size: %u\n", inode.i_size);
    printf("  i_mode: %u\n", inode.i_mode);
    printf("  i_blocks: %u\n", inode.i_blocks);
    printf("  i_uid: %u\n", inode.i_uid);
    printf("  i_gid: %u\n", inode.i_gid);
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

OwnPtr<ext2_inode> Ext2FileSystem::lookupExt2Inode(unsigned inode) const
{
    unsigned blockIndex;
    unsigned offset;
    auto block = readBlockContainingInode(inode, blockIndex, offset);

    if (!block)
        return nullptr;

    auto* e2inode = reinterpret_cast<ext2_inode*>(kmalloc(inodeSize()));
    memcpy(e2inode, reinterpret_cast<ext2_inode*>(block.offsetPointer(offset)), inodeSize());
#ifdef EXT2_DEBUG
    dumpExt2Inode(*e2inode);
#endif

    return OwnPtr<ext2_inode>(e2inode);
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

    auto processBlockArray = [&] (unsigned arrayBlockIndex, std::function<void(unsigned)> callback) {
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

Unix::ssize_t Ext2FileSystem::readInodeBytes(InodeIdentifier inode, Unix::off_t offset, Unix::size_t count, byte* buffer) const
{
    ASSERT(offset >= 0);
    ASSERT(inode.fileSystemID() == id());

    auto e2inode = lookupExt2Inode(inode.index());
    if (!e2inode) {
        printf("[ext2fs] readInodeBytes: metadata lookup for inode %u failed\n", inode.index());
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
        Unix::ssize_t nread = min(e2inode->i_size - offset, static_cast<Unix::off_t>(count));
        memcpy(buffer, e2inode->i_block + offset, nread);
        return nread;
    }

    // FIXME: It's grossly inefficient to fetch the blocklist on every call to readInodeBytes().
    //        It needs to be cached!
    auto list = blockListForInode(*e2inode);
    if (list.isEmpty()) {
        printf("[ext2fs] readInodeBytes: empty block list for inode %u\n", inode.index());
        return -EIO;
    }

    dword firstBlockLogicalIndex = offset / blockSize();
    dword lastBlockLogicalIndex = (offset + count) / blockSize();
    if (lastBlockLogicalIndex >= list.size())
        lastBlockLogicalIndex = list.size() - 1;

    dword offsetIntoFirstBlock = offset % blockSize();

    Unix::ssize_t nread = 0;
    Unix::size_t remainingCount = min((Unix::off_t)count, e2inode->i_size - offset);
    byte* out = buffer;

#ifdef EXT2_DEBUG
    printf("ok let's do it, read(%llu, %u) -> blocks %u thru %u, oifb: %u\n", offset, count, firstBlockLogicalIndex, lastBlockLogicalIndex, offsetIntoFirstBlock);
#endif

    for (dword bi = firstBlockLogicalIndex; bi <= lastBlockLogicalIndex; ++bi) {
        auto block = readBlock(list[bi]);
        if (!block) {
            printf("[ext2fs] readInodeBytes: readBlock(%u) failed (lbi: %u)\n", list[bi], bi);
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
        printf("[ext2fs] writeInode: metadata lookup for inode %u failed\n", inode.index());
        return false;
    }

    // FIXME: Support writing to symlink inodes.
    ASSERT(!isSymbolicLink(e2inode->i_mode));

    unsigned blocksNeededBefore = ceilDiv(e2inode->i_size, blockSize());
    unsigned blocksNeededAfter = ceilDiv(data.size(), blockSize());

    // FIXME: Support growing or shrinking the block list.
    ASSERT(blocksNeededBefore == blocksNeededAfter);

    auto list = blockListForInode(*e2inode);
    if (list.isEmpty()) {
        printf("[ext2fs] writeInode: empty block list for inode %u\n", inode.index());
        return false;
    }

    for (unsigned i = 0; i < list.size(); ++i) {
        auto section = data.slice(i * blockSize(), blockSize());
        printf("section = %p (%u)\n", section.pointer(), section.size());
        bool success = writeBlock(list[i], section);
        ASSERT(success);
    }

    return true;
}

bool Ext2FileSystem::enumerateDirectoryInode(InodeIdentifier inode, std::function<bool(const DirectoryEntry&)> callback) const
{
    ASSERT(inode.fileSystemID() == id());
    ASSERT(isDirectoryInode(inode.index()));

#ifdef EXT2_DEBUG
    printf("[ext2fs] Enumerating directory contents of inode %u:\n", inode.index());
#endif

    auto buffer = readEntireInode(inode);
    ASSERT(buffer);
    auto* entry = reinterpret_cast<ext2_dir_entry_2*>(buffer.pointer());

    char namebuf[EXT2_NAME_LEN + 1];

    while (entry < buffer.endPointer()) {
        if (entry->inode != 0) {
            memcpy(namebuf, entry->name, entry->name_len);
            namebuf[entry->name_len] = 0;
#ifndef EXT2_DEBUG
            printf("inode: %u, name_len: %u, rec_len: %u, file_type: %u, name: %s\n", entry->inode, entry->name_len, entry->rec_len, entry->file_type, namebuf);
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
    printf("[ext2fs] Adding inode %u with name '%s' to directory %u\n", inode, name.characters(), directoryInode);
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
        printf("[ext2fs] Name '%s' already exists in directory inode %u\n", name.characters(), directoryInode);
        return false;
    }

    entries.append({ name, { id(), inode }, fileType });

    return writeDirectoryInode(directoryInode, std::move(entries));
}

class BufferStream {
public:
    explicit BufferStream(ByteBuffer& buffer)
        : m_buffer(buffer)
    {
    }

    void operator<<(byte value)
    {
        m_buffer[m_offset++] = value & 0xffu;
    }

    void operator<<(word value)
    {
        m_buffer[m_offset++] = value & 0xffu;
        m_buffer[m_offset++] = (byte)(value >> 8) & 0xffu;
    }

    void operator<<(dword value)
    {
        m_buffer[m_offset++] = value & 0xffu;
        m_buffer[m_offset++] = (byte)(value >> 8) & 0xffu;
        m_buffer[m_offset++] = (byte)(value >> 16) & 0xffu;
        m_buffer[m_offset++] = (byte)(value >> 24) & 0xffu;
    }

    void operator<<(const String& value)
    {
        for (unsigned i = 0; i < value.length(); ++i)
            m_buffer[m_offset++] = value[i];
    }

    void fillToEnd(byte ch)
    {
        while (m_offset < m_buffer.size())
            m_buffer[m_offset++] = ch;
    }

private:
    ByteBuffer& m_buffer;
    Unix::size_t m_offset { 0 };
};

bool Ext2FileSystem::writeDirectoryInode(unsigned directoryInode, Vector<DirectoryEntry>&& entries)
{
    printf("[ext2fs] New directory inode %u contents to write:\n", directoryInode);

    unsigned directorySize = 0;
    for (auto& entry : entries) {
        printf("  - %08u %s\n", entry.inode.index(), entry.name.characters());
        directorySize += EXT2_DIR_REC_LEN(entry.name.length());
    }

    unsigned blocksNeeded = ceilDiv(directorySize, blockSize());
    unsigned occupiedSize = blocksNeeded * blockSize();

    printf("[ext2fs] directory size: %u (occupied: %u)\n", directorySize, occupiedSize);

    auto directoryData = ByteBuffer::createUninitialized(occupiedSize);

    BufferStream stream(directoryData);
    for (unsigned i = 0; i < entries.size(); ++i) {
        auto& entry = entries[i];

        unsigned recordLength = EXT2_DIR_REC_LEN(entry.name.length());
        if (i == entries.size() - 1)
            recordLength += occupiedSize - directorySize;

        printf("* inode: %u", entry.inode.index());
        printf(", name_len: %u", word(entry.name.length()));
        printf(", rec_len: %u", word(recordLength));
        printf(", file_type: %u", byte(entry.fileType));
        printf(", name: %s\n", entry.name.characters());

        stream << dword(entry.inode.index());
        stream << word(recordLength);
        stream << byte(entry.name.length());
        stream << byte(entry.fileType);
        stream << entry.name;

        unsigned padding = recordLength - entry.name.length() - 8;
        printf("  *** pad %u bytes\n", padding);
        for (unsigned j = 0; j < padding; ++j) {
            stream << byte(0);
        }
    }

    stream.fillToEnd(0);

#if 0
    printf("data to write (%u):\n", directoryData.size());
    for (unsigned i = 0; i < directoryData.size(); ++i) {
        printf("%02x ", directoryData[i]);
        if ((i + 1) % 8 == 0)
            printf(" ");
        if ((i + 1) % 16 == 0)
            printf("\n");
    }
    printf("\n");
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

    printf("[ext2fs] group[%u] block bitmap (bitmap occupies %u blocks):\n", groupIndex, blockCount);

    auto bitmap = Bitmap::wrap(bitmapBlocks.pointer(), blocksInGroup);
    for (unsigned i = 0; i < blocksInGroup; ++i) {
        printf("%c", bitmap.get(i) ? '1' : '0');
    }
    printf("\n");
}

void Ext2FileSystem::dumpInodeBitmap(unsigned groupIndex) const
{
    traverseInodeBitmap(groupIndex, [] (unsigned, const Bitmap& bitmap) {
        for (unsigned i = 0; i < bitmap.size(); ++i)
            printf("%c", bitmap.get(i) ? '1' : '0');
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
        bool shouldContinue = callback(i * (blockSize() / 8), Bitmap::wrap(block.pointer(), inodesInGroup));
        if (!shouldContinue)
            break;
    }
}

bool Ext2FileSystem::setModificationTime(InodeIdentifier inode, dword timestamp)
{
    ASSERT(inode.fileSystemID() == id());

    auto e2inode = lookupExt2Inode(inode.index());
    if (!e2inode)
        return false;

    printf("changing inode %u mtime from %u to %u\n", inode.index(), e2inode->i_mtime, timestamp);
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

unsigned Ext2FileSystem::allocateInode(unsigned preferredGroup, unsigned expectedSize)
{
    printf("[ext2fs] allocateInode(preferredGroup: %u, expectedSize: %u)\n", preferredGroup, expectedSize);

    unsigned neededBlocks = ceilDiv(expectedSize, blockSize());

    printf("[ext2fs] minimum needed blocks: %u\n", neededBlocks);

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
        printf("[ext2fs] allocateInode: no suitable group found for new inode with %u blocks needed :(\n", neededBlocks);
        return 0;
    }

    printf("[ext2fs] allocateInode: found suitable group [%u] for new inode with %u blocks needed :^)\n", groupIndex, neededBlocks);

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
        printf("[ext2fs] firstFreeInodeInGroup returned no inode, despite bgd claiming there are inodes :(\n");
        return 0;
    }

    unsigned inode = firstFreeInodeInGroup;
    printf("[ext2fs] found suitable inode %u\n", inode);

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
    printf("[ext2fs] setInodeAllocationState(%u) %u -> %u\n", inode, currentState, newState);

    if (currentState == newState)
        return true;

    bitmap.set(bitIndex, newState);
    writeBlock(bgd.bg_inode_bitmap + bitmapBlockIndex, block);

    // Update superblock
    auto& sb = *reinterpret_cast<ext2_super_block*>(m_cachedSuperBlock.pointer());
    printf("[ext2fs] superblock free inode count %u -> %u\n", sb.s_free_inodes_count, sb.s_free_inodes_count - 1);
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
    printf("[ext2fs] group free inode count %u -> %u\n", bgd.bg_free_inodes_count, bgd.bg_free_inodes_count - 1);

    unsigned blocksToWrite = ceilDiv(m_blockGroupCount * sizeof(ext2_group_desc), blockSize());
    unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
    writeBlocks(firstBlockOfBGDT, blocksToWrite, m_cachedBlockGroupDescriptorTable);
    
    return true;
}

InodeIdentifier Ext2FileSystem::createInode(InodeIdentifier parentInode, const String& name, word mode)
{
    ASSERT(parentInode.fileSystemID() == id());
    ASSERT(isDirectoryInode(parentInode.index()));

//#ifdef EXT2_DEBUG
    printf("[ext2fs] Adding inode '%s' (mode %o) to parent directory %u:\n", name.characters(), mode, parentInode.index());
//#endif

    // NOTE: This doesn't commit the inode allocation just yet!
    auto inode = allocateInode(0, 0);

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
        printf("[ext2fs] failed to add inode to directory :(\n");
        return { };
    }

    // Looks like we're good, time to update the inode bitmap and group+global inode counters.
    success = setInodeAllocationState(inode, true);
    ASSERT(success);

    auto timestamp = time(nullptr);
    auto e2inode = make<ext2_inode>();
    memset(e2inode.ptr(), 0, sizeof(ext2_inode));
    e2inode->i_mode = mode;
    e2inode->i_uid = 0;
    e2inode->i_size = 0;
    e2inode->i_atime = timestamp;
    e2inode->i_ctime = timestamp;
    e2inode->i_mtime = timestamp;
    e2inode->i_dtime = 0;
    e2inode->i_gid = 0;
    e2inode->i_links_count = 2;
    e2inode->i_blocks = 0;
    e2inode->i_flags = 0;
    success = writeExt2Inode(inode, *e2inode);
    ASSERT(success);

    return { id(), inode };
}

