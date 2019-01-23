#include "Ext2FileSystem.h"
#include "ext2_fs.h"
#include "UnixTypes.h"
#include "RTC.h"
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
    auto buffer = ByteBuffer::create_uninitialized(1024);
    device().read_block(2, buffer.pointer());
    device().read_block(3, buffer.offset_pointer(512));
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
    kprintf("ext2fs: inodes per block = %u\n", inodes_per_block());
    kprintf("ext2fs: inodes per group = %u\n", inodes_per_group());
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
        auto& group = group_descriptor(i);
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

Ext2FS::BlockListShape Ext2FS::compute_block_list_shape(unsigned blocks)
{
    BlockListShape shape;
    const unsigned entries_per_block = EXT2_ADDR_PER_BLOCK(&super_block());
    unsigned blocks_remaining = blocks;
    shape.direct_blocks = min((unsigned)EXT2_NDIR_BLOCKS, blocks_remaining);
    blocks_remaining -= shape.direct_blocks;
    if (!blocks_remaining)
        return shape;
    shape.indirect_blocks = min(blocks_remaining, entries_per_block);
    blocks_remaining -= shape.indirect_blocks;
    shape.meta_blocks += 1;
    if (!blocks_remaining)
        return shape;
    ASSERT_NOT_REACHED();
    // FIXME: Support dind/tind blocks.
    shape.doubly_indirect_blocks = min(blocks_remaining, entries_per_block * entries_per_block);
    blocks_remaining -= shape.doubly_indirect_blocks;
    if (!blocks_remaining)
        return shape;
    shape.triply_indirect_blocks = min(blocks_remaining, entries_per_block * entries_per_block * entries_per_block);
    blocks_remaining -= shape.triply_indirect_blocks;
    // FIXME: What do we do for files >= 16GB?
    ASSERT(!blocks_remaining);
    return shape;
}

bool Ext2FS::write_block_list_for_inode(InodeIndex inode_index, ext2_inode& e2inode, const Vector<BlockIndex>& blocks)
{
    dbgprintf("Ext2FS: writing %u block(s) to i_block array\n", min((size_t)EXT2_NDIR_BLOCKS, blocks.size()));

    auto old_shape = compute_block_list_shape(e2inode.i_blocks / (2 << super_block().s_log_block_size));
    auto new_shape = compute_block_list_shape(blocks.size());

    Vector<BlockIndex> new_meta_blocks;
    if (new_shape.meta_blocks > old_shape.meta_blocks) {
        new_meta_blocks = allocate_blocks(group_index_from_inode(inode_index), new_shape.meta_blocks - old_shape.meta_blocks);
        for (auto bi : new_meta_blocks)
            set_block_allocation_state(group_index_from_inode(inode_index), bi, true);
    }

    unsigned output_block_index = 0;
    unsigned remaining_blocks = blocks.size();
    for (unsigned i = 0; i < new_shape.direct_blocks; ++i) {
        e2inode.i_block[i] = blocks[output_block_index++];
        --remaining_blocks;
    }
    write_ext2_inode(inode_index, e2inode);

    if (!remaining_blocks)
        return true;

    if (!e2inode.i_block[EXT2_IND_BLOCK]) {
        e2inode.i_block[EXT2_IND_BLOCK] = new_meta_blocks.take_last();
        write_ext2_inode(inode_index, e2inode);
    }

    {
        dbgprintf("Ext2FS: Writing out indirect blockptr block for inode %u\n", inode_index);
        auto block_contents = ByteBuffer::create_uninitialized(blockSize());
        BufferStream stream(block_contents);
        ASSERT(new_shape.indirect_blocks <= EXT2_ADDR_PER_BLOCK(&super_block()));
        for (unsigned i = 0; i < new_shape.indirect_blocks; ++i) {
            stream << blocks[output_block_index++];
            --remaining_blocks;
        }
        stream.fill_to_end(0);
        writeBlock(e2inode.i_block[EXT2_IND_BLOCK], block_contents);
    }

    if (!remaining_blocks)
        return true;

    // FIXME: Implement!
    ASSERT_NOT_REACHED();
}

