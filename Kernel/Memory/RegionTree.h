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
#include <Kernel/Memory/VirtualRange.h>
#include <Kernel/VirtualAddress.h>

namespace Kernel::Memory {

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

    ErrorOr<NonnullOwnPtr<Region>> allocate_unbacked_anywhere(size_t size, size_t alignment = PAGE_SIZE);

    ErrorOr<void> place_anywhere(Region&, size_t size, size_t alignment = PAGE_SIZE);
    ErrorOr<void> place_specifically(Region&, VirtualRange const&);

    ErrorOr<NonnullOwnPtr<Memory::Region>> create_identity_mapped_region(PhysicalAddress, size_t);

    void delete_all_regions_assuming_they_are_unmapped();

private:
    ErrorOr<VirtualRange> try_allocate_anywhere(size_t size, size_t alignment = PAGE_SIZE);
    ErrorOr<VirtualRange> try_allocate_specific(VirtualAddress base, size_t size);
    ErrorOr<VirtualRange> try_allocate_randomized(size_t size, size_t alignment = PAGE_SIZE);

    Spinlock m_lock;

    IntrusiveRedBlackTree<&Region::m_tree_node> m_regions;
    VirtualRange const m_total_range;
};

}
