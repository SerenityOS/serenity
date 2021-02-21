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
#include <AK/HashMap.h>
#include <AK/MemoryStream.h>
#include <AK/StdLibExtras.h>
#include <AK/StringView.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/FileSystem/Ext2FileSystem.h>
#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/FileSystem/ext2_fs.h>
#include <Kernel/Process.h>
#include <Kernel/UnixTypes.h>
#include <LibC/errno_numbers.h>

namespace Kernel {

static const size_t max_link_count = 65535;
static const size_t max_block_size = 4096;
static const ssize_t max_inline_symlink_length = 60;

struct Ext2FSDirectoryEntry {
    String name;
    InodeIndex inode_index { 0 };
    u8 file_type { 0 };
};

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

static unsigned divide_rounded_up(unsigned a, unsigned b)
{
    return (a / b) + (a % b != 0);
}

NonnullRefPtr<Ext2FS> Ext2FS::create(FileDescription& file_description)
{
    return adopt(*new Ext2FS(file_description));
}

Ext2FS::Ext2FS(FileDescription& file_description)
    : BlockBasedFS(file_description)
{
}

Ext2FS::~Ext2FS()
{
}

bool Ext2FS::flush_super_block()
{
    LOCKER(m_lock);
    ASSERT((sizeof(ext2_super_block) % logical_block_size()) == 0);
    auto super_block_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&m_super_block);
    bool success = raw_write_blocks(2, (sizeof(ext2_super_block) / logical_block_size()), super_block_buffer);
    ASSERT(success);
    return true;
}

const ext2_group_desc& Ext2FS::group_descriptor(GroupIndex group_index) const
{
    // FIXME: Should this fail gracefully somehow?
    ASSERT(group_index <= m_block_group_count);
    ASSERT(group_index > 0);
    return block_group_descriptors()[group_index.value() - 1];
}

bool Ext2FS::initialize()
{
    LOCKER(m_lock);
    ASSERT((sizeof(ext2_super_block) % logical_block_size()) == 0);
    auto super_block_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&m_super_block);
    bool success = raw_read_blocks(2, (sizeof(ext2_super_block) / logical_block_size()), super_block_buffer);
    ASSERT(success);

    auto& super_block = this->super_block();
    if constexpr (EXT2_DEBUG) {
        klog() << "ext2fs: super block magic: " << String::format("%x", super_block.s_magic) << " (super block size: " << sizeof(ext2_super_block) << ")";
    }
    if (super_block.s_magic != EXT2_SUPER_MAGIC)
        return false;

    if constexpr (EXT2_DEBUG) {
        klog() << "ext2fs: " << super_block.s_inodes_count << " inodes, " << super_block.s_blocks_count << " blocks";
        klog() << "ext2fs: block size = " << EXT2_BLOCK_SIZE(&super_block);
        klog() << "ext2fs: first data block = " << super_block.s_first_data_block;
        klog() << "ext2fs: inodes per block = " << inodes_per_block();
        klog() << "ext2fs: inodes per group = " << inodes_per_group();
        klog() << "ext2fs: free inodes = " << super_block.s_free_inodes_count;
        klog() << "ext2fs: desc per block = " << EXT2_DESC_PER_BLOCK(&super_block);
        klog() << "ext2fs: desc size = " << EXT2_DESC_SIZE(&super_block);
    }

    set_block_size(EXT2_BLOCK_SIZE(&super_block));

    ASSERT(block_size() <= (int)max_block_size);

    m_block_group_count = ceil_div(super_block.s_blocks_count, super_block.s_blocks_per_group);

    if (m_block_group_count == 0) {
        klog() << "ext2fs: no block groups :(";
        return false;
    }

    unsigned blocks_to_read = ceil_div(m_block_group_count * sizeof(ext2_group_desc), block_size());
    BlockIndex first_block_of_bgdt = block_size() == 1024 ? 2 : 1;
    m_cached_group_descriptor_table = KBuffer::try_create_with_size(block_size() * blocks_to_read, Region::Access::Read | Region::Access::Write, "Ext2FS: Block group descriptors");
    if (!m_cached_group_descriptor_table) {
        dbgln("Ext2FS: Failed to allocate memory for group descriptor table");
        return false;
    }
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(m_cached_group_descriptor_table->data());
    auto result = read_blocks(first_block_of_bgdt, blocks_to_read, buffer);
    if (result.is_error()) {
        // FIXME: Propagate the error
        dbgln("Ext2FS: initialize had error: {}", result.error());
        return false;
    }

    if constexpr (EXT2_DEBUG) {
        for (unsigned i = 1; i <= m_block_group_count; ++i) {
            auto& group = group_descriptor(i);
            klog() << "ext2fs: group[" << i << "] { block_bitmap: " << group.bg_block_bitmap << ", inode_bitmap: " << group.bg_inode_bitmap << ", inode_table: " << group.bg_inode_table << " }";
        }
    }

    return true;
}

const char* Ext2FS::class_name() const
{
    return "Ext2FS";
}

NonnullRefPtr<Inode> Ext2FS::root_inode() const
{
    return *get_inode({ fsid(), EXT2_ROOT_INO });
}

bool Ext2FS::find_block_containing_inode(InodeIndex inode, BlockIndex& block_index, unsigned& offset) const
{
    LOCKER(m_lock);
    auto& super_block = this->super_block();

    if (inode != EXT2_ROOT_INO && inode < EXT2_FIRST_INO(&super_block))
        return false;

    if (inode > super_block.s_inodes_count)
        return false;

    auto& bgd = group_descriptor(group_index_from_inode(inode));

    offset = ((inode.value() - 1) % inodes_per_group()) * inode_size();
    block_index = bgd.bg_inode_table + (offset >> EXT2_BLOCK_SIZE_BITS(&super_block));
    offset &= block_size() - 1;

    return true;
}

