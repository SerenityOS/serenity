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
#include <Kernel/Memory/RegionTree.h>
#include <Kernel/UnixTypes.h>

namespace Kernel::Memory {

class AddressSpace {
public:
    static ErrorOr<NonnullOwnPtr<AddressSpace>> try_create(AddressSpace const* parent);
    ~AddressSpace();

    PageDirectory& page_directory() { return *m_page_directory; }
    PageDirectory const& page_directory() const { return *m_page_directory; }

    auto& regions() { return m_region_tree.regions(); }
    auto const& regions() const { return m_region_tree.regions(); }

    void dump_regions();

    ErrorOr<void> unmap_mmap_range(VirtualAddress, size_t);

    ErrorOr<Region*> allocate_region_with_vmobject(VirtualRange requested_range, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, StringView name, int prot, bool shared);
    ErrorOr<Region*> allocate_region_with_vmobject(RandomizeVirtualAddress, VirtualAddress requested_address, size_t requested_size, size_t requested_alignment, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, StringView name, int prot, bool shared);
    ErrorOr<Region*> allocate_region(RandomizeVirtualAddress, VirtualAddress requested_address, size_t requested_size, size_t requested_alignment, StringView name, int prot = PROT_READ | PROT_WRITE, AllocationStrategy strategy = AllocationStrategy::Reserve);
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

    auto& region_tree() { return m_region_tree; }

private:
    AddressSpace(NonnullRefPtr<PageDirectory>, VirtualRange total_range);

    mutable RecursiveSpinlock m_lock;

    RefPtr<PageDirectory> m_page_directory;

    RegionTree m_region_tree;

    bool m_enforces_syscall_regions { false };
};

}
