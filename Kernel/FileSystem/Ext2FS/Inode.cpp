/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntegralMath.h>
#include <AK/IterationDecision.h>
#include <AK/MemoryStream.h>
#include <Kernel/API/POSIX/errno.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>
#include <Kernel/FileSystem/Ext2FS/Inode.h>
#include <Kernel/FileSystem/InodeMetadata.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

static constexpr size_t max_inline_symlink_length = 60;

u8 Ext2FSInode::to_ext2_file_type(mode_t mode)
{
    if (Kernel::is_regular_file(mode))
        return EXT2_FT_REG_FILE;
    if (Kernel::is_directory(mode))
        return EXT2_FT_DIR;
    if (Kernel::is_character_device(mode))
        return EXT2_FT_CHRDEV;
    if (Kernel::is_block_device(mode))
        return EXT2_FT_BLKDEV;
    if (Kernel::is_fifo(mode))
        return EXT2_FT_FIFO;
    if (Kernel::is_socket(mode))
        return EXT2_FT_SOCK;
    if (Kernel::is_symlink(mode))
        return EXT2_FT_SYMLINK;
    return EXT2_FT_UNKNOWN;
}

bool Ext2FSInode::is_within_inode_bounds(FlatPtr base, FlatPtr value_offset, size_t value_size) const
{
    if (value_offset - base - value_size < EXT2_GOOD_OLD_INODE_SIZE)
        return true;

    VERIFY(static_cast<u64>(EXT2_GOOD_OLD_INODE_SIZE + m_raw_inode.i_extra_isize) <= fs().inode_size());

    return value_offset - base + value_size <= static_cast<u64>(EXT2_GOOD_OLD_INODE_SIZE + m_raw_inode.i_extra_isize);
}

ErrorOr<void> Ext2FSInode::write_singly_indirect_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index)
{
    auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    auto const block_size = fs().logical_block_size();

    auto offset_in_block = logical_block_index.value() - EXT2_IND_BLOCK;

    auto singly_indirect_block_storage = TRY(ByteBuffer::create_zeroed(block_size));
    auto singly_indirect_block_contents = Span<u32> { bit_cast<u32*>(singly_indirect_block_storage.data()), entries_per_block };
    auto singly_indirect_block_buffer = UserOrKernelBuffer::for_kernel_buffer(singly_indirect_block_storage.data());

    if (m_raw_inode.i_block[EXT2_IND_BLOCK] == 0) [[unlikely]] {
        m_raw_inode.i_block[EXT2_IND_BLOCK] = TRY(allocate_and_zero_block());
        set_metadata_dirty(true);
    }

    TRY(fs().read_block(m_raw_inode.i_block[EXT2_IND_BLOCK], &singly_indirect_block_buffer, block_size, 0));

    singly_indirect_block_contents[offset_in_block] = on_disk_index.value();
    TRY(fs().write_block(m_raw_inode.i_block[EXT2_IND_BLOCK], singly_indirect_block_buffer, block_size));

    if (on_disk_index != 0)
        return {};

    if (!singly_indirect_block_contents.filled_with(0))
        return {};

    TRY(fs().set_block_allocation_state(m_raw_inode.i_block[EXT2_IND_BLOCK], false));
    m_raw_inode.i_block[EXT2_IND_BLOCK] = 0;
    m_raw_inode.i_blocks -= fs().i_blocks_increment();
    set_metadata_dirty(true);

    return {};
}

