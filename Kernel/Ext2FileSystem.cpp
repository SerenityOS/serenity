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
    , m_lock("Ext2FS")
{
}

Ext2FS::~Ext2FS()
{
}

ByteBuffer Ext2FS::read_super_block() const
{
    LOCKER(m_lock);
    auto buffer = ByteBuffer::create_uninitialized(1024);
    bool success = device().read_block(2, buffer.pointer());
    ASSERT(success);
    success = device().read_block(3, buffer.offset_pointer(512));
    ASSERT(success);
    return buffer;
}

bool Ext2FS::write_super_block(const ext2_super_block& sb)
{
    LOCKER(m_lock);
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
    ASSERT(groupIndex <= m_block_group_count);

    if (!m_cached_group_descriptor_table) {
        LOCKER(m_lock);
        unsigned blocks_to_read = ceil_div(m_block_group_count * (unsigned)sizeof(ext2_group_desc), block_size());
        unsigned first_block_of_bgdt = block_size() == 1024 ? 2 : 1;
#ifdef EXT2_DEBUG
        kprintf("ext2fs: block group count: %u, blocks-to-read: %u\n", m_block_group_count, blocks_to_read);
        kprintf("ext2fs: first block of BGDT: %u\n", first_block_of_bgdt);
#endif
        m_cached_group_descriptor_table = read_blocks(first_block_of_bgdt, blocks_to_read);
    }
    return reinterpret_cast<ext2_group_desc*>(m_cached_group_descriptor_table.pointer())[groupIndex - 1];
}

bool Ext2FS::initialize()
{
    auto& super_block = this->super_block();
#ifdef EXT2_DEBUG
    kprintf("ext2fs: super block magic: %x (super block size: %u)\n", super_block.s_magic, sizeof(ext2_super_block));
#endif
    if (super_block.s_magic != EXT2_SUPER_MAGIC)
        return false;

#ifdef EXT2_DEBUG
    kprintf("ext2fs: %u inodes, %u blocks\n", super_block.s_inodes_count, super_block.s_blocks_count);
    kprintf("ext2fs: block size = %u\n", EXT2_BLOCK_SIZE(&super_block));
    kprintf("ext2fs: first data block = %u\n", super_block.s_first_data_block);
    kprintf("ext2fs: inodes per block = %u\n", inodes_per_block());
    kprintf("ext2fs: inodes per group = %u\n", inodes_per_group());
    kprintf("ext2fs: free inodes = %u\n", super_block.s_free_inodes_count);
    kprintf("ext2fs: desc per block = %u\n", EXT2_DESC_PER_BLOCK(&super_block));
    kprintf("ext2fs: desc size = %u\n", EXT2_DESC_SIZE(&super_block));
#endif

    set_block_size(EXT2_BLOCK_SIZE(&super_block));

    m_block_group_count = ceil_div(super_block.s_blocks_count, super_block.s_blocks_per_group);

    if (m_block_group_count == 0) {
        kprintf("ext2fs: no block groups :(\n");
        return false;
    }

    // Preheat the BGD cache.
    group_descriptor(0);

#ifdef EXT2_DEBUG
    for (unsigned i = 1; i <= m_block_group_count; ++i) {
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
    return "Ext2FS";
}

InodeIdentifier Ext2FS::root_inode() const
{
    return { fsid(), EXT2_ROOT_INO };
}

ByteBuffer Ext2FS::read_block_containing_inode(unsigned inode, unsigned& block_index, unsigned& offset) const
{
    LOCKER(m_lock);
    auto& super_block = this->super_block();

    if (inode != EXT2_ROOT_INO && inode < EXT2_FIRST_INO(&super_block))
        return { };

    if (inode > super_block.s_inodes_count)
        return { };

    auto& bgd = group_descriptor(group_index_from_inode(inode));

    offset = ((inode - 1) % inodes_per_group()) * inode_size();
    block_index = bgd.bg_inode_table + (offset >> EXT2_BLOCK_SIZE_BITS(&super_block));
    offset &= block_size() - 1;

    return read_block(block_index);
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
    LOCKER(m_lock);

    dbgprintf("Ext2FS: writing %u block(s) to i_block array\n", min((size_t)EXT2_NDIR_BLOCKS, blocks.size()));

    auto old_shape = compute_block_list_shape(e2inode.i_blocks / (2 << super_block().s_log_block_size));
    auto new_shape = compute_block_list_shape(blocks.size());

    Vector<BlockIndex> new_meta_blocks;
    if (new_shape.meta_blocks > old_shape.meta_blocks) {
        new_meta_blocks = allocate_blocks(group_index_from_inode(inode_index), new_shape.meta_blocks - old_shape.meta_blocks);
        for (auto block_index : new_meta_blocks)
            set_block_allocation_state(block_index, true);
    }

    e2inode.i_blocks = (blocks.size() + new_shape.meta_blocks) * (block_size() / 512);

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
        auto block_contents = ByteBuffer::create_uninitialized(block_size());
        BufferStream stream(block_contents);
        ASSERT(new_shape.indirect_blocks <= EXT2_ADDR_PER_BLOCK(&super_block()));
        for (unsigned i = 0; i < new_shape.indirect_blocks; ++i) {
            stream << blocks[output_block_index++];
            --remaining_blocks;
        }
        stream.fill_to_end(0);
        bool success = write_block(e2inode.i_block[EXT2_IND_BLOCK], block_contents);
        ASSERT(success);
    }

    if (!remaining_blocks)
        return true;

    // FIXME: Implement!
    ASSERT_NOT_REACHED();
}

