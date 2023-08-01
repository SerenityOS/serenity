/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Ext2FS/Inode.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

static constexpr size_t max_inline_symlink_length = 60;

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

ErrorOr<void> Ext2FSInode::write_indirect_block(BlockBasedFileSystem::BlockIndex block, Span<BlockBasedFileSystem::BlockIndex> blocks_indices)
{
    auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    VERIFY(blocks_indices.size() <= entries_per_block);

    auto block_contents = TRY(ByteBuffer::create_zeroed(fs().logical_block_size()));
    FixedMemoryStream stream { block_contents.bytes() };
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(block_contents.data());

    VERIFY(blocks_indices.size() <= EXT2_ADDR_PER_BLOCK(&fs().super_block()));
    for (unsigned i = 0; i < blocks_indices.size(); ++i)
        MUST(stream.write_value<u32>(blocks_indices[i].value()));

    return fs().write_block(block, buffer, block_contents.size());
}

ErrorOr<void> Ext2FSInode::grow_doubly_indirect_block(BlockBasedFileSystem::BlockIndex block, size_t old_blocks_length, Span<BlockBasedFileSystem::BlockIndex> blocks_indices, Vector<Ext2FS::BlockIndex>& new_meta_blocks, unsigned& meta_blocks)
{
    auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    auto const entries_per_doubly_indirect_block = entries_per_block * entries_per_block;
    auto const old_indirect_blocks_length = ceil_div(old_blocks_length, entries_per_block);
    auto const new_indirect_blocks_length = ceil_div(blocks_indices.size(), entries_per_block);
    VERIFY(blocks_indices.size() > 0);
    VERIFY(blocks_indices.size() > old_blocks_length);
    VERIFY(blocks_indices.size() <= entries_per_doubly_indirect_block);

    auto block_contents = TRY(ByteBuffer::create_zeroed(fs().logical_block_size()));
    auto* block_as_pointers = (unsigned*)block_contents.data();
    FixedMemoryStream stream { block_contents.bytes() };
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(block_contents.data());

    if (old_blocks_length > 0) {
        TRY(fs().read_block(block, &buffer, fs().logical_block_size()));
    }

    // Grow the doubly indirect block.
    for (unsigned i = 0; i < old_indirect_blocks_length; i++)
        MUST(stream.write_value<u32>(block_as_pointers[i]));
    for (unsigned i = old_indirect_blocks_length; i < new_indirect_blocks_length; i++) {
        auto new_block = new_meta_blocks.take_last().value();
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::grow_doubly_indirect_block(): Allocating indirect block {} at index {}", identifier(), new_block, i);
        MUST(stream.write_value<u32>(new_block));
        meta_blocks++;
    }

    // Write out the indirect blocks.
    for (unsigned i = old_blocks_length / entries_per_block; i < new_indirect_blocks_length; i++) {
        auto const offset_block = i * entries_per_block;
        TRY(write_indirect_block(block_as_pointers[i], blocks_indices.slice(offset_block, min(blocks_indices.size() - offset_block, entries_per_block))));
    }

    // Write out the doubly indirect block.
    return fs().write_block(block, buffer, block_contents.size());
}

ErrorOr<void> Ext2FSInode::shrink_doubly_indirect_block(BlockBasedFileSystem::BlockIndex block, size_t old_blocks_length, size_t new_blocks_length, unsigned& meta_blocks)
{
    auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    auto const entries_per_doubly_indirect_block = entries_per_block * entries_per_block;
    auto const old_indirect_blocks_length = ceil_div(old_blocks_length, entries_per_block);
    auto const new_indirect_blocks_length = ceil_div(new_blocks_length, entries_per_block);
    VERIFY(old_blocks_length > 0);
    VERIFY(old_blocks_length >= new_blocks_length);
    VERIFY(new_blocks_length <= entries_per_doubly_indirect_block);

    auto block_contents = TRY(ByteBuffer::create_uninitialized(fs().logical_block_size()));
    auto* block_as_pointers = (unsigned*)block_contents.data();
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(block_as_pointers));
    TRY(fs().read_block(block, &buffer, fs().logical_block_size()));

    // Free the unused indirect blocks.
    for (unsigned i = new_indirect_blocks_length; i < old_indirect_blocks_length; i++) {
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::shrink_doubly_indirect_block(): Freeing indirect block {} at index {}", identifier(), block_as_pointers[i], i);
        TRY(fs().set_block_allocation_state(block_as_pointers[i], false));
        meta_blocks--;
    }

    // Free the doubly indirect block if no longer needed.
    if (new_blocks_length == 0) {
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::shrink_doubly_indirect_block(): Freeing doubly indirect block {}", identifier(), block);
        TRY(fs().set_block_allocation_state(block, false));
        meta_blocks--;
    }

    return {};
}