ErrorOr<void> Ext2FSInode::write_doubly_indirect_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index)
{
    auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    auto const block_size = fs().logical_block_size();

    auto const offset = logical_block_index.value() - singly_indirect_block_capacity();
    auto const offset_in_doubly_indirect_block = offset / entries_per_block;
    auto const offset_in_singly_indirect_block = offset % entries_per_block;

    auto doubly_indirect_block_storage = TRY(ByteBuffer::create_zeroed(block_size));
    auto doubly_indirect_block_contents = Span<u32> { bit_cast<u32*>(doubly_indirect_block_storage.data()), entries_per_block };
    auto doubly_indirect_block_buffer = UserOrKernelBuffer::for_kernel_buffer(doubly_indirect_block_storage.data());

    auto singly_indirect_block_storage = TRY(ByteBuffer::create_zeroed(block_size));
    auto singly_indirect_block_contents = Span<u32> { bit_cast<u32*>(singly_indirect_block_storage.data()), entries_per_block };
    auto singly_indirect_block_buffer = UserOrKernelBuffer::for_kernel_buffer(singly_indirect_block_storage.data());

    if (m_raw_inode.i_block[EXT2_DIND_BLOCK] == 0) [[unlikely]] {
        m_raw_inode.i_block[EXT2_DIND_BLOCK] = TRY(allocate_and_zero_block());
        set_metadata_dirty(true);
    }

    TRY(fs().read_block(m_raw_inode.i_block[EXT2_DIND_BLOCK], &doubly_indirect_block_buffer, block_size, 0));

    if (doubly_indirect_block_contents[offset_in_doubly_indirect_block] == 0) [[unlikely]] {
        doubly_indirect_block_contents[offset_in_doubly_indirect_block] = TRY(allocate_and_zero_block());
        TRY(fs().write_block(m_raw_inode.i_block[EXT2_DIND_BLOCK], doubly_indirect_block_buffer, block_size));
    }

    TRY(fs().read_block(doubly_indirect_block_contents[offset_in_doubly_indirect_block], &singly_indirect_block_buffer, block_size, 0));

    singly_indirect_block_contents[offset_in_singly_indirect_block] = on_disk_index.value();
    TRY(fs().write_block(doubly_indirect_block_contents[offset_in_doubly_indirect_block], singly_indirect_block_buffer, block_size));

    if (on_disk_index != 0)
        return {};

    if (!singly_indirect_block_contents.filled_with(0))
        return {};

    TRY(fs().set_block_allocation_state(doubly_indirect_block_contents[offset_in_doubly_indirect_block], false));
    doubly_indirect_block_contents[offset_in_doubly_indirect_block] = 0;
    m_raw_inode.i_blocks -= fs().i_blocks_increment();
    TRY(fs().write_block(m_raw_inode.i_block[EXT2_DIND_BLOCK], doubly_indirect_block_buffer, block_size));

    if (!doubly_indirect_block_contents.filled_with(0))
        return {};

    TRY(fs().set_block_allocation_state(m_raw_inode.i_block[EXT2_DIND_BLOCK], false));
    m_raw_inode.i_block[EXT2_DIND_BLOCK] = 0;
    m_raw_inode.i_blocks -= fs().i_blocks_increment();
    set_metadata_dirty(true);

    return {};
}

ErrorOr<void> Ext2FSInode::write_triply_indirect_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index)
{
    auto const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());
    auto const block_size = fs().logical_block_size();

    auto const offset = logical_block_index.value() - doubly_indirect_block_capacity();
    auto const offset_in_triply_indirect_block = offset / (entries_per_block * entries_per_block);
    auto const skipped_blocks = entries_per_block * entries_per_block * offset_in_triply_indirect_block;
    auto const offset_in_doubly_indirect_block = (offset - skipped_blocks) / entries_per_block;
    auto const offset_in_singly_indirect_block = offset % entries_per_block;

    auto triply_indirect_block_storage = TRY(ByteBuffer::create_zeroed(block_size));
    auto triply_indirect_block_contents = Span<u32> { bit_cast<u32*>(triply_indirect_block_storage.data()), entries_per_block };
    auto triply_indirect_block_buffer = UserOrKernelBuffer::for_kernel_buffer(triply_indirect_block_storage.data());

    auto doubly_indirect_block_storage = TRY(ByteBuffer::create_zeroed(block_size));
    auto doubly_indirect_block_contents = Span<u32> { bit_cast<u32*>(doubly_indirect_block_storage.data()), entries_per_block };
    auto doubly_indirect_block_buffer = UserOrKernelBuffer::for_kernel_buffer(doubly_indirect_block_storage.data());

    auto singly_indirect_block_storage = TRY(ByteBuffer::create_zeroed(block_size));
    auto singly_indirect_block_contents = Span<u32> { bit_cast<u32*>(singly_indirect_block_storage.data()), entries_per_block };
    auto singly_indirect_block_buffer = UserOrKernelBuffer::for_kernel_buffer(singly_indirect_block_storage.data());

    if (m_raw_inode.i_block[EXT2_TIND_BLOCK] == 0) [[unlikely]] {
        m_raw_inode.i_block[EXT2_TIND_BLOCK] = TRY(allocate_and_zero_block());
        set_metadata_dirty(true);
    }

    TRY(fs().read_block(m_raw_inode.i_block[EXT2_TIND_BLOCK], &triply_indirect_block_buffer, block_size, 0));

    if (triply_indirect_block_contents[offset_in_triply_indirect_block] == 0) [[unlikely]] {
        triply_indirect_block_contents[offset_in_triply_indirect_block] = TRY(allocate_and_zero_block());
        TRY(fs().write_block(m_raw_inode.i_block[EXT2_TIND_BLOCK], triply_indirect_block_buffer, block_size));
    }

    TRY(fs().read_block(triply_indirect_block_contents[offset_in_triply_indirect_block], &doubly_indirect_block_buffer, block_size, 0));

    if (doubly_indirect_block_contents[offset_in_doubly_indirect_block] == 0) [[unlikely]] {
        doubly_indirect_block_contents[offset_in_doubly_indirect_block] = TRY(allocate_and_zero_block());
        TRY(fs().write_block(triply_indirect_block_contents[offset_in_triply_indirect_block], doubly_indirect_block_buffer, block_size));
    }

    TRY(fs().read_block(doubly_indirect_block_contents[offset_in_doubly_indirect_block], &singly_indirect_block_buffer, block_size, 0));

    singly_indirect_block_contents[offset_in_singly_indirect_block] = on_disk_index.value();
    TRY(fs().write_block(doubly_indirect_block_contents[offset_in_doubly_indirect_block], singly_indirect_block_buffer, block_size));

    if (on_disk_index != 0)
        return {};

    if (!singly_indirect_block_contents.filled_with(0))
        return {};

    TRY(fs().set_block_allocation_state(doubly_indirect_block_contents[offset_in_doubly_indirect_block], false));
    doubly_indirect_block_contents[offset_in_doubly_indirect_block] = 0;
    m_raw_inode.i_blocks -= fs().i_blocks_increment();
    TRY(fs().write_block(triply_indirect_block_contents[offset_in_triply_indirect_block], doubly_indirect_block_buffer, block_size));

    if (!doubly_indirect_block_contents.filled_with(0))
        return {};

    TRY(fs().set_block_allocation_state(triply_indirect_block_contents[offset_in_triply_indirect_block], false));
    triply_indirect_block_contents[offset_in_triply_indirect_block] = 0;
    m_raw_inode.i_blocks -= fs().i_blocks_increment();
    TRY(fs().write_block(m_raw_inode.i_block[EXT2_TIND_BLOCK], triply_indirect_block_buffer, block_size));

    if (!triply_indirect_block_contents.filled_with(0))
        return {};

    TRY(fs().set_block_allocation_state(m_raw_inode.i_block[EXT2_TIND_BLOCK], false));
    m_raw_inode.i_block[EXT2_TIND_BLOCK] = 0;
    m_raw_inode.i_blocks -= fs().i_blocks_increment();
    set_metadata_dirty(true);

    return {};
}

