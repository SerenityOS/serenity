/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

static constexpr size_t max_block_size = 4096;
static constexpr ssize_t max_inline_symlink_length = 60;

struct Ext2FSDirectoryEntry {
    String name;
    InodeIndex inode_index { 0 };
    u8 file_type { 0 };
    u16 record_length { 0 };
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
    return adopt_ref(*new Ext2FS(file_description));
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
    Locker locker(m_lock);
    VERIFY((sizeof(ext2_super_block) % logical_block_size()) == 0);
    auto super_block_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&m_super_block);
    bool success = raw_write_blocks(2, (sizeof(ext2_super_block) / logical_block_size()), super_block_buffer);
    VERIFY(success);
    return true;
}

const ext2_group_desc& Ext2FS::group_descriptor(GroupIndex group_index) const
{
    // FIXME: Should this fail gracefully somehow?
    VERIFY(group_index <= m_block_group_count);
    VERIFY(group_index > 0);
    return block_group_descriptors()[group_index.value() - 1];
}

bool Ext2FS::initialize()
{
    Locker locker(m_lock);
    VERIFY((sizeof(ext2_super_block) % logical_block_size()) == 0);
    auto super_block_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&m_super_block);
    bool success = raw_read_blocks(2, (sizeof(ext2_super_block) / logical_block_size()), super_block_buffer);
    VERIFY(success);

    auto& super_block = this->super_block();
    if constexpr (EXT2_DEBUG) {
        dmesgln("Ext2FS: super block magic: {:04x} (super block size: {})", super_block.s_magic, sizeof(ext2_super_block));
    }
    if (super_block.s_magic != EXT2_SUPER_MAGIC)
        return false;

    if constexpr (EXT2_DEBUG) {
        dmesgln("Ext2FS: {} inodes, {} blocks", super_block.s_inodes_count, super_block.s_blocks_count);
        dmesgln("Ext2FS: Block size: {}", EXT2_BLOCK_SIZE(&super_block));
        dmesgln("Ext2FS: First data block: {}", super_block.s_first_data_block);
        dmesgln("Ext2FS: Inodes per block: {}", inodes_per_block());
        dmesgln("Ext2FS: Inodes per group: {}", inodes_per_group());
        dmesgln("Ext2FS: Free inodes: {}", super_block.s_free_inodes_count);
        dmesgln("Ext2FS: Descriptors per block: {}", EXT2_DESC_PER_BLOCK(&super_block));
        dmesgln("Ext2FS: Descriptor size: {}", EXT2_DESC_SIZE(&super_block));
    }

    set_block_size(EXT2_BLOCK_SIZE(&super_block));
    set_fragment_size(EXT2_FRAG_SIZE(&super_block));

    VERIFY(block_size() <= (int)max_block_size);

    m_block_group_count = ceil_div(super_block.s_blocks_count, super_block.s_blocks_per_group);

    if (m_block_group_count == 0) {
        dmesgln("Ext2FS: no block groups :(");
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
    if (auto result = read_blocks(first_block_of_bgdt, blocks_to_read, buffer); result.is_error()) {
        // FIXME: Propagate the error
        dbgln("Ext2FS: initialize had error: {}", result.error());
        return false;
    }

    if constexpr (EXT2_DEBUG) {
        for (unsigned i = 1; i <= m_block_group_count; ++i) {
            auto& group = group_descriptor(i);
            dbgln("Ext2FS: group[{}] ( block_bitmap: {}, inode_bitmap: {}, inode_table: {} )", i, group.bg_block_bitmap, group.bg_inode_bitmap, group.bg_inode_table);
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
    VERIFY(blocks_remaining == 0);
    return shape;
}

KResult Ext2FSInode::write_indirect_block(BlockBasedFS::BlockIndex block, Span<BlockBasedFS::BlockIndex> blocks_indices)
{
    const auto entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    VERIFY(blocks_indices.size() <= entries_per_block);

    auto block_contents = ByteBuffer::create_uninitialized(fs().block_size());
    OutputMemoryStream stream { block_contents };
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(stream.data());

    VERIFY(blocks_indices.size() <= EXT2_ADDR_PER_BLOCK(&fs().super_block()));
    for (unsigned i = 0; i < blocks_indices.size(); ++i)
        stream << static_cast<u32>(blocks_indices[i].value());
    stream.fill_to_end(0);

    return fs().write_block(block, buffer, stream.size());
}

KResult Ext2FSInode::grow_doubly_indirect_block(BlockBasedFS::BlockIndex block, size_t old_blocks_length, Span<BlockBasedFS::BlockIndex> blocks_indices, Vector<Ext2FS::BlockIndex>& new_meta_blocks, unsigned& meta_blocks)
{
    const auto entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    const auto entries_per_doubly_indirect_block = entries_per_block * entries_per_block;
    const auto old_indirect_blocks_length = divide_rounded_up(old_blocks_length, entries_per_block);
    const auto new_indirect_blocks_length = divide_rounded_up(blocks_indices.size(), entries_per_block);
    VERIFY(blocks_indices.size() > 0);
    VERIFY(blocks_indices.size() > old_blocks_length);
    VERIFY(blocks_indices.size() <= entries_per_doubly_indirect_block);

    auto block_contents = ByteBuffer::create_uninitialized(fs().block_size());
    auto* block_as_pointers = (unsigned*)block_contents.data();
    OutputMemoryStream stream { block_contents };
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(stream.data());

    if (old_blocks_length > 0) {
        if (auto result = fs().read_block(block, &buffer, fs().block_size()); result.is_error())
            return result;
    }

    // Grow the doubly indirect block.
    for (unsigned i = 0; i < old_indirect_blocks_length; i++)
        stream << static_cast<u32>(block_as_pointers[i]);
    for (unsigned i = old_indirect_blocks_length; i < new_indirect_blocks_length; i++) {
        auto new_block = new_meta_blocks.take_last().value();
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::grow_doubly_indirect_block(): Allocating indirect block {} at index {}", identifier(), new_block, i);
        stream << static_cast<u32>(new_block);
        meta_blocks++;
    }
    stream.fill_to_end(0);

    // Write out the indirect blocks.
    for (unsigned i = old_blocks_length / entries_per_block; i < new_indirect_blocks_length; i++) {
        const auto offset_block = i * entries_per_block;
        if (auto result = write_indirect_block(block_as_pointers[i], blocks_indices.slice(offset_block, min(blocks_indices.size() - offset_block, entries_per_block))); result.is_error())
            return result;
    }

    // Write out the doubly indirect block.
    return fs().write_block(block, buffer, stream.size());
}

KResult Ext2FSInode::shrink_doubly_indirect_block(BlockBasedFS::BlockIndex block, size_t old_blocks_length, size_t new_blocks_length, unsigned& meta_blocks)
{
    const auto entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    const auto entries_per_doubly_indirect_block = entries_per_block * entries_per_block;
    const auto old_indirect_blocks_length = divide_rounded_up(old_blocks_length, entries_per_block);
    const auto new_indirect_blocks_length = divide_rounded_up(new_blocks_length, entries_per_block);
    VERIFY(old_blocks_length > 0);
    VERIFY(old_blocks_length >= new_blocks_length);
    VERIFY(new_blocks_length <= entries_per_doubly_indirect_block);

    auto block_contents = ByteBuffer::create_uninitialized(fs().block_size());
    auto* block_as_pointers = (unsigned*)block_contents.data();
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(block_as_pointers));
    if (auto result = fs().read_block(block, &buffer, fs().block_size()); result.is_error())
        return result;

    // Free the unused indirect blocks.
    for (unsigned i = new_indirect_blocks_length; i < old_indirect_blocks_length; i++) {
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::shrink_doubly_indirect_block(): Freeing indirect block {} at index {}", identifier(), block_as_pointers[i], i);
        if (auto result = fs().set_block_allocation_state(block_as_pointers[i], false); result.is_error())
            return result;
        meta_blocks--;
    }

    // Free the doubly indirect block if no longer needed.
    if (new_blocks_length == 0) {
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::shrink_doubly_indirect_block(): Freeing doubly indirect block {}", identifier(), block);
        if (auto result = fs().set_block_allocation_state(block, false); result.is_error())
            return result;
        meta_blocks--;
    }

    return KSuccess;
}

KResult Ext2FSInode::grow_triply_indirect_block(BlockBasedFS::BlockIndex block, size_t old_blocks_length, Span<BlockBasedFS::BlockIndex> blocks_indices, Vector<Ext2FS::BlockIndex>& new_meta_blocks, unsigned& meta_blocks)
{
    const auto entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    const auto entries_per_doubly_indirect_block = entries_per_block * entries_per_block;
    const auto entries_per_triply_indirect_block = entries_per_block * entries_per_block;
    const auto old_doubly_indirect_blocks_length = divide_rounded_up(old_blocks_length, entries_per_doubly_indirect_block);
    const auto new_doubly_indirect_blocks_length = divide_rounded_up(blocks_indices.size(), entries_per_doubly_indirect_block);
    VERIFY(blocks_indices.size() > 0);
    VERIFY(blocks_indices.size() > old_blocks_length);
    VERIFY(blocks_indices.size() <= entries_per_triply_indirect_block);

    auto block_contents = ByteBuffer::create_uninitialized(fs().block_size());
    auto* block_as_pointers = (unsigned*)block_contents.data();
    OutputMemoryStream stream { block_contents };
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(stream.data());

    if (old_blocks_length > 0) {
        if (auto result = fs().read_block(block, &buffer, fs().block_size()); result.is_error())
            return result;
    }

    // Grow the triply indirect block.
    for (unsigned i = 0; i < old_doubly_indirect_blocks_length; i++)
        stream << static_cast<u32>(block_as_pointers[i]);
    for (unsigned i = old_doubly_indirect_blocks_length; i < new_doubly_indirect_blocks_length; i++) {
        auto new_block = new_meta_blocks.take_last().value();
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::grow_triply_indirect_block(): Allocating doubly indirect block {} at index {}", identifier(), new_block, i);
        stream << static_cast<u32>(new_block);
        meta_blocks++;
    }
    stream.fill_to_end(0);

    // Write out the doubly indirect blocks.
    for (unsigned i = old_blocks_length / entries_per_doubly_indirect_block; i < new_doubly_indirect_blocks_length; i++) {
        const auto processed_blocks = i * entries_per_doubly_indirect_block;
        const auto old_doubly_indirect_blocks_length = min(old_blocks_length > processed_blocks ? old_blocks_length - processed_blocks : 0, entries_per_doubly_indirect_block);
        const auto new_doubly_indirect_blocks_length = min(blocks_indices.size() > processed_blocks ? blocks_indices.size() - processed_blocks : 0, entries_per_doubly_indirect_block);
        if (auto result = grow_doubly_indirect_block(block_as_pointers[i], old_doubly_indirect_blocks_length, blocks_indices.slice(processed_blocks, new_doubly_indirect_blocks_length), new_meta_blocks, meta_blocks); result.is_error())
            return result;
    }

    // Write out the triply indirect block.
    return fs().write_block(block, buffer, stream.size());
}

KResult Ext2FSInode::shrink_triply_indirect_block(BlockBasedFS::BlockIndex block, size_t old_blocks_length, size_t new_blocks_length, unsigned& meta_blocks)
{
    const auto entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    const auto entries_per_doubly_indirect_block = entries_per_block * entries_per_block;
    const auto entries_per_triply_indirect_block = entries_per_doubly_indirect_block * entries_per_block;
    const auto old_triply_indirect_blocks_length = divide_rounded_up(old_blocks_length, entries_per_doubly_indirect_block);
    const auto new_triply_indirect_blocks_length = new_blocks_length / entries_per_doubly_indirect_block;
    VERIFY(old_blocks_length > 0);
    VERIFY(old_blocks_length >= new_blocks_length);
    VERIFY(new_blocks_length <= entries_per_triply_indirect_block);

    auto block_contents = ByteBuffer::create_uninitialized(fs().block_size());
    auto* block_as_pointers = (unsigned*)block_contents.data();
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(block_as_pointers));
    if (auto result = fs().read_block(block, &buffer, fs().block_size()); result.is_error())
        return result;

    // Shrink the doubly indirect blocks.
    for (unsigned i = new_triply_indirect_blocks_length; i < old_triply_indirect_blocks_length; i++) {
        const auto processed_blocks = i * entries_per_doubly_indirect_block;
        const auto old_doubly_indirect_blocks_length = min(old_blocks_length > processed_blocks ? old_blocks_length - processed_blocks : 0, entries_per_doubly_indirect_block);
        const auto new_doubly_indirect_blocks_length = min(new_blocks_length > processed_blocks ? new_blocks_length - processed_blocks : 0, entries_per_doubly_indirect_block);
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::shrink_triply_indirect_block(): Shrinking doubly indirect block {} at index {}", identifier(), block_as_pointers[i], i);
        if (auto result = shrink_doubly_indirect_block(block_as_pointers[i], old_doubly_indirect_blocks_length, new_doubly_indirect_blocks_length, meta_blocks); result.is_error())
            return result;
    }

    // Free the triply indirect block if no longer needed.
    if (new_blocks_length == 0) {
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::shrink_triply_indirect_block(): Freeing triply indirect block {}", identifier(), block);
        if (auto result = fs().set_block_allocation_state(block, false); result.is_error())
            return result;
        meta_blocks--;
    }

    return KSuccess;
}

