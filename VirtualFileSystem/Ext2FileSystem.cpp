#include "Ext2FileSystem.h"
#include "ext2_fs.h"
#include "UnixTypes.h"
#include <AK/Bitmap.h>
#include <AK/StdLibExtras.h>
#include <AK/kmalloc.h>
#include <AK/ktime.h>
#include <AK/kstdio.h>
#include <AK/BufferStream.h>
#include <LibC/errno_numbers.h>

//#define EXT2_DEBUG

RetainPtr<Ext2FS> Ext2FS::create(RetainPtr<DiskDevice>&& device)
{
    return adopt(*new Ext2FS(move(device)));
}

Ext2FS::Ext2FS(RetainPtr<DiskDevice>&& device)
    : DiskBackedFS(move(device))
{
}

Ext2FS::~Ext2FS()
{
}

ByteBuffer Ext2FS::read_super_block() const
{
    auto buffer = ByteBuffer::createUninitialized(1024);
    device().read_block(2, buffer.pointer());
    device().read_block(3, buffer.offsetPointer(512));
    return buffer;
}

bool Ext2FS::write_super_block(const ext2_super_block& sb)
{
    const byte* raw = (const byte*)&sb;
    bool success;
    success = device().write_block(2, raw);
    ASSERT(success);
    success = device().write_block(3, raw + 512);
    ASSERT(success);
    // FIXME: This is an ugly way to refresh the superblock cache. :-|
    super_block();
    return true;
}

unsigned Ext2FS::first_block_of_group(unsigned groupIndex) const
{
    return super_block().s_first_data_block + (groupIndex * super_block().s_blocks_per_group);
}

const ext2_super_block& Ext2FS::super_block() const
{
    if (!m_cached_super_block)
        m_cached_super_block = read_super_block();
    return *reinterpret_cast<ext2_super_block*>(m_cached_super_block.pointer());
}

const ext2_group_desc& Ext2FS::group_descriptor(unsigned groupIndex) const
{
    // FIXME: Should this fail gracefully somehow?
    ASSERT(groupIndex <= m_blockGroupCount);

    if (!m_cached_group_descriptor_table) {
        unsigned blocksToRead = ceilDiv(m_blockGroupCount * (unsigned)sizeof(ext2_group_desc), blockSize());
        unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
#ifdef EXT2_DEBUG
        kprintf("ext2fs: block group count: %u, blocks-to-read: %u\n", m_blockGroupCount, blocksToRead);
        kprintf("ext2fs: first block of BGDT: %u\n", firstBlockOfBGDT);
#endif
        m_cached_group_descriptor_table = readBlocks(firstBlockOfBGDT, blocksToRead);
    }
    return reinterpret_cast<ext2_group_desc*>(m_cached_group_descriptor_table.pointer())[groupIndex - 1];
}

