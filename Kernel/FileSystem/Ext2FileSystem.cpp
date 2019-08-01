#include <AK/Bitmap.h>
#include <AK/BufferStream.h>
#include <AK/StdLibExtras.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/ext2_fs.h>
#include <Kernel/Process.h>
#include <Kernel/RTC.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>

//#define EXT2_DEBUG

static const ssize_t max_inline_symlink_length = 60;

static u8 to_ext2_file_type(mode_t mode)
{
    if (is_regular_file(mode))
        return EXT2_FT_REG_FILE;
    if (is_directory(mode))
        return EXT2_FT_DIR;
    if (is_character_device(mode))
        return EXT2_FT_CHRDEV;
    if (is_block_device(mode))
        return EXT2_FT_BLKDEV;
    if (is_fifo(mode))
        return EXT2_FT_FIFO;
    if (is_socket(mode))
        return EXT2_FT_SOCK;
    if (is_symlink(mode))
        return EXT2_FT_SYMLINK;
    return EXT2_FT_UNKNOWN;
}

NonnullRefPtr<Ext2FS> Ext2FS::create(NonnullRefPtr<DiskDevice> device)
{
    return adopt(*new Ext2FS(move(device)));
}

Ext2FS::Ext2FS(NonnullRefPtr<DiskDevice>&& device)
    : DiskBackedFS(move(device))
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
    const u8* raw = (const u8*)&sb;
    bool success;
    success = device().write_block(2, raw);
    ASSERT(success);
    success = device().write_block(3, raw + 512);
    ASSERT(success);
    // FIXME: This is an ugly way to refresh the superblock cache. :-|
    super_block();
    return true;
}

unsigned Ext2FS::first_block_of_group(GroupIndex group_index) const
{
    return super_block().s_first_data_block + (group_index * super_block().s_blocks_per_group);
}

const ext2_super_block& Ext2FS::super_block() const
{
    if (!m_cached_super_block)
        m_cached_super_block = read_super_block();
    return *reinterpret_cast<ext2_super_block*>(m_cached_super_block.pointer());
}