KResult Ext2FSInode::flush_block_list()
{
    Locker locker(m_lock);

    if (m_block_list.is_empty()) {
        m_raw_inode.i_blocks = 0;
        memset(m_raw_inode.i_block, 0, sizeof(m_raw_inode.i_block));
        set_metadata_dirty(true);
        return KSuccess;
    }

    // NOTE: There is a mismatch between i_blocks and blocks.size() since i_blocks includes meta blocks and blocks.size() does not.
    const auto old_block_count = ceil_div(size(), static_cast<u64>(fs().block_size()));

    auto old_shape = fs().compute_block_list_shape(old_block_count);
    const auto new_shape = fs().compute_block_list_shape(m_block_list.size());

    Vector<Ext2FS::BlockIndex> new_meta_blocks;
    if (new_shape.meta_blocks > old_shape.meta_blocks) {
        auto blocks_or_error = fs().allocate_blocks(fs().group_index_from_inode(index()), new_shape.meta_blocks - old_shape.meta_blocks);
        if (blocks_or_error.is_error())
            return blocks_or_error.error();
        new_meta_blocks = blocks_or_error.release_value();
    }

    m_raw_inode.i_blocks = (m_block_list.size() + new_shape.meta_blocks) * (fs().block_size() / 512);
    dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::flush_block_list(): Old shape=({};{};{};{}:{}), new shape=({};{};{};{}:{})", identifier(), old_shape.direct_blocks, old_shape.indirect_blocks, old_shape.doubly_indirect_blocks, old_shape.triply_indirect_blocks, old_shape.meta_blocks, new_shape.direct_blocks, new_shape.indirect_blocks, new_shape.doubly_indirect_blocks, new_shape.triply_indirect_blocks, new_shape.meta_blocks);

    unsigned output_block_index = 0;
    unsigned remaining_blocks = m_block_list.size();

    // Deal with direct blocks.
    bool inode_dirty = false;
    VERIFY(new_shape.direct_blocks <= EXT2_NDIR_BLOCKS);
    for (unsigned i = 0; i < new_shape.direct_blocks; ++i) {
        if (BlockBasedFS::BlockIndex(m_raw_inode.i_block[i]) != m_block_list[output_block_index])
            inode_dirty = true;
        m_raw_inode.i_block[i] = m_block_list[output_block_index].value();
        ++output_block_index;
        --remaining_blocks;
    }
    // e2fsck considers all blocks reachable through any of the pointers in
    // m_raw_inode.i_block as part of this inode regardless of the value in
    // m_raw_inode.i_size. When it finds more blocks than the amount that
    // is indicated by i_size or i_blocks it offers to repair the filesystem
    // by changing those values. That will actually cause further corruption.
    // So we must zero all pointers to blocks that are now unused.
    for (unsigned i = new_shape.direct_blocks; i < EXT2_NDIR_BLOCKS; ++i) {
        m_raw_inode.i_block[i] = 0;
    }
    if (inode_dirty) {
        if constexpr (EXT2_DEBUG) {
            dbgln("Ext2FSInode[{}]::flush_block_list(): Writing {} direct block(s) to i_block array of inode {}", identifier(), min((size_t)EXT2_NDIR_BLOCKS, m_block_list.size()), index());
            for (size_t i = 0; i < min((size_t)EXT2_NDIR_BLOCKS, m_block_list.size()); ++i)
                dbgln("   + {}", m_block_list[i]);
        }
        set_metadata_dirty(true);
    }

    // Deal with indirect blocks.
    if (old_shape.indirect_blocks != new_shape.indirect_blocks) {
        if (new_shape.indirect_blocks > old_shape.indirect_blocks) {
            // Write out the indirect block.
            if (old_shape.indirect_blocks == 0) {
                auto new_block = new_meta_blocks.take_last().value();
                dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::flush_block_list(): Allocating indirect block: {}", identifier(), new_block);
                m_raw_inode.i_block[EXT2_IND_BLOCK] = new_block;
                set_metadata_dirty(true);
                old_shape.meta_blocks++;
            }

            if (auto result = write_indirect_block(m_raw_inode.i_block[EXT2_IND_BLOCK], m_block_list.span().slice(output_block_index, new_shape.indirect_blocks)); result.is_error())
                return result;
        } else if ((new_shape.indirect_blocks == 0) && (old_shape.indirect_blocks != 0)) {
            dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::flush_block_list(): Freeing indirect block: {}", identifier(), m_raw_inode.i_block[EXT2_IND_BLOCK]);
            if (auto result = fs().set_block_allocation_state(m_raw_inode.i_block[EXT2_IND_BLOCK], false); result.is_error())
                return result;
            old_shape.meta_blocks--;
            m_raw_inode.i_block[EXT2_IND_BLOCK] = 0;
        }
    }

    remaining_blocks -= new_shape.indirect_blocks;
    output_block_index += new_shape.indirect_blocks;

    if (old_shape.doubly_indirect_blocks != new_shape.doubly_indirect_blocks) {
        // Write out the doubly indirect block.
        if (new_shape.doubly_indirect_blocks > old_shape.doubly_indirect_blocks) {
            if (old_shape.doubly_indirect_blocks == 0) {
                auto new_block = new_meta_blocks.take_last().value();
                dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::flush_block_list(): Allocating doubly indirect block: {}", identifier(), new_block);
                m_raw_inode.i_block[EXT2_DIND_BLOCK] = new_block;
                set_metadata_dirty(true);
                old_shape.meta_blocks++;
            }
            if (auto result = grow_doubly_indirect_block(m_raw_inode.i_block[EXT2_DIND_BLOCK], old_shape.doubly_indirect_blocks, m_block_list.span().slice(output_block_index, new_shape.doubly_indirect_blocks), new_meta_blocks, old_shape.meta_blocks); result.is_error())
                return result;
        } else {
            if (auto result = shrink_doubly_indirect_block(m_raw_inode.i_block[EXT2_DIND_BLOCK], old_shape.doubly_indirect_blocks, new_shape.doubly_indirect_blocks, old_shape.meta_blocks); result.is_error())
                return result;
            if (new_shape.doubly_indirect_blocks == 0)
                m_raw_inode.i_block[EXT2_DIND_BLOCK] = 0;
        }
    }

    remaining_blocks -= new_shape.doubly_indirect_blocks;
    output_block_index += new_shape.doubly_indirect_blocks;

    if (old_shape.triply_indirect_blocks != new_shape.triply_indirect_blocks) {
        // Write out the triply indirect block.
        if (new_shape.triply_indirect_blocks > old_shape.triply_indirect_blocks) {
            if (old_shape.triply_indirect_blocks == 0) {
                auto new_block = new_meta_blocks.take_last().value();
                dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::flush_block_list(): Allocating triply indirect block: {}", identifier(), new_block);
                m_raw_inode.i_block[EXT2_TIND_BLOCK] = new_block;
                set_metadata_dirty(true);
                old_shape.meta_blocks++;
            }
            if (auto result = grow_triply_indirect_block(m_raw_inode.i_block[EXT2_TIND_BLOCK], old_shape.triply_indirect_blocks, m_block_list.span().slice(output_block_index, new_shape.triply_indirect_blocks), new_meta_blocks, old_shape.meta_blocks); result.is_error())
                return result;
        } else {
            if (auto result = shrink_triply_indirect_block(m_raw_inode.i_block[EXT2_TIND_BLOCK], old_shape.triply_indirect_blocks, new_shape.triply_indirect_blocks, old_shape.meta_blocks); result.is_error())
                return result;
            if (new_shape.triply_indirect_blocks == 0)
                m_raw_inode.i_block[EXT2_TIND_BLOCK] = 0;
        }
    }

    remaining_blocks -= new_shape.triply_indirect_blocks;
    output_block_index += new_shape.triply_indirect_blocks;

    dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::flush_block_list(): New meta blocks count at {}, expecting {}", identifier(), old_shape.meta_blocks, new_shape.meta_blocks);
    VERIFY(new_meta_blocks.size() == 0);
    VERIFY(old_shape.meta_blocks == new_shape.meta_blocks);
    if (!remaining_blocks)
        return KSuccess;

    dbgln("we don't know how to write qind ext2fs blocks, they don't exist anyway!");
    VERIFY_NOT_REACHED();
}

