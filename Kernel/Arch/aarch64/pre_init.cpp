/*
 * Copyright (c) 2023, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <AK/Types.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/aarch64/CPU.h>
#include <Kernel/Sections.h>

#include <LibELF/Relocation.h>

// We arrive here from boot.S with the MMU disabled and in an unknown exception level (EL).

// FIXME: This should probably be shared with the Prekernel.

namespace Kernel {

extern "C" u8 start_of_kernel_image[];
extern "C" u8 _DYNAMIC[];

extern "C" [[noreturn]] void init();

extern "C" [[noreturn]] void pre_init(PhysicalPtr flattened_devicetree_paddr);
extern "C" [[noreturn]] void pre_init(PhysicalPtr flattened_devicetree_paddr)
{
    // Apply relative relocations as if we were running at KERNEL_MAPPING_BASE.
    // This means that we shouldn't access anything during pre_init that relies on relocations (e.g. vtables).
    // Otherwise, we would have to relocate twice: once while running identity mapped, and again when we enable the MMU.
    auto physical_load_base = bit_cast<PhysicalPtr>(+start_of_kernel_image);
    auto dynamic_section_addr = bit_cast<PhysicalPtr>(+_DYNAMIC);
    if (!ELF::perform_relative_relocations(physical_load_base, KERNEL_MAPPING_BASE, dynamic_section_addr))
        panic_without_mmu("Failed to perform relative relocations"sv);

    // We want to drop to EL1 as soon as possible, because that is the
    // exception level the kernel should run at.
    initialize_exceptions();

    // Set up page tables, enable the MMU, and jump to init.
    Memory::init_page_tables_and_jump_to_init(flattened_devicetree_paddr);
}

}
