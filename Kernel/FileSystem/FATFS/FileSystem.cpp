/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Debug.h>
#include <Kernel/FileSystem/FATFS/FileSystem.h>
#include <Kernel/FileSystem/FATFS/Inode.h>

namespace Kernel {

DOSBIOSParameterBlockVersion DOSBIOSParameterBlock::bpb_version() const
{
    bool dos3_valid = m_dos4_block->signature == 0x28;
    bool dos4_valid = m_dos4_block->signature == 0x29;
    bool dos7_valid = m_dos7_block->signature == 0x28 || m_dos7_block->signature == 0x29;
    // A DOS 7 EBPB should _never_ contain the values 0x28 or 0x29 at
    // the offset associated with `m_dos4_block->signature`
    // (aka `m_dos7_block->sectors_per_fat_32bit`) due to the maximum number of
    // clusters ensuring the number of sectors per fat will not exceed 0x200000.
    // As a result, it should be safe to determine BPB version through the
    // signature fields by checking the DOS 4 signature offset prior to the DOS 7 one.
    //
    // With a DOS 3 or DOS 4 EBPB, the DOS 7 signature offset references uninitialized
    // space. While unlikely to be set to a valid signature value, it is not implausible.
    // We warn the user here, but because it does not represent an invalid FS configuration,
    // do not error.
    if ((dos3_valid || dos4_valid) && dos7_valid)
        dbgln("FATFS: DOS 4 and DOS 7 EBPB signatures detected, EBPB/FAT version detection may be incorrect.");

    if (dos3_valid)
        return DOS_BPB_3;
    else if (dos4_valid)
        return DOS_BPB_4;
    else if (dos7_valid)
        return DOS_BPB_7;
    else
        return DOS_BPB_UNKNOWN;
}

DOS3BIOSParameterBlock const* DOSBIOSParameterBlock::common_bpb() const
{
    return m_common_block;
}

DOS4BIOSParameterBlock const* DOSBIOSParameterBlock::dos4_bpb() const
{
    // Only return parameter block if signature indicates this portion of the block is filled out.
    if (m_dos4_block->signature == 0x28 || m_dos4_block->signature == 0x29)
        return m_dos4_block;
    else
        return nullptr;
}

DOS7BIOSParameterBlock const* DOSBIOSParameterBlock::dos7_bpb() const
{
    // Only return parameter block if signature indicates this portion of the block is filled out.
    if (m_dos7_block->signature == 0x28 || m_dos7_block->signature == 0x29)
        return m_dos7_block;
    else
        return nullptr;
}

u16 DOSBIOSParameterBlock::sectors_per_fat() const
{
    return common_bpb()->sectors_per_fat_16bit != 0 ? common_bpb()->sectors_per_fat_16bit : m_dos7_block->sectors_per_fat_32bit;
}

u32 DOSBIOSParameterBlock::sector_count() const
{
    if (common_bpb()->sector_count_16bit != 0) {
        // The `16bit` field is only used on partitions smaller than 32 MB,
        // and never for FAT32.
        // It is set to `0` when the 32 bit field contains the sector count.
        return common_bpb()->sector_count_16bit;
    } else {
        return common_bpb()->sector_count_32bit;
        // FIXME: If this is 0 for a FAT32 EBPB with a signature of 0x29,
        // read 0x052, which is a 64-bit wide sector count.
    }
}

u8 DOSBIOSParameterBlock::signature() const
{
    if (bpb_version() == DOS_BPB_3 || bpb_version() == DOS_BPB_4)
        return m_dos4_block->signature;
    else
        return m_dos7_block->signature;
}

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
    m_parameter_block = TRY(adopt_nonnull_own_or_enomem(new (nothrow) DOSBIOSParameterBlock(m_boot_record)));

    // Alias for extended BPB.
    DOSBIOSParameterBlock& ebpb = *m_parameter_block;
    // Alias for block of common parameters in BPB.
    DOS3BIOSParameterBlock const* block = ebpb.common_bpb();