Vector<Ext2FS::BlockIndex> Ext2FSInode::compute_block_list() const
{
    return compute_block_list_impl(false);
}

Vector<Ext2FS::BlockIndex> Ext2FSInode::compute_block_list_with_meta_blocks() const
{
    return compute_block_list_impl(true);
}

Vector<Ext2FS::BlockIndex> Ext2FSInode::compute_block_list_impl(bool include_block_list_blocks) const
{
    // FIXME: This is really awkwardly factored.. foo_impl_internal :|
    auto block_list = compute_block_list_impl_internal(m_raw_inode, include_block_list_blocks);
    while (!block_list.is_empty() && block_list.last() == 0)
        block_list.take_last();
    return block_list;
}

Vector<Ext2FS::BlockIndex> Ext2FSInode::compute_block_list_impl_internal(const ext2_inode& e2inode, bool include_block_list_blocks) const
{
    unsigned entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());

    unsigned block_count = ceil_div(size(), static_cast<u64>(fs().block_size()));

    // If we are handling a symbolic link, the path is stored in the 60 bytes in
    // the inode that are used for the 12 direct and 3 indirect block pointers,
    // If the path is longer than 60 characters, a block is allocated, and the
    // block contains the destination path. The file size corresponds to the
    // path length of the destination.
    if (::is_symlink(e2inode.i_mode) && e2inode.i_blocks == 0)
        block_count = 0;

    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::block_list_for_inode(): i_size={}, i_blocks={}, block_count={}", identifier(), e2inode.i_size, e2inode.i_blocks, block_count);

    unsigned blocks_remaining = block_count;

    if (include_block_list_blocks) {
        auto shape = fs().compute_block_list_shape(block_count);
        blocks_remaining += shape.meta_blocks;
    }

    Vector<Ext2FS::BlockIndex> list;

    auto add_block = [&](auto bi) {
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
    // be called before compute_block_list_impl_internal finishes.
    auto process_block_array = [&](auto array_block_index, auto&& callback) {
        if (include_block_list_blocks)
            add_block(array_block_index);
        auto count = min(blocks_remaining, entries_per_block);
        if (!count)
            return;
        size_t read_size = count * sizeof(u32);
        auto array_storage = ByteBuffer::create_uninitialized(read_size);
        auto* array = (u32*)array_storage.data();
        auto buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)array);
        if (auto result = fs().read_block(array_block_index, &buffer, read_size, 0); result.is_error()) {
            // FIXME: Stop here and propagate this error.
            dbgln("Ext2FSInode[{}]::compute_block_list_impl_internal(): Error: {}", identifier(), result.error());
        }
        for (unsigned i = 0; i < count; ++i)
            callback(Ext2FS::BlockIndex(array[i]));
    };

    process_block_array(e2inode.i_block[EXT2_IND_BLOCK], [&](auto block_index) {
        add_block(block_index);
    });

    if (!blocks_remaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_DIND_BLOCK], [&](auto block_index) {
        process_block_array(block_index, [&](auto block_index2) {
            add_block(block_index2);
        });
    });

    if (!blocks_remaining)
        return list;

    process_block_array(e2inode.i_block[EXT2_TIND_BLOCK], [&](auto block_index) {
        process_block_array(block_index, [&](auto block_index2) {
            process_block_array(block_index2, [&](auto block_index3) {
                add_block(block_index3);
            });
        });
    });

    return list;
}

