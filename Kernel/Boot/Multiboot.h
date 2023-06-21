/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

struct multiboot_module_entry {
    u32 start;
    u32 end;
    u32 string_addr;
    u32 reserved;
};
typedef struct multiboot_module_entry multiboot_module_entry_t;

struct multiboot_aout_symbol_table {
    u32 tabsize;
    u32 strsize;
    u32 addr;
    u32 reserved;
};
typedef struct multiboot_aout_symbol_table multiboot_aout_symbol_table_t;

struct multiboot_elf_section_header_table {
    u32 num;
    u32 size;
    u32 addr;
    u32 shndx;
};
typedef struct multiboot_elf_section_header_table multiboot_elf_section_header_table_t;

#define MULTIBOOT_MEMORY_AVAILABLE 1
#define MULTIBOOT_MEMORY_RESERVED 2
#define MULTIBOOT_MEMORY_ACPI_RECLAIMABLE 3
#define MULTIBOOT_MEMORY_NVS 4
#define MULTIBOOT_MEMORY_BADRAM 5

struct multiboot_mmap_entry {
    u32 size;
    u64 addr;
    u64 len;
    u32 type;
#if ARCH(AARCH64)
    // __attribute__((packed)) causes alignment issues on aarch64
};
#else
} __attribute__((packed));
#endif
typedef struct multiboot_mmap_entry multiboot_memory_map_t;

#define MULTIBOOT_INFO_FRAMEBUFFER_INFO (1 << 12)

struct multiboot_info {
    // Multiboot info version number.
    u32 flags;

    // Available memory from BIOS.
    u32 mem_lower;
    u32 mem_upper;

    // "root" partition.
    u32 boot_device;

    // Kernel command line.
    u32 cmdline;

    // Boot-Module list.
    u32 mods_count;
    u32 mods_addr;

    union {
        multiboot_aout_symbol_table_t aout_sym;
        multiboot_elf_section_header_table_t elf_sec;
    } u;

    // Memory Mapping buffer.
    u32 mmap_length;
    u32 mmap_addr;

    // Drive Info buffer.
    u32 drives_length;
    u32 drives_addr;

    // ROM configuration table.
    u32 config_table;

    // Boot Loader Name.
    u32 boot_loader_name;

    // APM table.
    u32 apm_table;

    // Video.
    u32 vbe_control_info;
    u32 vbe_mode_info;
    u16 vbe_mode;
    u16 vbe_interface_seg;
    u16 vbe_interface_off;
    u16 vbe_interface_len;

    u64 framebuffer_addr;
    u32 framebuffer_pitch;
    u32 framebuffer_width;
    u32 framebuffer_height;
    u8 framebuffer_bpp;
#define MULTIBOOT_FRAMEBUFFER_TYPE_INDEXED 0
#define MULTIBOOT_FRAMEBUFFER_TYPE_RGB 1
#define MULTIBOOT_FRAMEBUFFER_TYPE_EGA_TEXT 2
    u8 framebuffer_type;
    union {
        struct
        {
            u32 framebuffer_palette_addr;
            u16 framebuffer_palette_num_colors;
        };
        struct
        {
            u8 framebuffer_red_field_position;
            u8 framebuffer_red_mask_size;
            u8 framebuffer_green_field_position;
            u8 framebuffer_green_mask_size;
            u8 framebuffer_blue_field_position;
            u8 framebuffer_blue_mask_size;
        };
    };
};
typedef struct multiboot_info multiboot_info_t;