ErrorOr<void> Ext2FSInode::grow_triply_indirect_block(BlockBasedFileSystem::BlockIndex block, size_t old_blocks_length, Span<BlockBasedFileSystem::BlockIndex> blocks_indices, Vector<Ext2FS::BlockIndex>& new_meta_blocks, unsigned& meta_blocks)
{
    auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    auto const entries_per_doubly_indirect_block = entries_per_block * entries_per_block;
    auto const entries_per_triply_indirect_block = entries_per_block * entries_per_block;
    auto const old_doubly_indirect_blocks_length = ceil_div(old_blocks_length, entries_per_doubly_indirect_block);
    auto const new_doubly_indirect_blocks_length = ceil_div(blocks_indices.size(), entries_per_doubly_indirect_block);
    VERIFY(blocks_indices.size() > 0);
    VERIFY(blocks_indices.size() > old_blocks_length);
    VERIFY(blocks_indices.size() <= entries_per_triply_indirect_block);

    auto block_contents = TRY(ByteBuffer::create_zeroed(fs().logical_block_size()));
    auto* block_as_pointers = (unsigned*)block_contents.data();
    FixedMemoryStream stream { block_contents.bytes() };
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(block_contents.data());

    if (old_blocks_length > 0) {
        TRY(fs().read_block(block, &buffer, fs().logical_block_size()));
    }

    // Grow the triply indirect block.
    for (unsigned i = 0; i < old_doubly_indirect_blocks_length; i++)
        MUST(stream.write_value<u32>(block_as_pointers[i]));
    for (unsigned i = old_doubly_indirect_blocks_length; i < new_doubly_indirect_blocks_length; i++) {
        auto new_block = new_meta_blocks.take_last().value();
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::grow_triply_indirect_block(): Allocating doubly indirect block {} at index {}", identifier(), new_block, i);
        MUST(stream.write_value<u32>(new_block));
        meta_blocks++;
    }

    // Write out the doubly indirect blocks.
    for (unsigned i = old_blocks_length / entries_per_doubly_indirect_block; i < new_doubly_indirect_blocks_length; i++) {
        auto const processed_blocks = i * entries_per_doubly_indirect_block;
        auto const old_doubly_indirect_blocks_length = min(old_blocks_length > processed_blocks ? old_blocks_length - processed_blocks : 0, entries_per_doubly_indirect_block);
        auto const new_doubly_indirect_blocks_length = min(blocks_indices.size() > processed_blocks ? blocks_indices.size() - processed_blocks : 0, entries_per_doubly_indirect_block);
        TRY(grow_doubly_indirect_block(block_as_pointers[i], old_doubly_indirect_blocks_length, blocks_indices.slice(processed_blocks, new_doubly_indirect_blocks_length), new_meta_blocks, meta_blocks));
    }

    // Write out the triply indirect block.
    return fs().write_block(block, buffer, block_contents.size());
}

ErrorOr<void> Ext2FSInode::shrink_triply_indirect_block(BlockBasedFileSystem::BlockIndex block, size_t old_blocks_length, size_t new_blocks_length, unsigned& meta_blocks)
{
    auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    auto const entries_per_doubly_indirect_block = entries_per_block * entries_per_block;
    auto const entries_per_triply_indirect_block = entries_per_doubly_indirect_block * entries_per_block;
    auto const old_triply_indirect_blocks_length = ceil_div(old_blocks_length, entries_per_doubly_indirect_block);
    auto const new_triply_indirect_blocks_length = new_blocks_length / entries_per_doubly_indirect_block;
    VERIFY(old_blocks_length > 0);
    VERIFY(old_blocks_length >= new_blocks_length);
    VERIFY(new_blocks_length <= entries_per_triply_indirect_block);

    auto block_contents = TRY(ByteBuffer::create_uninitialized(fs().logical_block_size()));
    auto* block_as_pointers = (unsigned*)block_contents.data();
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(block_as_pointers));
    TRY(fs().read_block(block, &buffer, fs().logical_block_size()));

    // Shrink the doubly indirect blocks.
    for (unsigned i = new_triply_indirect_blocks_length; i < old_triply_indirect_blocks_length; i++) {
        auto const processed_blocks = i * entries_per_doubly_indirect_block;
        auto const old_doubly_indirect_blocks_length = min(old_blocks_length > processed_blocks ? old_blocks_length - processed_blocks : 0, entries_per_doubly_indirect_block);
        auto const new_doubly_indirect_blocks_length = min(new_blocks_length > processed_blocks ? new_blocks_length - processed_blocks : 0, entries_per_doubly_indirect_block);
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::shrink_triply_indirect_block(): Shrinking doubly indirect block {} at index {}", identifier(), block_as_pointers[i], i);
        TRY(shrink_doubly_indirect_block(block_as_pointers[i], old_doubly_indirect_blocks_length, new_doubly_indirect_blocks_length, meta_blocks));
    }

    // Free the triply indirect block if no longer needed.
    if (new_blocks_length == 0) {
        dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::shrink_triply_indirect_block(): Freeing triply indirect block {}", identifier(), block);
        TRY(fs().set_block_allocation_state(block, false));
        meta_blocks--;
    }

    return {};
}

