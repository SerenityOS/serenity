/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntegralMath.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Ext2FS/FileSystem.h>
#include <Kernel/FileSystem/Ext2FS/Inode.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FileSystem>> Ext2FS::try_create(OpenFileDescription& file_description, FileSystemSpecificOptions const&)
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Ext2FS(file_description)));
}

Ext2FS::Ext2FS(OpenFileDescription& file_description)
    : BlockBasedFileSystem(file_description)
{
}

Ext2FS::~Ext2FS() = default;

ErrorOr<void> Ext2FS::rename(Inode& old_parent_inode, StringView old_basename, Inode& new_parent_inode, StringView new_basename)
{
    MutexLocker locker(m_lock);

    if (auto maybe_inode_to_be_replaced = new_parent_inode.lookup(new_basename); !maybe_inode_to_be_replaced.is_error()) {
        VERIFY(!maybe_inode_to_be_replaced.value()->is_directory());
        TRY(new_parent_inode.remove_child(new_basename));
    }

    auto old_inode = TRY(old_parent_inode.lookup(old_basename));

    TRY(new_parent_inode.add_child(old_inode, new_basename, old_inode->mode()));
    TRY(static_cast<Ext2FSInode&>(old_parent_inode).remove_child_impl(old_basename, Ext2FSInode::RemoveDotEntries::No));

    // If the inode that we moved is a directory and we changed parent
    // directories, then we also have to make .. point to the new parent inode,
    // because .. is its own inode.
    if (old_inode->is_directory() && old_parent_inode.index() != new_parent_inode.index()) {
        Vector<Ext2FSDirectoryEntry> entries;
        bool has_file_type_attribute = has_flag(get_features_optional(), Ext2FS::FeaturesOptional::ExtendedAttributes);

        Optional<InodeIndex> dot_dot_index;
        TRY(old_inode->traverse_as_directory([&](auto& entry) -> ErrorOr<void> {
            auto is_replacing_this_inode = entry.name == ".."sv;
            auto inode_index = is_replacing_this_inode ? new_parent_inode.index() : entry.inode.index();

            auto entry_name = TRY(KString::try_create(entry.name));
            TRY(entries.try_empend(move(entry_name), inode_index, has_file_type_attribute ? Ext2FSInode::to_ext2_file_type(new_parent_inode.mode()) : (u8)EXT2_FT_UNKNOWN));

            if (is_replacing_this_inode)
                dot_dot_index = entry.inode.index();

            return {};
        }));

        if (!dot_dot_index.has_value())
            return ENOENT;

        auto dot_dot = TRY(get_inode({ fsid(), *dot_dot_index }));
        auto new_inode = TRY(new_parent_inode.lookup(new_basename));

        auto old_index_it = static_cast<Ext2FSInode&>(*old_inode).m_lookup_cache.find(".."sv);
        bool has_cached_dot_dot = old_index_it != static_cast<Ext2FSInode&>(*old_inode).m_lookup_cache.end();
        if (has_cached_dot_dot)
            old_index_it->value = new_parent_inode.index();

        // NOTE: Between this line and the write_directory line, all operations must
        //       be atomic. Any changes made should be reverted.
        TRY(new_parent_inode.increment_link_count());

        auto maybe_decrement_error = dot_dot->decrement_link_count();
        if (maybe_decrement_error.is_error()) {
            if (has_cached_dot_dot)
                old_index_it->value = *dot_dot_index;

            MUST(new_parent_inode.decrement_link_count());
            return maybe_decrement_error;
        }

        // FIXME: The filesystem is left in an inconsistent state if this fails.
        //        Revert the changes made above if we can't write_directory.
        //        Ideally, decrement should be the last operation, but we currently
        //        can't "un-write" a directory entry list.
        TRY(static_cast<Ext2FSInode&>(*new_inode).write_directory(entries));
    }

    return {};
}

