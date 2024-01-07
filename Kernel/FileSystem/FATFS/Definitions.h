/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DOSPackedTime.h>
#include <AK/EnumBits.h>
#include <AK/Types.h>
#include <Kernel/Library/KBuffer.h>

namespace Kernel {

// This structure represents the DOS 3.31 BIOS Partition Block.
// While DOS 3.31 predates FAT verions 12/16/32 (the versions supported by this driver),
// the fields in this block are common with the DOS 4 and DOS 7 BIOS Parameter blocks.
// This structure will be followed by an "Extended BIOS Partition Block" (EBPB).
//
// The DOS 4 EBPB is *typically* used by FAT 12/16 file systems, while the DOS 7 EBPB
// is *typically* used by FAT 32. _However_, any combination is possible, as the FAT
// version is only determined by the number of clusters.
//
// Note that the DOS 4 and DOS 7 EBPB extensions are incompatible with each other
// (contain fields in different orders and of different lenghts) and do not contain
// an explicit indication to differentiate them.
// This driver uses heuristics to identify the EBPB version (based on the signature bytes
// and sector counts).
// FIXME: Consider also using the MBR parition type field in the future.
struct [[gnu::packed]] DOS3BIOSParameterBlock {
    u8 boot_jump[3];
    char oem_identifier[8];
    u16 bytes_per_sector; // Offset 0x0B -- beginning of DOS 3.31 BPB.
    u8 sectors_per_cluster;
    u16 reserved_sector_count;
    u8 fat_count;
    u16 root_directory_entry_count;
    u16 sector_count_16bit;
    u8 media_descriptor_type;
    u16 sectors_per_fat_16bit;
    u16 sectors_per_track;
    u16 head_count;
    u32 hidden_sector_count;
    u32 sector_count_32bit; // 0x020 -- end of DOS 3.31 BPB.
};
// 11 is the boot jump/OEM identifier prefix prior to the official BPB.
static_assert(AssertSize<DOS3BIOSParameterBlock, 11 + 25>());

struct [[gnu::packed]] DOS4BIOSParameterBlock {
    // Begins at sector offset 0x024.
    u8 drive_number; // 0x024
    u8 flags;
    u8 signature;
    u32 volume_id;
    char volume_label_string[11];
    char file_system_type[8];
};
static_assert(AssertSize<DOS4BIOSParameterBlock, 26>());

struct [[gnu::packed]] DOS7BIOSParameterBlock {
    // Begins at sector offset 0x024.
    u32 sectors_per_fat_32bit; // 0x024
    u16 flags;
    u16 fat_version; // Expected value 0x2b2a.
    u32 root_directory_cluster;
    u16 fs_info_sector;
    u16 backup_boot_sector;
    u8 unused3[12];
    u8 drive_number;
    u8 unused4;
    u8 signature;
    u32 volume_id;
    char volume_label_string[11];
    char file_system_type[8];
};
static_assert(AssertSize<DOS7BIOSParameterBlock, 54>());

enum DOSBIOSParameterBlockVersion {
    DOS_BPB_UNKNOWN,
    DOS_BPB_3, // Version 3.4.
    DOS_BPB_4, // Version 4.0
    DOS_BPB_7  // Version 7.0
};

enum class FATVersion {
    FAT12,
    FAT16,
    FAT32,
};

enum class FATAttributes : u8 {
    ReadOnly = 0x01,
    Hidden = 0x02,
    System = 0x04,
    VolumeID = 0x08,
    Directory = 0x10,
    Archive = 0x20,
    LongFileName = 0x0F
};

AK_ENUM_BITWISE_OPERATORS(FATAttributes);

struct [[gnu::packed]] FATEntry {
    char filename[8];
    char extension[3];
    FATAttributes attributes;
    u8 unused1;
    u8 creation_time_seconds;
    DOSPackedTime creation_time;
    DOSPackedDate creation_date;
    DOSPackedDate last_accessed_date;
    u16 first_cluster_high;
    DOSPackedTime modification_time;
    DOSPackedDate modification_date;
    u16 first_cluster_low;
    u32 file_size;
};
static_assert(AssertSize<FATEntry, 32>());

struct [[gnu::packed]] FATLongFileNameEntry {
    u8 entry_index;
    u16 characters1[5];
    FATAttributes attributes;
    u8 entry_type;
    u8 checksum;
    u16 characters2[6];
    u16 zero;
    u16 characters3[2];
};
static_assert(AssertSize<FATLongFileNameEntry, 32>());

}