void Ext2FS::free_inode(Ext2FSInode& inode)
{
    Locker locker(m_lock);
    VERIFY(inode.m_raw_inode.i_links_count == 0);
    dbgln_if(EXT2_DEBUG, "Ext2FS[{}]::free_inode(): Inode {} has no more links, time to delete!", fsid(), inode.index());

    // Mark all blocks used by this inode as free.
    for (auto block_index : inode.compute_block_list_with_meta_blocks()) {
        VERIFY(block_index <= super_block().s_blocks_count);
        if (block_index.value()) {
            if (auto result = set_block_allocation_state(block_index, false); result.is_error()) {
                dbgln("Ext2FS[{}]::free_inode(): Failed to deallocate block {} for inode {}", fsid(), block_index, inode.index());
            }
        }
    }

    // If the inode being freed is a directory, update block group directory counter.
    if (inode.is_directory()) {
        auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode.index())));
        --bgd.bg_used_dirs_count;
        dbgln_if(EXT2_DEBUG, "Ext2FS[{}]::free_inode(): Decremented bg_used_dirs_count to {} for inode {}", fsid(), bgd.bg_used_dirs_count, inode.index());
        m_block_group_descriptors_dirty = true;
    }

    // NOTE: After this point, the inode metadata is wiped.
    memset(&inode.m_raw_inode, 0, sizeof(ext2_inode));
    inode.m_raw_inode.i_dtime = kgettimeofday().to_truncated_seconds();
    write_ext2_inode(inode.index(), inode.m_raw_inode);

    // Mark the inode as free.
    if (auto result = set_inode_allocation_state(inode.index(), false); result.is_error())
        dbgln("Ext2FS[{}]::free_inode(): Failed to free inode {}: {}", fsid(), inode.index(), result.error());
}

void Ext2FS::flush_block_group_descriptor_table()
{
    Locker locker(m_lock);
    unsigned blocks_to_write = ceil_div(m_block_group_count * sizeof(ext2_group_desc), block_size());
    unsigned first_block_of_bgdt = block_size() == 1024 ? 2 : 1;
    auto buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)block_group_descriptors());
    if (auto result = write_blocks(first_block_of_bgdt, blocks_to_write, buffer); result.is_error())
        dbgln("Ext2FS[{}]::flush_block_group_descriptor_table(): Failed to write blocks: {}", fsid(), result.error());
}

void Ext2FS::flush_writes()
{
    Locker locker(m_lock);
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
            if (auto result = write_block(cached_bitmap->bitmap_block_index, buffer, block_size()); result.is_error()) {
                dbgln("Ext2FS[{}]::flush_writes(): Failed to write blocks: {}", fsid(), result.error());
            }
            cached_bitmap->dirty = false;
            dbgln_if(EXT2_DEBUG, "Ext2FS[{}]::flush_writes(): Flushed bitmap block {}", fsid(), cached_bitmap->bitmap_block_index);
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

u64 Ext2FSInode::size() const
{
    if (Kernel::is_regular_file(m_raw_inode.i_mode) && ((u32)fs().get_features_readonly() & (u32)Ext2FS::FeaturesReadOnly::FileSize64bits))
        return static_cast<u64>(m_raw_inode.i_dir_acl) << 32 | m_raw_inode.i_size;
    return m_raw_inode.i_size;
}

InodeMetadata Ext2FSInode::metadata() const
{
    Locker locker(m_lock);
    InodeMetadata metadata;
    metadata.inode = identifier();
    metadata.size = size();
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
    Locker locker(m_lock);
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::flush_metadata(): Flushing inode", identifier());
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
    Locker locker(m_lock);
    VERIFY(inode.fsid() == fsid());

    {
        auto it = m_inode_cache.find(inode.index());
        if (it != m_inode_cache.end())
            return (*it).value;
    }

    auto state_or_error = get_inode_allocation_state(inode.index());
    if (state_or_error.is_error())
        return {};

    if (!state_or_error.value()) {
        m_inode_cache.set(inode.index(), nullptr);
        return {};
    }

    BlockIndex block_index;
    unsigned offset;
    if (!find_block_containing_inode(inode.index(), block_index, offset))
        return {};

    auto new_inode = adopt_ref(*new Ext2FSInode(const_cast<Ext2FS&>(*this), inode.index()));
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&new_inode->m_raw_inode));
    if (auto result = read_block(block_index, &buffer, sizeof(ext2_inode), offset); result.is_error()) {
        // FIXME: Propagate the actual error.
        return nullptr;
    }
    m_inode_cache.set(inode.index(), new_inode);
    return new_inode;
}

