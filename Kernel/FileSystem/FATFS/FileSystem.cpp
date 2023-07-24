/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FATFS/FileSystem.h>
#include <Kernel/FileSystem/FATFS/Inode.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<FileSystem>> FATFS::try_create(OpenFileDescription& file_description, ReadonlyBytes)
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) FATFS(file_description)));
}

FATFS::FATFS(OpenFileDescription& file_description)
    : BlockBasedFileSystem(file_description)
{
}

bool FATFS::is_initialized_while_locked()
{
    VERIFY(m_lock.is_locked());
    return !m_root_inode.is_null();
}

ErrorOr<void> FATFS::initialize_while_locked()
{
    VERIFY(m_lock.is_locked());
    VERIFY(!is_initialized_while_locked());

    m_boot_record = TRY(KBuffer::try_create_with_size("FATFS: Boot Record"sv, m_device_block_size));
    auto boot_record_buffer = UserOrKernelBuffer::for_kernel_buffer(m_boot_record->data());
    TRY(raw_read(0, boot_record_buffer));

    if constexpr (FAT_DEBUG) {
        dbgln("FATFS: oem_identifier: {}", boot_record()->oem_identifier);
        dbgln("FATFS: bytes_per_sector: {}", boot_record()->bytes_per_sector);
        dbgln("FATFS: sectors_per_cluster: {}", boot_record()->sectors_per_cluster);
        dbgln("FATFS: reserved_sector_count: {}", boot_record()->reserved_sector_count);
        dbgln("FATFS: fat_count: {}", boot_record()->fat_count);
        dbgln("FATFS: root_directory_entry_count: {}", boot_record()->root_directory_entry_count);
        dbgln("FATFS: media_descriptor_type: {}", boot_record()->media_descriptor_type);
        dbgln("FATFS: sectors_per_track: {}", boot_record()->sectors_per_track);
        dbgln("FATFS: head_count: {}", boot_record()->head_count);
        dbgln("FATFS: hidden_sector_count: {}", boot_record()->hidden_sector_count);
        dbgln("FATFS: sector_count: {}", boot_record()->sector_count);
        dbgln("FATFS: sectors_per_fat: {}", boot_record()->sectors_per_fat);
        dbgln("FATFS: flags: {}", boot_record()->flags);
        dbgln("FATFS: fat_version: {}", boot_record()->fat_version);
        dbgln("FATFS: root_directory_cluster: {}", boot_record()->root_directory_cluster);
        dbgln("FATFS: fs_info_sector: {}", boot_record()->fs_info_sector);
        dbgln("FATFS: backup_boot_sector: {}", boot_record()->backup_boot_sector);
        dbgln("FATFS: drive_number: {}", boot_record()->drive_number);
        dbgln("FATFS: volume_id: {}", boot_record()->volume_id);
    }

    if (boot_record()->signature != signature_1 && boot_record()->signature != signature_2) {
        dbgln("FATFS: Invalid signature");
        return EINVAL;
    }

    m_device_block_size = boot_record()->bytes_per_sector;
    set_block_size(m_device_block_size);

    u32 root_directory_sectors = ((boot_record()->root_directory_entry_count * sizeof(FATEntry)) + (m_device_block_size - 1)) / m_device_block_size;
    m_first_data_sector = boot_record()->reserved_sector_count + (boot_record()->fat_count * boot_record()->sectors_per_fat) + root_directory_sectors;

    TRY(BlockBasedFileSystem::initialize_while_locked());

    FATEntry root_entry {};

    root_entry.first_cluster_low = boot_record()->root_directory_cluster & 0xFFFF;
    root_entry.first_cluster_high = boot_record()->root_directory_cluster >> 16;

    root_entry.attributes = FATAttributes::Directory;
    m_root_inode = TRY(FATInode::create(*this, root_entry));

    return {};
}

Inode& FATFS::root_inode()
{
    return *m_root_inode;
}

BlockBasedFileSystem::BlockIndex FATFS::first_block_of_cluster(u32 cluster) const
{
    return ((cluster - first_data_cluster) * boot_record()->sectors_per_cluster) + m_first_data_sector;
}

u8 FATFS::internal_file_type_to_directory_entry_type(DirectoryEntryView const& entry) const
{
    FATAttributes attrib = static_cast<FATAttributes>(entry.file_type);
    if (has_flag(attrib, FATAttributes::Directory)) {
        return DT_DIR;
    } else if (has_flag(attrib, FATAttributes::VolumeID)) {
        return DT_UNKNOWN;
    } else {
        // ReadOnly, Hidden, System, Archive, LongFileName.
        return DT_REG;
    }
    return DT_UNKNOWN;
}

}
