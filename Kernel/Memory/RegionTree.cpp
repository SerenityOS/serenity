/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/RegionTree.h>
#include <Kernel/Security/Random.h>

namespace Kernel::Memory {

RegionTree::~RegionTree()
{
    delete_all_regions_assuming_they_are_unmapped();
}

void RegionTree::delete_all_regions_assuming_they_are_unmapped()
{
    // FIXME: This could definitely be done in a more efficient manner.
    while (!m_regions.is_empty()) {
        auto& region = *m_regions.begin();
        m_regions.remove(region.vaddr().get());
        delete &region;
    }
}

ErrorOr<VirtualRange> RegionTree::allocate_range_anywhere(size_t size, size_t alignment)
{
    if (!size)
        return EINVAL;

    VERIFY((size % PAGE_SIZE) == 0);
    VERIFY((alignment % PAGE_SIZE) == 0);

    if (Checked<size_t>::addition_would_overflow(size, alignment))
        return EOVERFLOW;

    VirtualAddress window_start = m_total_range.base();

    auto allocate_from_window = [&](VirtualRange const& window) -> Optional<VirtualRange> {
        // FIXME: This check is probably excluding some valid candidates when using a large alignment.
        if (window.size() < (size + alignment))
            return {};

        FlatPtr initial_base = window.base().get();
        FlatPtr aligned_base = round_up_to_power_of_two(initial_base, alignment);

        VERIFY(size);

        return VirtualRange { VirtualAddress(aligned_base), size };
    };

    for (auto it = m_regions.begin(); !it.is_end(); ++it) {
        auto& region = *it;

        if (window_start == region.vaddr()) {
            window_start = region.range().end();
            continue;
        }

        VirtualRange window { window_start, region.vaddr().get() - window_start.get() };
        window_start = region.range().end();

        if (auto maybe_range = allocate_from_window(window); maybe_range.has_value())
            return maybe_range.release_value();
    }

    VirtualRange window { window_start, m_total_range.end().get() - window_start.get() };
    if (m_total_range.contains(window)) {
        if (auto maybe_range = allocate_from_window(window); maybe_range.has_value())
            return maybe_range.release_value();
    }

    dmesgln("RegionTree: Failed to allocate anywhere: size={}, alignment={}", size, alignment);
    return ENOMEM;
}

ErrorOr<VirtualRange> RegionTree::allocate_range_specific(VirtualAddress base, size_t size)
{
    if (!size)
        return EINVAL;

    VERIFY(base.is_page_aligned());
    VERIFY((size % PAGE_SIZE) == 0);

    VirtualRange const range { base, size };
    if (!m_total_range.contains(range))
        return ENOMEM;

    auto* region = m_regions.find_largest_not_above(base.offset(size - 1).get());
    if (!region) {
        // The range can be accommodated below the current lowest range.
        return range;
    }

    if (region->range().intersects(range)) {
        // Requested range overlaps an existing range.
        return ENOMEM;
    }

    // Requested range fits between first region and its next neighbor.
    return range;
}

ErrorOr<VirtualRange> RegionTree::allocate_range_randomized(size_t size, size_t alignment)
{
    if (!size)
        return EINVAL;

    VERIFY((size % PAGE_SIZE) == 0);
    VERIFY((alignment % PAGE_SIZE) == 0);

    // FIXME: I'm sure there's a smarter way to do this.
    constexpr size_t maximum_randomization_attempts = 1000;
    for (size_t i = 0; i < maximum_randomization_attempts; ++i) {
        VirtualAddress random_address { round_up_to_power_of_two(get_fast_random<FlatPtr>() % m_total_range.end().get(), alignment) };

        if (!m_total_range.contains(random_address, size))
            continue;

        auto range_or_error = allocate_range_specific(random_address, size);
        if (!range_or_error.is_error())
            return range_or_error.release_value();
    }

    return allocate_range_anywhere(size, alignment);
}

ErrorOr<void> RegionTree::place_anywhere(Region& region, RandomizeVirtualAddress randomize_virtual_address, size_t size, size_t alignment)
{
    auto range = TRY(randomize_virtual_address == RandomizeVirtualAddress::Yes ? allocate_range_randomized(size, alignment) : allocate_range_anywhere(size, alignment));
    region.m_range = range;
    m_regions.insert(region.vaddr().get(), region);
    return {};
}

ErrorOr<void> RegionTree::place_specifically(Region& region, VirtualRange const& range)
{
    auto allocated_range = TRY(allocate_range_specific(range.base(), range.size()));
    region.m_range = allocated_range;
    m_regions.insert(region.vaddr().get(), region);
    return {};
}

bool RegionTree::remove(Region& region)
{
    return m_regions.remove(region.range().base().get());
}

Region* RegionTree::find_region_containing(VirtualAddress address)
{
    auto* region = m_regions.find_largest_not_above(address.get());
    if (!region || !region->contains(address))
        return nullptr;
    return region;
}

Region* RegionTree::find_region_containing(VirtualRange range)
{
    auto* region = m_regions.find_largest_not_above(range.base().get());
    if (!region || !region->contains(range))
        return nullptr;
    return region;
}

}
