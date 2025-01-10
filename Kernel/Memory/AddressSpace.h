/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RedBlackTree.h>
#include <AK/SetOnce.h>
#include <AK/Vector.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Library/LockWeakPtr.h>
#include <Kernel/Locking/SpinlockProtected.h>
#include <Kernel/Memory/AllocationStrategy.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/RegionTree.h>
#include <Kernel/UnixTypes.h>

namespace Kernel::Memory {

class AddressSpace {
public:
    static ErrorOr<NonnullOwnPtr<AddressSpace>> try_create(Process&, AddressSpace const* parent);
    ~AddressSpace();

    PageDirectory& page_directory() { return *m_page_directory; }
    PageDirectory const& page_directory() const { return *m_page_directory; }

    RegionTree& region_tree() { return m_region_tree; }
    RegionTree const& region_tree() const { return m_region_tree; }

    void dump_regions();

    ErrorOr<void> unmap_mmap_range(VirtualAddress, size_t);

    ErrorOr<Region*> allocate_region_with_vmobject(VirtualRange requested_range, NonnullLockRefPtr<VMObject>, size_t offset_in_vmobject, StringView name, int prot, bool shared, MemoryType = MemoryType::Normal);
    ErrorOr<Region*> allocate_region_with_vmobject(RandomizeVirtualAddress, VirtualAddress requested_address, size_t requested_size, size_t requested_alignment, NonnullLockRefPtr<VMObject>, size_t offset_in_vmobject, StringView name, int prot, bool shared, MemoryType = MemoryType::Normal);
    ErrorOr<Region*> allocate_region(RandomizeVirtualAddress, VirtualAddress requested_address, size_t requested_size, size_t requested_alignment, StringView name, int prot = PROT_READ | PROT_WRITE, AllocationStrategy strategy = AllocationStrategy::Reserve);
    void deallocate_region(Region& region);
    NonnullOwnPtr<Region> take_region(Region& region);

    ErrorOr<Region*> try_allocate_split_region(Region const& source_region, VirtualRange const&, size_t offset_in_vmobject);
    ErrorOr<Vector<Region*, 2>> try_split_region_around_range(Region const& source_region, VirtualRange const&);

    Region* find_region_from_range(VirtualRange const&);
    Region* find_region_containing(VirtualRange const&);

    ErrorOr<Vector<Region*, 4>> find_regions_intersecting(VirtualRange const&);

    bool enforces_syscall_regions() const { return m_enforces_syscall_regions.was_set(); }
    void set_enforces_syscall_regions() { m_enforces_syscall_regions.set(); }

    void remove_all_regions(Badge<Process>);

    ErrorOr<size_t> amount_clean_inode() const;
    size_t amount_dirty_private() const;
    size_t amount_virtual() const;
    size_t amount_resident() const;
    size_t amount_shared() const;
    size_t amount_purgeable_volatile() const;
    size_t amount_purgeable_nonvolatile() const;

private:
    AddressSpace(NonnullLockRefPtr<PageDirectory>, VirtualRange total_range);

    LockRefPtr<PageDirectory> m_page_directory;

    RegionTree m_region_tree;

    SetOnce m_enforces_syscall_regions;
};

}