ErrorOr<void> Ext2FSInode::flush_block_list()
{
    MutexLocker locker(m_inode_lock);

    if (m_block_list.is_empty()) {
        m_raw_inode.i_blocks = 0;
        memset(m_raw_inode.i_block, 0, sizeof(m_raw_inode.i_block));
        set_metadata_dirty(true);
        return {};
    }

    // NOTE: There is a mismatch between i_blocks and blocks.size() since i_blocks includes meta blocks and blocks.size() does not.
    auto const old_block_count = ceil_div(size(), static_cast<u64>(fs().logical_block_size()));

    auto old_shape = fs().compute_block_list_shape(old_block_count);
    auto const new_shape = fs().compute_block_list_shape(m_block_list.size());

    Vector<Ext2FS::BlockIndex> new_meta_blocks;
    if (new_shape.meta_blocks > old_shape.meta_blocks) {
        new_meta_blocks = TRY(fs().allocate_blocks(fs().group_index_from_inode(index()), new_shape.meta_blocks - old_shape.meta_blocks));
    }

    m_raw_inode.i_blocks = (m_block_list.size() + new_shape.meta_blocks) * (fs().logical_block_size() / 512);
    dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::flush_block_list(): Old shape=({};{};{};{}:{}), new shape=({};{};{};{}:{})", identifier(), old_shape.direct_blocks, old_shape.indirect_blocks, old_shape.doubly_indirect_blocks, old_shape.triply_indirect_blocks, old_shape.meta_blocks, new_shape.direct_blocks, new_shape.indirect_blocks, new_shape.doubly_indirect_blocks, new_shape.triply_indirect_blocks, new_shape.meta_blocks);

    unsigned output_block_index = 0;
    unsigned remaining_blocks = m_block_list.size();

    // Deal with direct blocks.
    bool inode_dirty = false;
    VERIFY(new_shape.direct_blocks <= EXT2_NDIR_BLOCKS);
    for (unsigned i = 0; i < new_shape.direct_blocks; ++i) {
        if (BlockBasedFileSystem::BlockIndex(m_raw_inode.i_block[i]) != m_block_list[output_block_index])
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

            TRY(write_indirect_block(m_raw_inode.i_block[EXT2_IND_BLOCK], m_block_list.span().slice(output_block_index, new_shape.indirect_blocks)));
        } else if ((new_shape.indirect_blocks == 0) && (old_shape.indirect_blocks != 0)) {
            dbgln_if(EXT2_BLOCKLIST_DEBUG, "Ext2FSInode[{}]::flush_block_list(): Freeing indirect block: {}", identifier(), m_raw_inode.i_block[EXT2_IND_BLOCK]);
            TRY(fs().set_block_allocation_state(m_raw_inode.i_block[EXT2_IND_BLOCK], false));
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
            TRY(grow_doubly_indirect_block(m_raw_inode.i_block[EXT2_DIND_BLOCK], old_shape.doubly_indirect_blocks, m_block_list.span().slice(output_block_index, new_shape.doubly_indirect_blocks), new_meta_blocks, old_shape.meta_blocks));
        } else {
            TRY(shrink_doubly_indirect_block(m_raw_inode.i_block[EXT2_DIND_BLOCK], old_shape.doubly_indirect_blocks, new_shape.doubly_indirect_blocks, old_shape.meta_blocks));
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
            TRY(grow_triply_indirect_block(m_raw_inode.i_block[EXT2_TIND_BLOCK], old_shape.triply_indirect_blocks, m_block_list.span().slice(output_block_index, new_shape.triply_indirect_blocks), new_meta_blocks, old_shape.meta_blocks));
        } else {
            TRY(shrink_triply_indirect_block(m_raw_inode.i_block[EXT2_TIND_BLOCK], old_shape.triply_indirect_blocks, new_shape.triply_indirect_blocks, old_shape.meta_blocks));
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
        return {};

    dbgln("we don't know how to write qind ext2fs blocks, they don't exist anyway!");
    VERIFY_NOT_REACHED();
}

ErrorOr<Vector<Ext2FS::BlockIndex>> Ext2FSInode::compute_block_list() const
{
    return compute_block_list_impl(false);
}

ErrorOr<Vector<Ext2FS::BlockIndex>> Ext2FSInode::compute_block_list_with_meta_blocks() const
{
    return compute_block_list_impl(true);
}

ErrorOr<Vector<Ext2FS::BlockIndex>> Ext2FSInode::compute_block_list_impl(bool include_block_list_blocks) const
{
    // FIXME: This is really awkwardly factored.. foo_impl_internal :|
    auto block_list = TRY(compute_block_list_impl_internal(m_raw_inode, include_block_list_blocks));
    while (!block_list.is_empty() && block_list.last() == 0)
        block_list.take_last();
    return block_list;
}

ErrorOr<Vector<Ext2FS::BlockIndex>> Ext2FSInode::compute_block_list_impl_internal(ext2_inode const& e2inode, bool include_block_list_blocks) const
{
    unsigned entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());

    unsigned block_count = ceil_div(size(), static_cast<u64>(fs().logical_block_size()));

    // If we are handling a symbolic link, the path is stored in the 60 bytes in
    // the inode that are used for the 12 direct and 3 indirect block pointers,
    // If the path is longer than 60 characters, a block is allocated, and the
    // block contains the destination path. The file size corresponds to the
    // path length of the destination.
    if (Kernel::is_symlink(e2inode.i_mode) && e2inode.i_blocks == 0)
        block_count = 0;

    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::block_list_for_inode(): i_size={}, i_blocks={}, block_count={}", identifier(), e2inode.i_size, e2inode.i_blocks, block_count);

    unsigned blocks_remaining = block_count;

    if (include_block_list_blocks) {
        auto shape = fs().compute_block_list_shape(block_count);
        blocks_remaining += shape.meta_blocks;
    }

    Vector<Ext2FS::BlockIndex> list;

    auto add_block = [&](auto bi) -> ErrorOr<void> {
        if (blocks_remaining) {
            TRY(list.try_append(bi));
            --blocks_remaining;
        }
        return {};
    };

    if (include_block_list_blocks) {
        // This seems like an excessive over-estimate but w/e.
        TRY(list.try_ensure_capacity(blocks_remaining * 2));
    } else {
        TRY(list.try_ensure_capacity(blocks_remaining));
    }

    unsigned direct_count = min(block_count, (unsigned)EXT2_NDIR_BLOCKS);
    for (unsigned i = 0; i < direct_count; ++i) {
        auto block_index = e2inode.i_block[i];
        TRY(add_block(block_index));
    }

    if (!blocks_remaining)
        return list;

    // Don't need to make copy of add_block, since this capture will only
    // be called before compute_block_list_impl_internal finishes.
    auto process_block_array = [&](auto array_block_index, auto&& callback) -> ErrorOr<void> {
        if (include_block_list_blocks)
            TRY(add_block(array_block_index));
        auto count = min(blocks_remaining, entries_per_block);
        if (!count)
            return {};
        size_t read_size = count * sizeof(u32);
        auto array_storage = TRY(ByteBuffer::create_uninitialized(read_size));
        auto* array = (u32*)array_storage.data();
        auto buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)array);
        TRY(fs().read_block(array_block_index, &buffer, read_size, 0));
        for (unsigned i = 0; i < count; ++i)
            TRY(callback(Ext2FS::BlockIndex(array[i])));
        return {};
    };

    TRY(process_block_array(e2inode.i_block[EXT2_IND_BLOCK], [&](auto block_index) -> ErrorOr<void> {
        return add_block(block_index);
    }));

    if (!blocks_remaining)
        return list;

    TRY(process_block_array(e2inode.i_block[EXT2_DIND_BLOCK], [&](auto block_index) -> ErrorOr<void> {
        return process_block_array(block_index, [&](auto block_index2) -> ErrorOr<void> {
            return add_block(block_index2);
        });
    }));

    if (!blocks_remaining)
        return list;

    TRY(process_block_array(e2inode.i_block[EXT2_TIND_BLOCK], [&](auto block_index) -> ErrorOr<void> {
        return process_block_array(block_index, [&](auto block_index2) -> ErrorOr<void> {
            return process_block_array(block_index2, [&](auto block_index3) -> ErrorOr<void> {
                return add_block(block_index3);
            });
        });
    }));

    return list;
}

