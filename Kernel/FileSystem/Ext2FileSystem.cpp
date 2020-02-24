/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Bitmap.h>
#include <AK/BufferStream.h>
#include <AK/HashMap.h>
#include <AK/StdLibExtras.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/ext2_fs.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>

//#define EXT2_DEBUG

namespace Kernel {

static const size_t max_link_count = 65535;
static const size_t max_block_size = 4096;
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

NonnullRefPtr<Ext2FS> Ext2FS::create(BlockDevice& device)
{
    return adopt(*new Ext2FS(device));
}

Ext2FS::Ext2FS(BlockDevice& device)
    : DiskBackedFS(device)
{
}

Ext2FS::~Ext2FS()
{
}

bool Ext2FS::flush_super_block()
{
    LOCKER(m_lock);
    bool success = device().write_blocks(2, 1, (const u8*)&m_super_block);
    ASSERT(success);
    return true;
}

const ext2_group_desc& Ext2FS::group_descriptor(GroupIndex group_index) const
{
    // FIXME: Should this fail gracefully somehow?
    ASSERT(group_index <= m_block_group_count);
    return block_group_descriptors()[group_index - 1];
}

bool Ext2FS::initialize()
{
    LOCKER(m_lock);
    bool success = const_cast<BlockDevice&>(device()).read_blocks(2, 1, (u8*)&m_super_block);
    ASSERT(success);

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

    ASSERT(block_size() <= (int)max_block_size);

    m_block_group_count = ceil_div(super_block.s_blocks_count, super_block.s_blocks_per_group);

    if (m_block_group_count == 0) {
        kprintf("ext2fs: no block groups :(\n");
        return false;
    }

    unsigned blocks_to_read = ceil_div(m_block_group_count * (unsigned)sizeof(ext2_group_desc), block_size());
    BlockIndex first_block_of_bgdt = block_size() == 1024 ? 2 : 1;
    m_cached_group_descriptor_table = KBuffer::create_with_size(block_size() * blocks_to_read, Region::Access::Read | Region::Access::Write, "Ext2FS: Block group descriptors");
    read_blocks(first_block_of_bgdt, blocks_to_read, m_cached_group_descriptor_table.value().data());

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

bool Ext2FS::read_block_containing_inode(unsigned inode, unsigned& block_index, unsigned& offset, u8* buffer) const
{
    LOCKER(m_lock);
    auto& super_block = this->super_block();

    if (inode != EXT2_ROOT_INO && inode < EXT2_FIRST_INO(&super_block))
        return false;

    if (inode > super_block.s_inodes_count)
        return false;

    auto& bgd = group_descriptor(group_index_from_inode(inode));

    offset = ((inode - 1) % inodes_per_group()) * inode_size();
    block_index = bgd.bg_inode_table + (offset >> EXT2_BLOCK_SIZE_BITS(&super_block));
    offset &= block_size() - 1;

    return read_block(block_index, buffer);
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

    shape.doubly_indirect_blocks = min(blocks_remaining, entries_per_block * entries_per_block);
    blocks_remaining -= shape.doubly_indirect_blocks;
    shape.meta_blocks += 1;
    shape.meta_blocks += shape.doubly_indirect_blocks / entries_per_block;
    if ((shape.doubly_indirect_blocks % entries_per_block) != 0)
        shape.meta_blocks += 1;
    if (!blocks_remaining)
        return shape;

    dbg() << "we don't know how to compute tind ext2fs blocks yet!";
    ASSERT_NOT_REACHED();

    shape.triply_indirect_blocks = min(blocks_remaining, entries_per_block * entries_per_block * entries_per_block);
    blocks_remaining -= shape.triply_indirect_blocks;
    if (!blocks_remaining)
        return shape;

    ASSERT_NOT_REACHED();

    return {};
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
#ifdef EXT2_DEBUG
        dbgprintf("Ext2FS: Writing %u direct block(s) to i_block array of inode %u\n", min(EXT2_NDIR_BLOCKS, blocks.size()), inode_index);
        for (int i = 0; i < min(EXT2_NDIR_BLOCKS, blocks.size()); ++i)
            dbgprintf("   + %u\n", blocks[i]);
#endif
        write_ext2_inode(inode_index, e2inode);
        inode_dirty = false;
    }

    if (!remaining_blocks)
        return true;

    const unsigned entries_per_block = EXT2_ADDR_PER_BLOCK(&super_block());

    bool ind_block_new = !e2inode.i_block[EXT2_IND_BLOCK];
    if (ind_block_new) {
        BlockIndex new_indirect_block = new_meta_blocks.take_last();
        if (e2inode.i_block[EXT2_IND_BLOCK] != new_indirect_block)
            inode_dirty = true;
        e2inode.i_block[EXT2_IND_BLOCK] = new_indirect_block;
        if (inode_dirty) {
#ifdef EXT2_DEBUG
            dbgprintf("Ext2FS: Adding the indirect block to i_block array of inode %u\n", inode_index);
#endif
            write_ext2_inode(inode_index, e2inode);
            inode_dirty = false;
        }
    }

    if (old_shape.indirect_blocks == new_shape.indirect_blocks) {
        // No need to update the singly indirect block array.
        remaining_blocks -= new_shape.indirect_blocks;
        output_block_index += new_shape.indirect_blocks;
    } else {
        auto block_contents = ByteBuffer::create_uninitialized(block_size());
        BufferStream stream(block_contents);
        ASSERT(new_shape.indirect_blocks <= entries_per_block);
        for (unsigned i = 0; i < new_shape.indirect_blocks; ++i) {
            stream << blocks[output_block_index++];
            --remaining_blocks;
        }
        stream.fill_to_end(0);
        bool success = write_block(e2inode.i_block[EXT2_IND_BLOCK], block_contents.data());
        ASSERT(success);
    }

    if (!remaining_blocks)
        return true;

    bool dind_block_dirty = false;

    bool dind_block_new = !e2inode.i_block[EXT2_DIND_BLOCK];
    if (dind_block_new) {
        BlockIndex new_dindirect_block = new_meta_blocks.take_last();
        if (e2inode.i_block[EXT2_DIND_BLOCK] != new_dindirect_block)
            inode_dirty = true;
        e2inode.i_block[EXT2_DIND_BLOCK] = new_dindirect_block;
        if (inode_dirty) {
#ifdef EXT2_DEBUG
            dbgprintf("Ext2FS: Adding the doubly-indirect block to i_block array of inode %u\n", inode_index);
#endif
            write_ext2_inode(inode_index, e2inode);
            inode_dirty = false;
        }
    }

    if (old_shape.doubly_indirect_blocks == new_shape.doubly_indirect_blocks) {
        // No need to update the doubly indirect block data.
        remaining_blocks -= new_shape.doubly_indirect_blocks;
        output_block_index += new_shape.doubly_indirect_blocks;
    } else {
        unsigned indirect_block_count = new_shape.doubly_indirect_blocks / entries_per_block;
        if ((new_shape.doubly_indirect_blocks % entries_per_block) != 0)
            indirect_block_count++;

        auto dind_block_contents = ByteBuffer::create_uninitialized(block_size());
        read_block(e2inode.i_block[EXT2_DIND_BLOCK], dind_block_contents.data());
        if (dind_block_new) {
            memset(dind_block_contents.data(), 0, dind_block_contents.size());
            dind_block_dirty = true;
        }
        auto* dind_block_as_pointers = (unsigned*)dind_block_contents.data();

        ASSERT(indirect_block_count <= entries_per_block);
        for (unsigned i = 0; i < indirect_block_count; ++i) {
            bool ind_block_dirty = false;

            BlockIndex indirect_block_index = dind_block_as_pointers[i];

            bool ind_block_new = !indirect_block_index;
            if (ind_block_new) {
                indirect_block_index = new_meta_blocks.take_last();
                dind_block_as_pointers[i] = indirect_block_index;
                dind_block_dirty = true;
            }

            auto ind_block_contents = ByteBuffer::create_uninitialized(block_size());
            read_block(indirect_block_index, ind_block_contents.data());
            if (ind_block_new) {
                memset(ind_block_contents.data(), 0, dind_block_contents.size());
                ind_block_dirty = true;
            }
            auto* ind_block_as_pointers = (unsigned*)ind_block_contents.data();

            unsigned entries_to_write = new_shape.doubly_indirect_blocks - (i * entries_per_block);
            if (entries_to_write > entries_per_block)
                entries_to_write = entries_per_block;

            ASSERT(entries_to_write <= entries_per_block);
            for (unsigned j = 0; j < entries_to_write; ++j) {
                BlockIndex output_block = blocks[output_block_index++];
                if (ind_block_as_pointers[j] != output_block) {
                    ind_block_as_pointers[j] = output_block;
                    ind_block_dirty = true;
                }
                --remaining_blocks;
            }
            for (unsigned j = entries_to_write; j < entries_per_block; ++j) {
                if (ind_block_as_pointers[j] != 0) {
                    ind_block_as_pointers[j] = 0;
                    ind_block_dirty = true;
                }
            }

            if (ind_block_dirty) {
                bool success = write_block(indirect_block_index, ind_block_contents.data());
                ASSERT(success);
            }
        }
        for (unsigned i = indirect_block_count; i < entries_per_block; ++i) {
            if (dind_block_as_pointers[i] != 0) {
                dind_block_as_pointers[i] = 0;
                dind_block_dirty = true;
            }
        }

        if (dind_block_dirty) {
            bool success = write_block(e2inode.i_block[EXT2_DIND_BLOCK], dind_block_contents.data());
            ASSERT(success);
        }
    }

    if (!remaining_blocks)
        return true;

    // FIXME: Implement!
    dbg() << "we don't know how to write tind ext2fs blocks yet!";
    ASSERT_NOT_REACHED();
}

Vector<Ext2FS::BlockIndex> Ext2FS::block_list_for_inode(const ext2_inode& e2inode, bool include_block_list_blocks) const
{
    auto block_list = block_list_for_inode_impl(e2inode, include_block_list_blocks);
    while (!block_list.is_empty() && block_list.last() == 0)
        block_list.take_last();
    return block_list;
}

Vector<Ext2FS::BlockIndex> Ext2FS::block_list_for_inode_impl(const ext2_inode& e2inode, bool include_block_list_blocks) const
{
    LOCKER(m_lock);
    unsigned entries_per_block = EXT2_ADDR_PER_BLOCK(&super_block());

    unsigned block_count = ceil_div(e2inode.i_size, block_size());

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS::block_list_for_inode(): i_size=%u, i_blocks=%u, block_count=%u\n", e2inode.i_size, e2inode.i_blocks, block_count);
#endif

    unsigned blocks_remaining = block_count;
    Vector<BlockIndex> list;

    auto add_block = [&](BlockIndex bi) {
        if (blocks_remaining) {
            list.append(bi);
            --blocks_remaining;
        }
    };

    if (include_block_list_blocks) {
        // This seems like an excessive over-estimate but w/e.
        list.ensure_capacity(blocks_remaining * 2);
    } else {
        list.ensure_capacity(blocks_remaining);
    }

    unsigned direct_count = min(block_count, (unsigned)EXT2_NDIR_BLOCKS);
    for (unsigned i = 0; i < direct_count; ++i) {
        auto block_index = e2inode.i_block[i];
        add_block(block_index);
    }

    if (!blocks_remaining)
        return list;

    auto process_block_array = [&](unsigned array_block_index, auto&& callback) {
        if (include_block_list_blocks)
            callback(array_block_index);
        auto array_block = ByteBuffer::create_uninitialized(block_size());
        read_block(array_block_index, array_block.data());
        ASSERT(array_block);
        auto* array = reinterpret_cast<const __u32*>(array_block.data());
        unsigned count = min(blocks_remaining, entries_per_block);
        for (BlockIndex i = 0; i < count; ++i)
            callback(array[i]);
    };

    process_block_array(e2inode.i_block[EXT2_IND_BLOCK], [&](unsigned block_index) {
        add_block(block_index);
    });

    if (!blocks_remaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_DIND_BLOCK], [&](unsigned block_index) {
        process_block_array(block_index, [&](unsigned block_index2) {
            add_block(block_index2);
        });
    });

