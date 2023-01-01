/*
 * Copyright (c) 2023, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#define MULTIBOOT2_BOOTLOADER_MAGIC 0x36d76289

#define MULTIBOOT2_TAG_TYPE_END 0
#define MULTIBOOT2_TAG_TYPE_MODULE 3
#define MULTIBOOT2_TAG_TYPE_MMAP 6
#define MULTIBOOT2_TAG_TYPE_FRAMEBUFFER 8

struct multiboot2_tag {
    u32 type;
    u32 size;
};
typedef struct multiboot2_tag multiboot2_tag_t;

struct multiboot2_tag_mmap_entry {
    u64 addr;
    u64 len;
    u32 type; // Currently the same as in multiboot1
    u32 reserved;
};
typedef struct multiboot2_tag_mmap_entry multiboot2_tag_mmap_entry_t;

struct multiboot2_tag_mmap {
    u32 type;
    u32 size;
    u32 len;
    u32 version;
    struct multiboot2_tag_mmap_entry entries[0];
};
typedef struct multiboot2_tag_mmap multiboot2_tag_mmap_t;

struct multiboot2_tag_module {
    u32 type;
    u32 size;
    u32 start;
    u32 end;
    u8 cmdline[0];
};
typedef struct multiboot2_tag_module multiboot2_tag_module_t;

struct multiboot2_tag_framebuffer {
    u32 type;
    u32 size;
    u64 addr;
    u32 pitch;
    u32 width;
    u32 height;
    u8 bpp;
    u8 mode;
};
typedef struct multiboot2_tag_framebuffer multiboot2_tag_framebuffer_t;