bool Ext2FS::initialize()
{
    auto& superBlock = this->super_block();
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

    // Preheat the BGD cache.
    group_descriptor(0);

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

const char* Ext2FS::class_name() const
{
    return "ext2fs";
}

InodeIdentifier Ext2FS::root_inode() const
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

ByteBuffer Ext2FS::read_block_containing_inode(unsigned inode, unsigned& blockIndex, unsigned& offset) const
{
    auto& superBlock = this->super_block();

    if (inode != EXT2_ROOT_INO && inode < EXT2_FIRST_INO(&superBlock))
        return { };

    if (inode > superBlock.s_inodes_count)
        return { };

    auto& bgd = group_descriptor(group_index_from_inode(inode));

    offset = ((inode - 1) % inodes_per_group()) * inode_size();
    blockIndex = bgd.bg_inode_table + (offset >> EXT2_BLOCK_SIZE_BITS(&superBlock));
    offset &= blockSize() - 1;

    return readBlock(blockIndex);
}

OwnPtr<ext2_inode> Ext2FS::lookup_ext2_inode(unsigned inode) const
{
    unsigned blockIndex;
    unsigned offset;
    auto block = read_block_containing_inode(inode, blockIndex, offset);

    if (!block)
        return { };

    auto* e2inode = reinterpret_cast<ext2_inode*>(kmalloc(inode_size()));
    memcpy(e2inode, reinterpret_cast<ext2_inode*>(block.offsetPointer(offset)), inode_size());
#ifdef EXT2_DEBUG
    dumpExt2Inode(*e2inode);
#endif

    return OwnPtr<ext2_inode>(e2inode);
}

InodeMetadata Ext2FS::inode_metadata(InodeIdentifier inode) const
{
    ASSERT(inode.fsid() == id());

    auto e2inode = lookup_ext2_inode(inode.index());
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

Vector<unsigned> Ext2FS::block_list_for_inode(const ext2_inode& e2inode) const
{
    unsigned entriesPerBlock = EXT2_ADDR_PER_BLOCK(&super_block());

    // NOTE: i_blocks is number of 512-byte blocks, not number of fs-blocks.
    unsigned blockCount = e2inode.i_blocks / (blockSize() / 512);
    unsigned blocksRemaining = blockCount;
    Vector<unsigned> list;
    list.ensureCapacity(blocksRemaining);

    unsigned directCount = min(blockCount, (unsigned)EXT2_NDIR_BLOCKS);
    for (unsigned i = 0; i < directCount; ++i) {
        list.unchecked_append(e2inode.i_block[i]);
        --blocksRemaining;
    }

    if (!blocksRemaining)
        return list;

    auto processBlockArray = [&] (unsigned arrayBlockIndex, auto&& callback) {
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
        list.unchecked_append(entry);
    });

    if (!blocksRemaining)
        return list;

    processBlockArray(e2inode.i_block[EXT2_DIND_BLOCK], [&] (unsigned entry) {
        processBlockArray(entry, [&] (unsigned entry) {
            list.unchecked_append(entry);
        });
    });

    if (!blocksRemaining)
        return list;

    processBlockArray(e2inode.i_block[EXT2_TIND_BLOCK], [&] (unsigned entry) {
        processBlockArray(entry, [&] (unsigned entry) {
            processBlockArray(entry, [&] (unsigned entry) {
                list.unchecked_append(entry);
            });
        });
    });

    return list;
}

Ext2FSInode::Ext2FSInode(Ext2FS& fs, unsigned index, const ext2_inode& raw_inode)
    : Inode(fs, index)
    , m_raw_inode(raw_inode)
{
}

Ext2FSInode::~Ext2FSInode()
{
}

void Ext2FSInode::populate_metadata() const
{
    m_metadata.inode = identifier();
    m_metadata.size = m_raw_inode.i_size;
    m_metadata.mode = m_raw_inode.i_mode;
    m_metadata.uid = m_raw_inode.i_uid;
    m_metadata.gid = m_raw_inode.i_gid;
    m_metadata.linkCount = m_raw_inode.i_links_count;
    m_metadata.atime = m_raw_inode.i_atime;
    m_metadata.ctime = m_raw_inode.i_ctime;
    m_metadata.mtime = m_raw_inode.i_mtime;
    m_metadata.dtime = m_raw_inode.i_dtime;
    m_metadata.blockSize = fs().blockSize();
    m_metadata.blockCount = m_raw_inode.i_blocks;

    if (isBlockDevice(m_raw_inode.i_mode) || isCharacterDevice(m_raw_inode.i_mode)) {
        unsigned dev = m_raw_inode.i_block[0];
        m_metadata.majorDevice = (dev & 0xfff00) >> 8;
        m_metadata.minorDevice= (dev & 0xff) | ((dev >> 12) & 0xfff00);
    }
}

void Ext2FSInode::flush_metadata()
{
    m_raw_inode.i_size = m_metadata.size;
    m_raw_inode.i_mode = m_metadata.mode;
    m_raw_inode.i_uid = m_metadata.uid;
    m_raw_inode.i_gid = m_metadata.gid;
    m_raw_inode.i_links_count = m_metadata.linkCount;
    m_raw_inode.i_atime = m_metadata.atime;
    m_raw_inode.i_ctime = m_metadata.ctime;
    m_raw_inode.i_mtime = m_metadata.mtime;
    m_raw_inode.i_dtime = m_metadata.dtime;
    m_raw_inode.i_blocks = m_metadata.blockCount;
    fs().write_ext2_inode(index(), m_raw_inode);
}

RetainPtr<Inode> Ext2FS::get_inode(InodeIdentifier inode) const
{
    ASSERT(inode.fsid() == id());
    {
        LOCKER(m_inode_cache_lock);
        auto it = m_inode_cache.find(inode.index());
        if (it != m_inode_cache.end())
            return (*it).value;
    }
    auto raw_inode = lookup_ext2_inode(inode.index());
    if (!raw_inode)
        return nullptr;
    LOCKER(m_inode_cache_lock);
    auto it = m_inode_cache.find(inode.index());
    if (it != m_inode_cache.end())
        return (*it).value;
    auto new_inode = adopt(*new Ext2FSInode(const_cast<Ext2FS&>(*this), inode.index(), *raw_inode));
    m_inode_cache.set(inode.index(), new_inode.copyRef());
    return new_inode;
}

ssize_t Ext2FSInode::read_bytes(Unix::off_t offset, size_t count, byte* buffer, FileDescriptor*)
{
    ASSERT(offset >= 0);
    if (m_raw_inode.i_size == 0)
        return 0;

    // Symbolic links shorter than 60 characters are store inline inside the i_block array.
    // This avoids wasting an entire block on short links. (Most links are short.)
    static const unsigned max_inline_symlink_length = 60;
    if (is_symlink() && size() < max_inline_symlink_length) {
        ssize_t nread = min((Unix::off_t)size() - offset, static_cast<Unix::off_t>(count));
        memcpy(buffer, m_raw_inode.i_block + offset, nread);
        return nread;
    }

    if (m_block_list.isEmpty()) {
        auto block_list = fs().block_list_for_inode(m_raw_inode);
        LOCKER(m_lock);
        if (m_block_list.size() != block_list.size())
            m_block_list = move(block_list);
    }

    if (m_block_list.isEmpty()) {
        kprintf("ext2fs: read_bytes: empty block list for inode %u\n", index());
        return -EIO;
    }

    const size_t block_size = fs().blockSize();

    dword first_block_logical_index = offset / block_size;
    dword last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    dword offset_into_first_block = offset % block_size;

    ssize_t nread = 0;
    size_t remaining_count = min((Unix::off_t)count, (Unix::off_t)size() - offset);
    byte* out = buffer;

#ifdef EXT2_DEBUG
    kprintf("ok let's do it, read(%llu, %u) -> blocks %u thru %u, oifb: %u\n", offset, count, firstBlockLogicalIndex, lastBlockLogicalIndex, offsetIntoFirstBlock);
#endif

    for (dword bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; ++bi) {
        auto block = fs().readBlock(m_block_list[bi]);
        if (!block) {
            kprintf("ext2fs: read_bytes: readBlock(%u) failed (lbi: %u)\n", m_block_list[bi], bi);
            return -EIO;
        }

        dword offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        dword num_bytes_to_copy = min(block_size - offset_into_block, remaining_count);
        memcpy(out, block.pointer() + offset_into_block, num_bytes_to_copy);
        remaining_count -= num_bytes_to_copy;
        nread += num_bytes_to_copy;
        out += num_bytes_to_copy;
    }

    return nread;
}

ssize_t Ext2FS::read_inode_bytes(InodeIdentifier inode, Unix::off_t offset, size_t count, byte* buffer, FileDescriptor*) const
{
    ASSERT(offset >= 0);
    ASSERT(inode.fsid() == id());

    auto e2inode = lookup_ext2_inode(inode.index());
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
        ssize_t nread = min((Unix::off_t)e2inode->i_size - offset, static_cast<Unix::off_t>(count));
        memcpy(buffer, e2inode->i_block + offset, nread);
        return nread;
    }

    // FIXME: It's grossly inefficient to fetch the blocklist on every call to readInodeBytes().
    //        It needs to be cached!
    auto list = block_list_for_inode(*e2inode);
    if (list.isEmpty()) {
        kprintf("ext2fs: readInodeBytes: empty block list for inode %u\n", inode.index());
        return -EIO;
    }

    dword firstBlockLogicalIndex = offset / blockSize();
    dword lastBlockLogicalIndex = (offset + count) / blockSize();
    if (lastBlockLogicalIndex >= list.size())
        lastBlockLogicalIndex = list.size() - 1;

    dword offsetIntoFirstBlock = offset % blockSize();

    ssize_t nread = 0;
    size_t remainingCount = min((Unix::off_t)count, (Unix::off_t)e2inode->i_size - offset);
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

        size_t numBytesToCopy = min(blockSize() - offsetIntoBlock, remainingCount);
        memcpy(out, block.pointer() + offsetIntoBlock, numBytesToCopy);
        remainingCount -= numBytesToCopy;
        nread += numBytesToCopy;
        out += numBytesToCopy;
    }

    return nread;
}