Vector<unsigned> Ext2FS::block_list_for_inode(const ext2_inode& e2inode, bool include_block_list_blocks) const
{
    LOCKER(m_lock);
    unsigned entries_per_block = EXT2_ADDR_PER_BLOCK(&super_block());

    // NOTE: i_blocks is number of 512-byte blocks, not number of fs-blocks.
    unsigned block_count = e2inode.i_blocks / (block_size() / 512);
    unsigned blocksRemaining = block_count;
    Vector<unsigned> list;
    if (include_block_list_blocks) {
        // This seems like an excessive over-estimate but w/e.
        list.ensure_capacity(blocksRemaining * 2);
    } else {
        list.ensure_capacity(blocksRemaining);
    }

    unsigned direct_count = min(block_count, (unsigned)EXT2_NDIR_BLOCKS);
    for (unsigned i = 0; i < direct_count; ++i) {
        list.unchecked_append(e2inode.i_block[i]);
        --blocksRemaining;
    }

    if (!blocksRemaining)
        return list;

    auto process_block_array = [&] (unsigned array_block_index, auto&& callback) {
        if (include_block_list_blocks)
            callback(array_block_index);
        auto array_block = read_block(array_block_index);
        ASSERT(array_block);
        auto* array = reinterpret_cast<const __u32*>(array_block.pointer());
        unsigned count = min(blocksRemaining, entries_per_block);
        for (unsigned i = 0; i < count; ++i) {
            if (!array[i]) {
                blocksRemaining = 0;
                return;
            }
            callback(array[i]);
            --blocksRemaining;
        }
    };

    process_block_array(e2inode.i_block[EXT2_IND_BLOCK], [&] (unsigned entry) {
        list.unchecked_append(entry);
    });

    if (!blocksRemaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_DIND_BLOCK], [&] (unsigned entry) {
        process_block_array(entry, [&] (unsigned entry) {
            list.unchecked_append(entry);
        });
    });

    if (!blocksRemaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_TIND_BLOCK], [&] (unsigned entry) {
        process_block_array(entry, [&] (unsigned entry) {
            process_block_array(entry, [&] (unsigned entry) {
                list.unchecked_append(entry);
            });
        });
    });

    return list;
}

void Ext2FS::free_inode(Ext2FSInode& inode)
{
    LOCKER(m_lock);
    ASSERT(inode.m_raw_inode.i_links_count == 0);
    dbgprintf("Ext2FS: inode %u has no more links, time to delete!\n", inode.index());

    inode.m_raw_inode.i_dtime = RTC::now();
    write_ext2_inode(inode.index(), inode.m_raw_inode);

    auto block_list = block_list_for_inode(inode.m_raw_inode, true);

    for (auto block_index : block_list)
        set_block_allocation_state(block_index, false);

    set_inode_allocation_state(inode.index(), false);

    if (inode.is_directory()) {
        auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode.index())));
        --bgd.bg_used_dirs_count;
        dbgprintf("Ext2FS: decremented bg_used_dirs_count %u -> %u\n", bgd.bg_used_dirs_count - 1, bgd.bg_used_dirs_count);
        flush_block_group_descriptor_table();
    }
}

void Ext2FS::flush_block_group_descriptor_table()
{
    LOCKER(m_lock);
    unsigned blocks_to_write = ceil_div(m_block_group_count * (unsigned)sizeof(ext2_group_desc), block_size());
    unsigned first_block_of_bgdt = block_size() == 1024 ? 2 : 1;
    write_blocks(first_block_of_bgdt, blocks_to_write, m_cached_group_descriptor_table);
}

Ext2FSInode::Ext2FSInode(Ext2FS& fs, unsigned index)
    : Inode(fs, index)
{
}