ErrorOr<u32> Ext2FSInode::allocate_and_zero_block()
{
    auto const block_size = fs().logical_block_size();

    auto blocks = TRY(fs().allocate_blocks(fs().group_index_from_inode(index()), 1));
    m_raw_inode.i_blocks += fs().i_blocks_increment();
    auto block = blocks.first();

    auto buffer_content = TRY(ByteBuffer::create_zeroed(block_size));
    TRY(fs().write_block(block, UserOrKernelBuffer::for_kernel_buffer(buffer_content.data()), block_size));
    return block.value();
}

ErrorOr<void> Ext2FSInode::write_block_pointer(BlockBasedFileSystem::BlockIndex logical_block_index, BlockBasedFileSystem::BlockIndex on_disk_index)
{
    VERIFY(m_inode_lock.is_locked());

    if (logical_block_index < EXT2_NDIR_BLOCKS) {
        if (m_raw_inode.i_block[logical_block_index.value()] != on_disk_index) {
            m_raw_inode.i_block[logical_block_index.value()] = on_disk_index.value();
            set_metadata_dirty(true);
        }
        return {};
    }

    if (logical_block_index < singly_indirect_block_capacity())
        return write_singly_indirect_block_pointer(logical_block_index, on_disk_index);

    if (logical_block_index < doubly_indirect_block_capacity())
        return write_doubly_indirect_block_pointer(logical_block_index, on_disk_index);

    if (logical_block_index < triply_indirect_block_capacity())
        return write_triply_indirect_block_pointer(logical_block_index, on_disk_index);

    VERIFY_NOT_REACHED();
}

