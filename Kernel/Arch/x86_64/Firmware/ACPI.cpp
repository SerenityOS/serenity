/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86_64/Firmware/ACPI.h>
#include <Kernel/Arch/x86_64/Firmware/PCBIOS/Mapper.h>
#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/Memory/MemoryManager.h>

namespace Kernel::ACPI::StaticParsing {

static bool is_rsdp_valid(ReadonlyBytes rsdp)
{
    if (rsdp.size() < sizeof(Structures::RSDPDescriptor))
        return false;

    u8 revision = reinterpret_cast<Structures::RSDPDescriptor const*>(rsdp.data())->revision;

    u8 checksum = 0;
    for (size_t i = 0; i < sizeof(Structures::RSDPDescriptor); ++i)
        checksum += rsdp[i];

    if (checksum != 0)
        return false;

    if (revision == 0)
        // Checksum matched and there's nothing more to check.
        return true;

    if (rsdp.size() < sizeof(Structures::RSDPDescriptor20))
        return false;

    checksum = 0;
    for (size_t i = 0; i < sizeof(Structures::RSDPDescriptor20); ++i)
        checksum += rsdp[i];
    return checksum == 0;
}

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#finding-the-rsdp-on-ia-pc-systems
Optional<PhysicalAddress> find_rsdp_in_ia_pc_specific_memory_locations()
{
    static constexpr auto signature = "RSD PTR "sv;
    static constexpr size_t rsdp_alignment = 16;

    auto locate_rsdp = [](Memory::MappedROM mapping) -> Optional<PhysicalAddress> {
        return mapping.find_chunk_starting_with(signature, rsdp_alignment, is_rsdp_valid);
    };

    auto ebda_or_error = map_ebda();
    if (!ebda_or_error.is_error()) {
        auto maybe_rsdp = locate_rsdp(ebda_or_error.release_value());
        if (maybe_rsdp.has_value())
            return maybe_rsdp.value();
    }
    auto bios_or_error = map_bios();
    if (!bios_or_error.is_error()) {
        auto maybe_rsdp = locate_rsdp(bios_or_error.release_value());
        if (maybe_rsdp.has_value())
            return maybe_rsdp.value();
    }

    // On some systems the RSDP may be located in ACPI NVS or reclaimable memory regions
    Vector<Memory::PhysicalMemoryRange> potential_ranges;
    MM.for_each_physical_memory_range([&](auto& memory_range) {
        if (!(memory_range.type == Memory::PhysicalMemoryRangeType::ACPI_NVS || memory_range.type == Memory::PhysicalMemoryRangeType::ACPI_Reclaimable))
            return IterationDecision::Continue;

        potential_ranges.try_append(memory_range).release_value_but_fixme_should_propagate_errors();
        return IterationDecision::Continue;
    });

    for (auto const& memory_range : potential_ranges) {
        auto region_size_or_error = Memory::page_round_up(memory_range.length);
        if (region_size_or_error.is_error())
            continue;

        auto region_or_error = MM.allocate_mmio_kernel_region(memory_range.start, region_size_or_error.value(), {}, Memory::Region::Access::Read);
        if (region_or_error.is_error())
            continue;

        Memory::MappedROM mapping;
        mapping.region = region_or_error.release_value();
        mapping.offset = memory_range.start.offset_in_page();
        mapping.size = memory_range.length;
        mapping.paddr = memory_range.start;

        auto maybe_rsdp = locate_rsdp(move(mapping));
        if (maybe_rsdp.has_value())
            return maybe_rsdp.value();
    }

    return {};
}

}