Ext2FSInode::Ext2FSInode(Ext2FS& fs, InodeIndex index)
    : Inode(fs, index)
{
}

Ext2FSInode::~Ext2FSInode()
{
    if (m_raw_inode.i_links_count == 0) {
        // Alas, we have nowhere to propagate any errors that occur here.
        (void)fs().free_inode(*this);
    }
}

u64 Ext2FSInode::size() const
{
    if (Kernel::is_regular_file(m_raw_inode.i_mode) && ((u32)fs().get_features_readonly() & (u32)Ext2FS::FeaturesReadOnly::FileSize64bits))
        return static_cast<u64>(m_raw_inode.i_dir_acl) << 32 | m_raw_inode.i_size;
    return m_raw_inode.i_size;
}

InodeMetadata Ext2FSInode::metadata() const
{
    MutexLocker locker(m_inode_lock);
    InodeMetadata metadata;
    metadata.inode = identifier();
    metadata.size = size();
    metadata.mode = m_raw_inode.i_mode;
    metadata.uid = m_raw_inode.i_uid;
    metadata.gid = m_raw_inode.i_gid;
    metadata.link_count = m_raw_inode.i_links_count;
    metadata.atime = UnixDateTime::from_seconds_since_epoch(m_raw_inode.i_atime);
    metadata.ctime = UnixDateTime::from_seconds_since_epoch(m_raw_inode.i_ctime);
    metadata.mtime = UnixDateTime::from_seconds_since_epoch(m_raw_inode.i_mtime);
    metadata.dtime = UnixDateTime::from_seconds_since_epoch(m_raw_inode.i_dtime);
    metadata.block_size = fs().logical_block_size();
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

ErrorOr<void> Ext2FSInode::flush_metadata()
{
    MutexLocker locker(m_inode_lock);
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::flush_metadata(): Flushing inode", identifier());
    TRY(fs().write_ext2_inode(index(), m_raw_inode));
    if (is_directory()) {
        // Unless we're about to go away permanently, invalidate the lookup cache.
        if (m_raw_inode.i_links_count != 0) {
            // FIXME: This invalidation is way too hardcore. It's sad to throw away the whole cache.
            m_lookup_cache.clear();
        }
    }
    set_metadata_dirty(false);
    return {};
}

ErrorOr<void> Ext2FSInode::compute_block_list_with_exclusive_locking()
{
    // Note: We verify that the inode mutex is being held locked. Because only the read_bytes_locked()
    // method uses this method and the mutex can be locked in shared mode when reading the Inode if
    // it is an ext2 regular file, but also in exclusive mode, when the Inode is an ext2 directory and being
    // traversed, we use another exclusive lock to ensure we always mutate the block list safely.
    VERIFY(m_inode_lock.is_locked());
    MutexLocker block_list_locker(m_block_list_lock);
    if (m_block_list.is_empty())
        m_block_list = TRY(compute_block_list());
    return {};
}

ErrorOr<size_t> Ext2FSInode::read_bytes_locked(off_t offset, size_t count, UserOrKernelBuffer& buffer, OpenFileDescription* description) const
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(offset >= 0);
    if (m_raw_inode.i_size == 0)
        return 0;

    if (static_cast<u64>(offset) >= size())
        return 0;

    // Symbolic links shorter than 60 characters are store inline inside the i_block array.
    // This avoids wasting an entire block on short links. (Most links are short.)
    if (is_symlink() && size() < max_inline_symlink_length) {
        VERIFY(offset == 0);
        size_t nread = min((off_t)size() - offset, static_cast<off_t>(count));
        TRY(buffer.write(((u8 const*)m_raw_inode.i_block) + offset, nread));
        return nread;
    }

    // Note: We bypass the const declaration of this method, but this is a strong
    // requirement to be able to accomplish the read operation successfully.
    // We call this special method because it locks a separate mutex to ensure we
    // update the block list of the inode safely, as the m_inode_lock is locked in
    // shared mode.
    TRY(const_cast<Ext2FSInode&>(*this).compute_block_list_with_exclusive_locking());

    if (m_block_list.is_empty()) {
        dmesgln("Ext2FSInode[{}]::read_bytes(): Empty block list", identifier());
        return EIO;
    }

    bool allow_cache = !description || !description->is_direct();

    int const block_size = fs().logical_block_size();

    BlockBasedFileSystem::BlockIndex first_block_logical_index = offset / block_size;
    BlockBasedFileSystem::BlockIndex last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    int offset_into_first_block = offset % block_size;

    size_t nread = 0;
    auto remaining_count = min((off_t)count, (off_t)size() - offset);

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FSInode[{}]::read_bytes(): Reading up to {} bytes, {} bytes into inode to {}", identifier(), count, offset, buffer.user_or_kernel_ptr());

    for (auto bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; bi = bi.value() + 1) {
        auto block_index = m_block_list[bi.value()];
        size_t offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        size_t num_bytes_to_copy = min((size_t)block_size - offset_into_block, (size_t)remaining_count);
        auto buffer_offset = buffer.offset(nread);
        if (block_index.value() == 0) {
            // This is a hole, act as if it's filled with zeroes.
            TRY(buffer_offset.memset(0, num_bytes_to_copy));
        } else {
            if (auto result = fs().read_block(block_index, &buffer_offset, num_bytes_to_copy, offset_into_block, allow_cache); result.is_error()) {
                dmesgln("Ext2FSInode[{}]::read_bytes(): Failed to read block {} (index {})", identifier(), block_index.value(), bi);
                return result.release_error();
            }
        }
        remaining_count -= num_bytes_to_copy;
        nread += num_bytes_to_copy;
    }

    return nread;
}