KResultOr<ssize_t> Ext2FSInode::read_bytes(off_t offset, ssize_t count, UserOrKernelBuffer& buffer, FileDescription* description) const
{
    Locker inode_locker(m_lock);
    VERIFY(offset >= 0);
    if (m_raw_inode.i_size == 0)
        return 0;

    if (static_cast<u64>(offset) >= size())
        return 0;

    // Symbolic links shorter than 60 characters are store inline inside the i_block array.
    // This avoids wasting an entire block on short links. (Most links are short.)
    if (is_symlink() && size() < max_inline_symlink_length) {
        VERIFY(offset == 0);
        ssize_t nread = min((off_t)size() - offset, static_cast<off_t>(count));
        if (!buffer.write(((const u8*)m_raw_inode.i_block) + offset, (size_t)nread))
            return EFAULT;
        return nread;
    }

    if (m_block_list.is_empty())
        m_block_list = compute_block_list();

    if (m_block_list.is_empty()) {
        dmesgln("Ext2FSInode[{}]::read_bytes(): Empty block list", identifier());
        return EIO;
    }

    bool allow_cache = !description || !description->is_direct();

    const int block_size = fs().block_size();

    BlockBasedFS::BlockIndex first_block_logical_index = offset / block_size;
    BlockBasedFS::BlockIndex last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    int offset_into_first_block = offset % block_size;

    ssize_t nread = 0;
    auto remaining_count = min((off_t)count, (off_t)size() - offset);

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FSInode[{}]::read_bytes(): Reading up to {} bytes, {} bytes into inode to {}", identifier(), count, offset, buffer.user_or_kernel_ptr());

    for (auto bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; bi = bi.value() + 1) {
        auto block_index = m_block_list[bi.value()];
        size_t offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        size_t num_bytes_to_copy = min((size_t)block_size - offset_into_block, (size_t)remaining_count);
        auto buffer_offset = buffer.offset(nread);
        if (block_index.value() == 0) {
            // This is a hole, act as if it's filled with zeroes.
            if (!buffer_offset.memset(0, num_bytes_to_copy))
                return EFAULT;
        } else {
            if (auto result = fs().read_block(block_index, &buffer_offset, num_bytes_to_copy, offset_into_block, allow_cache); result.is_error()) {
                dmesgln("Ext2FSInode[{}]::read_bytes(): Failed to read block {} (index {})", identifier(), block_index.value(), bi);
                return result.error();
            }
        }
        remaining_count -= num_bytes_to_copy;
        nread += num_bytes_to_copy;
    }

    return nread;
}

KResult Ext2FSInode::resize(u64 new_size)
{
    auto old_size = size();
    if (old_size == new_size)
        return KSuccess;

    if (!((u32)fs().get_features_readonly() & (u32)Ext2FS::FeaturesReadOnly::FileSize64bits) && (new_size >= static_cast<u32>(-1)))
        return ENOSPC;

    u64 block_size = fs().block_size();
    auto blocks_needed_before = ceil_div(old_size, block_size);
    auto blocks_needed_after = ceil_div(new_size, block_size);

    if constexpr (EXT2_DEBUG) {
        dbgln("Ext2FSInode[{}]::resize(): Blocks needed before (size was {}): {}", identifier(), old_size, blocks_needed_before);
        dbgln("Ext2FSInode[{}]::resize(): Blocks needed after  (size is  {}): {}", identifier(), new_size, blocks_needed_after);
    }

    if (blocks_needed_after > blocks_needed_before) {
        auto additional_blocks_needed = blocks_needed_after - blocks_needed_before;
        if (additional_blocks_needed > fs().super_block().s_free_blocks_count)
            return ENOSPC;
    }

    if (m_block_list.is_empty())
        m_block_list = this->compute_block_list();

    if (blocks_needed_after > blocks_needed_before) {
        auto blocks_or_error = fs().allocate_blocks(fs().group_index_from_inode(index()), blocks_needed_after - blocks_needed_before);
        if (blocks_or_error.is_error())
            return blocks_or_error.error();
        if (!m_block_list.try_append(blocks_or_error.release_value()))
            return ENOMEM;
    } else if (blocks_needed_after < blocks_needed_before) {
        if constexpr (EXT2_VERY_DEBUG) {
            dbgln("Ext2FSInode[{}]::resize(): Shrinking inode, old block list is {} entries:", identifier(), m_block_list.size());
            for (auto block_index : m_block_list) {
                dbgln("    # {}", block_index);
            }
        }
        while (m_block_list.size() != blocks_needed_after) {
            auto block_index = m_block_list.take_last();
            if (block_index.value()) {
                if (auto result = fs().set_block_allocation_state(block_index, false); result.is_error()) {
                    dbgln("Ext2FSInode[{}]::resize(): Failed to free block {}: {}", identifier(), block_index, result.error());
                    return result;
                }
            }
        }
    }

    if (auto result = flush_block_list(); result.is_error())
        return result;

    m_raw_inode.i_size = new_size;
    if (Kernel::is_regular_file(m_raw_inode.i_mode))
        m_raw_inode.i_dir_acl = new_size >> 32;

    set_metadata_dirty(true);

    if (new_size > old_size) {
        // If we're growing the inode, make sure we zero out all the new space.
        // FIXME: There are definitely more efficient ways to achieve this.
        auto bytes_to_clear = new_size - old_size;
        auto clear_from = old_size;
        u8 zero_buffer[PAGE_SIZE] {};
        while (bytes_to_clear) {
            auto result = write_bytes(clear_from, min(static_cast<u64>(sizeof(zero_buffer)), bytes_to_clear), UserOrKernelBuffer::for_kernel_buffer(zero_buffer), nullptr);
            if (result.is_error())
                return result.error();
            VERIFY(result.value() != 0);
            bytes_to_clear -= result.value();
            clear_from += result.value();
        }
    }

    return KSuccess;
}

