/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

struct [[gnu::packed]] initramfs_image_header {
    u8 magic[8];   // stands for 534552454350494F ("SERECPIO")
    u8 endianness; // NOTE: Value 0 is for little-endian, 1 for big-endian
    u8 padding[7];
    u32 inodes_count;
    u32 data_blocks_count;
    u8 data_block_alignment_size_power_2; // NOTE: Minimum value is 12 for 4096 bytes, max value is 24 for 16 MiB
    u8 reserved;
    u16 flags;
    u32 inodes_section_start;
    u32 inodes_names_section_start;
    u32 data_blocks_section_start;
};

struct [[gnu::packed]] initramfs_inode {
    u32 name_offset;
    u32 name_length;
    u32 file_size;
    u32 blocks_count;
    u32 blocks_offset;
    u32 uid;
    u32 gid;
    i64 mtime_seconds;
    u64 mtime_nanoseconds;
    u32 major;
    u32 minor;
    u16 mode;
};