Ext2FSInode::~Ext2FSInode()
{
    if (m_raw_inode.i_links_count == 0)
        fs().free_inode(*this);
}

InodeMetadata Ext2FSInode::metadata() const
{
    // FIXME: This should probably take the inode lock, no?
    InodeMetadata metadata;
    metadata.inode = identifier();
    metadata.size = m_raw_inode.i_size;
    metadata.mode = m_raw_inode.i_mode;
    metadata.uid = m_raw_inode.i_uid;
    metadata.gid = m_raw_inode.i_gid;
    metadata.link_count = m_raw_inode.i_links_count;
    metadata.atime = m_raw_inode.i_atime;
    metadata.ctime = m_raw_inode.i_ctime;
    metadata.mtime = m_raw_inode.i_mtime;
    metadata.dtime = m_raw_inode.i_dtime;
    metadata.block_size = fs().block_size();
    metadata.block_count = m_raw_inode.i_blocks;

    if (::is_character_device(m_raw_inode.i_mode)) {
        unsigned dev = m_raw_inode.i_block[0];
        metadata.major_device = (dev & 0xfff00) >> 8;
        metadata.minor_device = (dev & 0xff) | ((dev >> 12) & 0xfff00);
    }
    if (::is_block_device(m_raw_inode.i_mode)) {
        unsigned dev = m_raw_inode.i_block[1];
        metadata.major_device = (dev & 0xfff00) >> 8;
        metadata.minor_device = (dev & 0xff) | ((dev >> 12) & 0xfff00);
    }
    return metadata;
}

void Ext2FSInode::flush_metadata()
{
    LOCKER(m_lock);
    dbgprintf("Ext2FSInode: flush_metadata for inode %u\n", index());
    fs().write_ext2_inode(index(), m_raw_inode);
    if (is_directory()) {
        // Unless we're about to go away permanently, invalidate the lookup cache.
        if (m_raw_inode.i_links_count != 0) {
            // FIXME: This invalidation is way too hardcore. It's sad to throw away the whole cache.
            m_lookup_cache.clear();
        }
    }
    set_metadata_dirty(false);
}

RetainPtr<Inode> Ext2FS::get_inode(InodeIdentifier inode) const
{
    LOCKER(m_lock);
    ASSERT(inode.fsid() == fsid());

    {
        auto it = m_inode_cache.find(inode.index());
        if (it != m_inode_cache.end())
            return (*it).value;
    }

    if (!get_inode_allocation_state(inode.index())) {
        m_inode_cache.set(inode.index(), nullptr);
        return nullptr;
    }

    unsigned block_index;
    unsigned offset;
    auto block = read_block_containing_inode(inode.index(), block_index, offset);
    if (!block)
        return { };

    auto it = m_inode_cache.find(inode.index());
    if (it != m_inode_cache.end())
        return (*it).value;
    auto new_inode = adopt(*new Ext2FSInode(const_cast<Ext2FS&>(*this), inode.index()));
    memcpy(&new_inode->m_raw_inode, reinterpret_cast<ext2_inode*>(block.offset_pointer(offset)), inode_size());
    m_inode_cache.set(inode.index(), new_inode.copy_ref());
    return new_inode;
}

