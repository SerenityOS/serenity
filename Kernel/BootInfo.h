/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Multiboot.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel {
class PageTableEntry;
}

extern "C" PhysicalAddress start_of_prekernel_image;
extern "C" PhysicalAddress end_of_prekernel_image;
extern "C" size_t physical_to_virtual_offset;
extern "C" FlatPtr kernel_mapping_base;
extern "C" FlatPtr kernel_load_base;
#if ARCH(X86_64)
extern "C" u32 gdt64ptr;
extern "C" u16 code64_sel;
extern "C" PhysicalAddress boot_pml4t;
#endif
extern "C" PhysicalAddress boot_pdpt;
extern "C" PhysicalAddress boot_pd0;
extern "C" PhysicalAddress boot_pd_kernel;
extern "C" Kernel::PageTableEntry* boot_pd_kernel_pt1023;
extern "C" const char* kernel_cmdline;
extern "C" u32 multiboot_flags;
extern "C" multiboot_memory_map_t* multiboot_memory_map;
extern "C" size_t multiboot_memory_map_count;
extern "C" multiboot_module_entry_t* multiboot_modules;
extern "C" size_t multiboot_modules_count;
extern "C" PhysicalAddress multiboot_framebuffer_addr;
extern "C" u32 multiboot_framebuffer_pitch;
extern "C" u32 multiboot_framebuffer_width;
extern "C" u32 multiboot_framebuffer_height;
extern "C" u8 multiboot_framebuffer_bpp;
extern "C" u8 multiboot_framebuffer_type;
