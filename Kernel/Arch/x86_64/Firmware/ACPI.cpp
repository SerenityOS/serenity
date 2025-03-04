/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/Firmware/ACPI.h>
#include <Kernel/Arch/x86_64/Firmware/PCBIOS/Mapper.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::ACPI::StaticParsing {

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#finding-the-rsdp-on-ia-pc-systems
Optional<PhysicalAddress> find_rsdp_in_ia_pc_specific_memory_locations()
{
    constexpr auto signature = "RSD PTR "sv;
    auto ebda_or_error = map_ebda();
    if (!ebda_or_error.is_error()) {
        auto rsdp = ebda_or_error.value().find_chunk_starting_with(signature, 16);
        if (rsdp.has_value())
            return rsdp;
    }
    auto bios_or_error = map_bios();
    if (!bios_or_error.is_error()) {
        auto rsdp = bios_or_error.value().find_chunk_starting_with(signature, 16);
        if (rsdp.has_value())
            return rsdp;
    }

    // On some systems the RSDP may be located in ACPI NVS or reclaimable memory regions
    Optional<PhysicalAddress> rsdp;
    MM.for_each_physical_memory_range([&](auto& memory_range) {
        if (!(memory_range.type == Memory::PhysicalMemoryRangeType::ACPI_NVS || memory_range.type == Memory::PhysicalMemoryRangeType::ACPI_Reclaimable))
            return IterationDecision::Continue;

        Memory::MappedROM mapping;
        auto region_size_or_error = Memory::page_round_up(memory_range.length);
        if (region_size_or_error.is_error())
            return IterationDecision::Continue;
        auto region_or_error = MM.allocate_mmio_kernel_region(memory_range.start, region_size_or_error.value(), {}, Memory::Region::Access::Read);
        if (region_or_error.is_error())
            return IterationDecision::Continue;
        mapping.region = region_or_error.release_value();
        mapping.offset = memory_range.start.offset_in_page();
        mapping.size = memory_range.length;
        mapping.paddr = memory_range.start;

        rsdp = mapping.find_chunk_starting_with(signature, 16);
        if (rsdp.has_value())
            return IterationDecision::Break;

        return IterationDecision::Continue;
    });
    return rsdp;
}

}