ErrorOr<void> Ext2FS::flush_super_block()
{
    MutexLocker locker(m_lock);
    auto super_block_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&m_super_block);
    auto const superblock_physical_block_count = (sizeof(ext2_super_block) / device_block_size());

    // FIXME: We currently have no ability of writing within a device block, but the ability to do so would allow us to use device block sizes larger than 1024.
    VERIFY((sizeof(ext2_super_block) % device_block_size()) == 0);
    TRY(raw_write_blocks(super_block_offset_on_device / device_block_size(), superblock_physical_block_count, super_block_buffer));

    auto is_sparse = has_flag(get_features_readonly(), FeaturesReadOnly::SparseSuperblock);

    for (auto group = 1u; group < m_block_group_count; ++group) {
        auto first_block_in_group = first_block_of_group(group);
        // Superblock copies with sparse layout are in group number 2 and powers of 3, 5, and 7.
        if (!is_sparse || group == 2 || AK::is_power_of<3>(group - 1) || AK::is_power_of<5>(group - 1) || AK::is_power_of<7>(group - 1)) {
            dbgln_if(EXT2_DEBUG, "Writing superblock backup to block group {} (block {})", group, first_block_in_group);
            TRY(write_blocks(first_block_in_group, 1, super_block_buffer));
        }
    }

    return {};
}

ext2_group_desc const& Ext2FS::group_descriptor(GroupIndex group_index) const
{
    // FIXME: Should this fail gracefully somehow?
    VERIFY(group_index <= m_block_group_count);
    VERIFY(group_index > 0);
    return block_group_descriptors()[group_index.value() - 1];
}

bool Ext2FS::is_initialized_while_locked()
{
    VERIFY(m_lock.is_locked());
    return !m_root_inode.is_null();
}

ErrorOr<void> Ext2FS::initialize_while_locked()
{
    VERIFY(m_lock.is_locked());
    VERIFY(!is_initialized_while_locked());

    VERIFY((sizeof(ext2_super_block) % device_block_size()) == 0);
    auto super_block_buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)&m_super_block);
    TRY(raw_read_blocks(super_block_offset_on_device / device_block_size(), (sizeof(ext2_super_block) / device_block_size()), super_block_buffer));

    auto const& super_block = this->super_block();
    if constexpr (EXT2_DEBUG) {
        dmesgln("Ext2FS: super block magic: {:04x} (super block size: {})", super_block.s_magic, sizeof(ext2_super_block));
    }
    if (super_block.s_magic != EXT2_SUPER_MAGIC) {
        dmesgln("Ext2FS: Bad super block magic");
        return EINVAL;
    }

    if (super_block.s_state == EXT2_ERROR_FS)
        dmesgln("Ext2FS: Was not unmounted cleanly, file system may be erroneous!");

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

    set_logical_block_size(EXT2_BLOCK_SIZE(&super_block));
    set_fragment_size(EXT2_FRAG_SIZE(&super_block));

    // Note: This depends on the block size being available.
    TRY(BlockBasedFileSystem::initialize_while_locked());

    VERIFY(logical_block_size() <= (int)max_block_size);

    m_i_blocks_increment = logical_block_size() / 512;

    m_block_group_count = ceil_div(super_block.s_blocks_count, super_block.s_blocks_per_group);

    if (m_block_group_count == 0) {
        dmesgln("Ext2FS: no block groups :(");
        return EINVAL;
    }

    auto blocks_to_read = ceil_div(m_block_group_count * sizeof(ext2_group_desc), logical_block_size());
    BlockIndex first_block_of_bgdt = first_block_of_block_group_descriptors();
    m_cached_group_descriptor_table = TRY(KBuffer::try_create_with_size("Ext2FS: Block group descriptors"sv, logical_block_size() * blocks_to_read, Memory::Region::Access::ReadWrite));
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(m_cached_group_descriptor_table->data());
    TRY(read_blocks(first_block_of_bgdt, blocks_to_read, buffer));

    if constexpr (EXT2_DEBUG) {
        for (unsigned i = 1; i <= m_block_group_count; ++i) {
            auto const& group = group_descriptor(i);
            dbgln("Ext2FS: group[{}] ( block_bitmap: {}, inode_bitmap: {}, inode_table: {} )", i, group.bg_block_bitmap, group.bg_inode_bitmap, group.bg_inode_table);
        }
    }

    m_root_inode = TRY(build_root_inode());

    // Set filesystem to "error" state until we unmount cleanly.
    dmesgln("Ext2FS: Mount successful, setting superblock to error state.");
    m_super_block.s_state = EXT2_ERROR_FS;
    TRY(flush_super_block());

    return {};
}

