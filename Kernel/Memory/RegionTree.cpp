/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/RegionTree.h>
#include <Kernel/Random.h>

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

ErrorOr<VirtualRange> RegionTree::try_allocate_anywhere(size_t size, size_t alignment)
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

    dmesgln("VirtualRangeAllocator: Failed to allocate anywhere: size={}, alignment={}", size, alignment);
    return ENOMEM;
}

ErrorOr<VirtualRange> RegionTree::try_allocate_specific(VirtualAddress base, size_t size)
{
    if (!size)
        return EINVAL;

    VERIFY(base.is_page_aligned());
    VERIFY((size % PAGE_SIZE) == 0);

    VirtualRange const range { base, size };
    if (!m_total_range.contains(range))
        return ENOMEM;

    auto* region = m_regions.find_largest_not_above(base.get());
    if (!region) {
        // The range can be accommodated below the current lowest range.
        return range;
    }

    if (region->range().intersects(range)) {
        // Requested range overlaps an existing range.
        return ENOMEM;
    }

    auto it = m_regions.begin_from(region->vaddr().get());
    VERIFY(!it.is_end());
    ++it;

    if (it.is_end()) {
        // The range can be accommodated above the nearest range.
        return range;
    }

    if (it->range().intersects(range)) {
        // Requested range overlaps the next neighbor.
        return ENOMEM;
    }

    // Requested range fits between first region and its next neighbor.
    return range;
}

ErrorOr<VirtualRange> RegionTree::try_allocate_randomized(size_t size, size_t alignment)
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

        auto range_or_error = try_allocate_specific(random_address, size);
        if (!range_or_error.is_error())
            return range_or_error.release_value();
    }

    return try_allocate_anywhere(size, alignment);
}

ErrorOr<NonnullOwnPtr<Region>> RegionTree::allocate_unbacked_anywhere(size_t size, size_t alignment)
{
    auto region = TRY(Region::create_unbacked());
    TRY(place_anywhere(*region, size, alignment));
    return region;
}

ErrorOr<void> RegionTree::place_anywhere(Region& region, size_t size, size_t alignment)
{
    SpinlockLocker locker(m_lock);
    auto range = TRY(try_allocate_anywhere(size, alignment));
    region.m_range = range;
    m_regions.insert(region.vaddr().get(), region);
    return {};
}

ErrorOr<void> RegionTree::place_specifically(Region& region, VirtualRange const& range)
{
    SpinlockLocker locker(m_lock);
    auto allocated_range = TRY(try_allocate_specific(range.base(), range.size()));
    region.m_range = allocated_range;
    m_regions.insert(region.vaddr().get(), region);
    return {};
}

ErrorOr<NonnullOwnPtr<Memory::Region>> RegionTree::create_identity_mapped_region(PhysicalAddress paddr, size_t size)
{
    auto vmobject = TRY(Memory::AnonymousVMObject::try_create_for_physical_range(paddr, size));
    auto region = TRY(Memory::Region::create_unplaced(move(vmobject), 0, {}, Memory::Region::Access::ReadWriteExecute));
    Memory::VirtualRange range { VirtualAddress { (FlatPtr)paddr.get() }, size };
    region->m_range = range;
    TRY(region->map(MM.kernel_page_directory()));
    return region;
}

}