ErrorOr<Ext2FS::BlockList> Ext2FSInode::compute_block_list(BlockBasedFileSystem::BlockIndex first_block, BlockBasedFileSystem::BlockIndex last_block) const
{
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::block_list_for_inode(): i_size={}, i_blocks={}", identifier(), m_raw_inode.i_size, m_raw_inode.i_blocks);
    Ext2FS::BlockList list {};

    // If we are handling a symbolic link, the path is stored in the 60 bytes in
    // the inode that are used for the 12 direct and 3 indirect block pointers,
    // If the path is longer than 60 characters, a block is allocated, and the
    // block contains the destination path. The file size corresponds to the
    // path length of the destination.
    if (Kernel::is_symlink(m_raw_inode.i_mode) && m_raw_inode.i_blocks == 0)
        return list;

    unsigned const block_size = fs().logical_block_size();
    unsigned const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());

    auto set_block = [&](auto logical_index, auto on_disk_index) -> ErrorOr<IterationDecision> {
        if (logical_index < first_block)
            return IterationDecision::Continue;
        if (logical_index > last_block)
            return IterationDecision::Break;

        TRY(list.try_set(logical_index, on_disk_index));
        return IterationDecision::Continue;
    };

    auto process_block_array = [&](auto current_logical_index, unsigned level, auto array_block_index, ByteBuffer& array_storage, auto&& callback) -> ErrorOr<IterationDecision> {
        TRY(array_storage.try_resize(block_size));
        auto* array = (u32*)array_storage.data();
        auto buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)array);
        TRY(fs().read_block(array_block_index, &buffer, block_size, 0));
        for (unsigned i = 0; i < block_size / sizeof(u32); ++i) {
            if (array[i] != 0) {
                if (TRY(callback(current_logical_index + i * AK::pow(entries_per_block, level - 1), array[i])) == IterationDecision::Break)
                    return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    };

    if (first_block < EXT2_NDIR_BLOCKS) {
        for (size_t i = 0; i < EXT2_NDIR_BLOCKS; ++i) {
            if (m_raw_inode.i_block[i] != 0)
                TRY(set_block(i, m_raw_inode.i_block[i]));
        }
    }

    ByteBuffer block_storage[3] = {};

    if (first_block < singly_indirect_block_capacity() && m_raw_inode.i_block[EXT2_IND_BLOCK]) {
        TRY(process_block_array(EXT2_NDIR_BLOCKS, 1, m_raw_inode.i_block[EXT2_IND_BLOCK], block_storage[0], [&](auto logical_block_index, auto on_disk_index) -> ErrorOr<IterationDecision> {
            return set_block(logical_block_index, on_disk_index);
        }));
    }

    if (first_block < doubly_indirect_block_capacity() && m_raw_inode.i_block[EXT2_DIND_BLOCK]) {
        TRY(process_block_array(singly_indirect_block_capacity(), 2, m_raw_inode.i_block[EXT2_DIND_BLOCK], block_storage[1], [&](auto logical_block_index, auto on_disk_index) -> ErrorOr<IterationDecision> {
            return process_block_array(logical_block_index, 1, on_disk_index, block_storage[0], [&](auto logical_block_index2, auto on_disk_index2) -> ErrorOr<IterationDecision> {
                return set_block(logical_block_index2, on_disk_index2);
            });
        }));
    }

    if (first_block < triply_indirect_block_capacity() && m_raw_inode.i_block[EXT2_TIND_BLOCK]) {
        TRY(process_block_array(doubly_indirect_block_capacity(), 3, m_raw_inode.i_block[EXT2_TIND_BLOCK], block_storage[2], [&](auto logical_block_index, auto on_disk_index) -> ErrorOr<IterationDecision> {
            return process_block_array(logical_block_index, 2, on_disk_index, block_storage[1], [&](auto logical_block_index2, auto on_disk_index2) -> ErrorOr<IterationDecision> {
                return process_block_array(logical_block_index2, 1, on_disk_index2, block_storage[0], [&](auto logical_block_index3, auto on_disk_index3) -> ErrorOr<IterationDecision> {
                    return set_block(logical_block_index3, on_disk_index3);
                });
            });
        }));
    }

    return list;
}