Inode& Ext2FS::root_inode()
{
    return *m_root_inode;
}

bool Ext2FS::find_block_containing_inode(InodeIndex inode, BlockIndex& block_index, unsigned& offset) const
{
    auto const& super_block = this->super_block();

    if (inode != EXT2_ROOT_INO && inode < EXT2_FIRST_INO(&super_block))
        return false;

    if (inode > super_block.s_inodes_count)
        return false;

    auto const& bgd = group_descriptor(group_index_from_inode(inode));

    u64 full_offset = ((inode.value() - 1) % inodes_per_group()) * inode_size();
    block_index = bgd.bg_inode_table + (full_offset >> EXT2_BLOCK_SIZE_BITS(&super_block));
    offset = full_offset & (logical_block_size() - 1);

    return true;
}

u8 Ext2FS::internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const
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

Ext2FS::FeaturesOptional Ext2FS::get_features_optional() const
{
    if (m_super_block.s_rev_level > 0)
        return static_cast<Ext2FS::FeaturesOptional>(m_super_block.s_feature_compat);
    return Ext2FS::FeaturesOptional::None;
}

Ext2FS::FeaturesReadOnly Ext2FS::get_features_readonly() const
{
    if (m_super_block.s_rev_level > 0)
        return static_cast<Ext2FS::FeaturesReadOnly>(m_super_block.s_feature_ro_compat);
    return Ext2FS::FeaturesReadOnly::None;
}

u64 Ext2FS::inodes_per_block() const
{
    return EXT2_INODES_PER_BLOCK(&super_block());
}

u64 Ext2FS::inodes_per_group() const
{
    return EXT2_INODES_PER_GROUP(&super_block());
}

u64 Ext2FS::inode_size() const
{
    return EXT2_INODE_SIZE(&super_block());
}

u64 Ext2FS::blocks_per_group() const
{
    return EXT2_BLOCKS_PER_GROUP(&super_block());
}

ErrorOr<void> Ext2FS::write_ext2_inode(InodeIndex inode, ext2_inode_large const& e2inode)
{
    BlockIndex block_index;
    unsigned offset;
    if (!find_block_containing_inode(inode, block_index, offset))
        return EINVAL;

    Vector<u8> inode_storage;
    TRY(inode_storage.try_resize(inode_size()));

    size_t used_inode_size = inode_size() > EXT2_GOOD_OLD_INODE_SIZE ? EXT2_GOOD_OLD_INODE_SIZE + e2inode.i_extra_isize : inode_size();
    VERIFY(used_inode_size >= EXT2_GOOD_OLD_INODE_SIZE && used_inode_size <= inode_size());

    memcpy(inode_storage.data(), &e2inode, min(used_inode_size, sizeof(ext2_inode_large)));

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(inode_storage.data());
    return write_block(block_index, buffer, inode_size(), offset);
}