ErrorOr<void> Ext2FSInode::resize(u64 new_size)
{
    auto old_size = size();
    if (old_size == new_size)
        return {};

    if (!((u32)fs().get_features_readonly() & (u32)Ext2FS::FeaturesReadOnly::FileSize64bits) && (new_size >= static_cast<u32>(-1)))
        return ENOSPC;

    u64 block_size = fs().logical_block_size();
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
        m_block_list = TRY(compute_block_list());

    if (blocks_needed_after > blocks_needed_before) {
        auto blocks = TRY(fs().allocate_blocks(fs().group_index_from_inode(index()), blocks_needed_after - blocks_needed_before));
        TRY(m_block_list.try_extend(move(blocks)));
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

    TRY(flush_block_list());

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
            auto nwritten = TRY(write_bytes(clear_from, min(static_cast<u64>(sizeof(zero_buffer)), bytes_to_clear), UserOrKernelBuffer::for_kernel_buffer(zero_buffer), nullptr));
            VERIFY(nwritten != 0);
            bytes_to_clear -= nwritten;
            clear_from += nwritten;
        }
    }

    return {};
}

ErrorOr<size_t> Ext2FSInode::write_bytes_locked(off_t offset, size_t count, UserOrKernelBuffer const& data, OpenFileDescription* description)
{
    VERIFY(m_inode_lock.is_locked());
    VERIFY(offset >= 0);

    if (count == 0)
        return 0;

    if (is_symlink()) {
        VERIFY(offset == 0);
        if (max((size_t)(offset + count), (size_t)m_raw_inode.i_size) < max_inline_symlink_length) {
            dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_bytes_locked(): Poking into i_block array for inline symlink ({} bytes)", identifier(), count);
            TRY(data.read(((u8*)m_raw_inode.i_block) + offset, count));
            if ((size_t)(offset + count) > (size_t)m_raw_inode.i_size)
                m_raw_inode.i_size = offset + count;
            set_metadata_dirty(true);
            return count;
        }
    }

    bool allow_cache = !description || !description->is_direct();

    auto const block_size = fs().logical_block_size();
    auto new_size = max(static_cast<u64>(offset) + count, size());

    TRY(resize(new_size));

    if (m_block_list.is_empty())
        m_block_list = TRY(compute_block_list());

    if (m_block_list.is_empty()) {
        dbgln("Ext2FSInode[{}]::write_bytes(): Empty block list", identifier());
        return EIO;
    }

    BlockBasedFileSystem::BlockIndex first_block_logical_index = offset / block_size;
    BlockBasedFileSystem::BlockIndex last_block_logical_index = (offset + count) / block_size;
    if (last_block_logical_index >= m_block_list.size())
        last_block_logical_index = m_block_list.size() - 1;

    size_t offset_into_first_block = offset % block_size;

    size_t nwritten = 0;
    auto remaining_count = min((off_t)count, (off_t)new_size - offset);

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FSInode[{}]::write_bytes_locked(): Writing {} bytes, {} bytes into inode from {}", identifier(), count, offset, data.user_or_kernel_ptr());

    for (auto bi = first_block_logical_index; remaining_count && bi <= last_block_logical_index; bi = bi.value() + 1) {
        size_t offset_into_block = (bi == first_block_logical_index) ? offset_into_first_block : 0;
        size_t num_bytes_to_copy = min((size_t)block_size - offset_into_block, (size_t)remaining_count);
        dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_bytes_locked(): Writing block {} (offset_into_block: {})", identifier(), m_block_list[bi.value()], offset_into_block);
        if (auto result = fs().write_block(m_block_list[bi.value()], data.offset(nwritten), num_bytes_to_copy, offset_into_block, allow_cache); result.is_error()) {
            dbgln("Ext2FSInode[{}]::write_bytes_locked(): Failed to write block {} (index {})", identifier(), m_block_list[bi.value()], bi);
            return result.release_error();
        }
        remaining_count -= num_bytes_to_copy;
        nwritten += num_bytes_to_copy;
    }

    did_modify_contents();

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FSInode[{}]::write_bytes_locked(): After write, i_size={}, i_blocks={} ({} blocks in list)", identifier(), size(), m_raw_inode.i_blocks, m_block_list.size());
    return nwritten;
}

