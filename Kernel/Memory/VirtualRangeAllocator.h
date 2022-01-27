/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RedBlackTree.h>
#include <AK/Traits.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/VirtualRange.h>

namespace Kernel::Memory {

class VirtualRangeAllocator {
public:
    VirtualRangeAllocator();
    ~VirtualRangeAllocator() = default;

    ErrorOr<void> initialize_with_range(VirtualAddress, size_t);
    ErrorOr<void> initialize_from_parent(VirtualRangeAllocator const&);

    ErrorOr<VirtualRange> try_allocate_anywhere(size_t, size_t alignment = PAGE_SIZE);
    ErrorOr<VirtualRange> try_allocate_specific(VirtualAddress, size_t);
    ErrorOr<VirtualRange> try_allocate_randomized(size_t, size_t alignment);
    void deallocate(VirtualRange const&);

    void dump() const;

    bool contains(VirtualRange const& range) const { return m_total_range.contains(range); }

private:
    ErrorOr<void> carve_from_region(VirtualRange const& from, VirtualRange const&);

    RedBlackTree<FlatPtr, VirtualRange> m_available_ranges;
    VirtualRange m_total_range;
    mutable Spinlock m_lock;
};

}

namespace AK {
template<>
struct Traits<Kernel::Memory::VirtualRange> : public GenericTraits<Kernel::Memory::VirtualRange> {
    static constexpr bool is_trivial() { return true; }
};
}
