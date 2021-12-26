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
#include <Kernel/UnixTypes.h>

namespace Kernel::Memory {

class AddressSpace {
public:
    static OwnPtr<AddressSpace> try_create(Process&, AddressSpace const* parent);
    ~AddressSpace();

    PageDirectory& page_directory() { return *m_page_directory; }
    const PageDirectory& page_directory() const { return *m_page_directory; }

    Region* add_region(NonnullOwnPtr<Region>);

    size_t region_count() const { return m_regions.size(); }

    RedBlackTree<FlatPtr, NonnullOwnPtr<Region>>& regions() { return m_regions; }
    const RedBlackTree<FlatPtr, NonnullOwnPtr<Region>>& regions() const { return m_regions; }

    void dump_regions();

    KResult unmap_mmap_range(VirtualAddress, size_t);

    Optional<VirtualRange> allocate_range(VirtualAddress, size_t, size_t alignment = PAGE_SIZE);

    KResultOr<Region*> allocate_region_with_vmobject(VirtualRange const&, NonnullRefPtr<VMObject>, size_t offset_in_vmobject, StringView name, int prot, bool shared);
    KResultOr<Region*> allocate_region(VirtualRange const&, StringView name, int prot = PROT_READ | PROT_WRITE, AllocationStrategy strategy = AllocationStrategy::Reserve);
    void deallocate_region(Region& region);
    NonnullOwnPtr<Region> take_region(Region& region);

    KResultOr<Region*> try_allocate_split_region(Region const& source_region, VirtualRange const&, size_t offset_in_vmobject);
    KResultOr<Vector<Region*, 2>> try_split_region_around_range(Region const& source_region, VirtualRange const&);

    Region* find_region_from_range(VirtualRange const&);
    Region* find_region_containing(VirtualRange const&);

    Vector<Region*> find_regions_intersecting(VirtualRange const&);

    bool enforces_syscall_regions() const { return m_enforces_syscall_regions; }
    void set_enforces_syscall_regions(bool b) { m_enforces_syscall_regions = b; }

    void remove_all_regions(Badge<Process>);

    RecursiveSpinLock& get_lock() const { return m_lock; }

    size_t amount_clean_inode() const;
    size_t amount_dirty_private() const;
    size_t amount_virtual() const;
    size_t amount_resident() const;
    size_t amount_shared() const;
    size_t amount_purgeable_volatile() const;
    size_t amount_purgeable_nonvolatile() const;

private:
    AddressSpace(Process&, NonnullRefPtr<PageDirectory>);

    Process* m_process { nullptr };
    mutable RecursiveSpinLock m_lock;

    RefPtr<PageDirectory> m_page_directory;

    RedBlackTree<FlatPtr, NonnullOwnPtr<Region>> m_regions;

    struct RegionLookupCache {
        Optional<VirtualRange> range;
        WeakPtr<Region> region;
    };
    RegionLookupCache m_region_lookup_cache;

    bool m_enforces_syscall_regions { false };
};

}