KResultOr<ssize_t> Ext2FSInode::write_bytes(off_t offset, ssize_t count, const UserOrKernelBuffer& data, FileDescription* description)
{
    VERIFY(offset >= 0);
    VERIFY(count >= 0);

    if (count == 0)
        return 0;

    Locker inode_locker(m_lock);

    if (auto result = prepare_to_write_data(); result.is_error())
        return result;

    if (is_symlink()) {
        VERIFY(offset == 0);
        if (max((size_t)(offset + count), (size_t)m_raw_inode.i_size) < max_inline_symlink_length) {
            dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_bytes(): Poking into i_block array for inline symlink '{}' ({} bytes)", identifier(), data.copy_into_string(count), count);
            if (!data.read(((u8*)m_raw_inode.i_block) + offset, (size_t)count))
                return EFAULT;
            if ((size_t)(offset + count) > (size_t)m_raw_inode.i_size)
                m_raw_inode.i_size = offset + count;
            set_metadata_dirty(true);
            return count;
        }
    }

    bool allow_cache = !description || !description->is_direct();

    const auto block_size = fs().block_size();
    auto new_size = max(static_cast<u64>(offset) + count, size());

    if (auto result = resize(new_size); result.is_error())
        return result;

    if (m_block_list.is_empty())
        m_block_list = compute_block_list();

    if (m_block_list.is_empty()) {
        dbgln("Ext2FSInode[{}]::write_bytes(): Empty block list", identifier());
        return EIO;
    }

    BlockBasedFS::BlockIndex first_block_logical_index = offset / block_size;
    BlockBasedFS::BlockIndex last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    size_t offset_into_first_block = offset % block_size;

    ssize_t nwritten = 0;
    auto remaining_count = min((off_t)count, (off_t)new_size - offset);

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FSInode[{}]::write_bytes(): Writing {} bytes, {} bytes into inode from {}", identifier(), count, offset, data.user_or_kernel_ptr());

    for (auto bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; bi = bi.value() + 1) {
        size_t offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        size_t num_bytes_to_copy = min((size_t)block_size - offset_into_block, (size_t)remaining_count);
        dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_bytes(): Writing block {} (offset_into_block: {})", identifier(), m_block_list[bi.value()], offset_into_block);
        if (auto result = fs().write_block(m_block_list[bi.value()], data.offset(nwritten), num_bytes_to_copy, offset_into_block, allow_cache); result.is_error()) {
            dbgln("Ext2FSInode[{}]::write_bytes(): Failed to write block {} (index {})", identifier(), m_block_list[bi.value()], bi);
            return result;
        }
        remaining_count -= num_bytes_to_copy;
        nwritten += num_bytes_to_copy;
    }

    did_modify_contents();

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FSInode[{}]::write_bytes(): After write, i_size={}, i_blocks={} ({} blocks in list)", identifier(), size(), m_raw_inode.i_blocks, m_block_list.size());
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

Ext2FS::FeaturesReadOnly Ext2FS::get_features_readonly() const
{
    if (m_super_block.s_rev_level > 0)
        return static_cast<Ext2FS::FeaturesReadOnly>(m_super_block.s_feature_ro_compat);
    return Ext2FS::FeaturesReadOnly::None;
}

KResult Ext2FSInode::traverse_as_directory(Function<bool(const FS::DirectoryEntryView&)> callback) const
{
    Locker locker(m_lock);
    VERIFY(is_directory());

    u8 buffer[max_block_size];
    auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);

    auto block_size = fs().block_size();
    bool allow_cache = true;

    if (m_block_list.is_empty())
        m_block_list = compute_block_list();

    // Directory entries are guaranteed not to span multiple blocks,
    // so we can iterate over blocks separately.
    for (auto& block_index : m_block_list) {
        VERIFY(block_index.value() != 0);
        if (auto result = fs().read_block(block_index, &buf, block_size, 0, allow_cache); result.is_error()) {
            return result;
        }
        auto* entry = reinterpret_cast<ext2_dir_entry_2*>(buffer);
        auto* entries_end = reinterpret_cast<ext2_dir_entry_2*>(buffer + block_size);
        while (entry < entries_end) {
            if (entry->inode != 0) {
                dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::traverse_as_directory(): inode {}, name_len: {}, rec_len: {}, file_type: {}, name: {}", identifier(), entry->inode, entry->name_len, entry->rec_len, entry->file_type, StringView(entry->name, entry->name_len));
                if (!callback({ { entry->name, entry->name_len }, { fsid(), entry->inode }, entry->file_type }))
                    return KSuccess;
            }
            entry = (ext2_dir_entry_2*)((char*)entry + entry->rec_len);
        }
    }

    return KSuccess;
}

KResult Ext2FSInode::write_directory(Vector<Ext2FSDirectoryEntry>& entries)
{
    Locker locker(m_lock);
    auto block_size = fs().block_size();

    // Calculate directory size and record length of entries so that
    // the following constraints are met:
    // - All used blocks must be entirely filled.
    // - Entries are aligned on a 4-byte boundary.
    // - No entry may span multiple blocks.
    size_t directory_size = 0;
    size_t space_in_block = block_size;
    for (size_t i = 0; i < entries.size(); ++i) {
        auto& entry = entries[i];
        entry.record_length = EXT2_DIR_REC_LEN(entry.name.length());
        space_in_block -= entry.record_length;
        if (i + 1 < entries.size()) {
            if (EXT2_DIR_REC_LEN(entries[i + 1].name.length()) > space_in_block) {
                entry.record_length += space_in_block;
                space_in_block = block_size;
            }
        } else {
            entry.record_length += space_in_block;
        }
        directory_size += entry.record_length;
    }

    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_directory(): New directory contents to write (size {}):", identifier(), directory_size);

    auto directory_data = ByteBuffer::create_uninitialized(directory_size);
    OutputMemoryStream stream { directory_data };

    for (auto& entry : entries) {
        dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_directory(): Writing inode: {}, name_len: {}, rec_len: {}, file_type: {}, name: {}", identifier(), entry.inode_index, u16(entry.name.length()), u16(entry.record_length), u8(entry.file_type), entry.name);

        stream << u32(entry.inode_index.value());
        stream << u16(entry.record_length);
        stream << u8(entry.name.length());
        stream << u8(entry.file_type);
        stream << entry.name.bytes();
        int padding = entry.record_length - entry.name.length() - 8;
        for (int j = 0; j < padding; ++j)
            stream << u8(0);
    }

    VERIFY(stream.is_end());

    if (auto result = resize(stream.size()); result.is_error())
        return result;

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(stream.data());
    auto result = write_bytes(0, stream.size(), buffer, nullptr);
    if (result.is_error())
        return result.error();
    set_metadata_dirty(true);
    if (static_cast<size_t>(result.value()) != directory_data.size())
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
    Locker locker(m_lock);
    VERIFY(is_directory());

    if (name.length() > EXT2_NAME_LEN)
        return ENAMETOOLONG;

    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::add_child(): Adding inode {} with name '{}' and mode {:o} to directory {}", identifier(), child.index(), name, mode, index());

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
        dbgln("Ext2FSInode[{}]::add_child(): Name '{}' already exists", identifier(), name);
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
    did_add_child(child.identifier(), name);
    return KSuccess;
}

KResult Ext2FSInode::remove_child(const StringView& name)
{
    Locker locker(m_lock);
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::remove_child(): Removing '{}'", identifier(), name);
    VERIFY(is_directory());

    auto it = m_lookup_cache.find(name);
    if (it == m_lookup_cache.end())
        return ENOENT;
    auto child_inode_index = (*it).value;

    InodeIdentifier child_id { fsid(), child_inode_index };

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

    did_remove_child(child_id, name);
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
    BlockIndex block_index;
    unsigned offset;
    if (!find_block_containing_inode(inode, block_index, offset))
        return false;
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(const_cast<u8*>((const u8*)&e2inode));
    return write_block(block_index, buffer, inode_size(), offset) >= 0;
}

auto Ext2FS::allocate_blocks(GroupIndex preferred_group_index, size_t count) -> KResultOr<Vector<BlockIndex>>
{
    Locker locker(m_lock);
    dbgln_if(EXT2_DEBUG, "Ext2FS: allocate_blocks(preferred group: {}, count {})", preferred_group_index, count);
    if (count == 0)
        return Vector<BlockIndex> {};

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

        VERIFY(found_a_group);
        auto& bgd = group_descriptor(group_index);

        auto cached_bitmap_or_error = get_bitmap_block(bgd.bg_block_bitmap);
        if (cached_bitmap_or_error.is_error())
            return cached_bitmap_or_error.error();
        auto& cached_bitmap = *cached_bitmap_or_error.value();

        int blocks_in_group = min(blocks_per_group(), super_block().s_blocks_count);
        auto block_bitmap = cached_bitmap.bitmap(blocks_in_group);

        BlockIndex first_block_in_group = (group_index.value() - 1) * blocks_per_group() + first_block_index().value();
        size_t free_region_size = 0;
        auto first_unset_bit_index = block_bitmap.find_longest_range_of_unset_bits(count - blocks.size(), free_region_size);
        VERIFY(first_unset_bit_index.has_value());
        dbgln_if(EXT2_DEBUG, "Ext2FS: allocating free region of size: {} [{}]", free_region_size, group_index);
        for (size_t i = 0; i < free_region_size; ++i) {
            BlockIndex block_index = (first_unset_bit_index.value() + i) + first_block_in_group.value();
            if (auto result = set_block_allocation_state(block_index, true); result.is_error()) {
                dbgln("Ext2FS: Failed to allocate block {} in allocate_blocks()", block_index);
                return result;
            }
            blocks.unchecked_append(block_index);
            dbgln_if(EXT2_DEBUG, "  allocated > {}", block_index);
        }
    }

    VERIFY(blocks.size() == count);
    return blocks;
}

