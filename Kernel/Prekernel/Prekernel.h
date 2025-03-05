/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifdef __cplusplus
#    include <Kernel/Boot/Multiboot.h>
#    include <Kernel/EFIPrekernel/EFIPrekernel.h>
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

struct EFIBootInfo {
    EFIMemoryMap memory_map;
    VirtualAddress bootstrap_page_vaddr;
    PhysicalAddress bootstrap_page_page_directory_paddr;
};

enum class BootMethod {
    Multiboot1,
    PreInit,
    EFI,
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

struct SMBIOSBootInfo {
    PhysicalAddress entry_point_paddr { 0 };
    u8 entry_point_length { 0 };
    bool entry_point_is_64_bit { false };
    u32 maximum_structure_table_length { 0 };
    PhysicalAddress structure_table_paddr { 0 };
};

union BootMethodSpecificBootInfo {
    PreInitBootInfo pre_init {};
    Multiboot1BootInfo multiboot1;
    EFIBootInfo efi;
};

struct BootInfo {
    ArchSpecificBootInfo arch_specific {};

    BootMethodSpecificBootInfo boot_method_specific {};
    BootMethod boot_method { BootMethod::PreInit };

    PhysicalAddress flattened_devicetree_paddr { 0 };
    size_t flattened_devicetree_size { 0 };

    PhysicalAddress acpi_rsdp_paddr { 0 };
    SMBIOSBootInfo smbios {};

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

template<>
struct AK::Formatter<Kernel::BootMethod> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::BootMethod boot_method)
    {
        using enum Kernel::BootMethod;
        switch (boot_method) {
        case PreInit:
            return builder.put_literal("PreInit"sv);
        case Multiboot1:
            return builder.put_literal("Multiboot1"sv);
        case EFI:
            return builder.put_literal("EFI"sv);
        }

        VERIFY_NOT_REACHED();
    }
};
#endif