auto Ext2FS::allocate_blocks(GroupIndex preferred_group_index, size_t count) -> ErrorOr<Vector<BlockIndex>>
{
    dbgln_if(EXT2_DEBUG, "Ext2FS: allocate_blocks(preferred group: {}, count {})", preferred_group_index, count);
    if (count == 0)
        return Vector<BlockIndex> {};

    Vector<BlockIndex> blocks;
    TRY(blocks.try_ensure_capacity(count));

    MutexLocker locker(m_lock);

    size_t free_blocks = 0;
    for (GroupIndex i = 1; i <= m_block_group_count; i = GroupIndex { i.value() + 1 }) {
        free_blocks += group_descriptor(i).bg_free_blocks_count;
        if (free_blocks >= count)
            break;
    }

    if (free_blocks < count)
        return Error::from_errno(ENOSPC);

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
        auto const& bgd = group_descriptor(group_index);

        auto* cached_bitmap = TRY(get_bitmap_block(bgd.bg_block_bitmap));

        int blocks_in_group = min(blocks_per_group(), super_block().s_blocks_count);
        auto block_bitmap = cached_bitmap->bitmap(blocks_in_group);

        BlockIndex first_block_in_group = first_block_of_group(group_index);
        size_t free_region_size = 0;
        auto first_unset_bit_index = block_bitmap.find_longest_range_of_unset_bits(count - blocks.size(), free_region_size);
        VERIFY(first_unset_bit_index.has_value());
        dbgln_if(EXT2_DEBUG, "Ext2FS: allocating free region of size: {} [{}]", free_region_size, group_index);
        for (size_t i = 0; i < free_region_size; ++i) {
            BlockIndex block_index = (first_unset_bit_index.value() + i) + first_block_in_group.value();
            TRY(set_block_allocation_state(block_index, true));
            blocks.unchecked_append(block_index);
            dbgln_if(EXT2_DEBUG, "  allocated > {}", block_index);
        }
    }

    VERIFY(blocks.size() == count);
    return blocks;
}

ErrorOr<InodeIndex> Ext2FS::allocate_inode(GroupIndex preferred_group)
{
    dbgln_if(EXT2_DEBUG, "Ext2FS: allocate_inode(preferred_group: {})", preferred_group);
    MutexLocker locker(m_lock);

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

    auto const& bgd = group_descriptor(group_index);
    unsigned inodes_in_group = min(inodes_per_group(), super_block().s_inodes_count);
    InodeIndex first_inode_in_group = (group_index.value() - 1) * inodes_per_group() + 1;

    auto* cached_bitmap = TRY(get_bitmap_block(bgd.bg_inode_bitmap));
    auto inode_bitmap = cached_bitmap->bitmap(inodes_in_group);
    for (size_t i = 0; i < inode_bitmap.size(); ++i) {
        if (inode_bitmap.get(i))
            continue;
        inode_bitmap.set(i, true);

        auto inode_index = InodeIndex(first_inode_in_group.value() + i);

        cached_bitmap->dirty = true;
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
    return (block_index.value() - first_block_index().value()) / blocks_per_group() + 1;
}

Ext2FS::BlockIndex Ext2FS::first_block_of_group(GroupIndex group_index) const
{
    return (group_index.value() - 1) * blocks_per_group() + first_block_index().value();
}

Ext2FS::BlockIndex Ext2FS::first_block_of_block_group_descriptors() const
{
    return logical_block_size() == 1024 ? 2 : 1;
}

auto Ext2FS::group_index_from_inode(InodeIndex inode) const -> GroupIndex
{
    if (!inode)
        return 0;
    return (inode.value() - 1) / inodes_per_group() + 1;
}

ErrorOr<bool> Ext2FS::get_inode_allocation_state(InodeIndex index) const
{
    MutexLocker locker(m_lock);
    if (index == 0)
        return EINVAL;
    auto group_index = group_index_from_inode(index);
    auto const& bgd = group_descriptor(group_index);
    unsigned index_in_group = index.value() - ((group_index.value() - 1) * inodes_per_group());
    unsigned bit_index = (index_in_group - 1) % inodes_per_group();

    auto* cached_bitmap = TRY(const_cast<Ext2FS&>(*this).get_bitmap_block(bgd.bg_inode_bitmap));
    return cached_bitmap->bitmap(inodes_per_group()).get(bit_index);
}

ErrorOr<void> Ext2FS::update_bitmap_block(BlockIndex bitmap_block, size_t bit_index, bool new_state, u32& super_block_counter, u16& group_descriptor_counter)
{
    auto* cached_bitmap = TRY(get_bitmap_block(bitmap_block));
    bool current_state = cached_bitmap->bitmap(blocks_per_group()).get(bit_index);
    if (current_state == new_state) {
        dbgln("Ext2FS: Bit {} in bitmap block {} had unexpected state {}", bit_index, bitmap_block, current_state);
        return EIO;
    }
    cached_bitmap->bitmap(blocks_per_group()).set(bit_index, new_state);
    cached_bitmap->dirty = true;

    if (new_state) {
        --super_block_counter;
        --group_descriptor_counter;
    } else {
        ++super_block_counter;
        ++group_descriptor_counter;
    }

    m_super_block_dirty = true;
    m_block_group_descriptors_dirty = true;
    return {};
}

ErrorOr<void> Ext2FS::set_inode_allocation_state(InodeIndex inode_index, bool new_state)
{
    MutexLocker locker(m_lock);
    auto group_index = group_index_from_inode(inode_index);
    unsigned index_in_group = inode_index.value() - ((group_index.value() - 1) * inodes_per_group());
    unsigned bit_index = (index_in_group - 1) % inodes_per_group();

    dbgln_if(EXT2_DEBUG, "Ext2FS: set_inode_allocation_state: Inode {} -> {}", inode_index, new_state);
    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index));
    return update_bitmap_block(bgd.bg_inode_bitmap, bit_index, new_state, m_super_block.s_free_inodes_count, bgd.bg_free_inodes_count);
}