const ext2_group_desc& Ext2FS::group_descriptor(GroupIndex group_index) const
{
    // FIXME: Should this fail gracefully somehow?
    ASSERT(group_index <= m_block_group_count);

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
    return reinterpret_cast<ext2_group_desc*>(m_cached_group_descriptor_table.pointer())[group_index - 1];
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
        return {};

    if (inode > super_block.s_inodes_count)
        return {};

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

    // NOTE: There is a mismatch between i_blocks and blocks.size() since i_blocks includes meta blocks and blocks.size() does not.
    auto old_block_count = ceil_div(e2inode.i_size, block_size());

    auto old_shape = compute_block_list_shape(old_block_count);
    auto new_shape = compute_block_list_shape(blocks.size());

    Vector<BlockIndex> new_meta_blocks;
    if (new_shape.meta_blocks > old_shape.meta_blocks) {
        new_meta_blocks = allocate_blocks(group_index_from_inode(inode_index), new_shape.meta_blocks - old_shape.meta_blocks);
        for (auto block_index : new_meta_blocks)
            set_block_allocation_state(block_index, true);
    }

    e2inode.i_blocks = (blocks.size() + new_shape.meta_blocks) * (block_size() / 512);

    bool inode_dirty = false;

    unsigned output_block_index = 0;
    unsigned remaining_blocks = blocks.size();
    for (unsigned i = 0; i < new_shape.direct_blocks; ++i) {
        if (e2inode.i_block[i] != blocks[output_block_index])
            inode_dirty = true;
        e2inode.i_block[i] = blocks[output_block_index];
        ++output_block_index;
        --remaining_blocks;
    }
    if (inode_dirty) {
        dbgprintf("Ext2FS: Writing %u direct block(s) to i_block array of inode %u\n", min(EXT2_NDIR_BLOCKS, blocks.size()), inode_index);
#ifdef EXT2_DEBUG
        for (int i = 0; i < min(EXT2_NDIR_BLOCKS, blocks.size()); ++i)
            dbgprintf("   + %u\n", blocks[i]);
#endif
        write_ext2_inode(inode_index, e2inode);
        inode_dirty = false;
    }

    if (!remaining_blocks)
        return true;

    if (!e2inode.i_block[EXT2_IND_BLOCK]) {
        BlockIndex new_indirect_block = new_meta_blocks.take_last();
        if (e2inode.i_block[EXT2_IND_BLOCK] != new_indirect_block)
            inode_dirty = true;
        e2inode.i_block[EXT2_IND_BLOCK] = new_indirect_block;
        if (inode_dirty) {
            dbgprintf("Ext2FS: Adding the indirect block to i_block array of inode %u\n", inode_index);
            write_ext2_inode(inode_index, e2inode);
            inode_dirty = false;
        }
    }

    if (old_shape.indirect_blocks == new_shape.indirect_blocks) {
        // No need to update the singly indirect block array.
        remaining_blocks -= new_shape.indirect_blocks;
    } else {
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

Vector<Ext2FS::BlockIndex> Ext2FS::block_list_for_inode(const ext2_inode& e2inode, bool include_block_list_blocks) const
{
    LOCKER(m_lock);
    unsigned entries_per_block = EXT2_ADDR_PER_BLOCK(&super_block());

    // NOTE: i_blocks is number of 512-byte blocks, not number of fs-blocks.
    unsigned block_count = e2inode.i_blocks / (block_size() / 512);

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS::block_list_for_inode(): i_size=%u, i_blocks=%u, block_count=%u\n", e2inode.i_size, block_count);
#endif

    unsigned blocks_remaining = block_count;
    Vector<BlockIndex> list;
    if (include_block_list_blocks) {
        // This seems like an excessive over-estimate but w/e.
        list.ensure_capacity(blocks_remaining * 2);
    } else {
        list.ensure_capacity(blocks_remaining);
    }

    unsigned direct_count = min(block_count, (unsigned)EXT2_NDIR_BLOCKS);
    for (unsigned i = 0; i < direct_count; ++i) {
        auto block_index = e2inode.i_block[i];
        if (!block_index)
            return list;
        list.unchecked_append(block_index);
        --blocks_remaining;
    }

    if (!blocks_remaining)
        return list;

    auto process_block_array = [&](unsigned array_block_index, auto&& callback) {
        if (include_block_list_blocks)
            callback(array_block_index);
        auto array_block = read_block(array_block_index);
        ASSERT(array_block);
        auto* array = reinterpret_cast<const __u32*>(array_block.pointer());
        unsigned count = min(blocks_remaining, entries_per_block);
        for (unsigned i = 0; i < count; ++i) {
            if (!array[i]) {
                blocks_remaining = 0;
                return;
            }
            callback(array[i]);
            --blocks_remaining;
        }
    };

    process_block_array(e2inode.i_block[EXT2_IND_BLOCK], [&](unsigned entry) {
        list.unchecked_append(entry);
    });

    if (!blocks_remaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_DIND_BLOCK], [&](unsigned entry) {
        process_block_array(entry, [&](unsigned entry) {
            list.unchecked_append(entry);
        });
    });

    if (!blocks_remaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_TIND_BLOCK], [&](unsigned entry) {
        process_block_array(entry, [&](unsigned entry) {
            process_block_array(entry, [&](unsigned entry) {
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
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: inode %u has no more links, time to delete!\n", inode.index());
#endif

    struct timeval now;
    kgettimeofday(now);
    inode.m_raw_inode.i_dtime = now.tv_sec;
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

    if (::is_character_device(m_raw_inode.i_mode) || ::is_block_device(m_raw_inode.i_mode)) {
        unsigned dev = m_raw_inode.i_block[0];
        if (!dev)
            dev = m_raw_inode.i_block[1];
        metadata.major_device = (dev & 0xfff00) >> 8;
        metadata.minor_device = (dev & 0xff) | ((dev >> 12) & 0xfff00);
    }
    return metadata;
}

void Ext2FSInode::flush_metadata()
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FSInode: flush_metadata for inode %u\n", index());
#endif
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

RefPtr<Inode> Ext2FS::get_inode(InodeIdentifier inode) const
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
        return {};

    auto it = m_inode_cache.find(inode.index());
    if (it != m_inode_cache.end())
        return (*it).value;
    auto new_inode = adopt(*new Ext2FSInode(const_cast<Ext2FS&>(*this), inode.index()));
    memcpy(&new_inode->m_raw_inode, reinterpret_cast<ext2_inode*>(block.offset_pointer(offset)), sizeof(ext2_inode));
    m_inode_cache.set(inode.index(), new_inode);
    return new_inode;
}

ssize_t Ext2FSInode::read_bytes(off_t offset, ssize_t count, u8* buffer, FileDescription*) const
{
    Locker inode_locker(m_lock);
    ASSERT(offset >= 0);
    if (m_raw_inode.i_size == 0)
        return 0;

    // Symbolic links shorter than 60 characters are store inline inside the i_block array.
    // This avoids wasting an entire block on short links. (Most links are short.)
    if (is_symlink() && size() < max_inline_symlink_length) {
        ssize_t nread = min((off_t)size() - offset, static_cast<off_t>(count));
        memcpy(buffer, ((const u8*)m_raw_inode.i_block) + offset, (size_t)nread);
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

    const int block_size = fs().block_size();

    int first_block_logical_index = offset / block_size;
    int last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    int offset_into_first_block = offset % block_size;

    ssize_t nread = 0;
    int remaining_count = min((off_t)count, (off_t)size() - offset);
    u8* out = buffer;

#ifdef EXT2_DEBUG
    kprintf("Ext2FS: Reading up to %u bytes %d bytes into inode %u:%u to %p\n", count, offset, identifier().fsid(), identifier().index(), buffer);
    //kprintf("ok let's do it, read(%u, %u) -> blocks %u thru %u, oifb: %u\n", offset, count, first_block_logical_index, last_block_logical_index, offset_into_first_block);
#endif

    for (int bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; ++bi) {
        auto block = fs().read_block(m_block_list[bi]);
        if (!block) {
            kprintf("ext2fs: read_bytes: read_block(%u) failed (lbi: %u)\n", m_block_list[bi], bi);
            return -EIO;
        }

        int offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        int num_bytes_to_copy = min(block_size - offset_into_block, remaining_count);
        memcpy(out, block.pointer() + offset_into_block, num_bytes_to_copy);
        remaining_count -= num_bytes_to_copy;
        nread += num_bytes_to_copy;
        out += num_bytes_to_copy;
    }

    return nread;
}

bool Ext2FSInode::resize(u64 new_size)
{
    u64 block_size = fs().block_size();
    u64 old_size = size();
    int blocks_needed_before = ceil_div(old_size, block_size);
    int blocks_needed_after = ceil_div(new_size, block_size);

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FSInode::resize(): blocks needed before (size was %Q): %d\n", old_size, blocks_needed_before);
    dbgprintf("Ext2FSInode::resize(): blocks needed after  (size is  %Q): %d\n", new_size, blocks_needed_after);
#endif

    auto block_list = fs().block_list_for_inode(m_raw_inode);
    if (blocks_needed_after > blocks_needed_before) {
        auto new_blocks = fs().allocate_blocks(fs().group_index_from_inode(index()), blocks_needed_after - blocks_needed_before);
        for (auto new_block_index : new_blocks)
            fs().set_block_allocation_state(new_block_index, true);
        block_list.append(move(new_blocks));
    } else if (blocks_needed_after < blocks_needed_before) {
#ifdef EXT2_DEBUG
        dbgprintf("Ext2FSInode::resize(): Shrinking. Old block list is %d entries:\n", block_list.size());
        for (auto block_index : block_list) {
            dbgprintf("    # %u\n", block_index);
        }
#endif
        while (block_list.size() != blocks_needed_after) {
            auto block_index = block_list.take_last();
            fs().set_block_allocation_state(block_index, false);
        }
    }

    bool success = fs().write_block_list_for_inode(index(), m_raw_inode, block_list);
    if (!success)
        return false;

    m_raw_inode.i_size = new_size;
    set_metadata_dirty(true);

    m_block_list = move(block_list);
    return true;
}

ssize_t Ext2FSInode::write_bytes(off_t offset, ssize_t count, const u8* data, FileDescription*)
{
    ASSERT(offset >= 0);
    ASSERT(count >= 0);

    Locker inode_locker(m_lock);
    Locker fs_locker(fs().m_lock);

    if (is_symlink()) {
        if ((offset + count) < max_inline_symlink_length) {
#ifdef EXT2_DEBUG
            dbgprintf("Ext2FSInode: write_bytes poking into i_block array for inline symlink '%s' (%u bytes)\n", String((const char*)data, count).characters(), count);
#endif
            memcpy(((u8*)m_raw_inode.i_block) + offset, data, (size_t)count);
            if ((offset + count) > (off_t)m_raw_inode.i_size)
                m_raw_inode.i_size = offset + count;
            set_metadata_dirty(true);
            return count;
        }
    }

    const ssize_t block_size = fs().block_size();
    u64 old_size = size();
    u64 new_size = max(static_cast<u64>(offset) + count, (u64)size());

    if (!resize(new_size))
        return -EIO;

    int first_block_logical_index = offset / block_size;
    int last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    int offset_into_first_block = offset % block_size;

    int last_logical_block_index_in_file = new_size / block_size;

    ssize_t nwritten = 0;
    int remaining_count = min((off_t)count, (off_t)new_size - offset);
    const u8* in = data;

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FSInode::write_bytes: Writing %u bytes %d bytes into inode %u:%u from %p\n", count, offset, fsid(), index(), data);
#endif

    auto buffer_block = ByteBuffer::create_uninitialized(block_size);
    for (int bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; ++bi) {
        int offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        int num_bytes_to_copy = min(block_size - offset_into_block, remaining_count);

        ByteBuffer block;
        if (offset_into_block != 0 || num_bytes_to_copy != block_size) {
            block = fs().read_block(m_block_list[bi]);
            if (!block) {
                kprintf("Ext2FSInode::write_bytes: read_block(%u) failed (lbi: %u)\n", m_block_list[bi], bi);
                return -EIO;
            }
        } else
            block = buffer_block;

        memcpy(block.pointer() + offset_into_block, in, num_bytes_to_copy);
        if (bi == last_logical_block_index_in_file && num_bytes_to_copy < block_size) {
            int padding_start = new_size % block_size;
            int padding_bytes = block_size - padding_start;
#ifdef EXT2_DEBUG
            dbgprintf("Ext2FSInode::write_bytes padding last block of file with zero x %u (new_size=%u, offset_into_block=%u, num_bytes_to_copy=%u)\n", padding_bytes, new_size, offset_into_block, num_bytes_to_copy);
#endif
            memset(block.pointer() + padding_start, 0, padding_bytes);
        }
#ifdef EXT2_DEBUG
        dbgprintf("Ext2FSInode::write_bytes: writing block %u (offset_into_block: %u)\n", m_block_list[bi], offset_into_block);
#endif
        bool success = fs().write_block(m_block_list[bi], block);
        if (!success) {
            kprintf("Ext2FSInode::write_bytes: write_block(%u) failed (lbi: %u)\n", m_block_list[bi], bi);
            ASSERT_NOT_REACHED();
            return -EIO;
        }
        remaining_count -= num_bytes_to_copy;
        nwritten += num_bytes_to_copy;
        in += num_bytes_to_copy;
    }

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FSInode::write_bytes: after write, i_size=%u, i_blocks=%u (%u blocks in list)\n", m_raw_inode.i_size, m_raw_inode.i_blocks, m_block_list.size());
#endif

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

bool Ext2FSInode::write_directory(const Vector<FS::DirectoryEntry>& entries)
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: New directory inode %u contents to write:\n", index());
#endif

    int directory_size = 0;
    for (auto& entry : entries) {
        //kprintf("  - %08u %s\n", entry.inode.index(), entry.name);
        directory_size += EXT2_DIR_REC_LEN(entry.name_length);
    }

    auto block_size = fs().block_size();

    int blocks_needed = ceil_div(directory_size, block_size);
    int occupied_size = blocks_needed * block_size;

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: directory size: %u (occupied: %u)\n", directory_size, occupied_size);
#endif

    auto directory_data = ByteBuffer::create_uninitialized(occupied_size);

    BufferStream stream(directory_data);
    for (int i = 0; i < entries.size(); ++i) {
        auto& entry = entries[i];

        int record_length = EXT2_DIR_REC_LEN(entry.name_length);
        if (i == entries.size() - 1)
            record_length += occupied_size - directory_size;

#ifdef EXT2_DEBUG
        dbgprintf("* inode: %u", entry.inode.index());
        dbgprintf(", name_len: %u", u16(entry.name_length));
        dbgprintf(", rec_len: %u", u16(record_length));
        dbgprintf(", file_type: %u", u8(entry.file_type));
        dbgprintf(", name: %s\n", entry.name);
#endif

        stream << u32(entry.inode.index());
        stream << u16(record_length);
        stream << u8(entry.name_length);
        stream << u8(entry.file_type);
        stream << entry.name;

        int padding = record_length - entry.name_length - 8;
        for (int j = 0; j < padding; ++j)
            stream << u8(0);
    }

    stream.fill_to_end(0);

    ssize_t nwritten = write_bytes(0, directory_data.size(), directory_data.pointer(), nullptr);
    return nwritten == directory_data.size();
}

KResult Ext2FSInode::add_child(InodeIdentifier child_id, const StringView& name, mode_t mode)
{
    LOCKER(m_lock);
    ASSERT(is_directory());

#ifdef EXT2_DEBUG
    dbg() << "Ext2FSInode::add_child(): Adding inode " << child_id.index() << " with name '" << name << " and mode " << mode << " to directory " << index();
#endif

    Vector<FS::DirectoryEntry> entries;
    bool name_already_exists = false;
    traverse_as_directory([&](auto& entry) {
        if (name == entry.name) {
            name_already_exists = true;
            return false;
        }
        entries.append(entry);
        return true;
    });
    if (name_already_exists) {
        dbg() << "Ext2FSInode::add_child(): Name '" << name << "' already exists in inode " << index();
        return KResult(-EEXIST);
    }

    auto child_inode = fs().get_inode(child_id);
    if (child_inode)
        child_inode->increment_link_count();

    entries.empend(name.characters_without_null_termination(), name.length(), child_id, to_ext2_file_type(mode));
    bool success = write_directory(entries);
    if (success)
        m_lookup_cache.set(name, child_id.index());
    return KSuccess;
}

KResult Ext2FSInode::remove_child(const StringView& name)
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbg() << "Ext2FSInode::remove_child(" << name << ") in inode " << index();
#endif
    ASSERT(is_directory());

    unsigned child_inode_index;
    auto it = m_lookup_cache.find(name);
    if (it == m_lookup_cache.end())
        return KResult(-ENOENT);
    child_inode_index = (*it).value;

    InodeIdentifier child_id { fsid(), child_inode_index };

#ifdef EXT2_DEBUG
    dbg() << "Ext2FSInode::remove_child(): Removing '" << name << "' in directory " << index();
#endif

    Vector<FS::DirectoryEntry> entries;
    traverse_as_directory([&](auto& entry) {
        if (name != entry.name)
            entries.append(entry);
        return true;
    });

    bool success = write_directory(entries);
    if (!success) {
        // FIXME: Plumb error from write_directory().
        return KResult(-EIO);
    }

    m_lookup_cache.remove(name);

    auto child_inode = fs().get_inode(child_id);
    child_inode->decrement_link_count();
    return KSuccess;
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

Vector<Ext2FS::BlockIndex> Ext2FS::allocate_blocks(GroupIndex group_index, int count)
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: allocate_blocks(group: %u, count: %u)\n", group_index, count);
#endif
    if (count == 0)
        return {};

    auto& bgd = group_descriptor(group_index);
    if (bgd.bg_free_blocks_count < count) {
        kprintf("Ext2FS: allocate_blocks can't allocate out of group %u, wanted %u but only %u available\n", group_index, count, bgd.bg_free_blocks_count);
        return {};
    }

    // FIXME: Implement a scan that finds consecutive blocks if possible.
    Vector<BlockIndex> blocks;
    auto bitmap_block = read_block(bgd.bg_block_bitmap);
    int blocks_in_group = min(blocks_per_group(), super_block().s_blocks_count);
    auto block_bitmap = Bitmap::wrap(bitmap_block.pointer(), blocks_in_group);
    BlockIndex first_block_in_group = (group_index - 1) * blocks_per_group() + 1;
    for (int i = 0; i < block_bitmap.size(); ++i) {
        if (!block_bitmap.get(i)) {
            blocks.append(first_block_in_group + i);
            if (blocks.size() == count)
                break;
        }
    }

    ASSERT(blocks.size() == count);
    dbgprintf("Ext2FS: allocate_block found these blocks:\n");
    for (auto& bi : blocks) {
        dbgprintf("  > %u\n", bi);
    }

    return blocks;
}

unsigned Ext2FS::allocate_inode(GroupIndex preferred_group, off_t expected_size)
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: allocate_inode(preferredGroup: %u, expected_size: %u)\n", preferred_group, expected_size);
#endif

    unsigned needed_blocks = ceil_div(expected_size, block_size());

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: minimum needed blocks: %u\n", needed_blocks);
#endif

    unsigned group_index = 0;

    auto is_suitable_group = [this, needed_blocks](GroupIndex group_index) {
        auto& bgd = group_descriptor(group_index);
        return bgd.bg_free_inodes_count && bgd.bg_free_blocks_count >= needed_blocks;
    };

    if (preferred_group && is_suitable_group(preferred_group)) {
        group_index = preferred_group;
    } else {
        for (unsigned i = 1; i <= m_block_group_count; ++i) {
            if (is_suitable_group(i))
                group_index = i;
        }
    }

    if (!group_index) {
        kprintf("Ext2FS: allocate_inode: no suitable group found for new inode with %u blocks needed :(\n", needed_blocks);
        return 0;
    }

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: allocate_inode: found suitable group [%u] for new inode with %u blocks needed :^)\n", group_index, needed_blocks);
#endif

    auto& bgd = group_descriptor(group_index);
    unsigned inodes_in_group = min(inodes_per_group(), super_block().s_inodes_count);
    unsigned first_free_inode_in_group = 0;

    unsigned first_inode_in_group = (group_index - 1) * inodes_per_group() + 1;

    auto bitmap_block = read_block(bgd.bg_inode_bitmap);
    auto inode_bitmap = Bitmap::wrap(bitmap_block.data(), inodes_in_group);
    for (int i = 0; i < inode_bitmap.size(); ++i) {
        if (inode_bitmap.get(i))
            continue;
        first_free_inode_in_group = first_inode_in_group + i;
        break;
    }

    if (!first_free_inode_in_group) {
        kprintf("Ext2FS: first_free_inode_in_group returned no inode, despite bgd claiming there are inodes :(\n");
        return 0;
    }

    unsigned inode = first_free_inode_in_group;
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: found suitable inode %u\n", inode);
#endif

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
    unsigned bit_index = (index_in_group - 1) % inodes_per_group();
    auto block = read_block(bgd.bg_inode_bitmap);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), inodes_per_group());
    return bitmap.get(bit_index);
}

bool Ext2FS::set_inode_allocation_state(InodeIndex inode_index, bool new_state)
{
    LOCKER(m_lock);
    unsigned group_index = group_index_from_inode(inode_index);
    auto& bgd = group_descriptor(group_index);
    unsigned index_in_group = inode_index - ((group_index - 1) * inodes_per_group());
    unsigned bit_index = (index_in_group - 1) % inodes_per_group();
    auto block = read_block(bgd.bg_inode_bitmap);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), inodes_per_group());
    bool current_state = bitmap.get(bit_index);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: set_inode_allocation_state(%u) %u -> %u\n", inode_index, current_state, new_state);