ErrorOr<void> Ext2FSInode::traverse_as_directory(Function<ErrorOr<void>(FileSystem::DirectoryEntryView const&)> callback) const
{
    MutexLocker locker(m_inode_lock);
    VERIFY(is_directory());

    u8 buffer[max_block_size];
    auto buf = UserOrKernelBuffer::for_kernel_buffer(buffer);

    auto block_size = fs().logical_block_size();
    auto file_size = size();

    // Directory entries are guaranteed not to span multiple blocks,
    // so we can iterate over blocks separately.

    for (u64 offset = 0; offset < file_size; offset += block_size) {
        TRY(read_bytes(offset, block_size, buf, nullptr));

        using ext2_extended_dir_entry = ext2_dir_entry_2;
        auto* entry = reinterpret_cast<ext2_extended_dir_entry*>(buffer);
        auto* entries_end = reinterpret_cast<ext2_extended_dir_entry*>(buffer + block_size);
        while (entry < entries_end) {
            if (entry->inode != 0) {
                dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::traverse_as_directory(): inode {}, name_len: {}, rec_len: {}, file_type: {}, name: {}", identifier(), entry->inode, entry->name_len, entry->rec_len, entry->file_type, StringView(entry->name, entry->name_len));
                TRY(callback({ { entry->name, entry->name_len }, { fsid(), entry->inode }, entry->file_type }));
            }
            entry = (ext2_extended_dir_entry*)((char*)entry + entry->rec_len);
        }
    }

    return {};
}

