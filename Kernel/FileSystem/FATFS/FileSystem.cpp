/*
 * Copyright (c) 2022-2024, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <AK/Endian.h>
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

ErrorOr<NonnullRefPtr<FileSystem>> FATFS::try_create(OpenFileDescription& file_description, FileSystemSpecificOptions const&)
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
        dbgln("FATFS: bytes_per_sector: {}", static_cast<u16>(block->bytes_per_sector));
        dbgln("FATFS: sectors_per_cluster: {}", block->sectors_per_cluster);
        dbgln("FATFS: reserved_sector_count: {}", block->reserved_sector_count);
        dbgln("FATFS: fat_count: {}", block->fat_count);
        dbgln("FATFS: root_directory_entry_count: {}", static_cast<u16>(block->root_directory_entry_count));
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
            dbgln("FATFS: volume_id: {}", static_cast<u32>(dos7_boot_record->volume_id));
        } else if (ebpb_version == DOSBIOSParameterBlockVersion::DOS_BPB_3 || ebpb_version == DOSBIOSParameterBlockVersion::DOS_BPB_4) {
            DOS4BIOSParameterBlock const* dos4_boot_record = ebpb.dos4_bpb();
            if (ebpb_version == DOSBIOSParameterBlockVersion::DOS_BPB_3) {
                dbgln("FATFS: EBPB: DOS 3.4");
            } else if (ebpb_version == DOSBIOSParameterBlockVersion::DOS_BPB_4) {
                dbgln("FATFS: EBPB: DOS 4");
            }
            dbgln("FATFS: drive_number: {}", dos4_boot_record->drive_number);
            dbgln("FATFS: flags: {}", dos4_boot_record->flags);
            dbgln("FATFS: volume_id: {}", static_cast<u32>(dos4_boot_record->volume_id));

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
    m_root_inode = TRY(FATInode::create(*this, root_entry, { 0, 1 }));

    if (m_fat_version == FATVersion::FAT32) {
        auto fs_info_buffer = UserOrKernelBuffer::for_kernel_buffer(bit_cast<u8*>(&m_fs_info));
        // We know that there is a DOS7 BPB, because if it wasn't present
        // we would have returned EINVAL above.
        TRY(read_block(ebpb.dos7_bpb()->fs_info_sector, &fs_info_buffer, sizeof(m_fs_info)));

        if (m_fs_info.lead_signature != fs_info_signature_1 || m_fs_info.struct_signature != fs_info_signature_2 || m_fs_info.trailing_signature != fs_info_signature_3) {
            dbgln("FATFS: Invalid FSInfo struct signature");
            dbgln_if(FAT_DEBUG, "FATFS: FSInfo signature1: {:#x}, expected: {:#x}", m_fs_info.lead_signature, fs_info_signature_1);
            dbgln_if(FAT_DEBUG, "FATFS: FSInfo signature2: {:#x}, expected: {:#x}", m_fs_info.struct_signature, fs_info_signature_2);
            dbgln_if(FAT_DEBUG, "FATFS: FSInfo signature3: {:#x}, expected: {:#x}", m_fs_info.trailing_signature, fs_info_signature_3);
            return Error::from_errno(EINVAL);
        }

        dbgln_if(FAT_DEBUG, "FATFS: fs_info.last_known_free_cluster_count: {}", m_fs_info.last_known_free_cluster_count);
        dbgln_if(FAT_DEBUG, "FATFS: fs_info.next_free_cluster_hint: {}", m_fs_info.next_free_cluster_hint);
    }

    return {};
}

Inode& FATFS::root_inode()
{
    return *m_root_inode;
}

ErrorOr<void> FATFS::rename(Inode& old_parent_inode, StringView old_basename, Inode& new_parent_inode, StringView new_basename)
{
    MutexLocker locker(m_lock);

    if (auto maybe_inode_to_be_replaced = new_parent_inode.lookup(new_basename); !maybe_inode_to_be_replaced.is_error()) {
        VERIFY(!maybe_inode_to_be_replaced.value()->is_directory());
        TRY(new_parent_inode.remove_child(new_basename));
    }

    auto old_inode = MUST(old_parent_inode.lookup(old_basename));

    TRY(new_parent_inode.add_child(old_inode, new_basename, old_inode->mode()));
    TRY(static_cast<FATInode&>(old_parent_inode).remove_child_impl(old_basename, FATInode::FreeClusters::No));

    if (old_inode->is_directory() && old_parent_inode.index() != new_parent_inode.index()) {
        auto dot_dot = TRY(old_inode->lookup(".."sv));
        if (m_fat_version == FATVersion::FAT32) {
            if (&new_parent_inode == &root_inode()) {
                static_cast<FATInode&>(*dot_dot).m_entry.first_cluster_low = 0;
                static_cast<FATInode&>(*dot_dot).m_entry.first_cluster_high = 0;
            } else {
                static_cast<FATInode&>(*dot_dot).m_entry.first_cluster_low = static_cast<FATInode&>(new_parent_inode).m_entry.first_cluster_low;
                static_cast<FATInode&>(*dot_dot).m_entry.first_cluster_high = static_cast<FATInode&>(new_parent_inode).m_entry.first_cluster_high;
            }
        } else {
            static_cast<FATInode&>(*dot_dot).m_entry.first_cluster_low = static_cast<FATInode&>(new_parent_inode).m_entry.first_cluster_low;
        }
        TRY(static_cast<FATInode&>(*dot_dot).flush_metadata());
    }

    return {};
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

size_t FATFS::fat_offset_for_cluster(u32 cluster) const
{
    switch (m_fat_version) {
    case FATVersion::FAT12: {
        // In FAT12, a cluster entry is stored in a byte, plus
        // the low/high nibble of an adjacent byte.
        //
        // CLSTR:   0 1      2 3      4 5
        // INDEX: [0 1 2], [3 4 5], [6 7 8]

        // Every 2 clusters are represented using 3 bytes.
        return (cluster * 3) / 2;
    } break;
    case FATVersion::FAT16:
        return cluster * 2; // Each cluster is stored in 2 bytes.
    case FATVersion::FAT32:
        return cluster * 4; // Each cluster is stored in 4 bytes.
    default:
        VERIFY_NOT_REACHED();
    }
}

u32 FATFS::cluster_number(KBuffer const& fat_sector, u32 entry_cluster_number, u32 entry_offset) const
{
    u32 cluster = 0;
    switch (m_fat_version) {
    case FATVersion::FAT12: {
        u16 fat12_bytes_le = 0;
        // Two FAT12 entries get stored in a total of 3 bytes, as follows:
        // AB CD EF are grouped as [D AB] and [E FC] (little-endian).
        // For a given cluster, we interpret the associated 2 bytes as a little-endian
        // 16-bit value ({CD AB} or {EF CD}), and then shift/mask the extra high or low nibble.
        ByteReader::load<u16>(fat_sector.bytes().offset(entry_offset), fat12_bytes_le);
        cluster = AK::convert_between_host_and_little_endian(fat12_bytes_le);
        if (entry_cluster_number % 2 == 0) {
            // CD AB -> D AB
            cluster &= 0x0FFF;
        } else {
            // EF CD -> E FC.
            cluster = cluster >> 4;
        }
        break;
    }
    case FATVersion::FAT16: {
        u16 cluster_u16_le = 0;
        ByteReader::load<u16>(fat_sector.bytes().offset(entry_offset), cluster_u16_le);
        cluster = AK::convert_between_host_and_little_endian(cluster_u16_le);
        break;
    }
    case FATVersion::FAT32: {
        u32 cluster_u32_le = 0;
        ByteReader::load<u32>(fat_sector.bytes().offset(entry_offset), cluster_u32_le);
        cluster = AK::convert_between_host_and_little_endian(cluster_u32_le);
        // FAT32 entries use 28-bits to represent the cluster number. The top 4 bits
        // may contain flags or other data and must be masked off.
        cluster &= 0x0FFFFFFF;
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
    return cluster;
}

u32 FATFS::end_of_chain_marker() const
{
    // Returns the end of chain entry for the given file system.
    // Any FAT entry of this value or greater signifies the end
    // of the chain has been reached for a given entry.
    switch (m_fat_version) {
    case FATVersion::FAT12:
        return 0xFF8;
    case FATVersion::FAT16:
        return 0xFFF8;
    case FATVersion::FAT32:
        return 0x0FFFFFF8;
    default:
        VERIFY_NOT_REACHED();
    }
}

ErrorOr<void> FATFS::update_fsinfo(u32 free_cluster_count, u32 next_free_cluster_hint)
{
    VERIFY(m_fat_version == FATVersion::FAT32);

    m_fs_info.last_known_free_cluster_count = free_cluster_count;
    m_fs_info.next_free_cluster_hint = next_free_cluster_hint;
    auto fs_info_buffer = UserOrKernelBuffer::for_kernel_buffer(bit_cast<u8*>(&m_fs_info));
    TRY(write_block(m_parameter_block->dos7_bpb()->fs_info_sector, fs_info_buffer, sizeof(m_fs_info)));

    return {};
}

ErrorOr<u32> FATFS::allocate_cluster()
{
    u32 start_cluster;
    if (m_fat_version == FATVersion::FAT32) {
        // If we have a hint, start there.
        if (m_fs_info.next_free_cluster_hint != fs_info_data_unknown) {
            start_cluster = m_fs_info.next_free_cluster_hint;
        } else {
            // Otherwise, start at the beginning of the data area.
            start_cluster = first_data_cluster;
        }
    } else {
        // For FAT12/16, start at the beginning of the data area, as there is no
        // FSInfo struct to store the hint.
        start_cluster = first_data_cluster;
    }

    MutexLocker locker(m_lock);

    for (u32 i = start_cluster; i < m_parameter_block->sector_count() / m_parameter_block->common_bpb()->sectors_per_cluster; i++) {
        if (TRY(fat_read(i)) == 0) {
            dbgln_if(FAT_DEBUG, "FATFS: Allocating cluster {}", i);

            if (m_fat_version == FATVersion::FAT32)
                TRY(update_fsinfo(m_fs_info.last_known_free_cluster_count == fs_info_data_unknown ? fs_info_data_unknown : (m_fs_info.last_known_free_cluster_count - 1), i + 1));

            TRY(fat_write(i, end_of_chain_marker()));
            return i;
        }
    }

    return Error::from_errno(ENOSPC);
}

ErrorOr<void> FATFS::notify_clusters_freed(u32 first_freed_cluster, u32 freed_cluster_count)
{
    if (m_fat_version == FATVersion::FAT32) {
        u32 free_cluster_count = (m_fs_info.last_known_free_cluster_count == fs_info_data_unknown) ? fs_info_data_unknown : (m_fs_info.last_known_free_cluster_count + freed_cluster_count);
        u32 first_free_cluster = (first_freed_cluster < m_fs_info.next_free_cluster_hint || m_fs_info.next_free_cluster_hint == fs_info_data_unknown) ? first_freed_cluster : m_fs_info.next_free_cluster_hint;
        TRY(update_fsinfo(free_cluster_count, first_free_cluster));
    }

    return {};
}

ErrorOr<void> FATFS::notify_cluster_freed(u32 cluster)
{
    return notify_clusters_freed(cluster, 1);
}

ErrorOr<u32> FATFS::fat_read(u32 cluster)
{
    dbgln_if(FAT_DEBUG, "FATFS: Reading FAT entry for cluster {}", cluster);

    u32 fat_offset = fat_offset_for_cluster(cluster);
    u32 fat_sector_index = m_parameter_block->common_bpb()->reserved_sector_count + (fat_offset / m_device_block_size);
    u32 entry_offset = fat_offset % m_device_block_size;

    // NOTE: On FAT12, FATs aren't necessarily block aligned, so in the worst case we have to read
    // an extra byte from the next block.
    bool read_extra_block = m_fat_version == FATVersion::FAT12 && entry_offset == m_device_block_size - 1;
    size_t buffer_size = m_device_block_size;
    if (read_extra_block)
        buffer_size += m_device_block_size;

    auto fat_sector = TRY(KBuffer::try_create_with_size("FATFS: FAT read buffer"sv, buffer_size));
    auto fat_sector_buffer = UserOrKernelBuffer::for_kernel_buffer(fat_sector->data());

    MutexLocker locker(m_lock);

    if (read_extra_block)
        TRY(read_blocks(fat_sector_index, 2, fat_sector_buffer));
    else
        TRY(read_block(fat_sector_index, &fat_sector_buffer, m_device_block_size));

    // Look up the next cluster to read, or read End of Chain marker from table.
    return cluster_number(*fat_sector, cluster, entry_offset);
}

ErrorOr<void> FATFS::fat_write(u32 cluster, u32 value)
{
    dbgln_if(FAT_DEBUG, "FATFS: Writing FAT entry for cluster {} with value {}", cluster, value);

    u32 fat_offset = fat_offset_for_cluster(cluster);
    u32 fat_sector_index = m_parameter_block->common_bpb()->reserved_sector_count + (fat_offset / m_device_block_size);
    u32 entry_offset = fat_offset % m_device_block_size;

    // See the comment in fat_read().
    bool need_extra_block = m_fat_version == FATVersion::FAT12 && entry_offset == m_device_block_size - 1;
    size_t buffer_size = m_device_block_size;
    if (need_extra_block)
        buffer_size += m_device_block_size;

    auto fat_sector = TRY(KBuffer::try_create_with_size("FATFS: FAT read buffer"sv, buffer_size));
    auto fat_sector_buffer = UserOrKernelBuffer::for_kernel_buffer(fat_sector->data());

    MutexLocker locker(m_lock);

    if (need_extra_block)
        TRY(read_blocks(fat_sector_index, 2, fat_sector_buffer));
    else
        TRY(read_block(fat_sector_index, &fat_sector_buffer, m_device_block_size));

    switch (m_fat_version) {
    case FATVersion::FAT12: {
        auto write_misaligned_u16 = [&](u16 word) {
            *bit_cast<u8*>(&fat_sector->data()[entry_offset]) = word & 0xFF;
            *(bit_cast<u8*>(&fat_sector->data()[entry_offset]) + 1) = word >> 8;
        };
        u16 existing_bytes_le = 0;
        ByteReader::load<u16>(fat_sector->bytes().offset(entry_offset), existing_bytes_le);
        u16 existing_bytes = AK::convert_between_host_and_little_endian(existing_bytes_le);
        if (cluster % 2 == 0) {
            existing_bytes &= 0xF000;
            existing_bytes |= static_cast<u16>(value) & 0xFFF;
        } else {
            existing_bytes &= 0x000F;
            existing_bytes |= static_cast<u16>(value) << 4;
        }
        write_misaligned_u16(AK::convert_between_host_and_little_endian(existing_bytes));
        break;
    }
    case FATVersion::FAT16: {
        *bit_cast<u16*>(&fat_sector->data()[entry_offset]) = AK::convert_between_host_and_little_endian(static_cast<u16>(value));
        break;
    }
    case FATVersion::FAT32: {
        *bit_cast<u32*>(&fat_sector->data()[entry_offset]) = AK::convert_between_host_and_little_endian(value);
        break;
    }
    }

    for (size_t i = 0; i < m_parameter_block->common_bpb()->fat_count; ++i) {
        u32 target_sector_index = fat_sector_index + i * m_parameter_block->sectors_per_fat();
        if (need_extra_block)
            TRY(write_blocks(target_sector_index, 2, fat_sector_buffer));
        else
            TRY(write_block(target_sector_index, fat_sector_buffer, m_device_block_size));
    }

    return {};
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