    if (!blocks_remaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_TIND_BLOCK], [&](unsigned block_index) {
        process_block_array(block_index, [&](unsigned block_index2) {
            process_block_array(block_index2, [&](unsigned block_index3) {
                add_block(block_index3);
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
    dbg() << "Ext2FS: Inode " << inode.identifier() << " has no more links, time to delete!";
#endif

    struct timeval now;
    kgettimeofday(now);
    inode.m_raw_inode.i_dtime = now.tv_sec;
    write_ext2_inode(inode.index(), inode.m_raw_inode);

    auto block_list = block_list_for_inode(inode.m_raw_inode, true);

    for (auto block_index : block_list) {
        if (block_index)
            set_block_allocation_state(block_index, false);
    }

    set_inode_allocation_state(inode.index(), false);

    if (inode.is_directory()) {
        auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode.index())));
        --bgd.bg_used_dirs_count;
        dbg() << "Ext2FS: Decremented bg_used_dirs_count to " << bgd.bg_used_dirs_count;
        m_block_group_descriptors_dirty = true;
    }
}

void Ext2FS::flush_block_group_descriptor_table()
{
    LOCKER(m_lock);
    unsigned blocks_to_write = ceil_div(m_block_group_count * (unsigned)sizeof(ext2_group_desc), block_size());
    unsigned first_block_of_bgdt = block_size() == 1024 ? 2 : 1;
    write_blocks(first_block_of_bgdt, blocks_to_write, (const u8*)block_group_descriptors());
}

void Ext2FS::flush_writes()
{
    LOCKER(m_lock);
    if (m_super_block_dirty) {
        flush_super_block();
        m_super_block_dirty = false;
    }
    if (m_block_group_descriptors_dirty) {
        flush_block_group_descriptor_table();
        m_block_group_descriptors_dirty = false;
    }
    for (auto& cached_bitmap : m_cached_bitmaps) {
        if (cached_bitmap->dirty) {
            write_block(cached_bitmap->bitmap_block_index, cached_bitmap->buffer.data());
            cached_bitmap->dirty = false;
#ifdef EXT2_DEBUG
            dbg() << "Flushed bitmap block " << cached_bitmap->bitmap_block_index;
#endif
        }
    }

    DiskBackedFS::flush_writes();

    // Uncache Inodes that are only kept alive by the index-to-inode lookup cache.
    // We don't uncache Inodes that are being watched by at least one InodeWatcher.

    // FIXME: It would be better to keep a capped number of Inodes around.
    //        The problem is that they are quite heavy objects, and use a lot of heap memory
    //        for their (child name lookup) and (block list) caches.
    Vector<InodeIndex> unused_inodes;
    for (auto& it : m_inode_cache) {
        if (it.value->ref_count() != 1)
            continue;
        if (it.value->has_watchers())
            continue;
        unused_inodes.append(it.key);
    }
    for (auto index : unused_inodes)
        uncache_inode(index);
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
    LOCKER(m_lock);
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

    if (Kernel::is_character_device(m_raw_inode.i_mode) || Kernel::is_block_device(m_raw_inode.i_mode)) {
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
    dbg() << "Ext2FS: flush_metadata for inode " << identifier();
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
    u8 block[max_block_size];
    if (!read_block_containing_inode(inode.index(), block_index, offset, block))
        return {};

    auto new_inode = adopt(*new Ext2FSInode(const_cast<Ext2FS&>(*this), inode.index()));
    memcpy(&new_inode->m_raw_inode, reinterpret_cast<ext2_inode*>(block + offset), sizeof(ext2_inode));
    m_inode_cache.set(inode.index(), new_inode);
    return new_inode;
}

ssize_t Ext2FSInode::read_bytes(off_t offset, ssize_t count, u8* buffer, FileDescription* description) const
{
    Locker inode_locker(m_lock);
    ASSERT(offset >= 0);
    if (m_raw_inode.i_size == 0)
        return 0;

    // Symbolic links shorter than 60 characters are store inline inside the i_block array.
    // This avoids wasting an entire block on short links. (Most links are short.)
    if (is_symlink() && size() < max_inline_symlink_length) {
        ASSERT(offset == 0);
        ssize_t nread = min((off_t)size() - offset, static_cast<off_t>(count));
        memcpy(buffer, ((const u8*)m_raw_inode.i_block) + offset, (size_t)nread);
        return nread;
    }

    Locker fs_locker(fs().m_lock);

    if (m_block_list.is_empty())
        m_block_list = fs().block_list_for_inode(m_raw_inode);

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
    dbg() << "Ext2FS: Reading up to " << count << " bytes " << offset << " bytes into inode " << identifier() << " to " << (const void*)buffer;
#endif

    u8 block[max_block_size];

    for (int bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; ++bi) {
        auto block_index = m_block_list[bi];
        ASSERT(block_index);
        bool success = fs().read_block(block_index, block, description);
        if (!success) {
            kprintf("ext2fs: read_bytes: read_block(%u) failed (lbi: %u)\n", block_index, bi);
            return -EIO;
        }

        int offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        int num_bytes_to_copy = min(block_size - offset_into_block, remaining_count);
        memcpy(out, block + offset_into_block, num_bytes_to_copy);
        remaining_count -= num_bytes_to_copy;
        nread += num_bytes_to_copy;
        out += num_bytes_to_copy;
    }

    return nread;
}

KResult Ext2FSInode::resize(u64 new_size)
{
    u64 old_size = size();
    if (old_size == new_size)
        return KSuccess;

    u64 block_size = fs().block_size();
    int blocks_needed_before = ceil_div(old_size, block_size);
    int blocks_needed_after = ceil_div(new_size, block_size);

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FSInode::resize(): blocks needed before (size was %Q): %d\n", old_size, blocks_needed_before);
    dbgprintf("Ext2FSInode::resize(): blocks needed after  (size is  %Q): %d\n", new_size, blocks_needed_after);
#endif

    if (blocks_needed_after > blocks_needed_before) {
        u32 additional_blocks_needed = blocks_needed_after - blocks_needed_before;
        if (additional_blocks_needed > fs().super_block().s_free_blocks_count)
            return KResult(-ENOSPC);
    }

    auto block_list = fs().block_list_for_inode(m_raw_inode);
    if (blocks_needed_after > blocks_needed_before) {
        auto new_blocks = fs().allocate_blocks(fs().group_index_from_inode(index()), blocks_needed_after - blocks_needed_before);
        block_list.append(move(new_blocks));
    } else if (blocks_needed_after < blocks_needed_before) {
#ifdef EXT2_DEBUG
        dbg() << "Ext2FS: Shrinking inode " << identifier() << ". Old block list is " << block_list.size() << " entries:";
        for (auto block_index : block_list) {
            dbg() << "    # " << block_index;
        }
#endif
        while (block_list.size() != blocks_needed_after) {
            auto block_index = block_list.take_last();
            if (block_index)
                fs().set_block_allocation_state(block_index, false);
        }
    }

    bool success = fs().write_block_list_for_inode(index(), m_raw_inode, block_list);
    if (!success)
        return KResult(-EIO);

    m_raw_inode.i_size = new_size;
    set_metadata_dirty(true);

    m_block_list = move(block_list);
    return KSuccess;
}

ssize_t Ext2FSInode::write_bytes(off_t offset, ssize_t count, const u8* data, FileDescription* description)
{
    ASSERT(offset >= 0);
    ASSERT(count >= 0);

    Locker inode_locker(m_lock);
    Locker fs_locker(fs().m_lock);

    if (is_symlink()) {
        ASSERT(offset == 0);
        if (max((size_t)(offset + count), (size_t)m_raw_inode.i_size) < max_inline_symlink_length) {
#ifdef EXT2_DEBUG
            dbg() << "Ext2FS: write_bytes poking into i_block array for inline symlink '" << StringView(data, count) << " ' (" << count << " bytes)";
#endif
            memcpy(((u8*)m_raw_inode.i_block) + offset, data, (size_t)count);
            if ((size_t)(offset + count) > (size_t)m_raw_inode.i_size)
                m_raw_inode.i_size = offset + count;
            set_metadata_dirty(true);
            return count;
        }
    }

    const ssize_t block_size = fs().block_size();
    u64 old_size = size();
    u64 new_size = max(static_cast<u64>(offset) + count, (u64)size());

    auto resize_result = resize(new_size);
    if (resize_result.is_error())
        return resize_result;

    if (m_block_list.is_empty())
        m_block_list = fs().block_list_for_inode(m_raw_inode);

    if (m_block_list.is_empty()) {
        dbg() << "Ext2FSInode::write_bytes(): empty block list for inode " << index();
        return -EIO;
    }

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
    dbg() << "Ext2FS: Writing " << count << " bytes " << offset << " bytes into inode " << identifier() << " from " << (const void*)data;
#endif

    auto buffer_block = ByteBuffer::create_uninitialized(block_size);
    for (int bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; ++bi) {
        int offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        int num_bytes_to_copy = min(block_size - offset_into_block, remaining_count);

        ByteBuffer block;
        if (offset_into_block != 0 || num_bytes_to_copy != block_size) {
            block = ByteBuffer::create_uninitialized(block_size);
            bool success = fs().read_block(m_block_list[bi], block.data(), description);
            if (!success) {
                dbg() << "Ext2FS: In write_bytes, read_block(" << m_block_list[bi] << ") failed (bi: " << bi << ")";
                return -EIO;
            }
        } else
            block = buffer_block;

        memcpy(block.data() + offset_into_block, in, num_bytes_to_copy);
        if (bi == last_logical_block_index_in_file && num_bytes_to_copy < block_size) {
            int padding_start = new_size % block_size;
            int padding_bytes = block_size - padding_start;
#ifdef EXT2_DEBUG
            dbg() << "Ext2FS: Padding last block of file with zero x " << padding_bytes << " (new_size=" << new_size << ", offset_into_block=" << offset_into_block << ", num_bytes_to_copy=" << num_bytes_to_copy << ")";
#endif
            memset(block.data() + padding_start, 0, padding_bytes);
        }
#ifdef EXT2_DEBUG
        dbg() << "Ext2FS: Writing block " << m_block_list[bi] << " (offset_into_block: " << offset_into_block << ")";
#endif
        bool success = fs().write_block(m_block_list[bi], block.data(), description);
        if (!success) {
            dbg() << "Ext2FS: write_block(" << m_block_list[bi] << ") failed (bi: " << bi << ")";
            ASSERT_NOT_REACHED();
            return -EIO;
        }
        remaining_count -= num_bytes_to_copy;
        nwritten += num_bytes_to_copy;
        in += num_bytes_to_copy;
    }

#ifdef EXT2_DEBUG
    dbg() << "Ext2FS: After write, i_size=" << m_raw_inode.i_size << ", i_blocks=" << m_raw_inode.i_blocks << " (" << m_block_list.size() << " blocks in list)";
#endif

    if (old_size != new_size)
        inode_size_changed(old_size, new_size);
    inode_contents_changed(offset, count, data);
    return nwritten;
}

bool Ext2FSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntry&)> callback) const
{
    LOCKER(m_lock);
    ASSERT(is_directory());

#ifdef EXT2_DEBUG
    dbg() << "Ext2FS: Traversing as directory: " << identifier();
#endif

    auto buffer = read_entire();
    ASSERT(buffer);
    auto* entry = reinterpret_cast<ext2_dir_entry_2*>(buffer.data());

    while (entry < buffer.end_pointer()) {
        if (entry->inode != 0) {
#ifdef EXT2_DEBUG
            dbgprintf("Ext2Inode::traverse_as_directory: %u, name_len: %u, rec_len: %u, file_type: %u, name: %s\n", entry->inode, entry->name_len, entry->rec_len, entry->file_type, String(entry->name, entry->name_len).characters());
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

    int directory_size = 0;
    for (auto& entry : entries)
        directory_size += EXT2_DIR_REC_LEN(entry.name_length);

    auto block_size = fs().block_size();

    int blocks_needed = ceil_div(directory_size, block_size);
    int occupied_size = blocks_needed * block_size;

#ifdef EXT2_DEBUG
    dbg() << "Ext2FS: New directory inode " << identifier() << " contents to write (size " << directory_size << ", occupied " << occupied_size << "):";
#endif

    auto directory_data = ByteBuffer::create_uninitialized(occupied_size);

    BufferStream stream(directory_data);
    for (int i = 0; i < entries.size(); ++i) {
        auto& entry = entries[i];

        int record_length = EXT2_DIR_REC_LEN(entry.name_length);
        if (i == entries.size() - 1)
            record_length += occupied_size - directory_size;

#ifdef EXT2_DEBUG
        dbg() << "* Inode: " << entry.inode
              << ", name_len: " << u16(entry.name_length)
              << ", rec_len: " << u16(record_length)
              << ", file_type: " << u8(entry.file_type)
              << ", name: " << entry.name;
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

    ssize_t nwritten = write_bytes(0, directory_data.size(), directory_data.data(), nullptr);
    if (nwritten < 0)
        return false;
    set_metadata_dirty(true);
    return static_cast<size_t>(nwritten) == directory_data.size();
}

KResult Ext2FSInode::add_child(InodeIdentifier child_id, const StringView& name, mode_t mode)
{
    LOCKER(m_lock);
    ASSERT(is_directory());

    if (name.length() > EXT2_NAME_LEN)
        return KResult(-ENAMETOOLONG);

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
    if (child_inode) {
        auto result = child_inode->increment_link_count();
        if (result.is_error())
            return result;
    }

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

    auto it = m_lookup_cache.find(name);
    if (it == m_lookup_cache.end())
        return KResult(-ENOENT);
    auto child_inode_index = (*it).value;

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
    u8 block[max_block_size];
    if (!read_block_containing_inode(inode, block_index, offset, block))
        return false;
    memcpy(reinterpret_cast<ext2_inode*>(block + offset), &e2inode, inode_size());
    bool success = write_block(block_index, block);
    ASSERT(success);
    return success;
}

Ext2FS::BlockIndex Ext2FS::allocate_block(GroupIndex preferred_group_index)
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbg() << "Ext2FS: allocate_block() preferred_group_index: " << preferred_group_index;
#endif
    bool found_a_group = false;
    GroupIndex group_index = preferred_group_index;

    if (group_descriptor(preferred_group_index).bg_free_blocks_count) {
        found_a_group = true;
    } else {
        for (group_index = 1; group_index < m_block_group_count; ++group_index) {
            if (group_descriptor(group_index).bg_free_blocks_count) {
                found_a_group = true;
                break;
            }
        }
    }
    ASSERT(found_a_group);
    auto& bgd = group_descriptor(group_index);
    auto& cached_bitmap = get_bitmap_block(bgd.bg_block_bitmap);

    int blocks_in_group = min(blocks_per_group(), super_block().s_blocks_count);
    auto block_bitmap = Bitmap::wrap(cached_bitmap.buffer.data(), blocks_in_group);

    BlockIndex first_block_in_group = (group_index - 1) * blocks_per_group() + first_block_index();
    int first_unset_bit_index = block_bitmap.find_first_unset();
    ASSERT(first_unset_bit_index != -1);
    BlockIndex block_index = (unsigned)first_unset_bit_index + first_block_in_group;
    set_block_allocation_state(block_index, true);
    return block_index;
}

Vector<Ext2FS::BlockIndex> Ext2FS::allocate_blocks(GroupIndex preferred_group_index, int count)
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: allocate_blocks(preferred group: %u, count: %u)\n", preferred_group_index, count);
#endif
    if (count == 0)
        return {};

    Vector<BlockIndex> blocks;
#ifdef EXT2_DEBUG
    dbg() << "Ext2FS: allocate_blocks:";
#endif
    blocks.ensure_capacity(count);

    bool found_a_group = false;
    GroupIndex group_index = preferred_group_index;

    if (!group_descriptor(preferred_group_index).bg_free_blocks_count) {
        group_index = 1;
    }

    while (blocks.size() < count) {
        if (group_descriptor(group_index).bg_free_blocks_count) {
            found_a_group = true;
        } else {
            if (group_index == preferred_group_index)
                group_index = 1;
            for (; group_index < m_block_group_count; ++group_index) {
                if (group_descriptor(group_index).bg_free_blocks_count) {
                    found_a_group = true;
                    break;
                }
            }
        }
        ASSERT(found_a_group);
        auto& bgd = group_descriptor(group_index);
        auto& cached_bitmap = get_bitmap_block(bgd.bg_block_bitmap);

        int blocks_in_group = min(blocks_per_group(), super_block().s_blocks_count);
        auto block_bitmap = Bitmap::wrap(cached_bitmap.buffer.data(), blocks_in_group);

        BlockIndex first_block_in_group = (group_index - 1) * blocks_per_group() + first_block_index();
        size_t free_region_size = 0;
        auto first_unset_bit_index = block_bitmap.find_longest_range_of_unset_bits(count - blocks.size(), free_region_size);
        ASSERT(first_unset_bit_index.has_value());
#ifdef EXT2_DEBUG
        dbg() << "Ext2FS: allocating free region of size: " << free_region_size << "[" << group_index << "]";
#endif
        for (size_t i = 0; i < free_region_size; ++i) {
            BlockIndex block_index = (first_unset_bit_index.value() + i) + first_block_in_group;
            set_block_allocation_state(block_index, true);
            blocks.unchecked_append(block_index);
#ifdef EXT2_DEBUG
            dbg() << "  allocated > " << block_index;
#endif
        }
    }

    ASSERT(blocks.size() == count);
    return blocks;
}

unsigned Ext2FS::find_a_free_inode(GroupIndex preferred_group, off_t expected_size)
{
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: find_a_free_inode(preferred_group: %u, expected_size: %ld)\n", preferred_group, expected_size);
#endif

    unsigned needed_blocks = ceil_div(expected_size, block_size());

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: minimum needed blocks: %u\n", needed_blocks);
#endif

    unsigned group_index = 0;

    // FIXME: We shouldn't refuse to allocate an inode if there is no group that can house the whole thing.
    //        In those cases we should just spread it across multiple groups.
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
        kprintf("Ext2FS: find_a_free_inode: no suitable group found for new inode with %u blocks needed :(\n", needed_blocks);
        return 0;
    }

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: find_a_free_inode: found suitable group [%u] for new inode with %u blocks needed :^)\n", group_index, needed_blocks);
#endif