#endif

    if (current_state == new_state) {
        ASSERT_NOT_REACHED();
        return true;
    }

    bitmap.set(bit_index, new_state);
    bool success = write_block(bgd.bg_inode_bitmap, block);
    ASSERT(success);

    // Update superblock
    auto& sb = *reinterpret_cast<ext2_super_block*>(m_cached_super_block.pointer());
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: superblock free inode count %u -> %u\n", sb.s_free_inodes_count, sb.s_free_inodes_count - 1);
#endif
    if (new_state)
        --sb.s_free_inodes_count;
    else
        ++sb.s_free_inodes_count;
    write_super_block(sb);

    // Update BGD
    auto& mutable_bgd = const_cast<ext2_group_desc&>(bgd);
    if (new_state)
        --mutable_bgd.bg_free_inodes_count;
    else
        ++mutable_bgd.bg_free_inodes_count;
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: group free inode count %u -> %u\n", bgd.bg_free_inodes_count, bgd.bg_free_inodes_count - 1);
#endif

    flush_block_group_descriptor_table();
    return true;
}

bool Ext2FS::set_block_allocation_state(BlockIndex block_index, bool new_state)
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: set_block_allocation_state(block=%u, state=%u)\n", block_index, new_state);
#endif
    unsigned group_index = group_index_from_block_index(block_index);
    auto& bgd = group_descriptor(group_index);
    BlockIndex index_in_group = (block_index - 1) - ((group_index - 1) * blocks_per_group());
    unsigned bit_index = index_in_group % blocks_per_group();