Ext2FS::BlockIndex Ext2FS::first_block_index() const
{
    return logical_block_size() == 1024 ? 1 : 0;
}

ErrorOr<Ext2FS::CachedBitmap*> Ext2FS::get_bitmap_block(BlockIndex bitmap_block_index)
{
    for (auto& cached_bitmap : m_cached_bitmaps) {
        if (cached_bitmap->bitmap_block_index == bitmap_block_index)
            return cached_bitmap.ptr();
    }

    auto block = TRY(KBuffer::try_create_with_size("Ext2FS: Cached bitmap block"sv, logical_block_size(), Memory::Region::Access::ReadWrite));
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(block->data());
    TRY(read_block(bitmap_block_index, &buffer, logical_block_size()));
    auto new_bitmap = TRY(adopt_nonnull_own_or_enomem(new (nothrow) CachedBitmap(bitmap_block_index, move(block))));
    TRY(m_cached_bitmaps.try_append(move(new_bitmap)));
    return m_cached_bitmaps.last().ptr();
}

ErrorOr<void> Ext2FS::set_block_allocation_state(BlockIndex block_index, bool new_state)
{
    VERIFY(block_index != 0);
    MutexLocker locker(m_lock);

    auto group_index = group_index_from_block_index(block_index);
    unsigned index_in_group = (block_index.value() - first_block_index().value()) - ((group_index.value() - 1) * blocks_per_group());
    unsigned bit_index = index_in_group % blocks_per_group();
    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index));

    dbgln_if(EXT2_DEBUG, "Ext2FS: Block {} state -> {} (in bitmap block {})", block_index, new_state, bgd.bg_block_bitmap);
    return update_bitmap_block(bgd.bg_block_bitmap, bit_index, new_state, m_super_block.s_free_blocks_count, bgd.bg_free_blocks_count);
}

ErrorOr<NonnullRefPtr<Inode>> Ext2FS::create_directory(Ext2FSInode& parent_inode, StringView name, mode_t mode, UserID uid, GroupID gid)
{
    MutexLocker locker(m_lock);
    VERIFY(is_directory(mode));

    auto inode = TRY(create_inode(parent_inode, name, mode, 0, uid, gid));

    dbgln_if(EXT2_DEBUG, "Ext2FS: create_directory: created new directory named '{} with inode {}", name, inode->index());

    Vector<Ext2FSDirectoryEntry> entries;
    auto current_directory_name = TRY(KString::try_create("."sv));
    TRY(entries.try_empend(move(current_directory_name), inode->index(), static_cast<u8>(EXT2_FT_DIR)));
    auto parent_directory_name = TRY(KString::try_create(".."sv));
    TRY(entries.try_empend(move(parent_directory_name), parent_inode.index(), static_cast<u8>(EXT2_FT_DIR)));

    TRY(static_cast<Ext2FSInode&>(*inode).write_directory(entries));
    TRY(parent_inode.increment_link_count());

    auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode->identifier().index())));
    ++bgd.bg_used_dirs_count;
    m_block_group_descriptors_dirty = true;

    return inode;
}