ssize_t Ext2FSInode::read_bytes(off_t offset, size_t count, byte* buffer, FileDescriptor*) const
{
    Locker inode_locker(m_lock);
    ASSERT(offset >= 0);
    if (m_raw_inode.i_size == 0)
        return 0;

    // Symbolic links shorter than 60 characters are store inline inside the i_block array.
    // This avoids wasting an entire block on short links. (Most links are short.)
    static const unsigned max_inline_symlink_length = 60;
    if (is_symlink() && size() < max_inline_symlink_length) {
        ssize_t nread = min((off_t)size() - offset, static_cast<off_t>(count));
        memcpy(buffer, m_raw_inode.i_block + offset, nread);
        return nread;
    }

    Locker fs_locker(fs().m_lock);

    if (m_block_list.is_empty()) {
        auto block_list = fs().block_list_for_inode(m_raw_inode);
        if (m_block_list.size() != block_list.size())
            m_block_list = move(block_list);
    }

    if (m_block_list.is_empty()) {
        kprintf("ext2fs: read_bytes: empty block list for inode %u\n", index());
        return -EIO;
    }

    const size_t block_size = fs().block_size();

    dword first_block_logical_index = offset / block_size;
    dword last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    dword offset_into_first_block = offset % block_size;

    ssize_t nread = 0;
    size_t remaining_count = min((off_t)count, (off_t)size() - offset);
    byte* out = buffer;

#ifdef EXT2_DEBUG
    kprintf("Ext2FS: Reading up to %u bytes %d bytes into inode %u:%u to %p\n", count, offset, identifier().fsid(), identifier().index(), buffer);
    //kprintf("ok let's do it, read(%u, %u) -> blocks %u thru %u, oifb: %u\n", offset, count, first_block_logical_index, last_block_logical_index, offset_into_first_block);
#endif

    for (dword bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; ++bi) {
        auto block = fs().read_block(m_block_list[bi]);
        if (!block) {
            kprintf("ext2fs: read_bytes: read_block(%u) failed (lbi: %u)\n", m_block_list[bi], bi);
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

ssize_t Ext2FSInode::write_bytes(off_t offset, size_t count, const byte* data, FileDescriptor*)
{
    Locker inode_locker(m_lock);
    Locker fs_locker(fs().m_lock);

    // FIXME: Support writing to symlink inodes.
    ASSERT(!is_symlink());

    ASSERT(offset >= 0);

    const size_t block_size = fs().block_size();
    size_t old_size = size();
    size_t new_size = max(static_cast<size_t>(offset) + count, size());

    unsigned blocks_needed_before = ceil_div(size(), block_size);
    unsigned blocks_needed_after = ceil_div(new_size, block_size);

    auto block_list = fs().block_list_for_inode(m_raw_inode);
    if (blocks_needed_after > blocks_needed_before) {
        auto new_blocks = fs().allocate_blocks(fs().group_index_from_inode(index()), blocks_needed_after - blocks_needed_before);
        for (auto new_block_index : new_blocks)
            fs().set_block_allocation_state(new_block_index, true);
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
    size_t remaining_count = min((off_t)count, (off_t)new_size - offset);
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
            block = fs().read_block(block_list[bi]);
            if (!block) {
                kprintf("Ext2FSInode::write_bytes: read_block(%u) failed (lbi: %u)\n", block_list[bi], bi);
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
        bool success = fs().write_block(block_list[bi], block);
        if (!success) {
            kprintf("Ext2FSInode::write_bytes: write_block(%u) failed (lbi: %u)\n", block_list[bi], bi);
            ASSERT_NOT_REACHED();
            return -EIO;
        }
        remaining_count -= num_bytes_to_copy;
        nwritten += num_bytes_to_copy;
        in += num_bytes_to_copy;
    }

    bool success = fs().write_block_list_for_inode(index(), m_raw_inode, block_list);
    ASSERT(success);

    m_raw_inode.i_size = new_size;
    fs().write_ext2_inode(index(), m_raw_inode);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FSInode::write_bytes: after write, i_size=%u, i_blocks=%u (%u blocks in list)\n", m_raw_inode.i_size, m_raw_inode.i_blocks, block_list.size());
#endif

    // NOTE: Make sure the cached block list is up to date!
    m_block_list = move(block_list);

    if (old_size != new_size)
        inode_size_changed(old_size, new_size);
    inode_contents_changed(offset, count, data);
    return nwritten;
}

bool Ext2FSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntry&)> callback) const
{
    LOCKER(m_lock);
    ASSERT(metadata().is_directory());

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
    LOCKER(m_lock);
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

    auto child_inode = fs().get_inode(child_id);
    if (child_inode)
        child_inode->increment_link_count();

    entries.append({ name.characters(), name.length(), child_id, file_type });
    bool success = fs().write_directory_inode(index(), move(entries));
    if (success)
        m_lookup_cache.set(name, child_id.index());
    return success;
}

bool Ext2FSInode::remove_child(const String& name, int& error)
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FSInode::remove_child(%s) in inode %u\n", name.characters(), index());
#endif
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
        if (strcmp(entry.name, name.characters()) != 0)
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
    LOCKER(m_lock);
    dbgprintf("Ext2FS: New directory inode %u contents to write:\n", directoryInode);

    unsigned directory_size = 0;
    for (auto& entry : entries) {
        //kprintf("  - %08u %s\n", entry.inode.index(), entry.name);
        directory_size += EXT2_DIR_REC_LEN(entry.name_length);
    }

    unsigned blocks_needed = ceil_div(directory_size, block_size());
    unsigned occupied_size = blocks_needed * block_size();

    dbgprintf("Ext2FS: directory size: %u (occupied: %u)\n", directory_size, occupied_size);

    auto directory_data = ByteBuffer::create_uninitialized(occupied_size);

    BufferStream stream(directory_data);
    for (unsigned i = 0; i < entries.size(); ++i) {
        auto& entry = entries[i];

        unsigned record_length = EXT2_DIR_REC_LEN(entry.name_length);
        if (i == entries.size() - 1)
            record_length += occupied_size - directory_size;

        dbgprintf("* inode: %u", entry.inode.index());
        dbgprintf(", name_len: %u", word(entry.name_length));
        dbgprintf(", rec_len: %u", word(record_length));
        dbgprintf(", file_type: %u", byte(entry.file_type));
        dbgprintf(", name: %s\n", entry.name);

        stream << dword(entry.inode.index());
        stream << word(record_length);
        stream << byte(entry.name_length);
        stream << byte(entry.file_type);
        stream << entry.name;

        unsigned padding = record_length - entry.name_length - 8;
        //dbgprintf("  *** pad %u bytes\n", padding);
        for (unsigned j = 0; j < padding; ++j) {
            stream << byte(0);
        }
    }

    stream.fill_to_end(0);

#if 0
    kprintf("data to write (%u):\n", directory_data.size());
    for (unsigned i = 0; i < directory_data.size(); ++i) {
        kprintf("%02x ", directory_data[i]);
        if ((i + 1) % 8 == 0)
            kprintf(" ");
        if ((i + 1) % 16 == 0)
            kprintf("\n");
    }
    kprintf("\n");
#endif

    auto directory_inode = get_inode({ fsid(), directoryInode });
    ssize_t nwritten = directory_inode->write_bytes(0, directory_data.size(), directory_data.pointer(), nullptr);
    return nwritten == directory_data.size();
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
    LOCKER(m_lock);
    ASSERT(groupIndex <= m_block_group_count);
    auto& bgd = group_descriptor(groupIndex);

    unsigned blocks_in_group = min(blocks_per_group(), super_block().s_blocks_count);
    unsigned block_count = ceil_div(blocks_in_group, 8u);

    auto bitmap_blocks = read_blocks(bgd.bg_block_bitmap, block_count);
    ASSERT(bitmap_blocks);

    kprintf("ext2fs: group[%u] block bitmap (bitmap occupies %u blocks):\n", groupIndex, block_count);

    auto bitmap = Bitmap::wrap(bitmap_blocks.pointer(), blocks_in_group);
    for (unsigned i = 0; i < blocks_in_group; ++i) {
        kprintf("%c", bitmap.get(i) ? '1' : '0');
    }
    kprintf("\n");
}

void Ext2FS::dump_inode_bitmap(unsigned groupIndex) const
{
    LOCKER(m_lock);
    traverse_inode_bitmap(groupIndex, [] (unsigned, const Bitmap& bitmap) {
        for (unsigned i = 0; i < bitmap.size(); ++i)
            kprintf("%c", bitmap.get(i) ? '1' : '0');
        return true;
    });
}

template<typename F>
void Ext2FS::traverse_inode_bitmap(unsigned group_index, F callback) const
{
    ASSERT(group_index <= m_block_group_count);
    auto& bgd = group_descriptor(group_index);

    unsigned inodes_in_group = min(inodes_per_group(), super_block().s_inodes_count);
    unsigned block_count = ceil_div(inodes_in_group, 8u);
    unsigned first_inode_in_group = (group_index - 1) * inodes_per_group();
    unsigned bits_per_block = block_size() * 8;

    for (unsigned i = 0; i < block_count; ++i) {
        auto block = read_block(bgd.bg_inode_bitmap + i);
        ASSERT(block);
        bool should_continue = callback(first_inode_in_group + i * (i * bits_per_block) + 1, Bitmap::wrap(block.pointer(), inodes_in_group));
        if (!should_continue)
            break;
    }
}

template<typename F>
void Ext2FS::traverse_block_bitmap(unsigned group_index, F callback) const
{
    ASSERT(group_index <= m_block_group_count);
    auto& bgd = group_descriptor(group_index);

    unsigned blocks_in_group = min(blocks_per_group(), super_block().s_blocks_count);
    unsigned block_count = ceil_div(blocks_in_group, 8u);
    unsigned first_block_in_group = (group_index - 1) * blocks_per_group();
    unsigned bits_per_block = block_size() * 8;

    for (unsigned i = 0; i < block_count; ++i) {
        auto block = read_block(bgd.bg_block_bitmap + i);
        ASSERT(block);
        bool should_continue = callback(first_block_in_group + (i * bits_per_block) + 1, Bitmap::wrap(block.pointer(), blocks_in_group));
        if (!should_continue)
            break;
    }
}

bool Ext2FS::write_ext2_inode(unsigned inode, const ext2_inode& e2inode)
{
    LOCKER(m_lock);
    unsigned block_index;
    unsigned offset;
    auto block = read_block_containing_inode(inode, block_index, offset);
    if (!block)
        return false;
    memcpy(reinterpret_cast<ext2_inode*>(block.offset_pointer(offset)), &e2inode, inode_size());
    bool success = write_block(block_index, block);
    ASSERT(success);
    return success;
}

Vector<Ext2FS::BlockIndex> Ext2FS::allocate_blocks(unsigned group, unsigned count)
{
    LOCKER(m_lock);
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

unsigned Ext2FS::allocate_inode(unsigned preferred_group, unsigned expected_size)
{
    LOCKER(m_lock);
    dbgprintf("Ext2FS: allocate_inode(preferredGroup: %u, expectedSize: %u)\n", preferred_group, expected_size);

    unsigned needed_blocks = ceil_div(expected_size, block_size());

    dbgprintf("Ext2FS: minimum needed blocks: %u\n", needed_blocks);

    unsigned groupIndex = 0;

    auto is_suitable_group = [this, needed_blocks] (unsigned groupIndex) {
        auto& bgd = group_descriptor(groupIndex);
        return bgd.bg_free_inodes_count && bgd.bg_free_blocks_count >= needed_blocks;
    };

    if (preferred_group && is_suitable_group(preferred_group)) {
        groupIndex = preferred_group;
    } else {
        for (unsigned i = 1; i <= m_block_group_count; ++i) {
            if (is_suitable_group(i))
                groupIndex = i;
        }
    }

    if (!groupIndex) {
        kprintf("Ext2FS: allocate_inode: no suitable group found for new inode with %u blocks needed :(\n", needed_blocks);
        return 0;
    }

    dbgprintf("Ext2FS: allocate_inode: found suitable group [%u] for new inode with %u blocks needed :^)\n", groupIndex, needed_blocks);

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

    ASSERT(get_inode_allocation_state(inode) == false);

    // FIXME: allocate blocks if needed!

    return inode;
}

Ext2FS::GroupIndex Ext2FS::group_index_from_block_index(BlockIndex block_index) const
{
    if (!block_index)
        return 0;
    return (block_index - 1) / blocks_per_group() + 1;
}

unsigned Ext2FS::group_index_from_inode(unsigned inode) const
{
    if (!inode)
        return 0;
    return (inode - 1) / inodes_per_group() + 1;
}

bool Ext2FS::get_inode_allocation_state(InodeIndex index) const
{
    LOCKER(m_lock);
    if (index == 0)
        return true;
    unsigned group_index = group_index_from_inode(index);
    auto& bgd = group_descriptor(group_index);
    unsigned index_in_group = index - ((group_index - 1) * inodes_per_group());
    unsigned inodes_per_bitmap_block = block_size() * 8;
    unsigned bitmap_block_index = (index_in_group - 1) / inodes_per_bitmap_block;
    unsigned bit_index = (index_in_group - 1) % inodes_per_bitmap_block;
    auto block = read_block(bgd.bg_inode_bitmap + bitmap_block_index);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), inodes_per_bitmap_block);
    return bitmap.get(bit_index);
}

