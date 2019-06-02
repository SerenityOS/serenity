#pragma once

#include <AK/Types.h>

struct multiboot_aout_symbol_table {
    dword tabsize;
    dword strsize;
    dword addr;
    dword reserved;
};
typedef struct multiboot_aout_symbol_table multiboot_aout_symbol_table_t;

struct multiboot_elf_section_header_table {
    dword num;
    dword size;
    dword addr;
    dword shndx;
};
typedef struct multiboot_elf_section_header_table multiboot_elf_section_header_table_t;

struct multiboot_info {
    // Multiboot info version number.
    dword flags;

    // Available memory from BIOS.
    dword mem_lower;
    dword mem_upper;

    // "root" partition.
    dword boot_device;

    // Kernel command line.
    dword cmdline;

    // Boot-Module list.
    dword mods_count;
    dword mods_addr;

    union {
        multiboot_aout_symbol_table_t aout_sym;
        multiboot_elf_section_header_table_t elf_sec;
    } u;

    // Memory Mapping buffer.
    dword mmap_length;
    dword mmap_addr;

    // Drive Info buffer.
    dword drives_length;
    dword drives_addr;

    // ROM configuration table.
    dword config_table;

    // Boot Loader Name.
    dword boot_loader_name;

    // APM table.
    dword apm_table;

    // Video.
    dword vbe_control_info;
    dword vbe_mode_info;
    word vbe_mode;
    word vbe_interface_seg;
    word vbe_interface_off;
    word vbe_interface_len;

    qword framebuffer_addr;
    dword framebuffer_pitch;
    dword framebuffer_width;
    dword framebuffer_height;
    byte framebuffer_bpp;
#define MULTIBOOT_FRAMEBUFFER_TYPE_INDEXED 0
#define MULTIBOOT_FRAMEBUFFER_TYPE_RGB 1
#define MULTIBOOT_FRAMEBUFFER_TYPE_EGA_TEXT 2
    byte framebuffer_type;
    union {
        struct
        {
            dword framebuffer_palette_addr;
            word framebuffer_palette_num_colors;
        };
        struct
        {
            byte framebuffer_red_field_position;
            byte framebuffer_red_mask_size;
            byte framebuffer_green_field_position;
            byte framebuffer_green_mask_size;
            byte framebuffer_blue_field_position;
            byte framebuffer_blue_mask_size;
        };
    };
};
typedef struct multiboot_info multiboot_info_t;

extern "C" multiboot_info_t* multiboot_info_ptr;
