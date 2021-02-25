/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/BinarySearch.h>
#include <AK/Checked.h>
#include <AK/QuickSort.h>
#include <Kernel/Random.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/RangeAllocator.h>

#define VM_GUARD_PAGES

namespace Kernel {

RangeAllocator::RangeAllocator()
    : m_total_range({}, 0)
{
}

void RangeAllocator::initialize_with_range(VirtualAddress base, size_t size)
{
    m_total_range = { base, size };
    m_available_ranges.append({ base, size });
}

void RangeAllocator::initialize_from_parent(const RangeAllocator& parent_allocator)
{
    ScopedSpinLock lock(parent_allocator.m_lock);
    m_total_range = parent_allocator.m_total_range;
    m_available_ranges = parent_allocator.m_available_ranges;
}

RangeAllocator::~RangeAllocator()
{
}

void RangeAllocator::dump() const
{
    VERIFY(m_lock.is_locked());
    dbgln("RangeAllocator({})", this);
    for (auto& range : m_available_ranges) {
        dbgln("    {:x} -> {:x}", range.base().get(), range.end().get() - 1);
    }
}

void RangeAllocator::carve_at_index(int index, const Range& range)
{
    VERIFY(m_lock.is_locked());
    auto remaining_parts = m_available_ranges[index].carve(range);
    VERIFY(remaining_parts.size() >= 1);
    VERIFY(m_total_range.contains(remaining_parts[0]));
    m_available_ranges[index] = remaining_parts[0];
    if (remaining_parts.size() == 2) {
        VERIFY(m_total_range.contains(remaining_parts[1]));
        m_available_ranges.insert(index + 1, move(remaining_parts[1]));
    }
}

Optional<Range> RangeAllocator::allocate_randomized(size_t size, size_t alignment)
{
    if (!size)
        return {};

    VERIFY((size % PAGE_SIZE) == 0);
    VERIFY((alignment % PAGE_SIZE) == 0);

    // FIXME: I'm sure there's a smarter way to do this.
    static constexpr size_t maximum_randomization_attempts = 1000;
    for (size_t i = 0; i < maximum_randomization_attempts; ++i) {
        VirtualAddress random_address { round_up_to_power_of_two(get_good_random<FlatPtr>(), alignment) };

        if (!m_total_range.contains(random_address, size))
            continue;

        auto range = allocate_specific(random_address, size);
        if (range.has_value())
            return range;
    }

    return allocate_anywhere(size, alignment);
}

Optional<Range> RangeAllocator::allocate_anywhere(size_t size, size_t alignment)
{
    if (!size)
        return {};

    VERIFY((size % PAGE_SIZE) == 0);
    VERIFY((alignment % PAGE_SIZE) == 0);

#ifdef VM_GUARD_PAGES
    // NOTE: We pad VM allocations with a guard page on each side.
    if (Checked<size_t>::addition_would_overflow(size, PAGE_SIZE * 2))
        return {};

    size_t effective_size = size + PAGE_SIZE * 2;
    size_t offset_from_effective_base = PAGE_SIZE;
#else
    size_t effective_size = size;
    size_t offset_from_effective_base = 0;
#endif

    if (Checked<size_t>::addition_would_overflow(effective_size, alignment))
        return {};

    ScopedSpinLock lock(m_lock);
    for (size_t i = 0; i < m_available_ranges.size(); ++i) {
        auto& available_range = m_available_ranges[i];
        // FIXME: This check is probably excluding some valid candidates when using a large alignment.
        if (available_range.size() < (effective_size + alignment))
            continue;

        FlatPtr initial_base = available_range.base().offset(offset_from_effective_base).get();
        FlatPtr aligned_base = round_up_to_power_of_two(initial_base, alignment);

        Range allocated_range(VirtualAddress(aligned_base), size);
        VERIFY(m_total_range.contains(allocated_range));

        if (available_range == allocated_range) {
            m_available_ranges.remove(i);
            return allocated_range;
        }
        carve_at_index(i, allocated_range);
        return allocated_range;
    }
    dmesgln("RangeAllocator: Failed to allocate anywhere: size={}, alignment={}", size, alignment);
    return {};
}

Optional<Range> RangeAllocator::allocate_specific(VirtualAddress base, size_t size)
{
    if (!size)
        return {};

    VERIFY(base.is_page_aligned());
    VERIFY((size % PAGE_SIZE) == 0);

    Range allocated_range(base, size);
    ScopedSpinLock lock(m_lock);
    for (size_t i = 0; i < m_available_ranges.size(); ++i) {
        auto& available_range = m_available_ranges[i];
        VERIFY(m_total_range.contains(allocated_range));
        if (!available_range.contains(base, size))
            continue;
        if (available_range == allocated_range) {
            m_available_ranges.remove(i);
            return allocated_range;
        }
        carve_at_index(i, allocated_range);
        return allocated_range;
    }
    return {};
}

void RangeAllocator::deallocate(const Range& range)
{
    ScopedSpinLock lock(m_lock);
    VERIFY(m_total_range.contains(range));
    VERIFY(range.size());
    VERIFY((range.size() % PAGE_SIZE) == 0);
    VERIFY(range.base() < range.end());
    VERIFY(!m_available_ranges.is_empty());

    size_t nearby_index = 0;
    auto* existing_range = binary_search(
        m_available_ranges.span(),
        range,
        &nearby_index,
        [](auto& a, auto& b) { return a.base().get() - b.end().get(); });

    size_t inserted_index = 0;
    if (existing_range) {
        existing_range->m_size += range.size();
        inserted_index = nearby_index;
    } else {
        m_available_ranges.insert_before_matching(
            Range(range), [&](auto& entry) {
                return entry.base() >= range.end();
            },
            nearby_index, &inserted_index);
    }

    if (inserted_index < (m_available_ranges.size() - 1)) {
        // We already merged with previous. Try to merge with next.
        auto& inserted_range = m_available_ranges[inserted_index];
        auto& next_range = m_available_ranges[inserted_index + 1];
        if (inserted_range.end() == next_range.base()) {
            inserted_range.m_size += next_range.size();
            m_available_ranges.remove(inserted_index + 1);
            return;
        }
    }
}

}
