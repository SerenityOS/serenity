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

    void initialize_with_range(VirtualAddress, size_t);
    void initialize_from_parent(VirtualRangeAllocator const&);

    Optional<VirtualRange> allocate_anywhere(size_t, size_t alignment = PAGE_SIZE);
    Optional<VirtualRange> allocate_specific(VirtualAddress, size_t);
    Optional<VirtualRange> allocate_randomized(size_t, size_t alignment);
    void deallocate(VirtualRange const&);

    void dump() const;

    bool contains(VirtualRange const& range) const { return m_total_range.contains(range); }

private:
    void carve_at_iterator(auto&, VirtualRange const&);

    RedBlackTree<FlatPtr, VirtualRange> m_available_ranges;
    VirtualRange m_total_range;
    mutable Spinlock<u8> m_lock;
};

}

namespace AK {
template<>
struct Traits<Kernel::Memory::VirtualRange> : public GenericTraits<Kernel::Memory::VirtualRange> {
    static constexpr bool is_trivial() { return true; }
};
}
