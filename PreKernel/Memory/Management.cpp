/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/kstdio.h>
#include <PreKernel/Assertions.h>
#include <PreKernel/Images/Multiboot.h>
#include <PreKernel/Memory/Management.h>

namespace Prekernel {

static AK::Singleton<MemoryManagement> s_the;

MemoryManagement::MemoryManagement()
{
}

bool MemoryManagement::was_initialized()
{
    if (s_the.is_initialized()) {
        return s_the->m_was_initialized;
    }
    return false;
}

MemoryManagement& MemoryManagement::the()
{
    return *s_the;
}

Optional<PhysicalRange> MemoryManagement::try_to_find_the_biggest_available_range() const
{
    PhysicalRange best_candidate;
    for (auto& physical_region : m_regions) {
        VERIFY(!physical_region.range().is_null());
        if (physical_region.type() != PhysicalRegion::Type::Usable)
            continue;
        if (best_candidate.is_null()) {
            best_candidate = physical_region.range();
            continue;
        }
        if (physical_region.range().length > best_candidate.length) {
            best_candidate = physical_region.range();
        }
    }
    if (best_candidate.is_null())
        return {};
    return best_candidate;
}

bool MemoryManagement::initialize(PhysicalAddress multiboot_memory_map_physical_address, size_t entries_count)
{
    VERIFY(!multiboot_memory_map_physical_address.is_null());
    auto max_32_bit_address = PhysicalAddress(0xffffffff);
    if (max_32_bit_address < multiboot_memory_map_physical_address)
        return false;
    VERIFY(!m_was_initialized);
    m_was_initialized = true;
    // FIXME: Try to find a way to not use a reinterpret_cast...
    auto* entry = reinterpret_cast<multiboot_mmap_entry*>(multiboot_memory_map_physical_address.as_ptr());
    for (size_t index = 0; index < entries_count; index++) {
        auto range_base_addr = entry->addr;
        auto length = entry->len;
        auto range_end_addr = range_base_addr + length;
        auto type = (Multiboot::MemoryEntryType)entry->type;
        dbgln("multiboot-e820: range {:016x} - {:016x}, {}", range_base_addr, range_end_addr, Multiboot::parse_entry_type(type));
        m_regions.append(PhysicalRegion::create(PhysicalAddress(range_base_addr), length, type));
        entry++;
    }
    return true;
}

}
