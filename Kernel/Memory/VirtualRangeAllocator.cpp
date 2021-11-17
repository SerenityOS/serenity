/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <Kernel/Memory/VirtualRangeAllocator.h>
#include <Kernel/Random.h>

#define VM_GUARD_PAGES

namespace Kernel::Memory {

VirtualRangeAllocator::VirtualRangeAllocator()
    : m_total_range({}, 0)
{
}

ErrorOr<void> VirtualRangeAllocator::initialize_with_range(VirtualAddress base, size_t size)
{
    m_total_range = { base, size };
    TRY(m_available_ranges.try_insert(base.get(), VirtualRange { base, size }));
    return {};
}

ErrorOr<void> VirtualRangeAllocator::initialize_from_parent(VirtualRangeAllocator const& parent_allocator)
{
    SpinlockLocker lock(parent_allocator.m_lock);
    m_total_range = parent_allocator.m_total_range;
    m_available_ranges.clear();
    for (auto it = parent_allocator.m_available_ranges.begin(); !it.is_end(); ++it) {
        TRY(m_available_ranges.try_insert(it.key(), VirtualRange(*it)));
    }
    return {};
}

void VirtualRangeAllocator::dump() const
{
    VERIFY(m_lock.is_locked());
    dbgln("VirtualRangeAllocator({})", this);
    for (auto& range : m_available_ranges) {
        dbgln("    {:x} -> {:x}", range.base().get(), range.end().get() - 1);
    }
}

void VirtualRangeAllocator::carve_from_region(VirtualRange const& from, VirtualRange const& range)
{
    VERIFY(m_lock.is_locked());
    auto remaining_parts = from.carve(range);
    VERIFY(remaining_parts.size() >= 1);
    VERIFY(m_total_range.contains(remaining_parts[0]));
    m_available_ranges.remove(from.base().get());
    m_available_ranges.insert(remaining_parts[0].base().get(), remaining_parts[0]);
    if (remaining_parts.size() == 2) {
        VERIFY(m_total_range.contains(remaining_parts[1]));
        m_available_ranges.insert(remaining_parts[1].base().get(), remaining_parts[1]);
    }
}

ErrorOr<VirtualRange> VirtualRangeAllocator::try_allocate_randomized(size_t size, size_t alignment)
{
    if (!size)
        return EINVAL;

    VERIFY((size % PAGE_SIZE) == 0);
    VERIFY((alignment % PAGE_SIZE) == 0);

    // FIXME: I'm sure there's a smarter way to do this.
    static constexpr size_t maximum_randomization_attempts = 1000;
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

ErrorOr<VirtualRange> VirtualRangeAllocator::try_allocate_anywhere(size_t size, size_t alignment)
{
    if (!size)
        return EINVAL;

    VERIFY((size % PAGE_SIZE) == 0);
    VERIFY((alignment % PAGE_SIZE) == 0);

#ifdef VM_GUARD_PAGES
    // NOTE: We pad VM allocations with a guard page on each side.
    if (Checked<size_t>::addition_would_overflow(size, PAGE_SIZE * 2))
        return EOVERFLOW;

    size_t effective_size = size + PAGE_SIZE * 2;
    size_t offset_from_effective_base = PAGE_SIZE;
#else
    size_t effective_size = size;
    size_t offset_from_effective_base = 0;
#endif

    if (Checked<size_t>::addition_would_overflow(effective_size, alignment))
        return EOVERFLOW;

    SpinlockLocker lock(m_lock);

    for (auto it = m_available_ranges.begin(); !it.is_end(); ++it) {
        auto& available_range = *it;
        // FIXME: This check is probably excluding some valid candidates when using a large alignment.
        if (available_range.size() < (effective_size + alignment))
            continue;

        FlatPtr initial_base = available_range.base().offset(offset_from_effective_base).get();
        FlatPtr aligned_base = round_up_to_power_of_two(initial_base, alignment);

        VirtualRange const allocated_range(VirtualAddress(aligned_base), size);

        VERIFY(m_total_range.contains(allocated_range));

        if (available_range == allocated_range) {
            m_available_ranges.remove(it.key());
            return allocated_range;
        }
        carve_from_region(*it, allocated_range);
        return allocated_range;
    }
    dmesgln("VirtualRangeAllocator: Failed to allocate anywhere: size={}, alignment={}", size, alignment);
    return ENOMEM;
}

ErrorOr<VirtualRange> VirtualRangeAllocator::try_allocate_specific(VirtualAddress base, size_t size)
{
    if (!size)
        return EINVAL;

    VERIFY(base.is_page_aligned());
    VERIFY((size % PAGE_SIZE) == 0);

    VirtualRange const allocated_range(base, size);
    if (!m_total_range.contains(allocated_range))
        return ENOMEM;

    SpinlockLocker lock(m_lock);
    auto available_range = m_available_ranges.find_largest_not_above(base.get());
    if (!available_range)
        return ENOMEM;
    if (!available_range->contains(allocated_range))
        return ENOMEM;
    if (*available_range == allocated_range) {
        m_available_ranges.remove(available_range->base().get());
        return allocated_range;
    }
    carve_from_region(*available_range, allocated_range);
    return allocated_range;
}

void VirtualRangeAllocator::deallocate(VirtualRange const& range)
{
    SpinlockLocker lock(m_lock);
    VERIFY(m_total_range.contains(range));
    VERIFY(range.size());
    VERIFY((range.size() % PAGE_SIZE) == 0);
    VERIFY(range.base() < range.end());
    VERIFY(!m_available_ranges.is_empty());

    VirtualRange merged_range = range;

    {
        // Try merging with preceding range.
        auto* preceding_range = m_available_ranges.find_largest_not_above(range.base().get());
        if (preceding_range && preceding_range->end() == range.base()) {
            preceding_range->m_size += range.size();
            merged_range = *preceding_range;
        } else {
            m_available_ranges.insert(range.base().get(), range);
        }
    }

    {
        // Try merging with following range.
        auto* following_range = m_available_ranges.find_largest_not_above(range.end().get());
        if (following_range && merged_range.end() == following_range->base()) {
            auto* existing_range = m_available_ranges.find_largest_not_above(range.base().get());
            VERIFY(existing_range->base() == merged_range.base());
            existing_range->m_size += following_range->size();
            m_available_ranges.remove(following_range->base().get());
        }
    }
}

}