Ext2FS::BlockListShape Ext2FS::compute_block_list_shape(unsigned blocks) const
{
    BlockListShape shape;
    const unsigned entries_per_block = EXT2_ADDR_PER_BLOCK(&super_block());
    unsigned blocks_remaining = blocks;

    shape.direct_blocks = min((unsigned)EXT2_NDIR_BLOCKS, blocks_remaining);
    blocks_remaining -= shape.direct_blocks;
    if (!blocks_remaining)
        return shape;

    shape.indirect_blocks = min(blocks_remaining, entries_per_block);
    shape.meta_blocks += 1;
    blocks_remaining -= shape.indirect_blocks;
    if (!blocks_remaining)
        return shape;

    shape.doubly_indirect_blocks = min(blocks_remaining, entries_per_block * entries_per_block);
    shape.meta_blocks += 1;
    shape.meta_blocks += divide_rounded_up(shape.doubly_indirect_blocks, entries_per_block);
    blocks_remaining -= shape.doubly_indirect_blocks;
    if (!blocks_remaining)
        return shape;

    shape.triply_indirect_blocks = min(blocks_remaining, entries_per_block * entries_per_block * entries_per_block);
    shape.meta_blocks += 1;
    shape.meta_blocks += divide_rounded_up(shape.triply_indirect_blocks, entries_per_block * entries_per_block);
    shape.meta_blocks += divide_rounded_up(shape.triply_indirect_blocks, entries_per_block);
    blocks_remaining -= shape.triply_indirect_blocks;
    ASSERT(blocks_remaining == 0);
    return shape;
}

