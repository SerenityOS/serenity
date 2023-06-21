/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/IntrusiveRedBlackTree.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/VirtualAddress.h>
#include <Kernel/Memory/VirtualRange.h>

namespace Kernel::Memory {

enum class RandomizeVirtualAddress {
    No,
    Yes,
};

// RegionTree represents a virtual address space.
// It is used by MemoryManager for kernel VM and by AddressSpace for user VM.
// Regions are stored in an intrusive data structure and there are no allocations when interacting with it.
class RegionTree {
    AK_MAKE_NONCOPYABLE(RegionTree);
    AK_MAKE_NONMOVABLE(RegionTree);

public:
    explicit RegionTree(VirtualRange total_range)
        : m_total_range(total_range)
    {
    }

    ~RegionTree();

    auto& regions() { return m_regions; }
    auto const& regions() const { return m_regions; }

    VirtualRange total_range() const { return m_total_range; }

    ErrorOr<void> place_anywhere(Region&, RandomizeVirtualAddress, size_t size, size_t alignment = PAGE_SIZE);
    ErrorOr<void> place_specifically(Region&, VirtualRange const&);

    void delete_all_regions_assuming_they_are_unmapped();

    bool remove(Region&);

    Region* find_region_containing(VirtualAddress);
    Region* find_region_containing(VirtualRange);

private:
    ErrorOr<VirtualRange> allocate_range_anywhere(size_t size, size_t alignment = PAGE_SIZE);
    ErrorOr<VirtualRange> allocate_range_specific(VirtualAddress base, size_t size);
    ErrorOr<VirtualRange> allocate_range_randomized(size_t size, size_t alignment = PAGE_SIZE);

    IntrusiveRedBlackTree<&Region::m_tree_node> m_regions;
    VirtualRange const m_total_range;
};

}