ErrorOr<void> Ext2FSInode::free_all_blocks()
{
    MutexLocker locker(m_inode_lock);

    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::free_all_blocks(): i_size={}, i_blocks={}", identifier(), m_raw_inode.i_size, m_raw_inode.i_blocks);

    if (Kernel::is_symlink(m_raw_inode.i_mode) && m_raw_inode.i_blocks == 0)
        return {};

    unsigned const block_size = fs().logical_block_size();
    unsigned const entries_per_block = EXT2_ADDR_PER_BLOCK(&fs().super_block());

    auto deallocate_block = [&](auto on_disk_block_index) -> ErrorOr<void> {
        TRY(fs().set_block_allocation_state(on_disk_block_index, false));
        m_raw_inode.i_blocks -= fs().i_blocks_increment();
        return {};
    };

    auto process_block_array = [&](auto current_logical_index, unsigned level, auto array_block_index, ByteBuffer& array_storage, auto&& callback) -> ErrorOr<void> {
        TRY(array_storage.try_resize(block_size));

        auto* array = bit_cast<u32*>(array_storage.data());
        auto buffer = UserOrKernelBuffer::for_kernel_buffer(bit_cast<u8*>(array));
        TRY(fs().read_block(array_block_index, &buffer, block_size, 0));

        for (unsigned i = 0; i < block_size / sizeof(u32); ++i) {
            if (array[i] != 0)
                TRY(callback(current_logical_index + i * AK::pow(entries_per_block, level - 1), array[i]));
        }

        return deallocate_block(array_block_index);
    };

    for (size_t i = 0; i < EXT2_NDIR_BLOCKS; ++i) {
        if (m_raw_inode.i_block[i] != 0)
            TRY(deallocate_block(m_raw_inode.i_block[i]));
    }

    ByteBuffer block_storage[3] = {};

    if (m_raw_inode.i_block[EXT2_IND_BLOCK]) {
        TRY(process_block_array(EXT2_NDIR_BLOCKS, 1, m_raw_inode.i_block[EXT2_IND_BLOCK], block_storage[0], [&]([[maybe_unused]] auto logical_block_index, auto on_disk_index) -> ErrorOr<void> {
            return deallocate_block(on_disk_index);
        }));
    }

    if (m_raw_inode.i_block[EXT2_DIND_BLOCK]) {
        TRY(process_block_array(singly_indirect_block_capacity(), 2, m_raw_inode.i_block[EXT2_DIND_BLOCK], block_storage[1], [&](auto logical_block_index, auto on_disk_index) -> ErrorOr<void> {
            return process_block_array(logical_block_index, 1, on_disk_index, block_storage[0], [&]([[maybe_unused]] auto logical_block_index2, auto on_disk_index2) -> ErrorOr<void> {
                return deallocate_block(on_disk_index2);
            });
        }));
    }

    if (m_raw_inode.i_block[EXT2_TIND_BLOCK]) {
        TRY(process_block_array(doubly_indirect_block_capacity(), 3, m_raw_inode.i_block[EXT2_TIND_BLOCK], block_storage[2], [&](auto logical_block_index, auto on_disk_index) -> ErrorOr<void> {
            return process_block_array(logical_block_index, 2, on_disk_index, block_storage[1], [&](auto logical_block_index2, auto on_disk_index2) -> ErrorOr<void> {
                return process_block_array(logical_block_index2, 1, on_disk_index2, block_storage[0], [&]([[maybe_unused]] auto logical_block_index3, auto on_disk_index3) -> ErrorOr<void> {
                    return deallocate_block(on_disk_index3);
                });
            });
        }));
    }

    return {};
}

Ext2FSInode::Ext2FSInode(Ext2FS& fs, InodeIndex index)
    : Inode(fs, index)
    , m_block_view(*this)
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
    metadata.uid = inode_uid(m_raw_inode);
    metadata.gid = inode_gid(m_raw_inode);
    metadata.link_count = m_raw_inode.i_links_count;

    auto decode_time = [this](u32 const& time, u32 const& time_extra) {
        // NOTE: All the *_extra fields have to be bounds-checked in case we have oddly-sized inodes.
        // This is simply a correctness measure, since an OOB read wouldn't happen anyway due to the
        // fact that we always store the raw inode as an ext2_inode_large.
        if (is_within_inode_bounds(bit_cast<FlatPtr>(&m_raw_inode), bit_cast<FlatPtr>(&time_extra), sizeof(time_extra))) {
            time_t seconds = decode_seconds_with_extra(time, time_extra);
            u32 nanoseconds = decode_nanoseconds_from_extra(time_extra);
            return UnixDateTime::from_unix_timespec({ .tv_sec = seconds, .tv_nsec = nanoseconds });
        }

        return UnixDateTime::from_seconds_since_epoch(static_cast<i32>(time));
    };

    metadata.atime = decode_time(m_raw_inode.i_atime, m_raw_inode.i_atime_extra);
    metadata.mtime = decode_time(m_raw_inode.i_mtime, m_raw_inode.i_mtime_extra);

    // NOTE: There's no i_dtime_extra, so we use i_ctime_extra to approximate the right epoch for metadata.dtime.
    if (is_within_inode_bounds(bit_cast<FlatPtr>(&m_raw_inode), bit_cast<FlatPtr>(&m_raw_inode.i_ctime_extra), sizeof(m_raw_inode.i_ctime_extra))) {
        time_t ctime_seconds = decode_seconds_with_extra(m_raw_inode.i_ctime, m_raw_inode.i_ctime_extra);
        u32 ctime_nanoseconds = decode_nanoseconds_from_extra(m_raw_inode.i_ctime_extra);

        metadata.ctime = UnixDateTime::from_unix_timespec({ .tv_sec = ctime_seconds, .tv_nsec = ctime_nanoseconds });
        metadata.dtime = UnixDateTime::from_seconds_since_epoch(decode_seconds_with_extra(m_raw_inode.i_dtime, m_raw_inode.i_ctime_extra));
    } else {
        metadata.ctime = UnixDateTime::from_seconds_since_epoch(static_cast<i32>(m_raw_inode.i_ctime));
        metadata.dtime = UnixDateTime::from_seconds_since_epoch(static_cast<i32>(m_raw_inode.i_dtime));
    }

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
    if (!is_metadata_dirty())
        return {};

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

    bool allow_cache = !description || !description->is_direct();

    int const block_size = fs().logical_block_size();

    BlockBasedFileSystem::BlockIndex first_block_logical_index = offset / block_size;

    int offset_into_first_block = offset % block_size;

    size_t nread = 0;
    auto remaining_count = min((off_t)count, (off_t)size() - offset);
    auto current_block_logical_index = first_block_logical_index;

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FSInode[{}]::read_bytes(): Reading up to {} bytes, {} bytes into inode to {}", identifier(), count, offset, buffer.user_or_kernel_ptr());

    while (remaining_count) {
        auto block_index = TRY(m_block_view.get_block(current_block_logical_index));
        size_t offset_into_block = (current_block_logical_index == first_block_logical_index) ? offset_into_first_block : 0;
        size_t num_bytes_to_copy = min((size_t)block_size - offset_into_block, (size_t)remaining_count);
        auto buffer_offset = buffer.offset(nread);
        if (block_index.value() == 0) {
            // This is a hole, act as if it's filled with zeroes.
            TRY(buffer_offset.memset(0, num_bytes_to_copy));
        } else {
            if (auto result = fs().read_block(block_index, &buffer_offset, num_bytes_to_copy, offset_into_block, allow_cache); result.is_error()) {
                dmesgln("Ext2FSInode[{}]::read_bytes(): Failed to read block {} (index {})", identifier(), block_index.value(), current_block_logical_index);
                return result.release_error();
            }
        }
        current_block_logical_index = current_block_logical_index.value() + 1;
        remaining_count -= num_bytes_to_copy;
        nread += num_bytes_to_copy;
    }

    return nread;
}