KResult Ext2FS::write_block_list_for_inode(InodeIndex inode_index, ext2_inode& e2inode, const Vector<BlockIndex>& blocks)
{
    LOCKER(m_lock);

    if (blocks.is_empty()) {
        e2inode.i_blocks = 0;
        memset(e2inode.i_block, 0, sizeof(e2inode.i_block));
        write_ext2_inode(inode_index, e2inode);
        return KSuccess;
    }

    // NOTE: There is a mismatch between i_blocks and blocks.size() since i_blocks includes meta blocks and blocks.size() does not.
    auto old_block_count = ceil_div(static_cast<size_t>(e2inode.i_size), block_size());

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
        e2inode.i_block[i] = blocks[output_block_index].value();
        ++output_block_index;
        --remaining_blocks;
    }
    if (inode_dirty) {
        if constexpr (EXT2_DEBUG) {
            dbgln("Ext2FS: Writing {} direct block(s) to i_block array of inode {}", min((size_t)EXT2_NDIR_BLOCKS, blocks.size()), inode_index);
            for (size_t i = 0; i < min((size_t)EXT2_NDIR_BLOCKS, blocks.size()); ++i)
                dbgln("   + {}", blocks[i]);
        }
        write_ext2_inode(inode_index, e2inode);
        inode_dirty = false;
    }

    if (!remaining_blocks)
        return KSuccess;

    const unsigned entries_per_block = EXT2_ADDR_PER_BLOCK(&super_block());

    bool ind_block_new = !e2inode.i_block[EXT2_IND_BLOCK];
    if (ind_block_new) {
        BlockIndex new_indirect_block = new_meta_blocks.take_last();
        if (e2inode.i_block[EXT2_IND_BLOCK] != new_indirect_block)
            inode_dirty = true;
        e2inode.i_block[EXT2_IND_BLOCK] = new_indirect_block.value();
        if (inode_dirty) {
            dbgln_if(EXT2_DEBUG, "Ext2FS: Adding the indirect block to i_block array of inode {}", inode_index);
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
        OutputMemoryStream stream { block_contents };

        ASSERT(new_shape.indirect_blocks <= entries_per_block);
        for (unsigned i = 0; i < new_shape.indirect_blocks; ++i) {
            stream << blocks[output_block_index++].value();
            --remaining_blocks;
        }

        stream.fill_to_end(0);

        auto buffer = UserOrKernelBuffer::for_kernel_buffer(stream.data());
        auto result = write_block(e2inode.i_block[EXT2_IND_BLOCK], buffer, stream.size());
        if (result.is_error())
            return result;
    }

    if (!remaining_blocks)
        return KSuccess;

    bool dind_block_dirty = false;

    bool dind_block_new = !e2inode.i_block[EXT2_DIND_BLOCK];
    if (dind_block_new) {
        BlockIndex new_dindirect_block = new_meta_blocks.take_last();
        if (e2inode.i_block[EXT2_DIND_BLOCK] != new_dindirect_block)
            inode_dirty = true;
        e2inode.i_block[EXT2_DIND_BLOCK] = new_dindirect_block.value();
        if (inode_dirty) {
            dbgln_if(EXT2_DEBUG, "Ext2FS: Adding the doubly-indirect block to i_block array of inode {}", inode_index);
            write_ext2_inode(inode_index, e2inode);
            inode_dirty = false;
        }
    }

    if (old_shape.doubly_indirect_blocks == new_shape.doubly_indirect_blocks) {
        // No need to update the doubly indirect block data.
        remaining_blocks -= new_shape.doubly_indirect_blocks;
        output_block_index += new_shape.doubly_indirect_blocks;
    } else {
        unsigned indirect_block_count = divide_rounded_up(new_shape.doubly_indirect_blocks, entries_per_block);

        auto dind_block_contents = ByteBuffer::create_uninitialized(block_size());
        if (dind_block_new) {
            dind_block_contents.zero_fill();
            dind_block_dirty = true;
        } else {
            auto buffer = UserOrKernelBuffer::for_kernel_buffer(dind_block_contents.data());
            auto result = read_block(e2inode.i_block[EXT2_DIND_BLOCK], &buffer, block_size());
            if (result.is_error()) {
                dbgln("Ext2FS: write_block_list_for_inode had error: {}", result.error());
                return result;
            }
        }
        auto* dind_block_as_pointers = (unsigned*)dind_block_contents.data();

        ASSERT(indirect_block_count <= entries_per_block);
        for (unsigned i = 0; i < indirect_block_count; ++i) {
            bool ind_block_dirty = false;

            BlockIndex indirect_block_index = dind_block_as_pointers[i];

            bool ind_block_new = !indirect_block_index;
            if (ind_block_new) {
                indirect_block_index = new_meta_blocks.take_last();
                dind_block_as_pointers[i] = indirect_block_index.value();
                dind_block_dirty = true;
            }

            auto ind_block_contents = ByteBuffer::create_uninitialized(block_size());
            if (ind_block_new) {
                ind_block_contents.zero_fill();
                ind_block_dirty = true;
            } else {
                auto buffer = UserOrKernelBuffer::for_kernel_buffer(ind_block_contents.data());
                auto result = read_block(indirect_block_index, &buffer, block_size());
                if (result.is_error()) {
                    dbgln("Ext2FS: write_block_list_for_inode had error: {}", result.error());
                    return result;
                }
            }
            auto* ind_block_as_pointers = (unsigned*)ind_block_contents.data();

            unsigned entries_to_write = new_shape.doubly_indirect_blocks - (i * entries_per_block);
            if (entries_to_write > entries_per_block)
                entries_to_write = entries_per_block;

            ASSERT(entries_to_write <= entries_per_block);
            for (unsigned j = 0; j < entries_to_write; ++j) {
                BlockIndex output_block = blocks[output_block_index++];
                if (ind_block_as_pointers[j] != output_block) {
                    ind_block_as_pointers[j] = output_block.value();
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
                auto buffer = UserOrKernelBuffer::for_kernel_buffer(ind_block_contents.data());
                int err = write_block(indirect_block_index, buffer, block_size());
                ASSERT(err >= 0);
            }
        }
        for (unsigned i = indirect_block_count; i < entries_per_block; ++i) {
            if (dind_block_as_pointers[i] != 0) {
                dind_block_as_pointers[i] = 0;
                dind_block_dirty = true;
            }
        }

        if (dind_block_dirty) {
            auto buffer = UserOrKernelBuffer::for_kernel_buffer(dind_block_contents.data());
            int err = write_block(e2inode.i_block[EXT2_DIND_BLOCK], buffer, block_size());
            ASSERT(err >= 0);
        }
    }

    if (!remaining_blocks)
        return KSuccess;

    // FIXME: Implement!
    dbgln("we don't know how to write tind ext2fs blocks yet!");
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

    unsigned block_count = ceil_div(static_cast<size_t>(e2inode.i_size), block_size());

    // If we are handling a symbolic link, the path is stored in the 60 bytes in
    // the inode that are used for the 12 direct and 3 indirect block pointers,
    // If the path is longer than 60 characters, a block is allocated, and the
    // block contains the destination path. The file size corresponds to the
    // path length of the destination.
    if (is_symlink(e2inode.i_mode) && e2inode.i_blocks == 0)
        block_count = 0;

    dbgln_if(EXT2_DEBUG, "Ext2FS::block_list_for_inode(): i_size={}, i_blocks={}, block_count={}", e2inode.i_size, e2inode.i_blocks, block_count);

    unsigned blocks_remaining = block_count;

    if (include_block_list_blocks) {
        auto shape = compute_block_list_shape(block_count);
        blocks_remaining += shape.meta_blocks;
    }

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

    // Don't need to make copy of add_block, since this capture will only
    // be called before block_list_for_inode_impl finishes.
    auto process_block_array = [&](BlockIndex array_block_index, auto&& callback) {
        if (include_block_list_blocks)
            add_block(array_block_index);
        auto count = min(blocks_remaining, entries_per_block);
        if (!count)
            return;
        u32 array[count];
        auto buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)array);
        auto result = read_block(array_block_index, &buffer, sizeof(array), 0);
        if (result.is_error()) {
            // FIXME: Stop here and propagate this error.
            dbgln("Ext2FS: block_list_for_inode_impl had error: {}", result.error());
        }
        for (unsigned i = 0; i < count; ++i)
            callback(BlockIndex(array[i]));
    };

    process_block_array(e2inode.i_block[EXT2_IND_BLOCK], [&](BlockIndex block_index) {
        add_block(block_index);
    });

    if (!blocks_remaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_DIND_BLOCK], [&](BlockIndex block_index) {
        process_block_array(block_index, [&](BlockIndex block_index2) {
            add_block(block_index2);
        });
    });

    if (!blocks_remaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_TIND_BLOCK], [&](BlockIndex block_index) {
        process_block_array(block_index, [&](BlockIndex block_index2) {
            process_block_array(block_index2, [&](BlockIndex block_index3) {
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
    dbgln_if(EXT2_DEBUG, "Ext2FS: Inode {} has no more links, time to delete!", inode.index());

    // Mark all blocks used by this inode as free.
    auto block_list = block_list_for_inode(inode.m_raw_inode, true);
    for (auto block_index : block_list) {
        ASSERT(block_index <= super_block().s_blocks_count);
        if (block_index.value())
            set_block_allocation_state(block_index, false);
    }

    // If the inode being freed is a directory, update block group directory counter.
    if (inode.is_directory()) {
        auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode.index())));
        --bgd.bg_used_dirs_count;
        dbgln("Ext2FS: Decremented bg_used_dirs_count to {}", bgd.bg_used_dirs_count);
        m_block_group_descriptors_dirty = true;
    }

    // NOTE: After this point, the inode metadata is wiped.
    memset(&inode.m_raw_inode, 0, sizeof(ext2_inode));
    inode.m_raw_inode.i_dtime = kgettimeofday().tv_sec;
    write_ext2_inode(inode.index(), inode.m_raw_inode);

    // Mark the inode as free.
    set_inode_allocation_state(inode.index(), false);
}

void Ext2FS::flush_block_group_descriptor_table()
{
    LOCKER(m_lock);
    unsigned blocks_to_write = ceil_div(m_block_group_count * sizeof(ext2_group_desc), block_size());
    unsigned first_block_of_bgdt = block_size() == 1024 ? 2 : 1;
    auto buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)block_group_descriptors());
    auto result = write_blocks(first_block_of_bgdt, blocks_to_write, buffer);
    if (result.is_error())
        dbgln("Ext2FS: flush_block_group_descriptor_table had error: {}", result.error());
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
            auto buffer = UserOrKernelBuffer::for_kernel_buffer(cached_bitmap->buffer.data());
            auto result = write_block(cached_bitmap->bitmap_block_index, buffer, block_size());
            if (result.is_error()) {
                dbgln("Ext2FS: flush_writes() had error {}", result.error());
            }
            cached_bitmap->dirty = false;
            dbgln_if(EXT2_DEBUG, "Flushed bitmap block {}", cached_bitmap->bitmap_block_index);
        }
    }

    BlockBasedFS::flush_writes();

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