#ifdef EXT2_DEBUG
    dbgprintf("  index_in_group: %u\n", index_in_group);
    dbgprintf("  blocks_per_group: %u\n", blocks_per_group());
    dbgprintf("  bit_index: %u\n", bit_index);
    dbgprintf("  read_block(%u)\n", bgd.bg_block_bitmap);
#endif
    auto block = read_block(bgd.bg_block_bitmap);
    ASSERT(block);
    auto bitmap = Bitmap::wrap(block.pointer(), blocks_per_group());
    bool current_state = bitmap.get(bit_index);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: block %u state: %u -> %u\n", block_index, current_state, new_state);
#endif

    if (current_state == new_state) {
        ASSERT_NOT_REACHED();
        return true;
    }

    bitmap.set(bit_index, new_state);
    bool success = write_block(bgd.bg_block_bitmap, block);
    ASSERT(success);

    // Update superblock
    auto& sb = *reinterpret_cast<ext2_super_block*>(m_cached_super_block.pointer());
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: superblock free block count %u -> %u\n", sb.s_free_blocks_count, sb.s_free_blocks_count - 1);
#endif
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
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: group %u free block count %u -> %u\n", group_index, bgd.bg_free_blocks_count, bgd.bg_free_blocks_count - 1);
#endif

    flush_block_group_descriptor_table();
    return true;
}