ErrorOr<void> Ext2FSInode::resize(u64 new_size)
{
    VERIFY(m_inode_lock.is_locked());
    if (size() == new_size)
        return {};

    if (!((u32)fs().get_features_readonly() & (u32)Ext2FS::FeaturesReadOnly::FileSize64bits) && (new_size >= static_cast<u32>(-1)))
        return ENOSPC;

    if (new_size < size()) {
        auto block_size = fs().logical_block_size();
        BlockBasedFileSystem::BlockIndex first_block_logical_index = ceil_div(new_size, block_size);
        BlockBasedFileSystem::BlockIndex last_block_logical_index = size() / block_size;

        for (auto bi = first_block_logical_index; bi <= last_block_logical_index; bi = bi.value() + 1) {
            auto block = TRY(m_block_view.get_block(bi));
            if (block == 0) {
                // This is a hole, skip it.
                continue;
            }
            if (auto result = fs().set_block_allocation_state(block, false); result.is_error()) {
                dbgln("Ext2FSInode[{}]::resize(): Failed to free block {}: {}", identifier(), block, result.error());
                return result;
            }
            m_raw_inode.i_blocks -= fs().i_blocks_increment();
            TRY(m_block_view.write_block_pointer(bi, 0));
        }
    }

    m_raw_inode.i_size = new_size;
    if (Kernel::is_regular_file(m_raw_inode.i_mode))
        m_raw_inode.i_dir_acl = new_size >> 32;

    set_metadata_dirty(true);
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

    BlockBasedFileSystem::BlockIndex first_block_logical_index = offset / block_size;

    size_t offset_into_first_block = offset % block_size;

    size_t nwritten = 0;
    auto remaining_count = min((off_t)count, (off_t)new_size - offset);
    auto current_block_logical_index = first_block_logical_index;

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FSInode[{}]::write_bytes_locked(): Writing {} bytes, {} bytes into inode from {}", identifier(), count, offset, data.user_or_kernel_ptr());

    while (remaining_count) {
        size_t offset_into_block = (current_block_logical_index == first_block_logical_index) ? offset_into_first_block : 0;
        size_t num_bytes_to_copy = min((size_t)block_size - offset_into_block, (size_t)remaining_count);
        auto block_index = TRY(m_block_view.get_or_allocate_block(current_block_logical_index, num_bytes_to_copy != block_size, allow_cache));
        TRY(m_block_view.write_block_pointer(current_block_logical_index, block_index));

        dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_bytes_locked(): Writing block {} (offset_into_block: {})", identifier(), block_index, offset_into_block);
        if (auto result = fs().write_block(block_index, data.offset(nwritten), num_bytes_to_copy, offset_into_block, allow_cache); result.is_error()) {
            dbgln("Ext2FSInode[{}]::write_bytes_locked(): Failed to write block {} (index {})", identifier(), block_index, current_block_logical_index);
            return result.release_error();
        }
        current_block_logical_index = current_block_logical_index.value() + 1;
        remaining_count -= num_bytes_to_copy;
        nwritten += num_bytes_to_copy;
    }

    did_modify_contents();

    dbgln_if(EXT2_VERY_DEBUG, "Ext2FSInode[{}]::write_bytes_locked(): After write, i_size={}, i_blocks={}", identifier(), size(), m_raw_inode.i_blocks);
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

    bool has_file_type_attribute = has_flag(fs().get_features_optional(), Ext2FS::FeaturesOptional::ExtendedAttributes);

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
                TRY(callback({ { entry->name, entry->name_len }, { fsid(), entry->inode }, has_file_type_attribute ? entry->file_type : (u8)EXT2_FT_UNKNOWN }));
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
    bool has_file_type_attribute = has_flag(fs().get_features_optional(), Ext2FS::FeaturesOptional::ExtendedAttributes);

    for (auto& entry : entries) {
        dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::write_directory(): Writing inode: {}, name_len: {}, rec_len: {}, file_type: {}, name: {}", identifier(), entry.inode_index, u16(entry.name->length()), u16(entry.record_length), u8(entry.file_type), entry.name);

        MUST(stream.write_value<u32>(entry.inode_index.value()));
        MUST(stream.write_value<u16>(entry.record_length));
        MUST(stream.write_value<u8>(entry.name->length()));
        MUST(stream.write_value<u8>(has_file_type_attribute ? entry.file_type : EXT2_FT_UNKNOWN));
        MUST(stream.write_until_depleted(entry.name->bytes()));
        int padding = entry.record_length - entry.name->length() - 8;
        for (int j = 0; j < padding; ++j)
            MUST(stream.write_value<u8>(0));
    }

    auto serialized_bytes_count = TRY(stream.tell());
    VERIFY(serialized_bytes_count == directory_size);

    TRY(resize(serialized_bytes_count));

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(directory_data.data());
    auto nwritten = TRY(prepare_and_write_bytes_locked(0, serialized_bytes_count, buffer, nullptr));
    set_metadata_dirty(true);
    if (nwritten != directory_data.size())
        return EIO;
    return {};
}