KResultOr<InodeIndex> Ext2FS::allocate_inode(GroupIndex preferred_group)
{
    dbgln_if(EXT2_DEBUG, "Ext2FS: allocate_inode(preferred_group: {})", preferred_group);
    Locker locker(m_lock);

    // FIXME: We shouldn't refuse to allocate an inode if there is no group that can house the whole thing.
    //        In those cases we should just spread it across multiple groups.
    auto is_suitable_group = [this](auto group_index) {
        auto& bgd = group_descriptor(group_index);
        return bgd.bg_free_inodes_count && bgd.bg_free_blocks_count >= 1;
    };

    GroupIndex group_index;
    if (preferred_group.value() && is_suitable_group(preferred_group)) {
        group_index = preferred_group;
    } else {
        for (unsigned i = 1; i <= m_block_group_count; ++i) {
            if (is_suitable_group(i)) {
                group_index = i;
                break;
            }
        }
    }

    if (!group_index) {
        dmesgln("Ext2FS: allocate_inode: no suitable group found for new inode");
        return ENOSPC;
    }

    dbgln_if(EXT2_DEBUG, "Ext2FS: allocate_inode: found suitable group [{}] for new inode :^)", group_index);

    auto& bgd = group_descriptor(group_index);
    unsigned inodes_in_group = min(inodes_per_group(), super_block().s_inodes_count);
    InodeIndex first_inode_in_group = (group_index.value() - 1) * inodes_per_group() + 1;

    auto cached_bitmap_or_error = get_bitmap_block(bgd.bg_inode_bitmap);
    if (cached_bitmap_or_error.is_error())
        return cached_bitmap_or_error.error();
    auto& cached_bitmap = *cached_bitmap_or_error.value();
    auto inode_bitmap = cached_bitmap.bitmap(inodes_in_group);
    for (size_t i = 0; i < inode_bitmap.size(); ++i) {
        if (inode_bitmap.get(i))
            continue;
        inode_bitmap.set(i, true);

        auto inode_index = InodeIndex(first_inode_in_group.value() + i);

        cached_bitmap.dirty = true;
        m_super_block.s_free_inodes_count--;
        m_super_block_dirty = true;
        const_cast<ext2_group_desc&>(bgd).bg_free_inodes_count--;
        m_block_group_descriptors_dirty = true;

        // In case the inode cache had this cached as "non-existent", uncache that info.
        m_inode_cache.remove(inode_index.value());

        return inode_index;
    }

    dmesgln("Ext2FS: allocate_inode found no available inode, despite bgd claiming there are inodes :(");
    return EIO;
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

KResultOr<bool> Ext2FS::get_inode_allocation_state(InodeIndex index) const
{
    Locker locker(m_lock);
    if (index == 0)
        return EINVAL;
    auto group_index = group_index_from_inode(index);
    auto& bgd = group_descriptor(group_index);
    unsigned index_in_group = index.value() - ((group_index.value() - 1) * inodes_per_group());
    unsigned bit_index = (index_in_group - 1) % inodes_per_group();

    auto cached_bitmap_or_error = const_cast<Ext2FS&>(*this).get_bitmap_block(bgd.bg_inode_bitmap);
    if (cached_bitmap_or_error.is_error())
        return cached_bitmap_or_error.error();
    return cached_bitmap_or_error.value()->bitmap(inodes_per_group()).get(bit_index);
}

KResult Ext2FS::update_bitmap_block(BlockIndex bitmap_block, size_t bit_index, bool new_state, u32& super_block_counter, u16& group_descriptor_counter)
{
    auto cached_bitmap_or_error = get_bitmap_block(bitmap_block);
    if (cached_bitmap_or_error.is_error())
        return cached_bitmap_or_error.error();
    auto& cached_bitmap = *cached_bitmap_or_error.value();
    bool current_state = cached_bitmap.bitmap(blocks_per_group()).get(bit_index);
    if (current_state == new_state) {
        dbgln("Ext2FS: Bit {} in bitmap block {} had unexpected state {}", bit_index, bitmap_block, current_state);
        return EIO;
    }
    cached_bitmap.bitmap(blocks_per_group()).set(bit_index, new_state);
    cached_bitmap.dirty = true;

    if (new_state) {
        --super_block_counter;
        --group_descriptor_counter;
    } else {
        ++super_block_counter;
        ++group_descriptor_counter;
    }

    m_super_block_dirty = true;
    m_block_group_descriptors_dirty = true;
    return KSuccess;
}

KResult Ext2FS::set_inode_allocation_state(InodeIndex inode_index, bool new_state)
{
    Locker locker(m_lock);
    auto group_index = group_index_from_inode(inode_index);
    unsigned index_in_group = inode_index.value() - ((group_index.value() - 1) * inodes_per_group());
    unsigned bit_index = (index_in_group - 1) % inodes_per_group();

    dbgln_if(EXT2_DEBUG, "Ext2FS: set_inode_allocation_state: Inode {} -> {}", inode_index, new_state);
    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index));
    return update_bitmap_block(bgd.bg_inode_bitmap, bit_index, new_state, m_super_block.s_free_inodes_count, bgd.bg_free_inodes_count);
}

Ext2FS::BlockIndex Ext2FS::first_block_index() const
{
    return block_size() == 1024 ? 1 : 0;
}

KResultOr<Ext2FS::CachedBitmap*> Ext2FS::get_bitmap_block(BlockIndex bitmap_block_index)
{
    for (auto& cached_bitmap : m_cached_bitmaps) {
        if (cached_bitmap->bitmap_block_index == bitmap_block_index)
            return cached_bitmap;
    }

    auto block = KBuffer::create_with_size(block_size(), Region::Access::Read | Region::Access::Write, "Ext2FS: Cached bitmap block");
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(block.data());
    if (auto result = read_block(bitmap_block_index, &buffer, block_size()); result.is_error()) {
        dbgln("Ext2FS: Failed to load bitmap block {}", bitmap_block_index);
        return result;
    }
    auto new_bitmap = adopt_own_if_nonnull(new CachedBitmap(bitmap_block_index, move(block)));
    if (!new_bitmap)
        return ENOMEM;
    if (!m_cached_bitmaps.try_append(move(new_bitmap)))
        return ENOMEM;
    return m_cached_bitmaps.last();
}

KResult Ext2FS::set_block_allocation_state(BlockIndex block_index, bool new_state)
{
    VERIFY(block_index != 0);
    Locker locker(m_lock);

    auto group_index = group_index_from_block_index(block_index);
    unsigned index_in_group = (block_index.value() - first_block_index().value()) - ((group_index.value() - 1) * blocks_per_group());
    unsigned bit_index = index_in_group % blocks_per_group();
    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index));

    dbgln_if(EXT2_DEBUG, "Ext2FS: Block {} state -> {} (in bitmap block {})", block_index, new_state, bgd.bg_block_bitmap);
    return update_bitmap_block(bgd.bg_block_bitmap, bit_index, new_state, m_super_block.s_free_blocks_count, bgd.bg_free_blocks_count);
}