Vector<unsigned> Ext2FS::block_list_for_inode(const ext2_inode& e2inode, bool include_block_list_blocks) const
{
    unsigned entriesPerBlock = EXT2_ADDR_PER_BLOCK(&super_block());

    // NOTE: i_blocks is number of 512-byte blocks, not number of fs-blocks.
    unsigned blockCount = e2inode.i_blocks / (blockSize() / 512);
    unsigned blocksRemaining = blockCount;
    Vector<unsigned> list;
    if (include_block_list_blocks) {
        // This seems like an excessive over-estimate but w/e.
        list.ensure_capacity(blocksRemaining * 2);
    } else {
        list.ensure_capacity(blocksRemaining);
    }

    unsigned directCount = min(blockCount, (unsigned)EXT2_NDIR_BLOCKS);
    for (unsigned i = 0; i < directCount; ++i) {
        list.unchecked_append(e2inode.i_block[i]);
        --blocksRemaining;
    }

    if (!blocksRemaining)
        return list;

    auto processBlockArray = [&] (unsigned arrayBlockIndex, auto&& callback) {
        if (include_block_list_blocks)
            callback(arrayBlockIndex);
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

void Ext2FS::free_inode(Ext2FSInode& inode)
{
    ASSERT(inode.m_raw_inode.i_links_count == 0);
    dbgprintf("Ext2FS: inode %u has no more links, time to delete!\n", inode.index());

    inode.m_raw_inode.i_dtime = RTC::now();
    write_ext2_inode(inode.index(), inode.m_raw_inode);

    auto block_list = block_list_for_inode(inode.m_raw_inode, true);

    auto group_index = group_index_from_inode(inode.index());
    for (auto block_index : block_list)
        set_block_allocation_state(group_index, block_index, false);

    set_inode_allocation_state(inode.index(), false);
}

Ext2FSInode::Ext2FSInode(Ext2FS& fs, unsigned index, const ext2_inode& raw_inode)
    : Inode(fs, index)
    , m_raw_inode(raw_inode)
{
}

Ext2FSInode::~Ext2FSInode()
{
    if (m_raw_inode.i_links_count == 0)
        fs().free_inode(*this);
}

InodeMetadata Ext2FSInode::metadata() const
{
    InodeMetadata metadata;
    metadata.inode = identifier();
    metadata.size = m_raw_inode.i_size;
    metadata.mode = m_raw_inode.i_mode;
    metadata.uid = m_raw_inode.i_uid;
    metadata.gid = m_raw_inode.i_gid;
    metadata.linkCount = m_raw_inode.i_links_count;
    metadata.atime = m_raw_inode.i_atime;
    metadata.ctime = m_raw_inode.i_ctime;
    metadata.mtime = m_raw_inode.i_mtime;
    metadata.dtime = m_raw_inode.i_dtime;
    metadata.blockSize = fs().blockSize();
    metadata.blockCount = m_raw_inode.i_blocks;

    if (isBlockDevice(m_raw_inode.i_mode) || isCharacterDevice(m_raw_inode.i_mode)) {
        unsigned dev = m_raw_inode.i_block[0];
        metadata.majorDevice = (dev & 0xfff00) >> 8;
        metadata.minorDevice= (dev & 0xff) | ((dev >> 12) & 0xfff00);
    }
    return metadata;
}

void Ext2FSInode::flush_metadata()
{
    dbgprintf("Ext2FSInode: flush_metadata for inode %u\n", index());
    fs().write_ext2_inode(index(), m_raw_inode);
    if (is_directory()) {
        // FIXME: This invalidation is way too hardcore.
        LOCKER(m_lock);
        m_lookup_cache.clear();
    }
    set_metadata_dirty(false);
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

    if (!get_inode_allocation_state(inode.index())) {
        LOCKER(m_inode_cache_lock);
        m_inode_cache.set(inode.index(), nullptr);
        return nullptr;
    }

    unsigned block_index;
    unsigned offset;
    auto block = read_block_containing_inode(inode.index(), block_index, offset);
    if (!block)
        return { };

    // FIXME: Avoid this extra allocation, copy the raw inode directly into the Ext2FSInode metadata somehow.
    auto* e2inode = reinterpret_cast<ext2_inode*>(kmalloc(inode_size()));
    memcpy(e2inode, reinterpret_cast<ext2_inode*>(block.offset_pointer(offset)), inode_size());
    auto raw_inode = OwnPtr<ext2_inode>(e2inode);
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

    if (m_block_list.is_empty()) {
        auto block_list = fs().block_list_for_inode(m_raw_inode);
        LOCKER(m_lock);
        if (m_block_list.size() != block_list.size())
            m_block_list = move(block_list);
    }

    if (m_block_list.is_empty()) {
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
    kprintf("Ext2FS: Reading up to %u bytes %d bytes into inode %u:%u to %p\n", count, offset, identifier().fsid(), identifier().index(), buffer);
    //kprintf("ok let's do it, read(%u, %u) -> blocks %u thru %u, oifb: %u\n", offset, count, first_block_logical_index, last_block_logical_index, offset_into_first_block);
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

ssize_t Ext2FSInode::write_bytes(Unix::off_t offset, size_t count, const byte* data, FileDescriptor*)
{
    LOCKER(m_lock);

    // FIXME: Support writing to symlink inodes.
    ASSERT(!is_symlink());

    ASSERT(offset >= 0);

    const size_t block_size = fs().blockSize();
    size_t new_size = max(static_cast<size_t>(offset) + count, size());

    unsigned blocks_needed_before = ceilDiv(size(), block_size);
    unsigned blocks_needed_after = ceilDiv(new_size, block_size);

    auto block_list = fs().block_list_for_inode(m_raw_inode);
    if (blocks_needed_after > blocks_needed_before) {
        auto new_blocks = fs().allocate_blocks(fs().group_index_from_inode(index()), blocks_needed_after - blocks_needed_before);
        for (auto new_block_index : new_blocks)
            fs().set_block_allocation_state(fs().group_index_from_inode(index()), new_block_index, true);
        block_list.append(move(new_blocks));
    } else if (blocks_needed_after < blocks_needed_before) {
        // FIXME: Implement block list shrinking!
        ASSERT_NOT_REACHED();
    }

    dword first_block_logical_index = offset / block_size;
    dword last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= block_list.size())
        last_block_logical_index = block_list.size() - 1;

    dword offset_into_first_block = offset % block_size;

    ssize_t nwritten = 0;
    size_t remaining_count = min((Unix::off_t)count, (Unix::off_t)new_size - offset);
    const byte* in = data;

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FSInode::write_bytes: Writing %u bytes %d bytes into inode %u:%u from %p\n", count, offset, fsid(), index(), data);
#endif

    auto buffer_block = ByteBuffer::create_uninitialized(block_size);
    for (dword bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; ++bi) {
        dword offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        dword num_bytes_to_copy = min(block_size - offset_into_block, remaining_count);

        ByteBuffer block;
        if (offset_into_block != 0) {
            block = fs().readBlock(block_list[bi]);
            if (!block) {
                kprintf("Ext2FSInode::write_bytes: readBlock(%u) failed (lbi: %u)\n", block_list[bi], bi);
                return -EIO;
            }
        } else
            block = buffer_block;

        memcpy(block.pointer() + offset_into_block, in, num_bytes_to_copy);
        if (offset_into_block == 0 && !num_bytes_to_copy)
            memset(block.pointer() + num_bytes_to_copy, 0, block_size - num_bytes_to_copy);
#ifdef EXT2_DEBUG
        dbgprintf("Ext2FSInode::write_bytes: writing block %u (offset_into_block: %u)\n", block_list[bi], offset_into_block);
#endif
        bool success = fs().writeBlock(block_list[bi], block);
        if (!success) {
            kprintf("Ext2FSInode::write_bytes: writeBlock(%u) failed (lbi: %u)\n", block_list[bi], bi);
            return -EIO;
        }
        remaining_count -= num_bytes_to_copy;
        nwritten += num_bytes_to_copy;
        in += num_bytes_to_copy;
    }

    bool success = fs().write_block_list_for_inode(index(), m_raw_inode, block_list);
    ASSERT(success);

    m_raw_inode.i_size = new_size;
    m_raw_inode.i_blocks = block_list.size() * (block_size / 512);
    fs().write_ext2_inode(index(), m_raw_inode);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FSInode::write_bytes: after write, i_size=%u, i_blocks=%u (%u blocks in list)\n", m_raw_inode.i_size, m_raw_inode.i_blocks, block_list.size());
#endif

    // NOTE: Make sure the cached block list is up to date!
    m_block_list = move(block_list);
    return nwritten;
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

    while (entry < buffer.end_pointer()) {
        if (entry->inode != 0) {
#ifdef EXT2_DEBUG
            kprintf("Ext2Inode::traverse_as_directory: %u, name_len: %u, rec_len: %u, file_type: %u, name: %s\n", entry->inode, entry->name_len, entry->rec_len, entry->file_type, String(entry->name, entry->name_len).characters());
#endif
            if (!callback({ entry->name, entry->name_len, { fsid(), entry->inode }, entry->file_type }))
                break;
        }
        entry = (ext2_dir_entry_2*)((char*)entry + entry->rec_len);
    }
    return true;
}

bool Ext2FSInode::add_child(InodeIdentifier child_id, const String& name, byte file_type, int& error)
{
    ASSERT(is_directory());

//#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: Adding inode %u with name '%s' to directory %u\n", child_id.index(), name.characters(), index());
//#endif

    Vector<FS::DirectoryEntry> entries;
    bool name_already_exists = false;
    traverse_as_directory([&] (auto& entry) {
        if (!strcmp(entry.name, name.characters())) {
            name_already_exists = true;
            return false;
        }
        entries.append(entry);
        return true;
    });
    if (name_already_exists) {
        kprintf("Ext2FS: Name '%s' already exists in directory inode %u\n", name.characters(), index());
        error = -EEXIST;
        return false;
    }

    entries.append({ name.characters(), name.length(), child_id, file_type });
    bool success = fs().write_directory_inode(index(), move(entries));
    if (success) {
        LOCKER(m_lock);
        m_lookup_cache.set(name, child_id.index());
    }
    return success;
}

bool Ext2FSInode::remove_child(const String& name, int& error)
{
    ASSERT(is_directory());

    unsigned child_inode_index;
    {
        LOCKER(m_lock);
        auto it = m_lookup_cache.find(name);
        if (it == m_lookup_cache.end()) {
            error = -ENOENT;
            return false;
        }
        child_inode_index = (*it).value;
    }
    InodeIdentifier child_id { fsid(), child_inode_index };

//#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: Removing '%s' in directory %u\n", name.characters(), index());
//#endif

    Vector<FS::DirectoryEntry> entries;
    traverse_as_directory([&] (auto& entry) {
        if (entry.inode != child_id)
        entries.append(entry);
        return true;
    });

    bool success = fs().write_directory_inode(index(), move(entries));
    if (!success) {
        // FIXME: Plumb error from write_directory_inode().
        error = -EIO;
        return false;
    }

    {
        LOCKER(m_lock);
        m_lookup_cache.remove(name);
    }

    auto child_inode = fs().get_inode(child_id);
    child_inode->decrement_link_count();
    return success;
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

    auto directoryData = ByteBuffer::create_uninitialized(occupiedSize);

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

    stream.fill_to_end(0);

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

    return get_inode({ id(), directoryInode })->write_bytes(0, directoryData.size(), directoryData.pointer(), nullptr);
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

bool Ext2FS::write_ext2_inode(unsigned inode, const ext2_inode& e2inode)
{
    unsigned blockIndex;
    unsigned offset;
    auto block = read_block_containing_inode(inode, blockIndex, offset);
    if (!block)
        return false;
    memcpy(reinterpret_cast<ext2_inode*>(block.offset_pointer(offset)), &e2inode, inode_size());
    writeBlock(blockIndex, block);
    return true;
}

Vector<Ext2FS::BlockIndex> Ext2FS::allocate_blocks(unsigned group, unsigned count)
{
    dbgprintf("Ext2FS: allocate_blocks(group: %u, count: %u)\n", group, count);
    if (count == 0)
        return { };

    auto& bgd = group_descriptor(group);
    if (bgd.bg_free_blocks_count < count) {
        kprintf("Ext2FS: allocate_blocks can't allocate out of group %u, wanted %u but only %u available\n", group, count, bgd.bg_free_blocks_count);
        return { };
    }

    // FIXME: Implement a scan that finds consecutive blocks if possible.
    Vector<BlockIndex> blocks;
    traverse_block_bitmap(group, [&blocks, count] (unsigned first_block_in_bitmap, const Bitmap& bitmap) {
        for (unsigned i = 0; i < bitmap.size(); ++i) {
            if (!bitmap.get(i)) {
                blocks.append(first_block_in_bitmap + i);
                if (blocks.size() == count)
                    return false;
            }
        }
        return true;
    });
    dbgprintf("Ext2FS: allocate_block found these blocks:\n");
    for (auto& bi : blocks) {
        dbgprintf("  > %u\n", bi);
    }

    return blocks;
}

unsigned Ext2FS::allocate_inode(unsigned preferredGroup, unsigned expectedSize)
{
    dbgprintf("Ext2FS: allocate_inode(preferredGroup: %u, expectedSize: %u)\n", preferredGroup, expectedSize);

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
        kprintf("Ext2FS: allocate_inode: no suitable group found for new inode with %u blocks needed :(\n", neededBlocks);
        return 0;
    }

    dbgprintf("Ext2FS: allocate_inode: found suitable group [%u] for new inode with %u blocks needed :^)\n", groupIndex, neededBlocks);

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
        kprintf("Ext2FS: first_free_inode_in_group returned no inode, despite bgd claiming there are inodes :(\n");
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

bool Ext2FS::get_inode_allocation_state(InodeIndex index) const
{
    if (index == 0)
        return true;
    auto& bgd = group_descriptor(group_index_from_inode(index));
    unsigned inodes_per_bitmap_block = blockSize() * 8;
    unsigned bitmap_block_index = (index - 1) / inodes_per_bitmap_block;
    unsigned bit_index = (index - 1) % inodes_per_bitmap_block;
    auto block = readBlock(bgd.bg_inode_bitmap + bitmap_block_index);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), block.size());
    return bitmap.get(bit_index);
}

bool Ext2FS::set_inode_allocation_state(unsigned index, bool newState)
{
    auto& bgd = group_descriptor(group_index_from_inode(index));

    // Update inode bitmap
    unsigned inodes_per_bitmap_block = blockSize() * 8;
    unsigned bitmap_block_index = (index - 1) / inodes_per_bitmap_block;
    unsigned bit_index = (index - 1) % inodes_per_bitmap_block;
    auto block = readBlock(bgd.bg_inode_bitmap + bitmap_block_index);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), block.size());
    bool currentState = bitmap.get(bit_index);
    dbgprintf("Ext2FS: set_inode_allocation_state(%u) %u -> %u\n", index, currentState, newState);

    if (currentState == newState)
        return true;

    bitmap.set(bit_index, newState);
    writeBlock(bgd.bg_inode_bitmap + bitmap_block_index, block);

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
    dbgprintf("Ext2FS: set_block_allocation_state(group=%u, block=%u, state=%u)\n", group, bi, newState);
    auto& bgd = group_descriptor(group);

    // Update block bitmap
    unsigned blocksPerBitmapBlock = blockSize() * 8;
    unsigned bitmapBlockIndex = (bi - 1) / blocksPerBitmapBlock;
    unsigned bitIndex = (bi - 1) % blocksPerBitmapBlock;
    auto block = readBlock(bgd.bg_block_bitmap + bitmapBlockIndex);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), blocksPerBitmapBlock);
    bool currentState = bitmap.get(bitIndex);
    dbgprintf("Ext2FS:      block %u state: %u -> %u\n", bi, currentState, newState);

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