    auto& bgd = group_descriptor(group_index);
    unsigned inodes_in_group = min(inodes_per_group(), super_block().s_inodes_count);
    unsigned first_free_inode_in_group = 0;

    unsigned first_inode_in_group = (group_index - 1) * inodes_per_group() + 1;

    auto& cached_bitmap = get_bitmap_block(bgd.bg_inode_bitmap);
    auto inode_bitmap = Bitmap::wrap(cached_bitmap.buffer.data(), inodes_in_group);
    for (size_t i = 0; i < inode_bitmap.size(); ++i) {
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

    auto& cached_bitmap = const_cast<Ext2FS&>(*this).get_bitmap_block(bgd.bg_inode_bitmap);
    return cached_bitmap.bitmap(inodes_per_group()).get(bit_index);
}

bool Ext2FS::set_inode_allocation_state(InodeIndex inode_index, bool new_state)
{
    LOCKER(m_lock);
    unsigned group_index = group_index_from_inode(inode_index);
    auto& bgd = group_descriptor(group_index);
    unsigned index_in_group = inode_index - ((group_index - 1) * inodes_per_group());
    unsigned bit_index = (index_in_group - 1) % inodes_per_group();

    auto& cached_bitmap = get_bitmap_block(bgd.bg_inode_bitmap);

    bool current_state = cached_bitmap.bitmap(inodes_per_group()).get(bit_index);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: set_inode_allocation_state(%u) %u -> %u\n", inode_index, current_state, new_state);
#endif

    if (current_state == new_state) {
        ASSERT_NOT_REACHED();
        return true;
    }

    cached_bitmap.bitmap(inodes_per_group()).set(bit_index, new_state);
    cached_bitmap.dirty = true;

    // Update superblock
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: superblock free inode count %u -> %u\n", m_super_block.s_free_inodes_count, m_super_block.s_free_inodes_count - 1);
#endif
    if (new_state)
        --m_super_block.s_free_inodes_count;
    else
        ++m_super_block.s_free_inodes_count;
    m_super_block_dirty = true;

    // Update BGD
    auto& mutable_bgd = const_cast<ext2_group_desc&>(bgd);
    if (new_state)
        --mutable_bgd.bg_free_inodes_count;
    else
        ++mutable_bgd.bg_free_inodes_count;
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: group free inode count %u -> %u\n", bgd.bg_free_inodes_count, bgd.bg_free_inodes_count - 1);
#endif

    m_block_group_descriptors_dirty = true;
    return true;
}

Ext2FS::BlockIndex Ext2FS::first_block_index() const
{
    return block_size() == 1024 ? 1 : 0;
}

Ext2FS::CachedBitmap& Ext2FS::get_bitmap_block(BlockIndex bitmap_block_index)
{
    for (auto& cached_bitmap : m_cached_bitmaps) {
        if (cached_bitmap->bitmap_block_index == bitmap_block_index)
            return *cached_bitmap;
    }

    auto block = KBuffer::create_with_size(block_size(), Region::Access::Read | Region::Access::Write, "Ext2FS: Cached bitmap block");
    bool success = read_block(bitmap_block_index, block.data());
    ASSERT(success);
    m_cached_bitmaps.append(make<CachedBitmap>(bitmap_block_index, move(block)));
    return *m_cached_bitmaps.last();
}

bool Ext2FS::set_block_allocation_state(BlockIndex block_index, bool new_state)
{
    ASSERT(block_index != 0);
    LOCKER(m_lock);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: set_block_allocation_state(block=%u, state=%u)\n", block_index, new_state);
#endif

    GroupIndex group_index = group_index_from_block_index(block_index);
    auto& bgd = group_descriptor(group_index);
    BlockIndex index_in_group = (block_index - first_block_index()) - ((group_index - 1) * blocks_per_group());
    unsigned bit_index = index_in_group % blocks_per_group();

    auto& cached_bitmap = get_bitmap_block(bgd.bg_block_bitmap);

    bool current_state = cached_bitmap.bitmap(blocks_per_group()).get(bit_index);
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: block %u state: %u -> %u (in bitmap block %u)\n", block_index, current_state, new_state, bgd.bg_block_bitmap);
#endif

    if (current_state == new_state) {
        ASSERT_NOT_REACHED();
        return true;
    }

    cached_bitmap.bitmap(blocks_per_group()).set(bit_index, new_state);
    cached_bitmap.dirty = true;

    // Update superblock
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: superblock free block count %u -> %u\n", m_super_block.s_free_blocks_count, m_super_block.s_free_blocks_count - 1);
#endif
    if (new_state)
        --m_super_block.s_free_blocks_count;
    else
        ++m_super_block.s_free_blocks_count;
    m_super_block_dirty = true;

    // Update BGD
    auto& mutable_bgd = const_cast<ext2_group_desc&>(bgd);
    if (new_state)
        --mutable_bgd.bg_free_blocks_count;
    else
        ++mutable_bgd.bg_free_blocks_count;
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: group %u free block count %u -> %u\n", group_index, bgd.bg_free_blocks_count, bgd.bg_free_blocks_count - 1);
#endif

    m_block_group_descriptors_dirty = true;
    return true;
}

KResult Ext2FS::create_directory(InodeIdentifier parent_id, const String& name, mode_t mode, uid_t uid, gid_t gid)
{
    LOCKER(m_lock);
    ASSERT(parent_id.fsid() == fsid());

    // Fix up the mode to definitely be a directory.
    // FIXME: This is a bit on the hackish side.
    mode &= ~0170000;
    mode |= 0040000;

    // NOTE: When creating a new directory, make the size 1 block.
    //       There's probably a better strategy here, but this works for now.
    auto inode_or_error = create_inode(parent_id, name, mode, block_size(), 0, uid, gid);
    if (inode_or_error.is_error())
        return inode_or_error.error();

    auto& inode = inode_or_error.value();

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: create_directory: created new directory named '%s' with inode %u\n", name.characters(), inode->identifier().index());
#endif

    Vector<DirectoryEntry> entries;
    entries.empend(".", inode->identifier(), EXT2_FT_DIR);
    entries.empend("..", parent_id, EXT2_FT_DIR);

    bool success = static_cast<Ext2FSInode&>(*inode).write_directory(entries);
    ASSERT(success);

    auto parent_inode = get_inode(parent_id);
    auto result = parent_inode->increment_link_count();
    if (result.is_error())
        return result;

    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode->identifier().index())));
    ++bgd.bg_used_dirs_count;