bool Ext2FS::write_inode(InodeIdentifier inode, const ByteBuffer& data)
{
    ASSERT(inode.fsid() == id());

    auto e2inode = lookup_ext2_inode(inode.index());
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

    auto list = block_list_for_inode(*e2inode);
    if (list.isEmpty()) {
        kprintf("ext2fs: writeInode: empty block list for inode %u\n", inode.index());
        return false;
    }

    for (unsigned i = 0; i < list.size(); ++i) {
        auto section = data.slice(i * blockSize(), blockSize());
        //kprintf("section = %p (%u)\n", section.pointer(), section.size());
        bool success = writeBlock(list[i], section);
        ASSERT(success);
    }

    return true;
}

bool Ext2FSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntry&)> callback)
{
    ASSERT(metadata().isDirectory());

#ifdef EXT2_DEBUG
    kprintf("Ext2Inode::traverse_as_directory: inode=%u:\n", index());
#endif

    auto buffer = read_entire();
    ASSERT(buffer);
    auto* entry = reinterpret_cast<ext2_dir_entry_2*>(buffer.pointer());

    while (entry < buffer.endPointer()) {
        if (entry->inode != 0) {
#ifdef EXT2_DEBUG
            kprintf("Ext2Inode::traverse_as_directory: %u, name_len: %u, rec_len: %u, file_type: %u, name: %s\n", entry->inode, entry->name_len, entry->rec_len, entry->file_type, namebuf);
#endif
            if (!callback({ entry->name, entry->name_len, { fsid(), entry->inode }, entry->file_type }))
                break;
        }
        entry = (ext2_dir_entry_2*)((char*)entry + entry->rec_len);
    }
    return true;
}