RetainPtr<Inode> Ext2FS::create_directory(InodeIdentifier parent_id, const String& name, Unix::mode_t mode, int& error)
{
    ASSERT(parent_id.fsid() == id());

    // Fix up the mode to definitely be a directory.
    // FIXME: This is a bit on the hackish side.
    mode &= ~0170000;
    mode |= 0040000;

    // NOTE: When creating a new directory, make the size 1 block.
    //       There's probably a better strategy here, but this works for now.
    auto inode = create_inode(parent_id, name, mode, blockSize(), error);
    if (!inode)
        return nullptr;

    dbgprintf("Ext2FS: create_directory: created new directory named '%s' with inode %u\n", name.characters(), inode->identifier().index());

    Vector<DirectoryEntry> entries;
    entries.append({ ".", inode->identifier(), EXT2_FT_DIR });
    entries.append({ "..", parent_id, EXT2_FT_DIR });

    bool success = write_directory_inode(inode->identifier().index(), move(entries));
    ASSERT(success);

    auto parent_inode = get_inode(parent_id);
    error = parent_inode->increment_link_count();
    if (error < 0)
        return nullptr;

    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode->identifier().index())));
    ++bgd.bg_used_dirs_count;
    dbgprintf("Ext2FS: incremented bg_used_dirs_count %u -> %u\n", bgd.bg_used_dirs_count - 1, bgd.bg_used_dirs_count);

    unsigned blocksToWrite = ceilDiv(m_blockGroupCount * (unsigned)sizeof(ext2_group_desc), blockSize());
    unsigned firstBlockOfBGDT = blockSize() == 1024 ? 2 : 1;
    writeBlocks(firstBlockOfBGDT, blocksToWrite, m_cached_group_descriptor_table);

    error = 0;
    return inode;
}