bool Ext2FS::set_inode_allocation_state(unsigned index, bool newState)
{
    LOCKER(m_lock);
    unsigned group_index = group_index_from_inode(index);
    auto& bgd = group_descriptor(group_index);
    unsigned index_in_group = index - ((group_index - 1) * inodes_per_group());
    unsigned inodes_per_bitmap_block = block_size() * 8;
    unsigned bitmap_block_index = (index_in_group - 1) / inodes_per_bitmap_block;
    unsigned bit_index = (index_in_group - 1) % inodes_per_bitmap_block;
    auto block = read_block(bgd.bg_inode_bitmap + bitmap_block_index);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), inodes_per_bitmap_block);
    bool current_state = bitmap.get(bit_index);
    dbgprintf("Ext2FS: set_inode_allocation_state(%u) %u -> %u\n", index, current_state, newState);

    if (current_state == newState)
        return true;

    bitmap.set(bit_index, newState);
    bool success = write_block(bgd.bg_inode_bitmap + bitmap_block_index, block);
    ASSERT(success);

    // Update superblock
    auto& sb = *reinterpret_cast<ext2_super_block*>(m_cached_super_block.pointer());
    dbgprintf("Ext2FS: superblock free inode count %u -> %u\n", sb.s_free_inodes_count, sb.s_free_inodes_count - 1);
    if (newState)
        --sb.s_free_inodes_count;
    else
        ++sb.s_free_inodes_count;
    write_super_block(sb);

    // Update BGD
    auto& mutable_bgd = const_cast<ext2_group_desc&>(bgd);
    if (newState)
        --mutable_bgd.bg_free_inodes_count;
    else
        ++mutable_bgd.bg_free_inodes_count;
    dbgprintf("Ext2FS: group free inode count %u -> %u\n", bgd.bg_free_inodes_count, bgd.bg_free_inodes_count - 1);

    flush_block_group_descriptor_table();
    return true;
}