RefPtr<Inode> Ext2FS::create_directory(InodeIdentifier parent_id, const String& name, mode_t mode, int& error)
{
    LOCKER(m_lock);
    ASSERT(parent_id.fsid() == fsid());

    // Fix up the mode to definitely be a directory.
    // FIXME: This is a bit on the hackish side.
    mode &= ~0170000;
    mode |= 0040000;

    // NOTE: When creating a new directory, make the size 1 block.
    //       There's probably a better strategy here, but this works for now.
    auto inode = create_inode(parent_id, name, mode, block_size(), 0, error);
    if (!inode)
        return nullptr;

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: create_directory: created new directory named '%s' with inode %u\n", name.characters(), inode->identifier().index());
#endif

    Vector<DirectoryEntry> entries;
    entries.empend(".", inode->identifier(), EXT2_FT_DIR);
    entries.empend("..", parent_id, EXT2_FT_DIR);

    bool success = static_cast<Ext2FSInode&>(*inode).write_directory(entries);
    ASSERT(success);

    auto parent_inode = get_inode(parent_id);
    error = parent_inode->increment_link_count();
    if (error < 0)
        return nullptr;

    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode->identifier().index())));
    ++bgd.bg_used_dirs_count;
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: incremented bg_used_dirs_count %u -> %u\n", bgd.bg_used_dirs_count - 1, bgd.bg_used_dirs_count);
#endif

    flush_block_group_descriptor_table();

    error = 0;
    return inode;
}