bool Ext2FS::add_inode_to_directory(InodeIndex parent, InodeIndex child, const String& name, byte fileType, int& error)
{
    auto e2inodeForDirectory = lookup_ext2_inode(parent);
    ASSERT(e2inodeForDirectory);
    ASSERT(isDirectory(e2inodeForDirectory->i_mode));

//#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: Adding inode %u with name '%s' to directory %u\n", child, name.characters(), parent);
//#endif

    Vector<DirectoryEntry> entries;
    bool nameAlreadyExists = false;
    auto directory = get_inode({ id(), parent });
    directory->traverse_as_directory([&] (auto& entry) {
        if (!strcmp(entry.name, name.characters())) {
            nameAlreadyExists = true;
            return false;
        }
        entries.append(entry);
        return true;
    });
    if (nameAlreadyExists) {
        kprintf("Ext2FS: Name '%s' already exists in directory inode %u\n", name.characters(), parent);
        error = -EEXIST;
        return false;
    }

    entries.append({ name.characters(), name.length(), { id(), child }, fileType });
    return write_directory_inode(parent, move(entries));
}

bool Ext2FS::write_directory_inode(unsigned directoryInode, Vector<DirectoryEntry>&& entries)
{
    dbgprintf("Ext2FS: New directory inode %u contents to write:\n", directoryInode);

    unsigned directorySize = 0;
    for (auto& entry : entries) {
        //kprintf("  - %08u %s\n", entry.inode.index(), entry.name);
        directorySize += EXT2_DIR_REC_LEN(entry.name_length);
    }

    unsigned blocksNeeded = ceilDiv(directorySize, blockSize());
    unsigned occupiedSize = blocksNeeded * blockSize();

    dbgprintf("Ext2FS: directory size: %u (occupied: %u)\n", directorySize, occupiedSize);

    auto directoryData = ByteBuffer::createUninitialized(occupiedSize);

    BufferStream stream(directoryData);
    for (unsigned i = 0; i < entries.size(); ++i) {
        auto& entry = entries[i];

        unsigned recordLength = EXT2_DIR_REC_LEN(entry.name_length);
        if (i == entries.size() - 1)
            recordLength += occupiedSize - directorySize;

        dbgprintf("* inode: %u", entry.inode.index());
        dbgprintf(", name_len: %u", word(entry.name_length));
        dbgprintf(", rec_len: %u", word(recordLength));
        dbgprintf(", file_type: %u", byte(entry.fileType));
        dbgprintf(", name: %s\n", entry.name);

        stream << dword(entry.inode.index());
        stream << word(recordLength);
        stream << byte(entry.name_length);
        stream << byte(entry.fileType);
        stream << entry.name;

        unsigned padding = recordLength - entry.name_length - 8;
        //dbgprintf("  *** pad %u bytes\n", padding);
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

    write_inode({ id(), directoryInode }, directoryData);

    return true;
}

unsigned Ext2FS::inodes_per_block() const
{
    return EXT2_INODES_PER_BLOCK(&super_block());
}

unsigned Ext2FS::inodes_per_group() const
{
    return EXT2_INODES_PER_GROUP(&super_block());
}

unsigned Ext2FS::inode_size() const
{
    return EXT2_INODE_SIZE(&super_block());

}
unsigned Ext2FS::blocks_per_group() const
{
    return EXT2_BLOCKS_PER_GROUP(&super_block());
}

void Ext2FS::dump_block_bitmap(unsigned groupIndex) const
{
    ASSERT(groupIndex <= m_blockGroupCount);
    auto& bgd = group_descriptor(groupIndex);

    unsigned blocksInGroup = min(blocks_per_group(), super_block().s_blocks_count);
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

void Ext2FS::dump_inode_bitmap(unsigned groupIndex) const
{
    traverse_inode_bitmap(groupIndex, [] (unsigned, const Bitmap& bitmap) {
        for (unsigned i = 0; i < bitmap.size(); ++i)
            kprintf("%c", bitmap.get(i) ? '1' : '0');
        return true;
    });
}

template<typename F>
void Ext2FS::traverse_inode_bitmap(unsigned groupIndex, F callback) const
{
    ASSERT(groupIndex <= m_blockGroupCount);
    auto& bgd = group_descriptor(groupIndex);

    unsigned inodesInGroup = min(inodes_per_group(), super_block().s_inodes_count);
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
void Ext2FS::traverse_block_bitmap(unsigned groupIndex, F callback) const
{
    ASSERT(groupIndex <= m_blockGroupCount);
    auto& bgd = group_descriptor(groupIndex);

    unsigned blocksInGroup = min(blocks_per_group(), super_block().s_blocks_count);
    unsigned blockCount = ceilDiv(blocksInGroup, 8u);

    for (unsigned i = 0; i < blockCount; ++i) {
        auto block = readBlock(bgd.bg_block_bitmap + i);
        ASSERT(block);
        bool shouldContinue = callback(i * (blockSize() / 8) + 1, Bitmap::wrap(block.pointer(), blocksInGroup));
        if (!shouldContinue)
            break;
    }
}

bool Ext2FS::modify_link_count(InodeIndex inode, int delta)
{
    ASSERT(inode);
    auto e2inode = lookup_ext2_inode(inode);
    if (!e2inode)
        return false;

    auto newLinkCount = e2inode->i_links_count + delta;
    dbgprintf("Ext2FS: changing inode %u link count from %u to %u\n", inode, e2inode->i_links_count, newLinkCount);
    e2inode->i_links_count = newLinkCount;

    return write_ext2_inode(inode, *e2inode);
}

bool Ext2FS::write_ext2_inode(unsigned inode, const ext2_inode& e2inode)
{
    unsigned blockIndex;
    unsigned offset;
    auto block = read_block_containing_inode(inode, blockIndex, offset);
    if (!block)
        return false;
    {
        LOCKER(m_inode_cache_lock);
        auto it = m_inode_cache.find(inode);
        if (it != m_inode_cache.end()) {
            auto& cached_inode = *(*it).value;
            LOCKER(cached_inode.m_lock);
            cached_inode.m_raw_inode = e2inode;
            cached_inode.populate_metadata();
            if (cached_inode.is_directory())
                cached_inode.m_lookup_cache.clear();
        }
    }
    memcpy(reinterpret_cast<ext2_inode*>(block.offsetPointer(offset)), &e2inode, inode_size());
    writeBlock(blockIndex, block);
    return true;
}

bool Ext2FS::is_directory_inode(unsigned inode) const
{
    if (auto e2inode = lookup_ext2_inode(inode))
        return isDirectory(e2inode->i_mode);
    return false;
}

Vector<Ext2FS::BlockIndex> Ext2FS::allocate_blocks(unsigned group, unsigned count)
{
    dbgprintf("Ext2FS: allocateBlocks(group: %u, count: %u)\n", group, count);

    auto& bgd = group_descriptor(group);
    if (bgd.bg_free_blocks_count < count) {
        kprintf("ExtFS: allocateBlocks can't allocate out of group %u, wanted %u but only %u available\n", group, count, bgd.bg_free_blocks_count);
        return { };
    }

    // FIXME: Implement a scan that finds consecutive blocks if possible.
    Vector<BlockIndex> blocks;
    traverse_block_bitmap(group, [&blocks, count] (unsigned firstBlockInBitmap, const Bitmap& bitmap) {
        for (unsigned i = 0; i < bitmap.size(); ++i) {
            if (!bitmap.get(i)) {
                blocks.append(firstBlockInBitmap + i);
                if (blocks.size() == count)
                    return false;
            }
        }
        return true;
    });
    dbgprintf("Ext2FS: allocateBlock found these blocks:\n");
    for (auto& bi : blocks) {
        dbgprintf("  > %u\n", bi);
    }

    return blocks;
}

unsigned Ext2FS::allocate_inode(unsigned preferredGroup, unsigned expectedSize)
{
    dbgprintf("Ext2FS: allocateInode(preferredGroup: %u, expectedSize: %u)\n", preferredGroup, expectedSize);

    unsigned neededBlocks = ceilDiv(expectedSize, blockSize());

    dbgprintf("Ext2FS: minimum needed blocks: %u\n", neededBlocks);

    unsigned groupIndex = 0;

    auto isSuitableGroup = [this, neededBlocks] (unsigned groupIndex) {
        auto& bgd = group_descriptor(groupIndex);
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
        kprintf("Ext2FS: allocateInode: no suitable group found for new inode with %u blocks needed :(\n", neededBlocks);
        return 0;
    }

    dbgprintf("Ext2FS: allocateInode: found suitable group [%u] for new inode with %u blocks needed :^)\n", groupIndex, neededBlocks);

    unsigned firstFreeInodeInGroup = 0;
    traverse_inode_bitmap(groupIndex, [&firstFreeInodeInGroup] (unsigned firstInodeInBitmap, const Bitmap& bitmap) {
        for (unsigned i = 0; i < bitmap.size(); ++i) {
            if (!bitmap.get(i)) {
                firstFreeInodeInGroup = firstInodeInBitmap + i;
                return false;
            }
        }
        return true;
    });

    if (!firstFreeInodeInGroup) {
        kprintf("Ext2FS: firstFreeInodeInGroup returned no inode, despite bgd claiming there are inodes :(\n");
        return 0;
    }

    unsigned inode = firstFreeInodeInGroup;
    dbgprintf("Ext2FS: found suitable inode %u\n", inode);

    // FIXME: allocate blocks if needed!

    return inode;
}

unsigned Ext2FS::group_index_from_inode(unsigned inode) const
{
    if (!inode)
        return 0;
    return (inode - 1) / inodes_per_group() + 1;
}

bool Ext2FS::set_inode_allocation_state(unsigned inode, bool newState)
{
    auto& bgd = group_descriptor(group_index_from_inode(inode));

    // Update inode bitmap
    unsigned inodesPerBitmapBlock = blockSize() * 8;
    unsigned bitmapBlockIndex = (inode - 1) / inodesPerBitmapBlock;
    unsigned bitIndex = (inode - 1) % inodesPerBitmapBlock;
    auto block = readBlock(bgd.bg_inode_bitmap + bitmapBlockIndex);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), block.size());
    bool currentState = bitmap.get(bitIndex);
    dbgprintf("ext2fs: setInodeAllocationState(%u) %u -> %u\n", inode, currentState, newState);

    if (currentState == newState)
        return true;

    bitmap.set(bitIndex, newState);
    writeBlock(bgd.bg_inode_bitmap + bitmapBlockIndex, block);

    // Update superblock
    auto& sb = *reinterpret_cast<ext2_super_block*>(m_cached_super_block.pointer());
    dbgprintf("Ext2FS: superblock free inode count %u -> %u\n", sb.s_free_inodes_count, sb.s_free_inodes_count - 1);
    if (newState)
        --sb.s_free_inodes_count;
    else
        ++sb.s_free_inodes_count;
    write_super_block(sb);

    // Update BGD
    auto& mutableBGD = const_cast<ext2_group_desc&>(bgd);
    if (newState)
        --mutableBGD.bg_free_inodes_count;
    else
        ++mutableBGD.bg_free_inodes_count;
    dbgprintf("Ext2FS: group free inode count %u -> %u\n", bgd.bg_free_inodes_count, bgd.bg_free_inodes_count - 1);

    unsigned blocksToWrite = ceilDiv(m_blockGroupCount * (unsigned)sizeof(ext2_group_desc), blockSize());
    unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
    writeBlocks(firstBlockOfBGDT, blocksToWrite, m_cached_group_descriptor_table);
    
    return true;
}

bool Ext2FS::set_block_allocation_state(GroupIndex group, BlockIndex bi, bool newState)
{
    auto& bgd = group_descriptor(group);

    // Update block bitmap
    unsigned blocksPerBitmapBlock = blockSize() * 8;
    unsigned bitmapBlockIndex = (bi - 1) / blocksPerBitmapBlock;
    unsigned bitIndex = (bi - 1) % blocksPerBitmapBlock;
    auto block = readBlock(bgd.bg_block_bitmap + bitmapBlockIndex);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), block.size());
    bool currentState = bitmap.get(bitIndex);
    dbgprintf("Ext2FS: setBlockAllocationState(%u) %u -> %u\n", bi, currentState, newState);

    if (currentState == newState)
        return true;

    bitmap.set(bitIndex, newState);
    writeBlock(bgd.bg_block_bitmap + bitmapBlockIndex, block);

    // Update superblock
    auto& sb = *reinterpret_cast<ext2_super_block*>(m_cached_super_block.pointer());
    dbgprintf("Ext2FS: superblock free block count %u -> %u\n", sb.s_free_blocks_count, sb.s_free_blocks_count - 1);
    if (newState)
        --sb.s_free_blocks_count;
    else
        ++sb.s_free_blocks_count;
    write_super_block(sb);

    // Update BGD
    auto& mutableBGD = const_cast<ext2_group_desc&>(bgd);
    if (newState)
        --mutableBGD.bg_free_blocks_count;
    else
        ++mutableBGD.bg_free_blocks_count;
    dbgprintf("Ext2FS: group free block count %u -> %u\n", bgd.bg_free_blocks_count, bgd.bg_free_blocks_count - 1);

    unsigned blocksToWrite = ceilDiv(m_blockGroupCount * (unsigned)sizeof(ext2_group_desc), blockSize());
    unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
    writeBlocks(firstBlockOfBGDT, blocksToWrite, m_cached_group_descriptor_table);
    
    return true;
}