ErrorOr<NonnullRefPtr<Inode>> Ext2FS::create_inode(Ext2FSInode& parent_inode, StringView name, mode_t mode, dev_t dev, UserID uid, GroupID gid)
{
    if (name.length() > EXT2_NAME_LEN)
        return ENAMETOOLONG;

    if (parent_inode.m_raw_inode.i_links_count == 0)
        return ENOENT;

    ext2_inode_large e2inode {};
    auto now = kgettimeofday().to_timespec();

    u32 extra = Ext2FSInode::encode_time_to_extra(now.tv_sec, now.tv_nsec);

    e2inode.i_mode = mode;
    e2inode.i_uid = static_cast<u16>(uid.value());
    ext2fs_set_i_uid_high(e2inode, uid.value() >> 16);
    e2inode.i_gid = static_cast<u16>(gid.value());
    ext2fs_set_i_gid_high(e2inode, gid.value() >> 16);
    e2inode.i_size = 0;
    e2inode.i_atime = now.tv_sec;
    e2inode.i_ctime = now.tv_sec;
    e2inode.i_mtime = now.tv_sec;
    e2inode.i_crtime = now.tv_sec;
    e2inode.i_atime_extra = extra;
    e2inode.i_ctime_extra = extra;
    e2inode.i_mtime_extra = extra;
    e2inode.i_crtime_extra = extra;
    e2inode.i_dtime = 0;
    e2inode.i_flags = 0;

    if (inode_size() > EXT2_GOOD_OLD_INODE_SIZE)
        e2inode.i_extra_isize = min(inode_size(), sizeof(ext2_inode_large)) - EXT2_GOOD_OLD_INODE_SIZE;

    // For directories, add +1 link count for the "." entry in self.
    e2inode.i_links_count = is_directory(mode);

    if (is_character_device(mode))
        e2inode.i_block[0] = dev;
    else if (is_block_device(mode))
        e2inode.i_block[1] = dev;

    auto inode_id = TRY(allocate_inode());

    dbgln_if(EXT2_DEBUG, "Ext2FS: writing initial metadata for inode {}", inode_id.value());
    TRY(write_ext2_inode(inode_id, e2inode));

    auto new_inode = TRY(get_inode({ fsid(), inode_id }));

    dbgln_if(EXT2_DEBUG, "Ext2FS: Adding inode '{}' (mode {:o}) to parent directory {}", name, mode, parent_inode.index());
    TRY(parent_inode.add_child(*new_inode, name, mode));
    return new_inode;
}

void Ext2FS::uncache_inode(InodeIndex index)
{
    MutexLocker locker(m_lock);
    m_inode_cache.remove(index);
}

unsigned Ext2FS::total_block_count() const
{
    MutexLocker locker(m_lock);
    return super_block().s_blocks_count;
}

unsigned Ext2FS::free_block_count() const
{
    MutexLocker locker(m_lock);
    return super_block().s_free_blocks_count;
}

unsigned Ext2FS::total_inode_count() const
{
    MutexLocker locker(m_lock);
    return super_block().s_inodes_count;
}

unsigned Ext2FS::free_inode_count() const
{
    MutexLocker locker(m_lock);
    return super_block().s_free_inodes_count;
}

ErrorOr<void> Ext2FS::prepare_to_clear_last_mount(Inode& mount_guest_inode)
{
    MutexLocker locker(m_lock);
    bool any_inode_busy = false;
    for (auto& it : m_inode_cache) {
        // We hold the last reference to the root inode, and the VFS Mount object holds the last reference to the mount_guest_inode,
        // so they are allowed to have one more reference.
        if ((it.value == m_root_inode || it.value->identifier() == mount_guest_inode.identifier()) && it.value->ref_count() > 2) {
            dbgln_if(EXT2_DEBUG, "Ext2FS: Ignoring root or mount point inode's last reference");
            continue;
        }
        // The Inode::all_instances list always holds one reference to all inodes, which we disregard.
        if (it.value->ref_count() > 1) {
            dbgln_if(EXT2_DEBUG, "Ext2FS: Busy inode {} ({} refs)", it.value->index(), it.value->ref_count());
            any_inode_busy = true;
        }
    }
    if (any_inode_busy)
        return EBUSY;

    m_inode_cache.clear();
    m_root_inode = nullptr;

    // Mark filesystem as valid before unmount.
    dmesgln("Ext2FS: Clean unmount, setting superblock to valid state");
    m_super_block.s_state = EXT2_VALID_FS;
    TRY(flush_super_block());

    return {};
}