ErrorOr<BlockBasedFileSystem::BlockIndex> Ext2FSInode::allocate_block(BlockBasedFileSystem::BlockIndex block_index, bool zero_newly_allocated_block, bool allow_cache)
{
    // FIXME: Preallocate some extra blocks here.
    auto blocks = TRY(fs().allocate_blocks(fs().group_index_from_inode(index()), 1));
    m_raw_inode.i_blocks += fs().i_blocks_increment();

    VERIFY(blocks.size() == 1);
    auto block = blocks.first();

    if (zero_newly_allocated_block) {
        u8 zero_buffer[PAGE_SIZE] {};
        if (auto result = fs().write_block(block, UserOrKernelBuffer::for_kernel_buffer(zero_buffer), fs().logical_block_size(), 0, allow_cache); result.is_error()) {
            dbgln("Ext2FSInode[{}]::allocate_block(): Failed to zero block {} (index {})", identifier(), block, block_index);
            return result.release_error();
        }
    }

    return block;
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
    bool has_file_type_attribute = has_flag(fs().get_features_optional(), Ext2FS::FeaturesOptional::ExtendedAttributes);

    Vector<Ext2FSDirectoryEntry> entries;
    TRY(traverse_as_directory([&](auto& entry) -> ErrorOr<void> {
        if (name == entry.name)
            return EEXIST;
        auto entry_name = TRY(KString::try_create(entry.name));
        TRY(entries.try_append({ move(entry_name), entry.inode.index(), has_file_type_attribute ? entry.file_type : (u8)EXT2_FT_UNKNOWN }));
        return {};
    }));

    TRY(child.increment_link_count());

    auto entry_name = TRY(KString::try_create(name));
    TRY(entries.try_empend(move(entry_name), child.index(), has_file_type_attribute ? to_ext2_file_type(mode) : (u8)EXT2_FT_UNKNOWN));

    TRY(write_directory(entries));
    TRY(populate_lookup_cache());

    auto cache_entry_name = TRY(KString::try_create(name));
    TRY(m_lookup_cache.try_set(move(cache_entry_name), child.index()));
    did_add_child(child.identifier(), name);
    return {};
}