Ext2FSInode::Ext2FSInode(Ext2FS& fs, InodeIndex index)
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
    dbgln_if(EXT2_DEBUG, "Ext2FS: flush_metadata for inode {}", index());
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

    BlockIndex block_index;
    unsigned offset;
    if (!find_block_containing_inode(inode.index(), block_index, offset))
        return {};

    auto new_inode = adopt(*new Ext2FSInode(const_cast<Ext2FS&>(*this), inode.index()));
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&new_inode->m_raw_inode));
    auto result = read_block(block_index, &buffer, sizeof(ext2_inode), offset);
    if (result.is_error()) {
        // FIXME: Propagate the actual error.
        return nullptr;
    }
    m_inode_cache.set(inode.index(), new_inode);
    return new_inode;
}

ssize_t Ext2FSInode::read_bytes(off_t offset, ssize_t count, UserOrKernelBuffer& buffer, FileDescription* description) const
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
        if (!buffer.write(((const u8*)m_raw_inode.i_block) + offset, (size_t)nread))
            return -EFAULT;
        return nread;
    }

    Locker fs_locker(fs().m_lock);

    if (m_block_list.is_empty())
        m_block_list = fs().block_list_for_inode(m_raw_inode);

    if (m_block_list.is_empty()) {
        dmesgln("Ext2FS: read_bytes: empty block list for inode {}", index());
        return -EIO;
    }

    bool allow_cache = !description || !description->is_direct();

    const int block_size = fs().block_size();

    size_t first_block_logical_index = offset / block_size;
    size_t last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    int offset_into_first_block = offset % block_size;

    ssize_t nread = 0;
    size_t remaining_count = min((off_t)count, (off_t)size() - offset);

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FS: Reading up to {} bytes, {} bytes into inode {} to {}", count, offset, index(), buffer.user_or_kernel_ptr());

    for (size_t bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; ++bi) {
        auto block_index = m_block_list[bi];
        ASSERT(block_index.value());
        size_t offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        size_t num_bytes_to_copy = min(block_size - offset_into_block, remaining_count);
        auto buffer_offset = buffer.offset(nread);
        int err = fs().read_block(block_index, &buffer_offset, num_bytes_to_copy, offset_into_block, allow_cache);
        if (err < 0) {
            dmesgln("Ext2FS: read_bytes: read_block({}) failed (bi: {})", block_index.value(), bi);
            return err;
        }
        remaining_count -= num_bytes_to_copy;
        nread += num_bytes_to_copy;
    }

    return nread;
}

KResult Ext2FSInode::resize(u64 new_size)
{
    u64 old_size = size();
    if (old_size == new_size)
        return KSuccess;

    u64 block_size = fs().block_size();
    size_t blocks_needed_before = ceil_div(old_size, block_size);
    size_t blocks_needed_after = ceil_div(new_size, block_size);

    if constexpr (EXT2_DEBUG) {
        dbgln("Ext2FSInode::resize(): blocks needed before (size was {}): {}", old_size, blocks_needed_before);
        dbgln("Ext2FSInode::resize(): blocks needed after  (size is  {}): {}", new_size, blocks_needed_after);
    }

    if (blocks_needed_after > blocks_needed_before) {
        u32 additional_blocks_needed = blocks_needed_after - blocks_needed_before;
        if (additional_blocks_needed > fs().super_block().s_free_blocks_count)
            return ENOSPC;
    }

    Vector<Ext2FS::BlockIndex> block_list;
    if (!m_block_list.is_empty())
        block_list = m_block_list;
    else
        block_list = fs().block_list_for_inode(m_raw_inode);

    if (blocks_needed_after > blocks_needed_before) {
        auto new_blocks = fs().allocate_blocks(fs().group_index_from_inode(index()), blocks_needed_after - blocks_needed_before);
        block_list.append(move(new_blocks));
    } else if (blocks_needed_after < blocks_needed_before) {
        if constexpr (EXT2_DEBUG) {
            dbgln("Ext2FS: Shrinking inode {}. Old block list is {} entries:", index(), block_list.size());
            for (auto block_index : block_list) {
                dbgln("    # {}", block_index);
            }
        }
        while (block_list.size() != blocks_needed_after) {
            auto block_index = block_list.take_last();
            if (block_index.value())
                fs().set_block_allocation_state(block_index, false);
        }
    }

    auto result = fs().write_block_list_for_inode(index(), m_raw_inode, block_list);
    if (result.is_error())
        return result;

    m_raw_inode.i_size = new_size;
    set_metadata_dirty(true);

    m_block_list = move(block_list);

    if (new_size > old_size) {
        // If we're growing the inode, make sure we zero out all the new space.
        // FIXME: There are definitely more efficient ways to achieve this.
        size_t bytes_to_clear = new_size - old_size;
        size_t clear_from = old_size;
        u8 zero_buffer[PAGE_SIZE];
        memset(zero_buffer, 0, sizeof(zero_buffer));
        while (bytes_to_clear) {
            auto nwritten = write_bytes(clear_from, min(sizeof(zero_buffer), bytes_to_clear), UserOrKernelBuffer::for_kernel_buffer(zero_buffer), nullptr);
            if (nwritten < 0)
                return KResult((ErrnoCode)-nwritten);
            ASSERT(nwritten != 0);
            bytes_to_clear -= nwritten;
            clear_from += nwritten;
        }
    }

    return KSuccess;
}