ErrorOr<void> Ext2FS::free_inode(Ext2FSInode& inode)
{
    MutexLocker locker(m_lock);
    VERIFY(inode.m_raw_inode.i_links_count == 0);
    dbgln_if(EXT2_DEBUG, "Ext2FS[{}]::free_inode(): Inode {} has no more links, time to delete!", fsid(), inode.index());

    TRY(inode.free_all_blocks());

    // If the inode being freed is a directory, update block group directory counter.
    if (inode.is_directory()) {
        auto& bgd = const_cast<ext2_group_desc&>(group_descriptor(group_index_from_inode(inode.index())));
        --bgd.bg_used_dirs_count;
        dbgln_if(EXT2_DEBUG, "Ext2FS[{}]::free_inode(): Decremented bg_used_dirs_count to {} for inode {}", fsid(), bgd.bg_used_dirs_count, inode.index());
        m_block_group_descriptors_dirty = true;
    }

    // NOTE: After this point, the inode metadata is wiped.
    memset(&inode.m_raw_inode, 0, sizeof(ext2_inode_large));
    inode.m_raw_inode.i_dtime = kgettimeofday().truncated_seconds_since_epoch();
    TRY(write_ext2_inode(inode.index(), inode.m_raw_inode));

    // Mark the inode as free.
    TRY(set_inode_allocation_state(inode.index(), false));

    return {};
}

void Ext2FS::flush_block_group_descriptor_table()
{
    MutexLocker locker(m_lock);
    auto blocks_to_write = ceil_div(m_block_group_count * sizeof(ext2_group_desc), logical_block_size());
    auto first_block_of_bgdt = first_block_of_block_group_descriptors();
    auto buffer = UserOrKernelBuffer::for_kernel_buffer((u8*)block_group_descriptors());
    auto write_bgdt_to_block = [&](BlockIndex index) {
        if (auto result = write_blocks(index, blocks_to_write, buffer); result.is_error())
            dbgln("Ext2FS[{}]::flush_block_group_descriptor_table(): Failed to write blocks: {}", fsid(), result.error());
    };

    write_bgdt_to_block(first_block_of_bgdt);

    auto is_sparse = has_flag(get_features_readonly(), FeaturesReadOnly::SparseSuperblock);

    for (auto group = 1u; group < m_block_group_count; ++group) {
        // First block is occupied by the super block
        BlockIndex second_block_in_group = first_block_of_group(group).value() + 1;
        // BGDT copies with sparse layout are in group number 2 and powers of 3, 5, and 7.
        if (!is_sparse || group == 2 || AK::is_power_of<3>(group - 1) || AK::is_power_of<5>(group - 1) || AK::is_power_of<7>(group - 1)) {
            dbgln_if(EXT2_DEBUG, "Writing block group descriptor table backup to block group {} (block {})", group, second_block_in_group);
            write_bgdt_to_block(second_block_in_group);
        }
    }
}