bool Ext2FS::set_block_allocation_state(BlockIndex block_index, bool new_state)
{
    LOCKER(m_lock);
    dbgprintf("Ext2FS: set_block_allocation_state(block=%u, state=%u)\n", block_index, new_state);
    unsigned group_index = group_index_from_block_index(block_index);
    auto& bgd = group_descriptor(group_index);
    BlockIndex index_in_group = block_index - ((group_index - 1) * blocks_per_group());
    unsigned blocks_per_bitmap_block = block_size() * 8;
    unsigned bitmap_block_index = (index_in_group - 1) / blocks_per_bitmap_block;
    unsigned bit_index = (index_in_group - 1) % blocks_per_bitmap_block;
    dbgprintf("  index_in_group: %u\n", index_in_group);
    dbgprintf("  blocks_per_bitmap_block: %u\n", blocks_per_bitmap_block);
    dbgprintf("  bitmap_block_index: %u\n", bitmap_block_index);
    dbgprintf("  bit_index: %u\n", bit_index);
    dbgprintf("  read_block(%u + %u = %u)\n", bgd.bg_block_bitmap, bitmap_block_index, bgd.bg_block_bitmap + bitmap_block_index);
    auto block = read_block(bgd.bg_block_bitmap + bitmap_block_index);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), blocks_per_bitmap_block);
    bool current_state = bitmap.get(bit_index);
    dbgprintf("Ext2FS:      block %u state: %u -> %u\n", block_index, current_state, new_state);

    if (current_state == new_state)
        return true;

    bitmap.set(bit_index, new_state);
    bool success = write_block(bgd.bg_block_bitmap + bitmap_block_index, block);
    ASSERT(success);

    // Update superblock
    auto& sb = *reinterpret_cast<ext2_super_block*>(m_cached_super_block.pointer());
    dbgprintf("Ext2FS: superblock free block count %u -> %u\n", sb.s_free_blocks_count, sb.s_free_blocks_count - 1);
    if (new_state)
        --sb.s_free_blocks_count;
    else
        ++sb.s_free_blocks_count;
    write_super_block(sb);

    // Update BGD
    auto& mutable_bgd = const_cast<ext2_group_desc&>(bgd);
    if (new_state)
        --mutable_bgd.bg_free_blocks_count;
    else
        ++mutable_bgd.bg_free_blocks_count;
    dbgprintf("Ext2FS: group free block count %u -> %u\n", bgd.bg_free_blocks_count, bgd.bg_free_blocks_count - 1);

    flush_block_group_descriptor_table();
    return true;
}