KResult Ext2FS::create_directory(Ext2FSInode& parent_inode, const String& name, mode_t mode, uid_t uid, gid_t gid)
{
    Locker locker(m_lock);
    VERIFY(is_directory(mode));

    auto inode_or_error = create_inode(parent_inode, name, mode, 0, uid, gid);
    if (inode_or_error.is_error())
        return inode_or_error.error();

    auto& inode = inode_or_error.value();

    dbgln_if(EXT2_DEBUG, "Ext2FS: create_directory: created new directory named '{} with inode {}", name, inode->index());

    Vector<Ext2FSDirectoryEntry> entries;
    entries.empend(".", inode->index(), static_cast<u8>(EXT2_FT_DIR));
    entries.empend("..", parent_inode.index(), static_cast<u8>(EXT2_FT_DIR));

    if (auto result = static_cast<Ext2FSInode&>(*inode).write_directory(entries); result.is_error())
        return result;

    if (auto result = parent_inode.increment_link_count(); result.is_error())
        return result;

    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode->identifier().index())));
    ++bgd.bg_used_dirs_count;
    m_block_group_descriptors_dirty = true;

    return KSuccess;
}

KResultOr<NonnullRefPtr<Inode>> Ext2FS::create_inode(Ext2FSInode& parent_inode, const String& name, mode_t mode, dev_t dev, uid_t uid, gid_t gid)
{
    if (name.length() > EXT2_NAME_LEN)
        return ENAMETOOLONG;

    if (parent_inode.m_raw_inode.i_links_count == 0)
        return ENOENT;

    ext2_inode e2inode {};
    auto now = kgettimeofday().to_truncated_seconds();
    e2inode.i_mode = mode;
    e2inode.i_uid = uid;
    e2inode.i_gid = gid;
    e2inode.i_size = 0;
    e2inode.i_atime = now;
    e2inode.i_ctime = now;
    e2inode.i_mtime = now;
    e2inode.i_dtime = 0;
    e2inode.i_flags = 0;

    // For directories, add +1 link count for the "." entry in self.
    e2inode.i_links_count = is_directory(mode);

    if (is_character_device(mode))
        e2inode.i_block[0] = dev;
    else if (is_block_device(mode))
        e2inode.i_block[1] = dev;

    auto inode_id = allocate_inode();
    if (inode_id.is_error())
        return inode_id.error();

    dbgln_if(EXT2_DEBUG, "Ext2FS: writing initial metadata for inode {}", inode_id.value());
    auto success = write_ext2_inode(inode_id.value(), e2inode);
    VERIFY(success);

    auto new_inode = get_inode({ fsid(), inode_id.value() });
    VERIFY(new_inode);

    dbgln_if(EXT2_DEBUG, "Ext2FS: Adding inode '{}' (mode {:o}) to parent directory {}", name, mode, parent_inode.index());
    if (auto result = parent_inode.add_child(*new_inode, name, mode); result.is_error())
        return result;
    return new_inode.release_nonnull();
}

bool Ext2FSInode::populate_lookup_cache() const
{
    Locker locker(m_lock);
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
    VERIFY(is_directory());
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]:lookup(): Looking up '{}'", identifier(), name);
    if (!populate_lookup_cache())
        return {};
    Locker locker(m_lock);
    auto it = m_lookup_cache.find(name.hash(), [&](auto& entry) { return entry.key == name; });
    if (it != m_lookup_cache.end())
        return fs().get_inode({ fsid(), (*it).value });
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]:lookup(): '{}' not found", identifier(), name);
    return {};
}

void Ext2FSInode::one_ref_left()
{
    // FIXME: I would like to not live forever, but uncached Ext2FS is fucking painful right now.
}

KResult Ext2FSInode::set_atime(time_t t)
{
    Locker locker(m_lock);
    if (fs().is_readonly())
        return EROFS;
    m_raw_inode.i_atime = t;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::set_ctime(time_t t)
{
    Locker locker(m_lock);
    if (fs().is_readonly())
        return EROFS;
    m_raw_inode.i_ctime = t;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::set_mtime(time_t t)
{
    Locker locker(m_lock);
    if (fs().is_readonly())
        return EROFS;
    m_raw_inode.i_mtime = t;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::increment_link_count()
{
    Locker locker(m_lock);
    if (fs().is_readonly())
        return EROFS;
    constexpr size_t max_link_count = 65535;
    if (m_raw_inode.i_links_count == max_link_count)
        return EMLINK;
    ++m_raw_inode.i_links_count;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::decrement_link_count()
{
    Locker locker(m_lock);
    if (fs().is_readonly())
        return EROFS;
    VERIFY(m_raw_inode.i_links_count);

    --m_raw_inode.i_links_count;
    set_metadata_dirty(true);
    if (m_raw_inode.i_links_count == 0)
        did_delete_self();

    if (ref_count() == 1 && m_raw_inode.i_links_count == 0)
        fs().uncache_inode(index());

    return KSuccess;
}

void Ext2FS::uncache_inode(InodeIndex index)
{
    Locker locker(m_lock);
    m_inode_cache.remove(index);
}

KResultOr<size_t> Ext2FSInode::directory_entry_count() const
{
    VERIFY(is_directory());
    Locker locker(m_lock);
    populate_lookup_cache();
    return m_lookup_cache.size();
}

KResult Ext2FSInode::chmod(mode_t mode)
{
    Locker locker(m_lock);
    if (m_raw_inode.i_mode == mode)
        return KSuccess;
    m_raw_inode.i_mode = mode;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::chown(uid_t uid, gid_t gid)
{
    Locker locker(m_lock);
    if (m_raw_inode.i_uid == uid && m_raw_inode.i_gid == gid)
        return KSuccess;
    m_raw_inode.i_uid = uid;
    m_raw_inode.i_gid = gid;
    set_metadata_dirty(true);
    return KSuccess;
}

KResult Ext2FSInode::truncate(u64 size)
{
    Locker locker(m_lock);
    if (static_cast<u64>(m_raw_inode.i_size) == size)
        return KSuccess;
    if (auto result = resize(size); result.is_error())
        return result;
    set_metadata_dirty(true);
    return KSuccess;
}

KResultOr<int> Ext2FSInode::get_block_address(int index)
{
    Locker locker(m_lock);

    if (m_block_list.is_empty())
        m_block_list = compute_block_list();

    if (index < 0 || (size_t)index >= m_block_list.size())
        return 0;

    return m_block_list[index].value();
}

unsigned Ext2FS::total_block_count() const
{
    Locker locker(m_lock);
    return super_block().s_blocks_count;
}

unsigned Ext2FS::free_block_count() const
{
    Locker locker(m_lock);
    return super_block().s_free_blocks_count;
}

unsigned Ext2FS::total_inode_count() const
{
    Locker locker(m_lock);
    return super_block().s_inodes_count;
}

unsigned Ext2FS::free_inode_count() const
{
    Locker locker(m_lock);
    return super_block().s_free_inodes_count;
}

KResult Ext2FS::prepare_to_unmount() const
{
    Locker locker(m_lock);

    for (auto& it : m_inode_cache) {
        if (it.value->ref_count() > 1)
            return EBUSY;
    }

    m_inode_cache.clear();
    return KSuccess;
}
}
