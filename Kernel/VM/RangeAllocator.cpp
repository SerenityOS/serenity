/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/QuickSort.h>
#include <Kernel/Random.h>
#include <Kernel/VM/RangeAllocator.h>
#include <Kernel/kstdio.h>

//#define VRA_DEBUG
#define VM_GUARD_PAGES

RangeAllocator::RangeAllocator()
{
}

void RangeAllocator::initialize_with_range(VirtualAddress base, size_t size)
{
    m_available_ranges.append({ base, size });
#ifdef VRA_DEBUG
    dump();
#endif
}

void RangeAllocator::initialize_from_parent(const RangeAllocator& parent_allocator)
{
    m_available_ranges = parent_allocator.m_available_ranges;
}

RangeAllocator::~RangeAllocator()
{
}

void RangeAllocator::dump() const
{
    dbgprintf("RangeAllocator{%p}\n", this);
    for (auto& range : m_available_ranges) {
        dbgprintf("    %x -> %x\n", range.base().get(), range.end().get() - 1);
    }
}

Vector<Range, 2> Range::carve(const Range& taken)
{
    Vector<Range, 2> parts;
    if (taken == *this)
        return {};
    if (taken.base() > base())
        parts.append({ base(), taken.base().get() - base().get() });
    if (taken.end() < end())
        parts.append({ taken.end(), end().get() - taken.end().get() });
#ifdef VRA_DEBUG
    dbgprintf("VRA: carve: take %x-%x from %x-%x\n",
        taken.base().get(), taken.end().get() - 1,
        base().get(), end().get() - 1);
    for (int i = 0; i < parts.size(); ++i)
        dbgprintf("        %x-%x\n", parts[i].base().get(), parts[i].end().get() - 1);
#endif
    return parts;
}

void RangeAllocator::carve_at_index(int index, const Range& range)
{
    auto remaining_parts = m_available_ranges[index].carve(range);
    ASSERT(remaining_parts.size() >= 1);
    m_available_ranges[index] = remaining_parts[0];
    if (remaining_parts.size() == 2)
        m_available_ranges.insert(index + 1, move(remaining_parts[1]));
}

Range RangeAllocator::allocate_anywhere(size_t size)
{
#ifdef VM_GUARD_PAGES
    // NOTE: We pad VM allocations with a guard page on each side.
    size_t effective_size = size + PAGE_SIZE * 2;
    size_t offset_from_effective_base = PAGE_SIZE;
#else
    size_t effective_size = size;
    size_t offset_from_effective_base = 0;
#endif
    for (int i = 0; i < m_available_ranges.size(); ++i) {
        auto& available_range = m_available_ranges[i];
        if (available_range.size() < effective_size)
            continue;
        Range allocated_range(available_range.base().offset(offset_from_effective_base), size);
        if (available_range.size() == effective_size) {
#ifdef VRA_DEBUG
            dbgprintf("VRA: Allocated perfect-fit anywhere(%u): %x\n", size, allocated_range.base().get());
#endif
            m_available_ranges.remove(i);
            return allocated_range;
        }
        carve_at_index(i, allocated_range);
#ifdef VRA_DEBUG
        dbgprintf("VRA: Allocated anywhere(%u): %x\n", size, allocated_range.base().get());
        dump();
#endif
        return allocated_range;
    }
    kprintf("VRA: Failed to allocate anywhere: %u\n", size);
    return {};
}

Range RangeAllocator::allocate_specific(VirtualAddress base, size_t size)
{
    Range allocated_range(base, size);
    for (int i = 0; i < m_available_ranges.size(); ++i) {
        auto& available_range = m_available_ranges[i];
        if (!available_range.contains(base, size))
            continue;
        if (available_range == allocated_range) {
            m_available_ranges.remove(i);
            return allocated_range;
        }
        carve_at_index(i, allocated_range);
#ifdef VRA_DEBUG
        dbgprintf("VRA: Allocated specific(%u): %x\n", size, available_range.base().get());
        dump();
#endif
        return allocated_range;
    }
    kprintf("VRA: Failed to allocate specific range: %x(%u)\n", base.get(), size);
    return {};
}

void RangeAllocator::deallocate(Range range)
{
#ifdef VRA_DEBUG
    dbgprintf("VRA: Deallocate: %x(%u)\n", range.base().get(), range.size());
    dump();
#endif

    for (auto& available_range : m_available_ranges) {
        if (available_range.end() == range.base()) {
            available_range.m_size += range.size();
            goto sort_and_merge;
        }
    }
    m_available_ranges.append(range);

sort_and_merge:
    // FIXME: We don't have to sort if we insert at the right position immediately.
    quick_sort(m_available_ranges.begin(), m_available_ranges.end(), [](auto& a, auto& b) {
        return a.base() < b.base();
    });

    Vector<Range> merged_ranges;
    merged_ranges.ensure_capacity(m_available_ranges.size());

    for (auto& range : m_available_ranges) {
        if (merged_ranges.is_empty()) {
            merged_ranges.append(range);
            continue;
        }
        if (range.base() == merged_ranges.last().end()) {
            merged_ranges.last().m_size += range.size();
            continue;
        }
        merged_ranges.append(range);
    }

    m_available_ranges = move(merged_ranges);

#ifdef VRA_DEBUG
    dbgprintf("VRA: After deallocate\n");
    dump();
#endif
}