RetainPtr<Inode> Ext2FS::create_directory(InodeIdentifier parent_id, const String& name, mode_t mode, int& error)
{
    LOCKER(m_lock);
    ASSERT(parent_id.fsid() == fsid());

    // Fix up the mode to definitely be a directory.
    // FIXME: This is a bit on the hackish side.
    mode &= ~0170000;
    mode |= 0040000;

    // NOTE: When creating a new directory, make the size 1 block.
    //       There's probably a better strategy here, but this works for now.
    auto inode = create_inode(parent_id, name, mode, block_size(), error);
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

    flush_block_group_descriptor_table();

    error = 0;
    return inode;
}

RetainPtr<Inode> Ext2FS::create_inode(InodeIdentifier parent_id, const String& name, mode_t mode, unsigned size, int& error)
{
    LOCKER(m_lock);
    ASSERT(parent_id.fsid() == fsid());
    auto parent_inode = get_inode(parent_id);

    dbgprintf("Ext2FS: Adding inode '%s' (mode %u) to parent directory %u:\n", name.characters(), mode, parent_inode->identifier().index());

    // NOTE: This doesn't commit the inode allocation just yet!
    auto inode_id = allocate_inode(0, size);
    if (!inode_id) {
        kprintf("Ext2FS: create_inode: allocate_inode failed\n");
        error = -ENOSPC;
        return { };
    }

    auto needed_blocks = ceil_div(size, block_size());
    auto blocks = allocate_blocks(group_index_from_inode(inode_id), needed_blocks);
    if (blocks.size() != needed_blocks) {
        kprintf("Ext2FS: create_inode: allocate_blocks failed\n");
        error = -ENOSPC;
        return { };
    }

    byte file_type = 0;
    if (is_regular_file(mode))
        file_type = EXT2_FT_REG_FILE;
    else if (is_directory(mode))
        file_type = EXT2_FT_DIR;
    else if (is_character_device(mode))
        file_type = EXT2_FT_CHRDEV;
    else if (is_block_device(mode))
        file_type = EXT2_FT_BLKDEV;
    else if (is_fifo(mode))
        file_type = EXT2_FT_FIFO;
    else if (is_socket(mode))
        file_type = EXT2_FT_SOCK;
    else if (is_symlink(mode))
        file_type = EXT2_FT_SYMLINK;

    // Try adding it to the directory first, in case the name is already in use.
    bool success = parent_inode->add_child({ fsid(), inode_id }, name, file_type, error);
    if (!success)
        return { };

    // Looks like we're good, time to update the inode bitmap and group+global inode counters.
    success = set_inode_allocation_state(inode_id, true);
    ASSERT(success);

    for (auto block_index : blocks) {
        success = set_block_allocation_state(block_index, true);
        ASSERT(success);
    }

    unsigned initial_links_count;
    if (is_directory(mode))
        initial_links_count = 2; // (parent directory + "." entry in self)
    else
        initial_links_count = 1;

    auto timestamp = RTC::now();
    ext2_inode e2inode;
    memset(&e2inode, 0, sizeof(ext2_inode));
    e2inode.i_mode = mode;
    e2inode.i_uid = 0;
    e2inode.i_size = size;
    e2inode.i_atime = timestamp;
    e2inode.i_ctime = timestamp;
    e2inode.i_mtime = timestamp;
    e2inode.i_dtime = 0;
    e2inode.i_gid = 0;
    e2inode.i_links_count = initial_links_count;

    success = write_block_list_for_inode(inode_id, e2inode, blocks);
    ASSERT(success);

    dbgprintf("Ext2FS: writing initial metadata for inode %u\n", inode_id);
    e2inode.i_flags = 0;
    success = write_ext2_inode(inode_id, e2inode);
    ASSERT(success);

    // We might have cached the fact that this inode didn't exist. Wipe the slate.
    m_inode_cache.remove(inode_id);

    return get_inode({ fsid(), inode_id });
}

