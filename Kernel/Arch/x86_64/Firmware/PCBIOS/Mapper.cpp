/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/Firmware/PCBIOS/Mapper.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

ErrorOr<Memory::MappedROM> map_bios()
{
    VERIFY(g_boot_info.boot_method == BootMethod::Multiboot1);

    Memory::MappedROM mapping;
    mapping.size = 128 * KiB;
    mapping.paddr = PhysicalAddress(0xe0000);
    auto region_size = TRY(Memory::page_round_up(mapping.size));
    mapping.region = TRY(MM.allocate_mmio_kernel_region(mapping.paddr, region_size, {}, Memory::Region::Access::Read));
    return mapping;
}

ErrorOr<Memory::MappedROM> map_ebda()
{
    VERIFY(g_boot_info.boot_method == BootMethod::Multiboot1);

    auto ebda_segment_ptr = TRY(Memory::map_typed<u16>(PhysicalAddress(0x40e)));
    PhysicalAddress ebda_paddr(PhysicalAddress(*ebda_segment_ptr).get() << 4);
    // The EBDA size is stored in the first byte of the EBDA in 1K units
    size_t ebda_size = *TRY(Memory::map_typed<u8>(ebda_paddr));
    ebda_size *= 1024;

    Memory::MappedROM mapping;
    auto region_size = TRY(Memory::page_round_up(ebda_size));
    mapping.region = TRY(MM.allocate_mmio_kernel_region(ebda_paddr.page_base(), region_size, {}, Memory::Region::Access::Read));
    mapping.offset = ebda_paddr.offset_in_page();
    mapping.size = ebda_size;
    mapping.paddr = ebda_paddr;
    return mapping;
}

}
