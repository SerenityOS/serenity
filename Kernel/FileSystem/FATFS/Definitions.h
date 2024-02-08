/*
 * Copyright (c) 2022, Undefine <undefine@undefine.pl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DOSPackedTime.h>
#include <AK/EnumBits.h>
#include <AK/Types.h>
#include <Kernel/API/FileSystem/FATStructures.h>
#include <Kernel/Library/KBuffer.h>

namespace Kernel {

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