RetainPtr<Inode> Ext2FSInode::parent() const
{
    LOCKER(m_lock);
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

void Ext2FSInode::populate_lookup_cache() const
{
    LOCKER(m_lock);
    if (!m_lookup_cache.is_empty())
        return;
    HashMap<String, unsigned> children;

    traverse_as_directory([&children] (auto& entry) {
        children.set(String(entry.name, entry.name_length), entry.inode.index());
        return true;
    });

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

int Ext2FSInode::set_atime(time_t t)
{
    LOCKER(m_lock);
    if (fs().is_readonly())
        return -EROFS;
    m_raw_inode.i_atime = t;
    set_metadata_dirty(true);
    return 0;
}

int Ext2FSInode::set_ctime(time_t t)
{
    LOCKER(m_lock);
    if (fs().is_readonly())
        return -EROFS;
    m_raw_inode.i_ctime = t;
    set_metadata_dirty(true);
    return 0;
}

int Ext2FSInode::set_mtime(time_t t)
{
    LOCKER(m_lock);
    if (fs().is_readonly())
        return -EROFS;
    m_raw_inode.i_mtime = t;
    set_metadata_dirty(true);
    return 0;
}

int Ext2FSInode::increment_link_count()
{
    LOCKER(m_lock);
    if (fs().is_readonly())
        return -EROFS;
    ++m_raw_inode.i_links_count;
    set_metadata_dirty(true);
    return 0;
}

int Ext2FSInode::decrement_link_count()
{
    LOCKER(m_lock);
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
    LOCKER(m_lock);
    m_inode_cache.remove(index);
}

size_t Ext2FSInode::directory_entry_count() const
{
    ASSERT(is_directory());
    populate_lookup_cache();
    LOCKER(m_lock);
    return m_lookup_cache.size();
}

bool Ext2FSInode::chmod(mode_t mode, int& error)
{
    LOCKER(m_lock);
    error = 0;
    if (m_raw_inode.i_mode == mode)
        return true;
    m_raw_inode.i_mode = mode;
    set_metadata_dirty(true);
    return true;
}

unsigned Ext2FS::total_block_count() const
{
    LOCKER(m_lock);
    return super_block().s_blocks_count;
}

unsigned Ext2FS::free_block_count() const
{
    LOCKER(m_lock);
    return super_block().s_free_blocks_count;
}

unsigned Ext2FS::total_inode_count() const
{
    LOCKER(m_lock);
    return super_block().s_inodes_count;
}

unsigned Ext2FS::free_inode_count() const
{
    LOCKER(m_lock);
    return super_block().s_free_inodes_count;
}
