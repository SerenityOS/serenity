/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

namespace Kernel {

struct [[gnu::packed]] FAT32BootRecord {
    u8 boot_jump[3];
    char oem_identifier[8];
    u16 bytes_per_sector;
    u8 sectors_per_cluster;
    u16 reserved_sector_count;
    u8 fat_count;
    u16 root_directory_entry_count;
    u16 unused1;
    u8 media_descriptor_type;
    u16 unused2;
    u16 sectors_per_track;
    u16 head_count;
    u32 hidden_sector_count;
    u32 sector_count;
    u32 sectors_per_fat;
    u16 flags;
    u16 fat_version;
    u32 root_directory_cluster;
    u16 fs_info_sector;
    u16 backup_boot_sector;
    u8 unused3[12];
    u8 drive_number;
    u8 unused4;
    u8 signature;
    u32 volume_id;
    char volume_label_string[11];
    char system_identifier_string[8];
};
static_assert(sizeof(FAT32BootRecord) == 90);

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

union FATPackedTime {
    u16 value;
    struct {
        u16 second : 5;
        u16 minute : 6;
        u16 hour : 5;
    };
};
static_assert(sizeof(FATPackedTime) == 2);

union FATPackedDate {
    u16 value;
    struct {
        u16 day : 5;
        u16 month : 4;
        u16 year : 7;
    };
};
static_assert(sizeof(FATPackedDate) == 2);

struct [[gnu::packed]] FATEntry {
    char filename[8];
    char extension[3];
    FATAttributes attributes;
    u8 unused1;
    u8 creation_time_seconds;
    FATPackedTime creation_time;
    FATPackedDate creation_date;
    FATPackedDate last_accessed_date;
    u16 first_cluster_high;
    FATPackedTime modification_time;
    FATPackedDate modification_date;
    u16 first_cluster_low;
    u32 file_size;
};
static_assert(sizeof(FATEntry) == 32);

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
static_assert(sizeof(FATLongFileNameEntry) == 32);

}