ErrorOr<void> Ext2FSInode::remove_child_impl(StringView name, RemoveDotEntries remove_dot_entries)
{
    MutexLocker locker(m_inode_lock);
    VERIFY(is_directory());

    TRY(populate_lookup_cache());

    auto it = m_lookup_cache.find(name);
    if (it == m_lookup_cache.end())
        return ENOENT;
    auto child_inode_index = (*it).value;

    InodeIdentifier child_id { fsid(), child_inode_index };
    auto child_inode = TRY(fs().get_inode(child_id));
    if (child_inode->is_directory() && remove_dot_entries == RemoveDotEntries::Yes) {
        TRY(static_cast<Ext2FSInode&>(*child_inode).remove_child_impl("."sv, RemoveDotEntries::No));
        TRY(static_cast<Ext2FSInode&>(*child_inode).remove_child_impl(".."sv, RemoveDotEntries::No));
    }

    bool has_file_type_attribute = has_flag(fs().get_features_optional(), Ext2FS::FeaturesOptional::ExtendedAttributes);

    Vector<Ext2FSDirectoryEntry> entries;
    TRY(traverse_as_directory([&](auto& entry) -> ErrorOr<void> {
        if (name != entry.name) {
            auto entry_name = TRY(KString::try_create(entry.name));
            TRY(entries.try_append({ move(entry_name), entry.inode.index(), has_file_type_attribute ? entry.file_type : (u8)EXT2_FT_UNKNOWN }));
        }
        return {};
    }));

    TRY(write_directory(entries));

    m_lookup_cache.remove(it);

    TRY(child_inode->decrement_link_count());

    did_remove_child(child_id, name);
    return {};
}

ErrorOr<void> Ext2FSInode::remove_child(StringView name)
{
    dbgln_if(EXT2_DEBUG, "Ext2FSInode[{}]::remove_child(): Removing '{}'", identifier(), name);
    // TODO: Implement something like remove_directory so we can get rid of remove_child_impl.
    return remove_child_impl(name, RemoveDotEntries::Yes);
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
    if (atime.value_or({}).to_timespec().tv_sec > NumericLimits<i32>::max() && !is_within_inode_bounds(bit_cast<FlatPtr>(&m_raw_inode), bit_cast<FlatPtr>(&m_raw_inode.i_atime_extra), sizeof(m_raw_inode.i_atime_extra)))
        return EINVAL;
    if (ctime.value_or({}).to_timespec().tv_sec > NumericLimits<i32>::max() && !is_within_inode_bounds(bit_cast<FlatPtr>(&m_raw_inode), bit_cast<FlatPtr>(&m_raw_inode.i_ctime_extra), sizeof(m_raw_inode.i_ctime_extra)))
        return EINVAL;
    if (mtime.value_or({}).to_timespec().tv_sec > NumericLimits<i32>::max() && !is_within_inode_bounds(bit_cast<FlatPtr>(&m_raw_inode), bit_cast<FlatPtr>(&m_raw_inode.i_mtime_extra), sizeof(m_raw_inode.i_mtime_extra)))
        return EINVAL;

    auto maybe_encode_time = [](auto const& source, u32& time, u32& time_extra) {
        if (!source.has_value())
            return;

        time_t seconds = source.value().to_timespec().tv_sec;
        u32 nanoseconds = source.value().to_timespec().tv_nsec;
        time = static_cast<u32>(seconds);
        time_extra = encode_time_to_extra(seconds, nanoseconds);
    };

    maybe_encode_time(atime, m_raw_inode.i_atime, m_raw_inode.i_atime_extra);
    maybe_encode_time(ctime, m_raw_inode.i_ctime, m_raw_inode.i_ctime_extra);
    maybe_encode_time(mtime, m_raw_inode.i_mtime, m_raw_inode.i_mtime_extra);

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
    if (inode_uid(m_raw_inode) == uid && inode_gid(m_raw_inode) == gid)
        return {};
    m_raw_inode.i_uid = static_cast<u16>(uid.value());
    ext2fs_set_i_uid_high(m_raw_inode, uid.value() >> 16);
    m_raw_inode.i_gid = static_cast<u16>(gid.value());
    ext2fs_set_i_gid_high(m_raw_inode, gid.value() >> 16);
    set_metadata_dirty(true);
    return {};
}

ErrorOr<void> Ext2FSInode::truncate_locked(u64 size)
{
    VERIFY(m_inode_lock.is_locked());
    if (static_cast<u64>(m_raw_inode.i_size) == size)
        return {};
    TRY(resize(size));
    set_metadata_dirty(true);
    did_modify_contents();
    return {};
}

ErrorOr<int> Ext2FSInode::get_block_address(int index)
{
    MutexLocker locker(m_inode_lock);

    if (index < 0)
        return 0;

    return TRY(m_block_view.get_block(index)).value();
}

}