ErrorOr<void> Ext2FSInode::write_directory(Vector<Ext2FSDirectoryEntry>& entries)
{
    MutexLocker locker(m_inode_lock);
    auto block_size = fs().logical_block_size();

    // Calculate directory size and record length of entries so that
    // the following constraints are met:
    // - All used blocks must be entirely filled.
    // - Entries are aligned on a 4-byte boundary.
    // - No entry may span multiple blocks.
    size_t directory_size = 0;
    size_t space_in_block = block_size;
    for (size_t i = 0; i < entries.size(); ++i) {
        auto& entry = entries[i];
        entry.record_length = EXT2_DIR_REC_LEN(entry.name->length());
        space_in_block -= entry.record_length;
        if (i + 1 < entries.size()) {
            if (EXT2_DIR_REC_LEN(entries[i + 1].name->length()) > space_in_block) {
                entry.record_length += space_in_block;
                space_in_block = block_size;
            }
        } else {
            entry.record_length += space_in_block;
        }
        directory_size += entry.record_length;
    }

    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_directory(): New directory contents to write (size {}):", identifier(), directory_size);

    auto directory_data = TRY(ByteBuffer::create_uninitialized(directory_size));
    FixedMemoryStream stream { directory_data.bytes() };

    for (auto& entry : entries) {
        dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_directory(): Writing inode: {}, name_len: {}, rec_len: {}, file_type: {}, name: {}", identifier(), entry.inode_index, u16(entry.name->length()), u16(entry.record_length), u8(entry.file_type), entry.name);

        MUST(stream.write_value<u32>(entry.inode_index.value()));
        MUST(stream.write_value<u16>(entry.record_length));
        MUST(stream.write_value<u8>(entry.name->length()));
        MUST(stream.write_value<u8>(entry.file_type));
        MUST(stream.write_until_depleted(entry.name->bytes()));
        int padding = entry.record_length - entry.name->length() - 8;
        for (int j = 0; j < padding; ++j)
            MUST(stream.write_value<u8>(0));
    }

    auto serialized_bytes_count = TRY(stream.tell());
    VERIFY(serialized_bytes_count == directory_size);

    TRY(resize(serialized_bytes_count));

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(directory_data.data());
    auto nwritten = TRY(write_bytes(0, serialized_bytes_count, buffer, nullptr));
    set_metadata_dirty(true);
    if (nwritten != directory_data.size())
        return EIO;
    return {};
}

ErrorOr<NonnullRefPtr<Inode>> Ext2FSInode::create_child(StringView name, mode_t mode, dev_t dev, UserID uid, GroupID gid)
{
    if (Kernel::is_directory(mode))
        return fs().create_directory(*this, name, mode, uid, gid);
    return fs().create_inode(*this, name, mode, dev, uid, gid);
}

ErrorOr<void> Ext2FSInode::add_child(Inode& child, StringView name, mode_t mode)
{
    MutexLocker locker(m_inode_lock);
    VERIFY(is_directory());

    if (name.length() > EXT2_NAME_LEN)
        return ENAMETOOLONG;

    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::add_child(): Adding inode {} with name '{}' and mode {:o} to directory {}", identifier(), child.index(), name, mode, index());

    Vector<Ext2FSDirectoryEntry> entries;
    TRY(traverse_as_directory([&](auto& entry) -> ErrorOr<void> {
        if (name == entry.name)
            return EEXIST;
        auto entry_name = TRY(KString::try_create(entry.name));
        TRY(entries.try_append({ move(entry_name), entry.inode.index(), entry.file_type }));
        return {};
    }));

    TRY(child.increment_link_count());

    auto entry_name = TRY(KString::try_create(name));
    TRY(entries.try_empend(move(entry_name), child.index(), to_ext2_file_type(mode)));

    TRY(write_directory(entries));
    TRY(populate_lookup_cache());

    auto cache_entry_name = TRY(KString::try_create(name));
    TRY(m_lookup_cache.try_set(move(cache_entry_name), child.index()));
    did_add_child(child.identifier(), name);
    return {};
}

ErrorOr<void> Ext2FSInode::remove_child(StringView name)
{
    MutexLocker locker(m_inode_lock);
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::remove_child(): Removing '{}'", identifier(), name);
    VERIFY(is_directory());

    TRY(populate_lookup_cache());

    auto it = m_lookup_cache.find(name);
    if (it == m_lookup_cache.end())
        return ENOENT;
    auto child_inode_index = (*it).value;

    InodeIdentifier child_id { fsid(), child_inode_index };

    Vector<Ext2FSDirectoryEntry> entries;
    TRY(traverse_as_directory([&](auto& entry) -> ErrorOr<void> {
        if (name != entry.name) {
            auto entry_name = TRY(KString::try_create(entry.name));
            TRY(entries.try_append({ move(entry_name), entry.inode.index(), entry.file_type }));
        }
        return {};
    }));

    TRY(write_directory(entries));

    m_lookup_cache.remove(it);

    auto child_inode = TRY(fs().get_inode(child_id));
    TRY(child_inode->decrement_link_count());

    did_remove_child(child_id, name);
    return {};
}

ErrorOr<void> Ext2FSInode::replace_child(StringView name, Inode& child)
{
    MutexLocker locker(m_inode_lock);
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::replace_child(): Replacing '{}' with inode {}", identifier(), name, child.index());
    VERIFY(is_directory());

    TRY(populate_lookup_cache());

    if (name.length() > EXT2_NAME_LEN)
        return ENAMETOOLONG;

    Vector<Ext2FSDirectoryEntry> entries;

    Optional<InodeIndex> old_child_index;
    TRY(traverse_as_directory([&](auto& entry) -> ErrorOr<void> {
        auto is_replacing_this_inode = name == entry.name;
        auto inode_index = is_replacing_this_inode ? child.index() : entry.inode.index();

        auto entry_name = TRY(KString::try_create(entry.name));
        TRY(entries.try_empend(move(entry_name), inode_index, to_ext2_file_type(child.mode())));
        if (is_replacing_this_inode)
            old_child_index = entry.inode.index();

        return {};
    }));

    if (!old_child_index.has_value())
        return ENOENT;

    auto old_child = TRY(fs().get_inode({ fsid(), *old_child_index }));

    auto old_index_it = m_lookup_cache.find(name);
    VERIFY(old_index_it != m_lookup_cache.end());
    old_index_it->value = child.index();

    // NOTE: Between this line and the write_directory line, all operations must
    //       be atomic. Any changes made should be reverted.
    TRY(child.increment_link_count());

    auto maybe_decrement_error = old_child->decrement_link_count();
    if (maybe_decrement_error.is_error()) {
        old_index_it->value = *old_child_index;
        MUST(child.decrement_link_count());
        return maybe_decrement_error;
    }

    // FIXME: The filesystem is left in an inconsistent state if this fails.
    //        Revert the changes made above if we can't write_directory.
    //        Ideally, decrement should be the last operation, but we currently
    //        can't "un-write" a directory entry list.
    TRY(write_directory(entries));

    // TODO: Emit a did_replace_child event.

    return {};
}