ssize_t Ext2FSInode::write_bytes(off_t offset, ssize_t count, const UserOrKernelBuffer& data, FileDescription* description)
{
    ASSERT(offset >= 0);
    ASSERT(count >= 0);

    Locker inode_locker(m_lock);
    Locker fs_locker(fs().m_lock);

    auto result = prepare_to_write_data();
    if (result.is_error())
        return result;

    if (is_symlink()) {
        ASSERT(offset == 0);
        if (max((size_t)(offset + count), (size_t)m_raw_inode.i_size) < max_inline_symlink_length) {
            dbgln_if(EXT2_DEBUG, "Ext2FS: write_bytes poking into i_block array for inline symlink '{}' ({} bytes)", data.copy_into_string(count), count);
            if (!data.read(((u8*)m_raw_inode.i_block) + offset, (size_t)count))
                return -EFAULT;
            if ((size_t)(offset + count) > (size_t)m_raw_inode.i_size)
                m_raw_inode.i_size = offset + count;
            set_metadata_dirty(true);
            return count;
        }
    }

    bool allow_cache = !description || !description->is_direct();

    const size_t block_size = fs().block_size();
    u64 old_size = size();
    u64 new_size = max(static_cast<u64>(offset) + count, (u64)size());

    auto resize_result = resize(new_size);
    if (resize_result.is_error())
        return resize_result;

    if (m_block_list.is_empty())
        m_block_list = fs().block_list_for_inode(m_raw_inode);

    if (m_block_list.is_empty()) {
        dbgln("Ext2FSInode::write_bytes(): empty block list for inode {}", index());
        return -EIO;
    }

    size_t first_block_logical_index = offset / block_size;
    size_t last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    size_t offset_into_first_block = offset % block_size;

    ssize_t nwritten = 0;
    size_t remaining_count = min((off_t)count, (off_t)new_size - offset);

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FS: Writing {} bytes, {} bytes into inode {} from {}", count, offset, index(), data.user_or_kernel_ptr());

    for (size_t bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; ++bi) {
        size_t offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        size_t num_bytes_to_copy = min(block_size - offset_into_block, remaining_count);
        dbgln_if(EXT2_DEBUG, "Ext2FS: Writing block {} (offset_into_block: {})", m_block_list[bi], offset_into_block);
        result = fs().write_block(m_block_list[bi], data.offset(nwritten), num_bytes_to_copy, offset_into_block, allow_cache);
        if (result.is_error()) {
            dbgln("Ext2FS: write_block({}) failed (bi: {})", m_block_list[bi], bi);
            return result;
        }
        remaining_count -= num_bytes_to_copy;
        nwritten += num_bytes_to_copy;
    }

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FS: After write, i_size={}, i_blocks={} ({} blocks in list)", m_raw_inode.i_size, m_raw_inode.i_blocks, m_block_list.size());

    if (old_size != new_size)
        inode_size_changed(old_size, new_size);
    inode_contents_changed(offset, count, data);
    return nwritten;
}

u8 Ext2FS::internal_file_type_to_directory_entry_type(const DirectoryEntryView& entry) const
{
    switch (entry.file_type) {
    case EXT2_FT_REG_FILE:
        return DT_REG;
    case EXT2_FT_DIR:
        return DT_DIR;
    case EXT2_FT_CHRDEV:
        return DT_CHR;
    case EXT2_FT_BLKDEV:
        return DT_BLK;
    case EXT2_FT_FIFO:
        return DT_FIFO;
    case EXT2_FT_SOCK:
        return DT_SOCK;
    case EXT2_FT_SYMLINK:
        return DT_LNK;
    default:
        return DT_UNKNOWN;
    }
}

KResult Ext2FSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    LOCKER(m_lock);
    ASSERT(is_directory());

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FS: Traversing as directory: {}", index());

    auto buffer_or = read_entire();
    if (buffer_or.is_error())
        return buffer_or.error();

    auto& buffer = *buffer_or.value();
    auto* entry = reinterpret_cast<ext2_dir_entry_2*>(buffer.data());

    while (entry < buffer.end_pointer()) {
        if (entry->inode != 0) {
            dbgln_if(EXT2_DEBUG, "Ext2Inode::traverse_as_directory: {}, name_len: {}, rec_len: {}, file_type: {}, name: {}", entry->inode, entry->name_len, entry->rec_len, entry->file_type, StringView(entry->name, entry->name_len));
            if (!callback({ { entry->name, entry->name_len }, { fsid(), entry->inode }, entry->file_type }))
                break;
        }
        entry = (ext2_dir_entry_2*)((char*)entry + entry->rec_len);
    }

    return KSuccess;
}