#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: incremented bg_used_dirs_count %u -> %u\n", bgd.bg_used_dirs_count - 1, bgd.bg_used_dirs_count);
#endif

    m_block_group_descriptors_dirty = true;

    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> Ext2FS::create_inode(InodeIdentifier parent_id, const String& name, mode_t mode, off_t size, dev_t dev, uid_t uid, gid_t gid)
{
    LOCKER(m_lock);
    ASSERT(parent_id.fsid() == fsid());
    auto parent_inode = get_inode(parent_id);
    ASSERT(parent_inode);

    if (static_cast<const Ext2FSInode&>(*parent_inode).m_raw_inode.i_links_count == 0)
        return KResult(-ENOENT);

#ifdef EXT2_DEBUG
    dbgprintf("Ext2FS: Adding inode '%s' (mode %o) to parent directory %u:\n", name.characters(), mode, parent_inode->identifier().index());
#endif

    auto needed_blocks = ceil_div(size, block_size());
    if ((size_t)needed_blocks > super_block().s_free_blocks_count) {
        dbg() << "Ext2FS: create_inode: not enough free blocks";
        return KResult(-ENOSPC);
    }

    // NOTE: This doesn't commit the inode allocation just yet!
    auto inode_id = find_a_free_inode(0, size);
    if (!inode_id) {
        kprintf("Ext2FS: create_inode: allocate_inode failed\n");
        return KResult(-ENOSPC);
    }

    // Try adding it to the directory first, in case the name is already in use.
    auto result = parent_inode->add_child({ fsid(), inode_id }, name, mode);
    if (result.is_error())
        return result;

    auto blocks = allocate_blocks(group_index_from_inode(inode_id), needed_blocks);
    ASSERT(blocks.size() == needed_blocks);

    // Looks like we're good, time to update the inode bitmap and group+global inode counters.
    bool success = set_inode_allocation_state(inode_id, true);
    ASSERT(success);

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
    e2inode.i_uid = uid;
    e2inode.i_gid = gid;
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

    auto inode = get_inode({ fsid(), inode_id });
    // If we've already computed a block list, no sense in throwing it away.
    static_cast<Ext2FSInode&>(*inode).m_block_list = move(blocks);
    return inode.release_nonnull();
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

RefPtr<Inode> Ext2FSInode::lookup(StringView name)
{
    ASSERT(is_directory());
    populate_lookup_cache();
    LOCKER(m_lock);
    auto it = m_lookup_cache.find(name.hash(), [&](auto& entry) { return entry.key == name; });
    if (it != m_lookup_cache.end())
        return fs().get_inode({ fsid(), (*it).value });
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

KResult Ext2FSInode::increment_link_count()
{
    LOCKER(m_lock);
    if (fs().is_readonly())
        return KResult(-EROFS);
    if (m_raw_inode.i_links_count == max_link_count)
        return KResult(-EMLINK);
    ++m_raw_inode.i_links_count;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::decrement_link_count()
{
    LOCKER(m_lock);
    if (fs().is_readonly())
        return KResult(-EROFS);
    ASSERT(m_raw_inode.i_links_count);
    --m_raw_inode.i_links_count;
    if (ref_count() == 1 && m_raw_inode.i_links_count == 0)
        fs().uncache_inode(index());
    set_metadata_dirty(true);
    return KSuccess;
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

KResult Ext2FSInode::truncate(u64 size)
{
    LOCKER(m_lock);
    if (static_cast<u64>(m_raw_inode.i_size) == size)
        return KSuccess;
    auto result = resize(size);
    if (result.is_error())
        return result;
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

KResult Ext2FS::prepare_to_unmount() const
{
    LOCKER(m_lock);

    for (auto& it : m_inode_cache) {
        if (it.value->ref_count() > 1)
            return KResult(-EBUSY);
    }

    m_inode_cache.clear();
    return KSuccess;
}

}
