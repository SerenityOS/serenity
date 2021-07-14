/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Traits.h>
#include <AK/Vector.h>
#include <Kernel/SpinLock.h>
#include <Kernel/VM/Range.h>

namespace Kernel {

class RangeAllocator {
public:
    RangeAllocator();
    ~RangeAllocator();

    void initialize_with_range(VirtualAddress, size_t);
    void initialize_from_parent(RangeAllocator const&);

    Optional<Range> allocate_anywhere(size_t, size_t alignment = PAGE_SIZE);
    Optional<Range> allocate_specific(VirtualAddress, size_t);
    Optional<Range> allocate_randomized(size_t, size_t alignment);
    void deallocate(Range const&);

    void dump() const;

    bool contains(Range const& range) const { return m_total_range.contains(range); }

private:
    void carve_at_index(int, Range const&);

    Vector<Range> m_available_ranges;
    Range m_total_range;
    mutable SpinLock<u8> m_lock;
};

}

namespace AK {
template<>
struct Traits<Kernel::Range> : public GenericTraits<Kernel::Range> {
    static constexpr bool is_trivial() { return true; }
};
}