KResult Ext2FSInode::write_directory(const Vector<Ext2FSDirectoryEntry>& entries)
{
    LOCKER(m_lock);

    int directory_size = 0;
    for (auto& entry : entries)
        directory_size += EXT2_DIR_REC_LEN(entry.name.length());

    auto block_size = fs().block_size();

    int blocks_needed = ceil_div(static_cast<size_t>(directory_size), block_size);
    int occupied_size = blocks_needed * block_size;

    dbgln_if(EXT2_DEBUG, "Ext2FS: New directory inode {} contents to write (size {}, occupied {}):", index(), directory_size, occupied_size);

    auto directory_data = ByteBuffer::create_uninitialized(occupied_size);
    OutputMemoryStream stream { directory_data };

    for (size_t i = 0; i < entries.size(); ++i) {
        auto& entry = entries[i];

        int record_length = EXT2_DIR_REC_LEN(entry.name.length());
        if (i == entries.size() - 1)
            record_length += occupied_size - directory_size;

        dbgln_if(EXT2_DEBUG, "* Inode: {}, name_len: {}, rec_len: {}, file_type: {}, name: {}", entry.inode_index, u16(entry.name.length()), u16(record_length), u8(entry.file_type), entry.name);

        stream << u32(entry.inode_index.value());
        stream << u16(record_length);
        stream << u8(entry.name.length());
        stream << u8(entry.file_type);
        stream << entry.name.bytes();

        int padding = record_length - entry.name.length() - 8;
        for (int j = 0; j < padding; ++j)
            stream << u8(0);
    }

    stream.fill_to_end(0);

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(stream.data());
    ssize_t nwritten = write_bytes(0, stream.size(), buffer, nullptr);
    if (nwritten < 0)
        return KResult((ErrnoCode)-nwritten);
    set_metadata_dirty(true);
    if (static_cast<size_t>(nwritten) != directory_data.size())
        return EIO;
    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> Ext2FSInode::create_child(const String& name, mode_t mode, dev_t dev, uid_t uid, gid_t gid)
{
    if (::is_directory(mode))
        return fs().create_directory(*this, name, mode, uid, gid);
    return fs().create_inode(*this, name, mode, dev, uid, gid);
}

KResult Ext2FSInode::add_child(Inode& child, const StringView& name, mode_t mode)
{
    LOCKER(m_lock);
    ASSERT(is_directory());

    if (name.length() > EXT2_NAME_LEN)
        return ENAMETOOLONG;

    dbgln_if(EXT2_DEBUG, "Ext2FSInode::add_child: Adding inode {} with name '{}' and mode {:o} to directory {}", child.index(), name, mode, index());

    Vector<Ext2FSDirectoryEntry> entries;
    bool name_already_exists = false;
    KResult result = traverse_as_directory([&](auto& entry) {
        if (name == entry.name) {
            name_already_exists = true;
            return false;
        }
        entries.append({ entry.name, entry.inode.index(), entry.file_type });
        return true;
    });

    if (result.is_error())
        return result;

    if (name_already_exists) {
        dbgln("Ext2FSInode::add_child: Name '{}' already exists in inode {}", name, index());
        return EEXIST;
    }

    result = child.increment_link_count();
    if (result.is_error())
        return result;

    entries.empend(name, child.index(), to_ext2_file_type(mode));
    result = write_directory(entries);
    if (result.is_error())
        return result;

    m_lookup_cache.set(name, child.index());
    did_add_child(child.identifier());
    return KSuccess;
}

KResult Ext2FSInode::remove_child(const StringView& name)
{
    LOCKER(m_lock);
    dbgln_if(EXT2_DEBUG, "Ext2FSInode::remove_child('{}') in inode {}", name, index());
    ASSERT(is_directory());

    auto it = m_lookup_cache.find(name);
    if (it == m_lookup_cache.end())
        return ENOENT;
    auto child_inode_index = (*it).value;

    InodeIdentifier child_id { fsid(), child_inode_index };

    dbgln_if(EXT2_DEBUG, "Ext2FSInode::remove_child(): Removing '{}' in directory {}", name, index());

    Vector<Ext2FSDirectoryEntry> entries;
    KResult result = traverse_as_directory([&](auto& entry) {
        if (name != entry.name)
            entries.append({ entry.name, entry.inode.index(), entry.file_type });
        return true;
    });
    if (result.is_error())
        return result;

    result = write_directory(entries);
    if (result.is_error())
        return result;

    m_lookup_cache.remove(name);

    auto child_inode = fs().get_inode(child_id);
    result = child_inode->decrement_link_count();
    if (result.is_error())
        return result;

    did_remove_child(child_id);
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

bool Ext2FS::write_ext2_inode(InodeIndex inode, const ext2_inode& e2inode)
{
    LOCKER(m_lock);
    BlockIndex block_index;
    unsigned offset;
    if (!find_block_containing_inode(inode, block_index, offset))
        return false;
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>((const u8*)&e2inode));
    return write_block(block_index, buffer, inode_size(), offset) >= 0;
}

auto Ext2FS::allocate_blocks(GroupIndex preferred_group_index, size_t count) -> Vector<BlockIndex>
{
    LOCKER(m_lock);
    dbgln_if(EXT2_DEBUG, "Ext2FS: allocate_blocks(preferred group: {}, count {})", preferred_group_index, count);
    if (count == 0)
        return {};

    Vector<BlockIndex> blocks;
    dbgln_if(EXT2_DEBUG, "Ext2FS: allocate_blocks:");
    blocks.ensure_capacity(count);

    auto group_index = preferred_group_index;

    if (!group_descriptor(preferred_group_index).bg_free_blocks_count) {
        group_index = 1;
    }

    while (blocks.size() < count) {

        bool found_a_group = false;
        if (group_descriptor(group_index).bg_free_blocks_count) {
            found_a_group = true;
        } else {
            if (group_index == preferred_group_index)
                group_index = 1;
            for (; group_index <= m_block_group_count; group_index = GroupIndex { group_index.value() + 1 }) {
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

        BlockIndex first_block_in_group = (group_index.value() - 1) * blocks_per_group() + first_block_index().value();
        size_t free_region_size = 0;
        auto first_unset_bit_index = block_bitmap.find_longest_range_of_unset_bits(count - blocks.size(), free_region_size);
        ASSERT(first_unset_bit_index.has_value());
        dbgln_if(EXT2_DEBUG, "Ext2FS: allocating free region of size: {} [{}]", free_region_size, group_index);
        for (size_t i = 0; i < free_region_size; ++i) {
            BlockIndex block_index = (first_unset_bit_index.value() + i) + first_block_in_group.value();
            set_block_allocation_state(block_index, true);
            blocks.unchecked_append(block_index);
            dbgln_if(EXT2_DEBUG, "  allocated > {}", block_index);
        }
    }

    ASSERT(blocks.size() == count);
    return blocks;
}

InodeIndex Ext2FS::find_a_free_inode(GroupIndex preferred_group)
{
    LOCKER(m_lock);
    dbgln_if(EXT2_DEBUG, "Ext2FS: find_a_free_inode(preferred_group: {})", preferred_group);

    GroupIndex group_index;

    // FIXME: We shouldn't refuse to allocate an inode if there is no group that can house the whole thing.
    //        In those cases we should just spread it across multiple groups.
    auto is_suitable_group = [this](auto group_index) {
        auto& bgd = group_descriptor(group_index);
        return bgd.bg_free_inodes_count && bgd.bg_free_blocks_count >= 1;
    };

    if (preferred_group.value() && is_suitable_group(preferred_group)) {
        group_index = preferred_group;
    } else {
        for (unsigned i = 1; i <= m_block_group_count; ++i) {
            if (is_suitable_group(i))
                group_index = i;
        }
    }

    if (!group_index) {
        dmesgln("Ext2FS: find_a_free_inode: no suitable group found for new inode");
        return 0;
    }

    dbgln_if(EXT2_DEBUG, "Ext2FS: find_a_free_inode: found suitable group [{}] for new inode :^)", group_index);

    auto& bgd = group_descriptor(group_index);
    unsigned inodes_in_group = min(inodes_per_group(), super_block().s_inodes_count);
    InodeIndex first_free_inode_in_group = 0;

    InodeIndex first_inode_in_group = (group_index.value() - 1) * inodes_per_group() + 1;

    auto& cached_bitmap = get_bitmap_block(bgd.bg_inode_bitmap);
    auto inode_bitmap = Bitmap::wrap(cached_bitmap.buffer.data(), inodes_in_group);
    for (size_t i = 0; i < inode_bitmap.size(); ++i) {
        if (inode_bitmap.get(i))
            continue;
        first_free_inode_in_group = InodeIndex(first_inode_in_group.value() + i);
        break;
    }

    if (!first_free_inode_in_group) {
        klog() << "Ext2FS: first_free_inode_in_group returned no inode, despite bgd claiming there are inodes :(";
        return 0;
    }

    InodeIndex inode = first_free_inode_in_group;
    dbgln_if(EXT2_DEBUG, "Ext2FS: found suitable inode {}", inode);

    ASSERT(get_inode_allocation_state(inode) == false);
    return inode;
}

Ext2FS::GroupIndex Ext2FS::group_index_from_block_index(BlockIndex block_index) const
{
    if (!block_index)
        return 0;
    return (block_index.value() - 1) / blocks_per_group() + 1;
}

auto Ext2FS::group_index_from_inode(InodeIndex inode) const -> GroupIndex
{
    if (!inode)
        return 0;
    return (inode.value() - 1) / inodes_per_group() + 1;
}

bool Ext2FS::get_inode_allocation_state(InodeIndex index) const
{
    LOCKER(m_lock);
    if (index == 0)
        return true;
    auto group_index = group_index_from_inode(index);
    auto& bgd = group_descriptor(group_index);
    unsigned index_in_group = index.value() - ((group_index.value() - 1) * inodes_per_group());
    unsigned bit_index = (index_in_group - 1) % inodes_per_group();

    auto& cached_bitmap = const_cast<Ext2FS&>(*this).get_bitmap_block(bgd.bg_inode_bitmap);
    return cached_bitmap.bitmap(inodes_per_group()).get(bit_index);
}

bool Ext2FS::set_inode_allocation_state(InodeIndex inode_index, bool new_state)
{
    LOCKER(m_lock);
    auto group_index = group_index_from_inode(inode_index);
    auto& bgd = group_descriptor(group_index);
    unsigned index_in_group = inode_index.value() - ((group_index.value() - 1) * inodes_per_group());
    unsigned bit_index = (index_in_group - 1) % inodes_per_group();

    auto& cached_bitmap = get_bitmap_block(bgd.bg_inode_bitmap);

    bool current_state = cached_bitmap.bitmap(inodes_per_group()).get(bit_index);
    dbgln_if(EXT2_DEBUG, "Ext2FS: set_inode_allocation_state({}) {} -> {}", inode_index, current_state, new_state);

    if (current_state == new_state) {
        ASSERT_NOT_REACHED();
        return true;
    }

    cached_bitmap.bitmap(inodes_per_group()).set(bit_index, new_state);
    cached_bitmap.dirty = true;

    // Update superblock
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
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(block.data());
    int err = read_block(bitmap_block_index, &buffer, block_size());
    ASSERT(err >= 0);
    m_cached_bitmaps.append(make<CachedBitmap>(bitmap_block_index, move(block)));
    return *m_cached_bitmaps.last();
}

bool Ext2FS::set_block_allocation_state(BlockIndex block_index, bool new_state)
{
    ASSERT(block_index != 0);
    LOCKER(m_lock);

    auto group_index = group_index_from_block_index(block_index);
    auto& bgd = group_descriptor(group_index);
    unsigned index_in_group = (block_index.value() - first_block_index().value()) - ((group_index.value() - 1) * blocks_per_group());
    unsigned bit_index = index_in_group % blocks_per_group();

    auto& cached_bitmap = get_bitmap_block(bgd.bg_block_bitmap);

    bool current_state = cached_bitmap.bitmap(blocks_per_group()).get(bit_index);
    dbgln_if(EXT2_DEBUG, "Ext2FS: block {} state: {} -> {} (in bitmap block {})", block_index, current_state, new_state, bgd.bg_block_bitmap);

    if (current_state == new_state) {
        ASSERT_NOT_REACHED();
        return true;
    }

    cached_bitmap.bitmap(blocks_per_group()).set(bit_index, new_state);
    cached_bitmap.dirty = true;

    // Update superblock
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

    m_block_group_descriptors_dirty = true;
    return true;
}

KResult Ext2FS::create_directory(Ext2FSInode& parent_inode, const String& name, mode_t mode, uid_t uid, gid_t gid)
{
    LOCKER(m_lock);
    ASSERT(is_directory(mode));

    auto inode_or_error = create_inode(parent_inode, name, mode, 0, uid, gid);
    if (inode_or_error.is_error())
        return inode_or_error.error();

    auto& inode = inode_or_error.value();

    dbgln_if(EXT2_DEBUG, "Ext2FS: create_directory: created new directory named '{} with inode {}", name, inode->index());

    Vector<Ext2FSDirectoryEntry> entries;
    entries.empend(".", inode->index(), static_cast<u8>(EXT2_FT_DIR));
    entries.empend("..", parent_inode.index(), static_cast<u8>(EXT2_FT_DIR));

    auto result = static_cast<Ext2FSInode&>(*inode).write_directory(entries);
    if (result.is_error())
        return result;

    result = parent_inode.increment_link_count();
    if (result.is_error())
        return result;

    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode->identifier().index())));
    ++bgd.bg_used_dirs_count;
    m_block_group_descriptors_dirty = true;

    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> Ext2FS::create_inode(Ext2FSInode& parent_inode, const String& name, mode_t mode, dev_t dev, uid_t uid, gid_t gid)
{
    LOCKER(m_lock);

    if (parent_inode.m_raw_inode.i_links_count == 0)
        return ENOENT;

    if (name.length() > EXT2_NAME_LEN)
        return ENAMETOOLONG;

    dbgln_if(EXT2_DEBUG, "Ext2FS: Adding inode '{}' (mode {:o}) to parent directory {}", name, mode, parent_inode.index());

    // NOTE: This doesn't commit the inode allocation just yet!
    auto inode_id = find_a_free_inode();
    if (!inode_id) {
        klog() << "Ext2FS: create_inode: allocate_inode failed";
        return ENOSPC;
    }

    // Looks like we're good, time to update the inode bitmap and group+global inode counters.
    bool success = set_inode_allocation_state(inode_id, true);
    ASSERT(success);

    struct timeval now;
    kgettimeofday(now);
    ext2_inode e2inode;
    memset(&e2inode, 0, sizeof(ext2_inode));
    e2inode.i_mode = mode;
    e2inode.i_uid = uid;
    e2inode.i_gid = gid;
    e2inode.i_size = 0;
    e2inode.i_atime = now.tv_sec;
    e2inode.i_ctime = now.tv_sec;
    e2inode.i_mtime = now.tv_sec;
    e2inode.i_dtime = 0;

    // For directories, add +1 link count for the "." entry in self.
    e2inode.i_links_count = is_directory(mode);

    if (is_character_device(mode))
        e2inode.i_block[0] = dev;
    else if (is_block_device(mode))
        e2inode.i_block[1] = dev;

    dbgln_if(EXT2_DEBUG, "Ext2FS: writing initial metadata for inode {}", inode_id);

    e2inode.i_flags = 0;
    success = write_ext2_inode(inode_id, e2inode);
    ASSERT(success);

    // We might have cached the fact that this inode didn't exist. Wipe the slate.
    m_inode_cache.remove(inode_id);

    auto inode = get_inode({ fsid(), inode_id });

    auto result = parent_inode.add_child(*inode, name, mode);
    if (result.is_error())
        return result;

    return inode.release_nonnull();
}

bool Ext2FSInode::populate_lookup_cache() const
{
    LOCKER(m_lock);
    if (!m_lookup_cache.is_empty())
        return true;
    HashMap<String, InodeIndex> children;

    KResult result = traverse_as_directory([&children](auto& entry) {
        children.set(entry.name, entry.inode.index());
        return true;
    });

    if (!result.is_success())
        return false;

    if (!m_lookup_cache.is_empty())
        return false;
    m_lookup_cache = move(children);
    return true;
}

RefPtr<Inode> Ext2FSInode::lookup(StringView name)
{
    ASSERT(is_directory());
    if (!populate_lookup_cache())
        return {};
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
        return EROFS;
    if (m_raw_inode.i_links_count == max_link_count)
        return EMLINK;
    ++m_raw_inode.i_links_count;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::decrement_link_count()
{
    LOCKER(m_lock);
    if (fs().is_readonly())
        return EROFS;
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

KResultOr<size_t> Ext2FSInode::directory_entry_count() const
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

KResultOr<int> Ext2FSInode::get_block_address(int index)
{
    LOCKER(m_lock);

    if (m_block_list.is_empty())
        m_block_list = fs().block_list_for_inode(m_raw_inode);

    if (index < 0 || (size_t)index >= m_block_list.size())
        return 0;

    return m_block_list[index].value();
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
            return EBUSY;
    }

    m_inode_cache.clear();
    return KSuccess;
}

}
