/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/FileSystem/RAMFS/FileSystem.h>
#include <Kernel/FileSystem/RAMFS/Inode.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

ErrorOr<NonnullLockRefPtr<FileSystem>> RAMFS::try_create()
{
    return TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) RAMFS));
}

RAMFS::RAMFS() = default;
RAMFS::~RAMFS() = default;

ErrorOr<void> RAMFS::initialize()
{
    m_root_inode = TRY(RAMFSInode::try_create_root(*this));
    return {};
}

Inode& RAMFS::root_inode()
{
    VERIFY(!m_root_inode.is_null());
    return *m_root_inode;
}

unsigned RAMFS::next_inode_index()
{
    MutexLocker locker(m_lock);

    return m_next_inode_index++;
}

UNMAP_AFTER_INIT ErrorOr<NonnullLockRefPtr<RAMFSInode>> RAMFS::try_create_ramfs_inode_for_initramfs(PhysicalAddress initramfs_image_inodes_data_blocks_section_start, initramfs_inode const& inode, RAMFSInode const& parent_directory_inode)
{
    InodeMetadata metadata;
    metadata.size = inode.file_size;
    metadata.mode = inode.mode;
    metadata.uid = inode.uid;
    metadata.gid = inode.gid;
    metadata.link_count = 1;
    metadata.atime = Time::from_timespec({ inode.mtime_seconds, 0 });
    metadata.ctime = Time::from_timespec({ inode.mtime_seconds, 0 });
    metadata.mtime = Time::from_timespec({ inode.mtime_seconds, 0 });
    metadata.dtime = Time::from_timespec({ inode.mtime_seconds, 0 });
    metadata.block_size = PAGE_SIZE;
    metadata.block_count = inode.blocks_count;

    if (Kernel::is_character_device(inode.mode) || Kernel::is_block_device(inode.mode)) {
        metadata.major_device = inode.major;
        metadata.minor_device = inode.minor;
    }

    if (metadata.is_directory()) {
        return RAMFSInode::try_create_as_directory(*this, metadata, parent_directory_inode);
    }

    if (inode.file_size == 0) {
        return RAMFSInode::try_create_with_empty_content(*this, metadata, parent_directory_inode);
    }

    auto inode_data_blocks_offset = initramfs_image_inodes_data_blocks_section_start.offset(inode.blocks_offset << 12);
    return RAMFSInode::try_create_with_content(*this, metadata, inode_data_blocks_offset, metadata.block_count, parent_directory_inode);
}

static ErrorOr<NonnullLockRefPtr<RAMFSInode>> ensure_initramfs_path(RAMFSInode& inode, StringView full_name)
{
    auto first_path_part = full_name.find_first_split_view('/');
    if (first_path_part == full_name)
        return inode;
    auto result = inode.lookup(first_path_part);
    if (result.is_error()) {
        VERIFY(result.error() != Error::from_errno(ENOENT));
        return result.release_error();
    }
    auto next_inode = static_ptr_cast<RAMFSInode>(result.release_value());
    return ensure_initramfs_path(*next_inode, full_name.substring_view(1 + first_path_part.length()));
}

UNMAP_AFTER_INIT ErrorOr<void> RAMFS::populate_initramfs(PhysicalAddress initramfs_image_start, PhysicalAddress initramfs_image_end)
{
    auto& fs_root_inode = static_cast<RAMFSInode&>(root_inode());
    auto current_address = initramfs_image_start;
    auto image_header = TRY(Memory::map_typed<initramfs_image_header>(current_address));
    if (StringView { image_header->magic, 8 } != "SERECPIO"sv) {
        dmesgln("InitRAMFS: invalid magic bytes");
        return Error::from_errno(EINVAL);
    }

    // FIXME: Although we could create an initramfs archive with block size larger than
    // 4096 bytes, for now we still can't handle it elegantly, so for now let's not support it.
    if (image_header->data_block_alignment_size_power_2 != 12) {
        dmesgln("InitRAMFS: data block size alignment is not supported");
        return Error::from_errno(ENOTSUP);
    }

    dmesgln("InitRAMFS: {} endian, inodes count {}, data block size - {} bytes", image_header->endianness == 1 ? "big" : "little", image_header->inodes_count, 1 << image_header->data_block_alignment_size_power_2);
    auto initramfs_image_inodes_section_start = initramfs_image_start.offset(image_header->inodes_section_start);
    auto initramfs_image_inodes_names_section_start = initramfs_image_start.offset(image_header->inodes_names_section_start);
    auto initramfs_image_inodes_data_blocks_section_start = initramfs_image_start.offset(image_header->data_blocks_section_start);

    for (size_t inode_index = 0; inode_index < image_header->inodes_count; inode_index++) {
        auto inode_paddr_location = initramfs_image_inodes_section_start.offset(sizeof(initramfs_inode) * inode_index);
        VERIFY(initramfs_image_end > inode_paddr_location);
        auto inode = TRY(Memory::map_typed<initramfs_inode>(inode_paddr_location));

        auto name_offset = initramfs_image_inodes_names_section_start.offset(inode->name_offset);
        VERIFY(initramfs_image_end > name_offset);
        auto inode_name = TRY(Memory::map_typed<u8>(name_offset, inode->name_length));
        auto name = StringView { inode_name.ptr(), inode->name_length };

        auto parent_directory_inode = TRY(ensure_initramfs_path(fs_root_inode, name));
        auto new_inode = TRY(try_create_ramfs_inode_for_initramfs(initramfs_image_inodes_data_blocks_section_start, *inode, parent_directory_inode));

        auto basename = name.find_last_split_view('/');
        TRY(parent_directory_inode->add_child(new_inode, basename, inode->mode));
    }
    return {};
}

UNMAP_AFTER_INIT ErrorOr<NonnullLockRefPtr<RAMFS>> RAMFS::try_create_as_populated_initramfs(Badge<StorageManagement>, PhysicalAddress initramfs_image_start, PhysicalAddress initramfs_image_end)
{
    auto fs = static_ptr_cast<RAMFS>(TRY(RAMFS::try_create()));
    TRY(fs->initialize());
    TRY(fs->populate_initramfs(initramfs_image_start, initramfs_image_end));
    return fs;
}

}