ErrorOr<void> Ext2FS::flush_writes()
{
    {
        MutexLocker locker(m_lock);
        if (m_super_block_dirty) {
            auto result = flush_super_block();
            if (result.is_error()) {
                dbgln("Ext2FS[{}]::flush_writes(): Failed to write superblock: {}", fsid(), result.error());
                return result.release_error();
            }
            m_super_block_dirty = false;
        }
        if (m_block_group_descriptors_dirty) {
            flush_block_group_descriptor_table();
            m_block_group_descriptors_dirty = false;
        }
        for (auto& cached_bitmap : m_cached_bitmaps) {
            if (cached_bitmap->dirty) {
                auto buffer = UserOrKernelBuffer::for_kernel_buffer(cached_bitmap->buffer->data());
                if (auto result = write_block(cached_bitmap->bitmap_block_index, buffer, logical_block_size()); result.is_error()) {
                    dbgln("Ext2FS[{}]::flush_writes(): Failed to write blocks: {}", fsid(), result.error());
                }
                cached_bitmap->dirty = false;
                dbgln_if(EXT2_DEBUG, "Ext2FS[{}]::flush_writes(): Flushed bitmap block {}", fsid(), cached_bitmap->bitmap_block_index);
            }
        }

        // Uncache Inodes that are only kept alive by the index-to-inode lookup cache.
        // We don't uncache Inodes that are being watched by at least one InodeWatcher.

        // FIXME: It would be better to keep a capped number of Inodes around.
        //        The problem is that they are quite heavy objects, and use a lot of heap memory
        //        for their (child name lookup) and (block list) caches.

        m_inode_cache.remove_all_matching([](InodeIndex, RefPtr<Ext2FSInode> const& cached_inode) {
            // NOTE: If we're asked to look up an inode by number (via get_inode) and it turns out
            //       to not exist, we remember the fact that it doesn't exist by caching a nullptr.
            //       This seems like a reasonable time to uncache ideas about unknown inodes, so do that.
            if (cached_inode == nullptr)
                return true;

            return cached_inode->ref_count() == 1 && !cached_inode->has_watchers();
        });
    }

    auto result = BlockBasedFileSystem::flush_writes();
    if (result.is_error()) {
        dbgln("Ext2FS[{}]::flush_writes(): Failed to flush writes: {}", BlockBasedFileSystem::fsid(), result.error());
        return result.release_error();
    }

    return {};
}

ErrorOr<NonnullRefPtr<Ext2FSInode>> Ext2FS::build_root_inode() const
{
    MutexLocker locker(m_lock);
    BlockIndex block_index;
    unsigned offset;
    if (!find_block_containing_inode(EXT2_ROOT_INO, block_index, offset))
        return EINVAL;

    auto inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Ext2FSInode(const_cast<Ext2FS&>(*this), EXT2_ROOT_INO)));

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&inode->m_raw_inode));

    size_t size = min(inode_size(), sizeof(ext2_inode_large));
    VERIFY(size >= EXT2_GOOD_OLD_INODE_SIZE);

    TRY(read_block(block_index, &buffer, size, offset));
    return inode;
}

ErrorOr<NonnullRefPtr<Inode>> Ext2FS::get_inode(InodeIdentifier inode) const
{
    MutexLocker locker(m_lock);
    VERIFY(inode.fsid() == fsid());
    VERIFY(m_root_inode);

    if (inode.index() == EXT2_ROOT_INO)
        return *m_root_inode;

    {
        auto it = m_inode_cache.find(inode.index());
        if (it != m_inode_cache.end()) {
            if (!it->value)
                return ENOENT;
            return NonnullRefPtr<Inode> { *it->value };
        }
    }

    auto inode_allocation_state = TRY(get_inode_allocation_state(inode.index()));

    if (!inode_allocation_state) {
        TRY(m_inode_cache.try_set(inode.index(), nullptr));
        return ENOENT;
    }

    BlockIndex block_index;
    unsigned offset;
    if (!find_block_containing_inode(inode.index(), block_index, offset))
        return EINVAL;

    auto new_inode = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Ext2FSInode(const_cast<Ext2FS&>(*this), inode.index())));

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(reinterpret_cast<u8*>(&new_inode->m_raw_inode));

    size_t size = min(inode_size(), sizeof(ext2_inode_large));
    VERIFY(size >= EXT2_GOOD_OLD_INODE_SIZE);

    TRY(read_block(block_index, &buffer, size, offset));

    TRY(m_inode_cache.try_set(inode.index(), new_inode));
    return new_inode;
}

}