RetainPtr<Inode> Ext2FS::create_inode(InodeIdentifier parent_id, const String& name, Unix::mode_t mode, unsigned size, int& error)
{
    ASSERT(parent_id.fsid() == id());
    auto parent_inode = get_inode(parent_id);

    dbgprintf("Ext2FS: Adding inode '%s' (mode %u) to parent directory %u:\n", name.characters(), mode, parent_inode->identifier().index());

    // NOTE: This doesn't commit the inode allocation just yet!
    auto inode_id = allocate_inode(0, size);
    if (!inode_id) {
        kprintf("Ext2FS: create_inode: allocate_inode failed\n");
        error = -ENOSPC;
        return { };
    }

    auto needed_blocks = ceilDiv(size, blockSize());
    auto blocks = allocate_blocks(group_index_from_inode(inode_id), needed_blocks);
    if (blocks.size() != needed_blocks) {
        kprintf("Ext2FS: create_inode: allocate_blocks failed\n");
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
    bool success = parent_inode->add_child({ id(), inode_id }, name, fileType, error);
    if (!success)
        return { };

    // Looks like we're good, time to update the inode bitmap and group+global inode counters.
    success = set_inode_allocation_state(inode_id, true);
    ASSERT(success);

    for (auto bi : blocks) {
        success = set_block_allocation_state(group_index_from_inode(inode_id), bi, true);
        ASSERT(success);
    }

    unsigned initialLinksCount;
    if (isDirectory(mode))
        initialLinksCount = 2; // (parent directory + "." entry in self)
    else
        initialLinksCount = 1;

    auto timestamp = RTC::now();
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

    success = write_block_list_for_inode(inode_id, *e2inode, blocks);
    ASSERT(success);

    dbgprintf("Ext2FS: writing initial metadata for inode %u\n", inode_id);
    e2inode->i_flags = 0;
    success = write_ext2_inode(inode_id, *e2inode);
    ASSERT(success);

    {
        // We might have cached the fact that this inode didn't exist. Wipe the slate.
        LOCKER(m_inode_cache_lock);
        m_inode_cache.remove(inode_id);
    }
    return get_inode({ id(), inode_id });
}

RetainPtr<Inode> Ext2FSInode::parent() const
{
    if (m_parent_id.is_valid())
        return fs().get_inode(m_parent_id);

    unsigned group_index = fs().group_index_from_inode(index());
    unsigned first_inode_in_group = fs().inodes_per_group() * (group_index - 1);

    Vector<RetainPtr<Ext2FSInode>> directories_in_group;

    for (unsigned i = 0; i < fs().inodes_per_group(); ++i) {
        auto group_member = fs().get_inode({ fsid(), first_inode_in_group + i });
        if (!group_member)
            continue;
        if (group_member->is_directory())
            directories_in_group.append(move(group_member));
    }

    for (auto& directory : directories_in_group) {
        if (!directory->reverse_lookup(identifier()).is_null()) {
            m_parent_id = directory->identifier();
            break;
        }
    }

    ASSERT(m_parent_id.is_valid());
    return fs().get_inode(m_parent_id);
}

void Ext2FSInode::populate_lookup_cache()
{
    {
        LOCKER(m_lock);
        if (!m_lookup_cache.is_empty())
            return;
    }
    HashMap<String, unsigned> children;

    traverse_as_directory([&children] (auto& entry) {
        children.set(String(entry.name, entry.name_length), entry.inode.index());
        return true;
    });

    LOCKER(m_lock);
    if (!m_lookup_cache.is_empty())
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

void Ext2FSInode::one_retain_left()
{
    // FIXME: I would like to not live forever, but uncached Ext2FS is fucking painful right now.
}

int Ext2FSInode::set_atime(Unix::time_t t)
{
    if (fs().is_readonly())
        return -EROFS;
    m_raw_inode.i_atime = t;
    set_metadata_dirty(true);
    return 0;
}

int Ext2FSInode::set_ctime(Unix::time_t t)
{
    if (fs().is_readonly())
        return -EROFS;
    m_raw_inode.i_ctime = t;
    set_metadata_dirty(true);
    return 0;
}

int Ext2FSInode::set_mtime(Unix::time_t t)
{
    if (fs().is_readonly())
        return -EROFS;
    m_raw_inode.i_mtime = t;
    set_metadata_dirty(true);
    return 0;
}

int Ext2FSInode::increment_link_count()
{
    if (fs().is_readonly())
        return -EROFS;
    ++m_raw_inode.i_links_count;
    set_metadata_dirty(true);
    return 0;
}

int Ext2FSInode::decrement_link_count()
{
    if (fs().is_readonly())
        return -EROFS;
    ASSERT(m_raw_inode.i_links_count);
    --m_raw_inode.i_links_count;
    if (m_raw_inode.i_links_count == 0)
        fs().uncache_inode(index());
    set_metadata_dirty(true);
    return 0;
}

void Ext2FS::uncache_inode(InodeIndex index)
{
    LOCKER(m_inode_cache_lock);
    m_inode_cache.remove(index);
}