RefPtr<Inode> Ext2FS::create_inode(InodeIdentifier parent_id, const String& name, mode_t mode, off_t size, dev_t dev, int& error)
{
    LOCKER(m_lock);
    ASSERT(parent_id.fsid() == fsid());
    auto parent_inode = get_inode(parent_id);

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: Adding inode '%s' (mode %o) to parent directory %u:\n", name.characters(), mode, parent_inode->identifier().index());
#endif

    // NOTE: This doesn't commit the inode allocation just yet!
    auto inode_id = allocate_inode(0, size);
    if (!inode_id) {
        kprintf("Ext2FS: create_inode: allocate_inode failed\n");
        error = -ENOSPC;
        return {};
    }

    auto needed_blocks = ceil_div(size, block_size());
    auto blocks = allocate_blocks(group_index_from_inode(inode_id), needed_blocks);
    if (blocks.size() != needed_blocks) {
        kprintf("Ext2FS: create_inode: allocate_blocks failed\n");
        error = -ENOSPC;
        return {};
    }

    // Try adding it to the directory first, in case the name is already in use.
    auto result = parent_inode->add_child({ fsid(), inode_id }, name, mode);
    if (result.is_error()) {
        error = result;
        return {};
    }

    // Looks like we're good, time to update the inode bitmap and group+global inode counters.
    bool success = set_inode_allocation_state(inode_id, true);
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

    struct timeval now;
    kgettimeofday(now);
    ext2_inode e2inode;
    memset(&e2inode, 0, sizeof(ext2_inode));
    e2inode.i_mode = mode;
    e2inode.i_uid = current->process().euid();
    e2inode.i_gid = current->process().egid();
    e2inode.i_size = size;
    e2inode.i_atime = now.tv_sec;
    e2inode.i_ctime = now.tv_sec;
    e2inode.i_mtime = now.tv_sec;
    e2inode.i_dtime = 0;
    e2inode.i_links_count = initial_links_count;

    if (is_character_device(mode))
        e2inode.i_block[0] = dev;
    else if (is_block_device(mode))
        e2inode.i_block[1] = dev;

    success = write_block_list_for_inode(inode_id, e2inode, blocks);
    ASSERT(success);

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: writing initial metadata for inode %u\n", inode_id);
#endif
    e2inode.i_flags = 0;
    success = write_ext2_inode(inode_id, e2inode);
    ASSERT(success);

    // We might have cached the fact that this inode didn't exist. Wipe the slate.
    m_inode_cache.remove(inode_id);

    return get_inode({ fsid(), inode_id });
}

