#include <Kernel/VM/RangeAllocator.h>
#include <Kernel/kstdio.h>
#include <AK/QuickSort.h>

//#define VRA_DEBUG

RangeAllocator::RangeAllocator(LinearAddress base, size_t size)
{
    m_available_ranges.append({ base, size });
#ifdef VRA_DEBUG
    dump();
#endif
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
        return { };
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
    for (int i = 0; i < m_available_ranges.size(); ++i) {
        auto& available_range = m_available_ranges[i];
        if (available_range.size() < size)
            continue;
        Range allocated_range(available_range.base(), size);
        if (available_range.size() == size) {
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
    return { };
}

Range RangeAllocator::allocate_specific(LinearAddress base, size_t size)
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
    return { };
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
    quick_sort(m_available_ranges.begin(), m_available_ranges.end(), [] (auto& a, auto& b) {
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
