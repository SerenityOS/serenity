/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RedBlackTree.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <Kernel/Memory/AllocationStrategy.h>
#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/UnixTypes.h>

namespace Kernel::Memory {

class AddressSpace {
public:
    static ErrorOr<NonnullOwnPtr<AddressSpace>> try_create(AddressSpace const* parent);
    ~AddressSpace();

    PageDirectory& page_directory() { return *m_page_directory; }
    PageDirectory const& page_directory() const { return *m_page_directory; }

    ErrorOr<Region*> add_region(NonnullOwnPtr<Region>);

    size_t region_count() const { return m_regions.size(); }

    auto& regions() { return m_regions; }
    auto const& regions() const { return m_regions; }

    void dump_regions();

    ErrorOr<void> unmap_mmap_range(VirtualAddress, size_t);

    ErrorOr<VirtualRange> try_allocate_range(VirtualAddress, size_t, size_t alignment = PAGE_SIZE);

    ErrorOr<Region*> allocate_region_with_vmobject(VirtualRange const&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, StringView name, int prot, bool shared);
    ErrorOr<Region*> allocate_region(VirtualRange const&, StringView name, int prot = PROT_READ | PROT_WRITE, AllocationStrategy strategy = AllocationStrategy::Reserve);
    void deallocate_region(Region& region);
    NonnullOwnPtr<Region> take_region(Region& region);

    ErrorOr<Region*> try_allocate_split_region(Region const& source_region, VirtualRange const&, size_t offset_in_vmobject);
    ErrorOr<Vector<Region*, 2>> try_split_region_around_range(Region const& source_region, VirtualRange const&);

    Region* find_region_from_range(VirtualRange const&);
    Region* find_region_containing(VirtualRange const&);

    ErrorOr<Vector<Region*>> find_regions_intersecting(VirtualRange const&);

    bool enforces_syscall_regions() const { return m_enforces_syscall_regions; }
    void set_enforces_syscall_regions(bool b) { m_enforces_syscall_regions = b; }

    void remove_all_regions(Badge<Process>);

    RecursiveSpinlock& get_lock() const { return m_lock; }

    ErrorOr<size_t> amount_clean_inode() const;
    size_t amount_dirty_private() const;
    size_t amount_virtual() const;
    size_t amount_resident() const;
    size_t amount_shared() const;
    size_t amount_purgeable_volatile() const;
    size_t amount_purgeable_nonvolatile() const;

    ErrorOr<VirtualRange> try_allocate_anywhere(size_t size, size_t alignment);
    ErrorOr<VirtualRange> try_allocate_specific(VirtualAddress base, size_t size);
    ErrorOr<VirtualRange> try_allocate_randomized(size_t size, size_t alignment);

private:
    AddressSpace(NonnullRefPtr<PageDirectory>, VirtualRange total_range);

    void delete_all_regions_assuming_they_are_unmapped();

    mutable RecursiveSpinlock m_lock;

    RefPtr<PageDirectory> m_page_directory;

    IntrusiveRedBlackTree<&Region::m_tree_node> m_regions;
    VirtualRange const m_total_range;

    bool m_enforces_syscall_regions { false };
};

}