    if constexpr (FAT_DEBUG) {
        dbgln("FATFS: oem_identifier: {}", block->oem_identifier);
        dbgln("FATFS: bytes_per_sector: {}", block->bytes_per_sector);
        dbgln("FATFS: sectors_per_cluster: {}", block->sectors_per_cluster);
        dbgln("FATFS: reserved_sector_count: {}", block->reserved_sector_count);
        dbgln("FATFS: fat_count: {}", block->fat_count);
        dbgln("FATFS: root_directory_entry_count: {}", block->root_directory_entry_count);
        dbgln("FATFS: media_descriptor_type: {}", block->media_descriptor_type);
        dbgln("FATFS: sectors_per_track: {}", block->sectors_per_track);
        dbgln("FATFS: head_count: {}", block->head_count);
        dbgln("FATFS: hidden_sector_count: {}", block->hidden_sector_count);
        dbgln("FATFS: sector_count: {}", ebpb.sector_count());
        dbgln("FATFS: sectors_per_fat: {}", ebpb.sectors_per_fat());

        auto ebpb_version = ebpb.bpb_version();
        if (ebpb_version == DOSBIOSParameterBlockVersion::DOS_BPB_7) {
            DOS7BIOSParameterBlock const* dos7_boot_record = ebpb.dos7_bpb();
            dbgln("FATFS: EBPB: DOS 7");
            dbgln("FATFS: flags: {}", dos7_boot_record->flags);
            dbgln("FATFS: fat_version: {}", dos7_boot_record->fat_version);
            dbgln("FATFS: root_directory_cluster: {}", dos7_boot_record->root_directory_cluster);
            dbgln("FATFS: fs_info_sector: {}", dos7_boot_record->fs_info_sector);
            dbgln("FATFS: backup_boot_sector: {}", dos7_boot_record->backup_boot_sector);
            dbgln("FATFS: drive_number: {}", dos7_boot_record->drive_number);
            dbgln("FATFS: volume_id: {}", dos7_boot_record->volume_id);
        } else if (ebpb_version == DOSBIOSParameterBlockVersion::DOS_BPB_3 || ebpb_version == DOSBIOSParameterBlockVersion::DOS_BPB_4) {
            DOS4BIOSParameterBlock const* dos4_boot_record = ebpb.dos4_bpb();
            if (ebpb_version == DOSBIOSParameterBlockVersion::DOS_BPB_3) {
                dbgln("FATFS: EBPB: DOS 3.4");
            } else if (ebpb_version == DOSBIOSParameterBlockVersion::DOS_BPB_4) {
                dbgln("FATFS: EBPB: DOS 4");
            }
            dbgln("FATFS: drive_number: {}", dos4_boot_record->drive_number);
            dbgln("FATFS: flags: {}", dos4_boot_record->flags);
            dbgln("FATFS: volume_id: {}", dos4_boot_record->volume_id);

            // volume_label_string and file_system_type are only valid when
            // ebpb_version == DOSBIOSParameterBlockVersion::DOS4.
        }
    }

    if (ebpb.signature() != signature_1 && ebpb.signature() != signature_2) {
        dbgln("FATFS: Invalid signature");
        return EINVAL;
    }

    DOS3BIOSParameterBlock const* ebpb_block = ebpb.common_bpb();
    // The number of data area sectors is what DOS/Windows used to determine
    // if a partition was a FAT12, FAT16, or FAT32 file system.
    // From "FAT Type Determination" section of Microsoft FAT Specification
    // (fatgen103.doc):
    //     The FAT type—one of FAT12, FAT16, or FAT32—is determined by the count
    //     of clusters on the volume and nothing else.
    //
    // The following calculations are based on the equations provided in this
    // section.

    // "RootDirSectors" from MS FAT Specification. This is calculated as:
    //     Number of bytes occupied by root directory area (0 on FAT32)
    //         +
    //     Bytes to fill final sector (ie, round up)
    // Converted into sector count (by dividing by bytes per sector).
    u32 root_directory_sectors = ((ebpb_block->root_directory_entry_count * sizeof(FATEntry)) + (ebpb_block->bytes_per_sector - 1)) / ebpb_block->bytes_per_sector;