ErrorOr<void> Ext2FSInode::populate_lookup_cache()
{
    VERIFY(m_inode_lock.is_exclusively_locked_by_current_thread());
    if (!m_lookup_cache.is_empty())
        return {};
    HashMap<NonnullOwnPtr<KString>, InodeIndex> children;

    TRY(traverse_as_directory([&children](auto& entry) -> ErrorOr<void> {
        auto entry_name = TRY(KString::try_create(entry.name));
        TRY(children.try_set(move(entry_name), entry.inode.index()));
        return {};
    }));

    VERIFY(m_lookup_cache.is_empty());
    m_lookup_cache = move(children);
    return {};
}

ErrorOr<NonnullRefPtr<Inode>> Ext2FSInode::lookup(StringView name)
{
    VERIFY(is_directory());
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]:lookup(): Looking up '{}'", identifier(), name);

    InodeIndex inode_index;
    {
        MutexLocker locker(m_inode_lock);
        TRY(populate_lookup_cache());
        auto it = m_lookup_cache.find(name);
        if (it == m_lookup_cache.end()) {
            dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]:lookup(): '{}' not found", identifier(), name);
            return ENOENT;
        }
        inode_index = it->value;
    }

    return fs().get_inode({ fsid(), inode_index });
}

ErrorOr<void> Ext2FSInode::update_timestamps(Optional<UnixDateTime> atime, Optional<UnixDateTime> ctime, Optional<UnixDateTime> mtime)
{
    MutexLocker locker(m_inode_lock);
    if (fs().is_readonly())
        return EROFS;
    if (atime.value_or({}).to_timespec().tv_sec > NumericLimits<i32>::max())
        return EINVAL;
    if (ctime.value_or({}).to_timespec().tv_sec > NumericLimits<i32>::max())
        return EINVAL;
    if (mtime.value_or({}).to_timespec().tv_sec > NumericLimits<i32>::max())
        return EINVAL;
    if (atime.has_value())
        m_raw_inode.i_atime = atime.value().to_timespec().tv_sec;
    if (ctime.has_value())
        m_raw_inode.i_ctime = ctime.value().to_timespec().tv_sec;
    if (mtime.has_value())
        m_raw_inode.i_mtime = mtime.value().to_timespec().tv_sec;
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> Ext2FSInode::increment_link_count()
{
    MutexLocker locker(m_inode_lock);
    if (fs().is_readonly())
        return EROFS;
    constexpr size_t max_link_count = 65535;
    if (m_raw_inode.i_links_count == max_link_count)
        return EMLINK;
    ++m_raw_inode.i_links_count;
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> Ext2FSInode::decrement_link_count()
{
    MutexLocker locker(m_inode_lock);
    if (fs().is_readonly())
        return EROFS;
    VERIFY(m_raw_inode.i_links_count);

    --m_raw_inode.i_links_count;
    set_metadata_dirty(true);
    if (m_raw_inode.i_links_count == 0)
        did_delete_self();

    if (ref_count() == 1 && m_raw_inode.i_links_count == 0)
        fs().uncache_inode(index());

    return {};
}

ErrorOr<void> Ext2FSInode::chmod(mode_t mode)
{
    MutexLocker locker(m_inode_lock);
    if (m_raw_inode.i_mode == mode)
        return {};
    m_raw_inode.i_mode = mode;
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> Ext2FSInode::chown(UserID uid, GroupID gid)
{
    MutexLocker locker(m_inode_lock);
    if (m_raw_inode.i_uid == uid && m_raw_inode.i_gid == gid)
        return {};
    m_raw_inode.i_uid = uid.value();
    m_raw_inode.i_gid = gid.value();
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> Ext2FSInode::truncate(u64 size)
{
    MutexLocker locker(m_inode_lock);
    if (static_cast<u64>(m_raw_inode.i_size) == size)
        return {};
    TRY(resize(size));
    set_metadata_dirty(true);
    return {};
}

ErrorOr<int> Ext2FSInode::get_block_address(int index)
{
    MutexLocker locker(m_inode_lock);

    if (m_block_list.is_empty())
        m_block_list = TRY(compute_block_list());

    if (index < 0 || (size_t)index >= m_block_list.size())
        return 0;

    return m_block_list[index].value();
}

}