InodeIdentifier Ext2FS::create_directory(InodeIdentifier parentInode, const String& name, Unix::mode_t mode, int& error)
{
    ASSERT(parentInode.fsid() == id());
    ASSERT(is_directory_inode(parentInode.index()));

    // Fix up the mode to definitely be a directory.
    // FIXME: This is a bit on the hackish side.
    mode &= ~0170000;
    mode |= 0040000;

    // NOTE: When creating a new directory, make the size 1 block.
    //       There's probably a better strategy here, but this works for now.
    auto inode = create_inode(parentInode, name, mode, blockSize(), error);
    if (!inode.is_valid())
        return { };

    dbgprintf("Ext2FS: create_directory: created new directory named '%s' with inode %u\n", name.characters(), inode.index());

    Vector<DirectoryEntry> entries;
    entries.append({ ".", inode, EXT2_FT_DIR });
    entries.append({ "..", parentInode, EXT2_FT_DIR });

    bool success = write_directory_inode(inode.index(), move(entries));
    ASSERT(success);

    success = modify_link_count(parentInode.index(), 1);
    ASSERT(success);

    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode.index())));
    ++bgd.bg_used_dirs_count;
    dbgprintf("Ext2FS: incremented bg_used_dirs_count %u -> %u\n", bgd.bg_used_dirs_count - 1, bgd.bg_used_dirs_count);

    unsigned blocksToWrite = ceilDiv(m_blockGroupCount * (unsigned)sizeof(ext2_group_desc), blockSize());
    unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
    writeBlocks(firstBlockOfBGDT, blocksToWrite, m_cached_group_descriptor_table);

    error = 0;
    return inode;
}

