/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Range.h"
#include <AK/Vector.h>

namespace UserspaceEmulator {

class RangeAllocator {
public:
    RangeAllocator();

    void initialize_with_range(VirtualAddress, size_t);

    Optional<Range> allocate_anywhere(size_t, size_t alignment = PAGE_SIZE);
    Optional<Range> allocate_specific(VirtualAddress, size_t);
    Optional<Range> allocate_randomized(size_t, size_t alignment);
    void deallocate(Range const&);

    void reserve_user_range(VirtualAddress, size_t);

    void dump() const;

    bool contains(Range const& range) const { return m_total_range.contains(range); }

private:
    void carve_at_index(int, Range const&);

    Vector<Range> m_available_ranges;
    Range m_total_range;
};

}