    // "DataSec" from MS FAT Specification.
    u32 data_area_sectors = ebpb.sector_count() - ((ebpb_block->reserved_sector_count) + (ebpb_block->fat_count * ebpb.sectors_per_fat()) + root_directory_sectors);

    // CountofClusters from MS FAT Specification.
    u32 data_area_clusters = data_area_sectors / ebpb_block->sectors_per_cluster;

    // Cluster thresholds and operators as defined in MS FAT Specification.
    if (data_area_clusters < 4085) {
        dbgln("FATFS: Detected FAT12 with {} data area clusters", data_area_clusters);
        m_fat_version = FATVersion::FAT12;
    } else if (data_area_clusters < 65525) {
        dbgln("FATFS: Detected FAT16 with {} data area clusters", data_area_clusters);
        m_fat_version = FATVersion::FAT16;
    } else {
        dbgln("FATFS: Assuming FAT32 with {} data area clusters", data_area_clusters);
        m_fat_version = FATVersion::FAT32;
    }

    m_device_block_size = ebpb_block->bytes_per_sector;
    set_logical_block_size(m_device_block_size);

    m_first_data_sector = block->reserved_sector_count + (block->fat_count * ebpb.sectors_per_fat()) + root_directory_sectors;

    TRY(BlockBasedFileSystem::initialize_while_locked());

    FATEntry root_entry {};

    if (m_fat_version == FATVersion::FAT32) {
        // FAT32 stores the root directory within the FAT (at the clusters specified
        // in the boot record), as opposed to the root directory area
        // (as done by FAT 12/16).

        DOS7BIOSParameterBlock const* boot_record = ebpb.dos7_bpb();
        // Ensure we have a DOS7 BPB (so that we can find the root directory cluster).
        if (boot_record == nullptr) {
            dbgln("FATFS: Non-DOS7 BPB for FAT32 FS.");
            return EINVAL;
        }
        root_entry.first_cluster_low = boot_record->root_directory_cluster & 0xFFFF;
        root_entry.first_cluster_high = boot_record->root_directory_cluster >> 16;
    } else {
        // FAT12/FAT16.
        // Use cluster = 0 as a signal to `first_block_of_cluster()` to look in the
        // root directory area for the root entry.
        // Clusters 0 and 1 hold special values, and will never be used to store file
        // data.
        root_entry.first_cluster_low = 0;
        root_entry.first_cluster_high = 0;
    }

    root_entry.attributes = FATAttributes::Directory;
    m_root_inode = TRY(FATInode::create(*this, root_entry));

    return {};
}

Inode& FATFS::root_inode()
{
    return *m_root_inode;
}

FatBlockSpan FATFS::first_block_of_cluster(u32 cluster) const
{
    // For FAT12/16, we use a value of cluster 0 to indicate this is a cluster for the root directory.
    // Cluster 0 and cluster 1 hold special values (cluster 0 holds the FAT ID, and cluster 1
    // the "end of chain marker"), neither of which will be present in the table or associated
    // with any file.
    // "Entries with the Volume Label flag, subdirectory ".." pointing to the FAT12 and FAT16 root, and empty files with size 0 should have first cluster 0."
    // --Wikipedia
    //
    DOSBIOSParameterBlock ebpb(m_boot_record);
    DOS3BIOSParameterBlock const* ebpb_block = ebpb.common_bpb();
    if (m_fat_version != FATVersion::FAT32 && cluster == 0) {
        // Root directory area follows the FATs after the reserved sectors.
        return FatBlockSpan {
            ebpb_block->reserved_sector_count + (ebpb_block->fat_count * ebpb.sectors_per_fat()),
            (ebpb_block->root_directory_entry_count * sizeof(FATEntry)) / ebpb_block->bytes_per_sector
        };
    } else {
        return FatBlockSpan {
            ((cluster - first_data_cluster) * ebpb_block->sectors_per_cluster) + m_first_data_sector,
            ebpb_block->sectors_per_cluster
        };
    }
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