InodeIdentifier Ext2FS::create_inode(InodeIdentifier parentInode, const String& name, Unix::mode_t mode, unsigned size, int& error)
{
    ASSERT(parentInode.fsid() == id());
    ASSERT(is_directory_inode(parentInode.index()));

    dbgprintf("Ext2FS: Adding inode '%s' (mode %u) to parent directory %u:\n", name.characters(), mode, parentInode.index());

    // NOTE: This doesn't commit the inode allocation just yet!
    auto inode = allocate_inode(0, 0);
    if (!inode) {
        kprintf("Ext2FS: createInode: allocateInode failed\n");
        error = -ENOSPC;
        return { };
    }

    auto blocks = allocate_blocks(group_index_from_inode(inode), ceilDiv(size, blockSize()));
    if (blocks.isEmpty()) {
        kprintf("Ext2FS: createInode: allocateBlocks failed\n");
        error = -ENOSPC;
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
    bool success = add_inode_to_directory(parentInode.index(), inode, name, fileType, error);
    if (!success)
        return { };

    // Looks like we're good, time to update the inode bitmap and group+global inode counters.
    success = set_inode_allocation_state(inode, true);
    ASSERT(success);

    for (auto bi : blocks) {
        success = set_block_allocation_state(group_index_from_inode(inode), bi, true);
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

    dbgprintf("Ext2FS: writing %zu blocks to i_block array\n", min((size_t)EXT2_NDIR_BLOCKS, blocks.size()));
    for (unsigned i = 0; i < min((size_t)EXT2_NDIR_BLOCKS, blocks.size()); ++i) {
        e2inode->i_block[i] = blocks[i];
    }

    e2inode->i_flags = 0;
    success = write_ext2_inode(inode, *e2inode);
    ASSERT(success);

    return { id(), inode };
}

InodeIdentifier Ext2FS::find_parent_of_inode(InodeIdentifier inode_id) const
{
    auto inode = get_inode(inode_id);
    ASSERT(inode);

    unsigned groupIndex = group_index_from_inode(inode->index());
    unsigned firstInodeInGroup = inodes_per_group() * (groupIndex - 1);

    Vector<RetainPtr<Ext2FSInode>> directories_in_group;

    for (unsigned i = 0; i < inodes_per_group(); ++i) {
        auto group_member = get_inode({ id(), firstInodeInGroup + i });
        if (!group_member)
            continue;
        if (group_member->is_directory())
            directories_in_group.append(move(group_member));
    }

    InodeIdentifier foundParent;
    for (auto& directory : directories_in_group) {
        if (!directory->reverse_lookup(inode->identifier()).isNull()) {
            foundParent = directory->identifier();
            break;
        }
    }

    return foundParent;
}

void Ext2FSInode::populate_lookup_cache()
{
    {
        LOCKER(m_lock);
        if (!m_lookup_cache.isEmpty())
            return;
    }
    HashMap<String, unsigned> children;

    traverse_as_directory([&children] (auto& entry) {
        children.set(String(entry.name, entry.name_length), entry.inode.index());
        return true;
    });

    LOCKER(m_lock);
    if (!m_lookup_cache.isEmpty())
        return;
    m_lookup_cache = move(children);
}

InodeIdentifier Ext2FSInode::lookup(const String& name)
{
    ASSERT(is_directory());
    populate_lookup_cache();
    LOCKER(m_lock);
    auto it = m_lookup_cache.find(name);
    if (it != m_lookup_cache.end())
        return { fsid(), (*it).value };
    return { };
}

String Ext2FSInode::reverse_lookup(InodeIdentifier child_id)
{
    ASSERT(is_directory());
    ASSERT(child_id.fsid() == fsid());
    populate_lookup_cache();
    LOCKER(m_lock);
    for (auto it : m_lookup_cache) {
        if (it.value == child_id.index())
            return it.key;
    }
    return { };
}