void Ext2FSInode::populate_lookup_cache() const
{
    LOCKER(m_lock);
    if (!m_lookup_cache.is_empty())
        return;
    HashMap<String, unsigned> children;

    traverse_as_directory([&children](auto& entry) {
        children.set(String(entry.name, entry.name_length), entry.inode.index());
        return true;
    });

    if (!m_lookup_cache.is_empty())
        return;
    m_lookup_cache = move(children);
}

InodeIdentifier Ext2FSInode::lookup(StringView name)
{
    ASSERT(is_directory());
    populate_lookup_cache();
    LOCKER(m_lock);
    auto it = m_lookup_cache.find(name);
    if (it != m_lookup_cache.end())
        return { fsid(), (*it).value };
    return {};
}

void Ext2FSInode::one_ref_left()
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
    LOCKER(m_lock);
    populate_lookup_cache();
    return m_lookup_cache.size();
}

KResult Ext2FSInode::chmod(mode_t mode)
{
    LOCKER(m_lock);
    if (m_raw_inode.i_mode == mode)
        return KSuccess;
    m_raw_inode.i_mode = mode;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::chown(uid_t uid, gid_t gid)
{
    LOCKER(m_lock);
    if (m_raw_inode.i_uid == uid && m_raw_inode.i_gid == gid)
        return KSuccess;
    m_raw_inode.i_uid = uid;
    m_raw_inode.i_gid = gid;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::truncate(off_t size)
{
    LOCKER(m_lock);
    if ((off_t)m_raw_inode.i_size == size)
        return KSuccess;
    resize(size);
    set_metadata_dirty(true);
    return KSuccess;
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
