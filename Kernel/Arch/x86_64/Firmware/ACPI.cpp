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

static bool is_rsdp_valid(u8 const* rsdp, size_t region_size)
{
    if (region_size < sizeof(Structures::RSDPDescriptor))
        return false;

    u8 revision = reinterpret_cast<Structures::RSDPDescriptor const*>(rsdp)->revision;

    u8 checksum = 0;
    for (size_t i = 0; i < sizeof(Structures::RSDPDescriptor); ++i)
        checksum += rsdp[i];

    if (checksum != 0)
        return false;

    if (revision == 0)
        // Checksum matched and there's nothing more to check.
        return true;

    if (region_size < sizeof(Structures::RSDPDescriptor20))
        return false;

    checksum = 0;
    for (size_t i = 0; i < sizeof(Structures::RSDPDescriptor20); ++i)
        checksum += rsdp[i];
    return checksum == 0;
}

// https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#finding-the-rsdp-on-ia-pc-systems
Optional<PhysicalAddress> find_rsdp_in_ia_pc_specific_memory_locations()
{
    constexpr auto signature = "RSD PTR "sv;

    auto locate_rsdp = [&signature](Memory::MappedROM mapping) -> Optional<PhysicalAddress> {
        constexpr size_t rsdp_alignment = 16;
        size_t start_paddr = mapping.paddr.get() + mapping.offset;
        size_t alignment_offset = align_up_to(start_paddr, rsdp_alignment) - start_paddr;

        mapping.offset += alignment_offset;
        mapping.size = align_down_to(mapping.size - alignment_offset, rsdp_alignment);

        VERIFY((mapping.paddr.get() + mapping.offset) % rsdp_alignment == 0);
        VERIFY(mapping.size % rsdp_alignment == 0);

        while (true) {
            u8 const* rsdp = mapping.pointer_to_chunk_starting_with(signature, rsdp_alignment);
            if (!rsdp)
                return {};
            if (is_rsdp_valid(rsdp, static_cast<size_t>(mapping.end() - rsdp)))
                return mapping.paddr_of(rsdp);
            size_t mapping_offset = static_cast<size_t>(rsdp - mapping.base()) + rsdp_alignment;
            mapping.offset += mapping_offset;
            mapping.size -= mapping_offset;
        }
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
