/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef __cplusplus
#    include <Kernel/Boot/Multiboot.h>
#    include <Kernel/Memory/PhysicalAddress.h>
#    include <Kernel/Memory/VirtualAddress.h>
#    include <Kernel/Prekernel/Arch/ArchSpecificBootInfo.h>
#endif

#define MAX_KERNEL_SIZE 0x4000000
#define KERNEL_PD_SIZE 0x31000000

// FIXME: This should be using the define from Sections.h, but that currently is not possible
//        and causes linker errors, because Sections.h includes BootInfo.h.
#define KERNEL_MAPPING_BASE 0x2000000000

#ifdef __cplusplus

extern "C" multiboot_info_t* multiboot_info_ptr;

namespace Kernel {

namespace Memory {
class PageTableEntry;
}

struct Multiboot1BootInfo {
    u32 flags { 0 };
    multiboot_memory_map_t const* memory_map { 0 };
    u32 memory_map_count { 0 };
    PhysicalAddress module_physical_ptr { 0 };
    u32 module_length { 0 };

    PhysicalAddress start_of_prekernel_image { 0 };
    PhysicalAddress end_of_prekernel_image { 0 };

    PhysicalAddress boot_pd0 { 0 };
};

struct PreInitBootInfo {
};

enum class BootMethod {
    Multiboot1,
    PreInit,
};

enum class BootFramebufferType {
    None,
    BGRx8888,
};

struct BootFramebufferInfo {
    PhysicalAddress paddr { 0 };
    size_t pitch { 0 };
    size_t width { 0 };
    size_t height { 0 };
    u8 bpp { 0 };
    BootFramebufferType type { BootFramebufferType::None };
};

union BootMethodSpecificBootInfo {
    PreInitBootInfo pre_init {};
    Multiboot1BootInfo multiboot1;
};

struct BootInfo {
    ArchSpecificBootInfo arch_specific {};

    BootMethodSpecificBootInfo boot_method_specific {};
    BootMethod boot_method { BootMethod::PreInit };

    PhysicalAddress flattened_devicetree_paddr { 0 };
    size_t flattened_devicetree_size { 0 };

    size_t physical_to_virtual_offset { 0 };
    FlatPtr kernel_mapping_base { 0 };
    FlatPtr kernel_load_base { 0 };

    PhysicalAddress boot_pml4t { 0 };
    PhysicalAddress boot_pdpt { 0 };
    PhysicalAddress boot_pd_kernel { 0 };

    Memory::PageTableEntry* boot_pd_kernel_pt1023 { 0 };

    StringView cmdline;

    BootFramebufferInfo boot_framebuffer {};
};

}
#endif
