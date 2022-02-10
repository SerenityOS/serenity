/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AddressSpace.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/InodeVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/Scheduler.h>

namespace Kernel::Memory {

ErrorOr<NonnullOwnPtr<AddressSpace>> AddressSpace::try_create(AddressSpace const* parent)
{
    auto page_directory = TRY(PageDirectory::try_create_for_userspace(parent ? &parent->page_directory().range_allocator() : nullptr));
    auto space = TRY(adopt_nonnull_own_or_enomem(new (nothrow) AddressSpace(page_directory)));
    space->page_directory().set_space({}, *space);
    return space;
}

AddressSpace::AddressSpace(NonnullRefPtr<PageDirectory> page_directory)
    : m_page_directory(move(page_directory))
{
}

AddressSpace::~AddressSpace()
{
}

ErrorOr<void> AddressSpace::unmap_mmap_range(VirtualAddress addr, size_t size)
{
    if (!size)
        return EINVAL;

    auto range_to_unmap = TRY(VirtualRange::expand_to_page_boundaries(addr.get(), size));

    if (!is_user_range(range_to_unmap))
        return EFAULT;

    if (auto* whole_region = find_region_from_range(range_to_unmap)) {
        if (!whole_region->is_mmap())
            return EPERM;

        PerformanceManager::add_unmap_perf_event(Process::current(), whole_region->range());

        deallocate_region(*whole_region);
        return {};
    }

    if (auto* old_region = find_region_containing(range_to_unmap)) {
        if (!old_region->is_mmap())
            return EPERM;

        // Remove the old region from our regions tree, since were going to add another region
        // with the exact same start address, but don't deallocate it yet.
        auto region = take_region(*old_region);

        // We manually unmap the old region here, specifying that we *don't* want the VM deallocated.
        region->unmap(Region::ShouldDeallocateVirtualRange::No);

        auto new_regions = TRY(try_split_region_around_range(*region, range_to_unmap));

        // Instead we give back the unwanted VM manually.
        page_directory().range_allocator().deallocate(range_to_unmap);

        // And finally we map the new region(s) using our page directory (they were just allocated and don't have one).
        for (auto* new_region : new_regions) {
            // TODO: Ideally we should do this in a way that can be rolled back on failure, as failing here
            // leaves the caller in an undefined state.
            TRY(new_region->map(page_directory()));
        }

        PerformanceManager::add_unmap_perf_event(Process::current(), range_to_unmap);

        return {};
    }

    // Try again while checking multiple regions at a time.
    auto const& regions = TRY(find_regions_intersecting(range_to_unmap));
    if (regions.is_empty())
        return {};

    // Check if any of the regions is not mmap'ed, to not accidentally
    // error out with just half a region map left.
    for (auto* region : regions) {
        if (!region->is_mmap())
            return EPERM;
    }

    Vector<Region*, 2> new_regions;

    for (auto* old_region : regions) {
        // If it's a full match we can remove the entire old region.
        if (old_region->range().intersect(range_to_unmap).size() == old_region->size()) {
            deallocate_region(*old_region);
            continue;
        }

        // Remove the old region from our regions tree, since were going to add another region
        // with the exact same start address, but don't deallocate it yet.
        auto region = take_region(*old_region);

        // We manually unmap the old region here, specifying that we *don't* want the VM deallocated.
        region->unmap(Region::ShouldDeallocateVirtualRange::No);

        // Otherwise, split the regions and collect them for future mapping.
        auto split_regions = TRY(try_split_region_around_range(*region, range_to_unmap));
        TRY(new_regions.try_extend(split_regions));
    }

    // Give back any unwanted VM to the range allocator.
    page_directory().range_allocator().deallocate(range_to_unmap);

    // And finally map the new region(s) into our page directory.
    for (auto* new_region : new_regions) {
        // TODO: Ideally we should do this in a way that can be rolled back on failure, as failing here
        // leaves the caller in an undefined state.
        TRY(new_region->map(page_directory()));
    }

    PerformanceManager::add_unmap_perf_event(Process::current(), range_to_unmap);

    return {};
}

ErrorOr<VirtualRange> AddressSpace::try_allocate_range(VirtualAddress vaddr, size_t size, size_t alignment)
{
    vaddr.mask(PAGE_MASK);
    size = TRY(page_round_up(size));
    if (vaddr.is_null())
        return page_directory().range_allocator().try_allocate_anywhere(size, alignment);
    return page_directory().range_allocator().try_allocate_specific(vaddr, size);
}

ErrorOr<Region*> AddressSpace::try_allocate_split_region(Region const& source_region, VirtualRange const& range, size_t offset_in_vmobject)
{
    OwnPtr<KString> region_name;
    if (!source_region.name().is_null())
        region_name = TRY(KString::try_create(source_region.name()));

    auto new_region = TRY(Region::try_create_user_accessible(
        range, source_region.vmobject(), offset_in_vmobject, move(region_name), source_region.access(), source_region.is_cacheable() ? Region::Cacheable::Yes : Region::Cacheable::No, source_region.is_shared()));
    new_region->set_syscall_region(source_region.is_syscall_region());
    new_region->set_mmap(source_region.is_mmap());
    new_region->set_stack(source_region.is_stack());
    size_t page_offset_in_source_region = (offset_in_vmobject - source_region.offset_in_vmobject()) / PAGE_SIZE;
    for (size_t i = 0; i < new_region->page_count(); ++i) {
        if (source_region.should_cow(page_offset_in_source_region + i))
            TRY(new_region->set_should_cow(i, true));
    }
    return add_region(move(new_region));
}

ErrorOr<Region*> AddressSpace::allocate_region(VirtualRange const& range, StringView name, int prot, AllocationStrategy strategy)
{
    VERIFY(range.is_valid());
    OwnPtr<KString> region_name;
    if (!name.is_null())
        region_name = TRY(KString::try_create(name));
    auto vmobject = TRY(AnonymousVMObject::try_create_with_size(range.size(), strategy));
    auto region = TRY(Region::try_create_user_accessible(range, move(vmobject), 0, move(region_name), prot_to_region_access_flags(prot), Region::Cacheable::Yes, false));
    TRY(region->map(page_directory(), ShouldFlushTLB::No));
    return add_region(move(region));
}

ErrorOr<Region*> AddressSpace::allocate_region_with_vmobject(VirtualRange const& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, StringView name, int prot, bool shared)
{
    VERIFY(range.is_valid());
    size_t end_in_vmobject = offset_in_vmobject + range.size();
    if (end_in_vmobject <= offset_in_vmobject) {
        dbgln("allocate_region_with_vmobject: Overflow (offset + size)");
        return EINVAL;
    }
    if (offset_in_vmobject >= vmobject->size()) {
        dbgln("allocate_region_with_vmobject: Attempt to allocate a region with an offset past the end of its VMObject.");
        return EINVAL;
    }
    if (end_in_vmobject > vmobject->size()) {
        dbgln("allocate_region_with_vmobject: Attempt to allocate a region with an end past the end of its VMObject.");
        return EINVAL;
    }
    offset_in_vmobject &= PAGE_MASK;
    OwnPtr<KString> region_name;
    if (!name.is_null())
        region_name = TRY(KString::try_create(name));
    auto region = TRY(Region::try_create_user_accessible(range, move(vmobject), offset_in_vmobject, move(region_name), prot_to_region_access_flags(prot), Region::Cacheable::Yes, shared));
    if (prot == PROT_NONE) {
        // For PROT_NONE mappings, we don't have to set up any page table mappings.
        // We do still need to attach the region to the page_directory though.
        SpinlockLocker mm_locker(s_mm_lock);
        region->set_page_directory(page_directory());
    } else {
        TRY(region->map(page_directory(), ShouldFlushTLB::No));
    }
    return add_region(move(region));
}

void AddressSpace::deallocate_region(Region& region)
{
    (void)take_region(region);
}

NonnullOwnPtr<Region> AddressSpace::take_region(Region& region)
{
    SpinlockLocker lock(m_lock);
    auto found_region = m_regions.unsafe_remove(region.vaddr().get());
    VERIFY(found_region.ptr() == &region);
    return found_region;
}

Region* AddressSpace::find_region_from_range(VirtualRange const& range)
{
    SpinlockLocker lock(m_lock);
    auto* found_region = m_regions.find(range.base().get());
    if (!found_region)
        return nullptr;
    auto& region = *found_region;
    auto rounded_range_size = page_round_up(range.size());
    if (rounded_range_size.is_error() || region->size() != rounded_range_size.value())
        return nullptr;
    return region;
}

Region* AddressSpace::find_region_containing(VirtualRange const& range)
{
    SpinlockLocker lock(m_lock);
    auto* candidate = m_regions.find_largest_not_above(range.base().get());
    if (!candidate)
        return nullptr;
    return (*candidate)->range().contains(range) ? candidate->ptr() : nullptr;
}

ErrorOr<Vector<Region*>> AddressSpace::find_regions_intersecting(VirtualRange const& range)
{
    Vector<Region*> regions = {};
    size_t total_size_collected = 0;

    SpinlockLocker lock(m_lock);

    auto* found_region = m_regions.find_largest_not_above(range.base().get());
    if (!found_region)
        return regions;
    for (auto iter = m_regions.begin_from((*found_region)->vaddr().get()); !iter.is_end(); ++iter) {
        const auto& iter_range = (*iter)->range();
        if (iter_range.base() < range.end() && iter_range.end() > range.base()) {
            TRY(regions.try_append(*iter));

            total_size_collected += (*iter)->size() - iter_range.intersect(range).size();
            if (total_size_collected == range.size())
                break;
        }
    }

    return regions;
}

ErrorOr<Region*> AddressSpace::add_region(NonnullOwnPtr<Region> region)
{
    auto* ptr = region.ptr();
    SpinlockLocker lock(m_lock);
    TRY(m_regions.try_insert(region->vaddr().get(), move(region)));
    return ptr;
}

// Carve out a virtual address range from a region and return the two regions on either side
ErrorOr<Vector<Region*, 2>> AddressSpace::try_split_region_around_range(const Region& source_region, VirtualRange const& desired_range)
{
    VirtualRange old_region_range = source_region.range();
    auto remaining_ranges_after_unmap = old_region_range.carve(desired_range);

    VERIFY(!remaining_ranges_after_unmap.is_empty());
    auto try_make_replacement_region = [&](VirtualRange const& new_range) -> ErrorOr<Region*> {
        VERIFY(old_region_range.contains(new_range));
        size_t new_range_offset_in_vmobject = source_region.offset_in_vmobject() + (new_range.base().get() - old_region_range.base().get());
        return try_allocate_split_region(source_region, new_range, new_range_offset_in_vmobject);
    };
    Vector<Region*, 2> new_regions;
    for (auto& new_range : remaining_ranges_after_unmap) {
        auto* new_region = TRY(try_make_replacement_region(new_range));
        new_regions.unchecked_append(new_region);
    }
    return new_regions;
}

void AddressSpace::dump_regions()
{
    dbgln("Process regions:");
#if ARCH(I386)
    char const* addr_padding = "";
#else
    char const* addr_padding = "        ";
#endif
    dbgln("BEGIN{}         END{}        SIZE{}       ACCESS NAME",
        addr_padding, addr_padding, addr_padding);

    SpinlockLocker lock(m_lock);

    for (auto const& sorted_region : m_regions) {
        auto const& region = *sorted_region;
        dbgln("{:p} -- {:p} {:p} {:c}{:c}{:c}{:c}{:c}{:c} {}", region.vaddr().get(), region.vaddr().offset(region.size() - 1).get(), region.size(),
            region.is_readable() ? 'R' : ' ',
            region.is_writable() ? 'W' : ' ',
            region.is_executable() ? 'X' : ' ',
            region.is_shared() ? 'S' : ' ',
            region.is_stack() ? 'T' : ' ',
            region.is_syscall_region() ? 'C' : ' ',
            region.name());
    }
    MM.dump_kernel_regions();
}

void AddressSpace::remove_all_regions(Badge<Process>)
{
    VERIFY(Thread::current() == g_finalizer);
    SpinlockLocker locker(m_lock);
    {
        SpinlockLocker pd_locker(m_page_directory->get_lock());
        SpinlockLocker mm_locker(s_mm_lock);
        for (auto& region : m_regions)
            (*region).unmap_with_locks_held(Region::ShouldDeallocateVirtualRange::No, ShouldFlushTLB::No, pd_locker, mm_locker);
    }
    m_regions.clear();
}

size_t AddressSpace::amount_dirty_private() const
{
    SpinlockLocker lock(m_lock);
    // FIXME: This gets a bit more complicated for Regions sharing the same underlying VMObject.
    //        The main issue I'm thinking of is when the VMObject has physical pages that none of the Regions are mapping.
    //        That's probably a situation that needs to be looked at in general.
    size_t amount = 0;
    for (auto const& region : m_regions) {
        if (!region->is_shared())
            amount += region->amount_dirty();
    }
    return amount;
}

ErrorOr<size_t> AddressSpace::amount_clean_inode() const
{
    SpinlockLocker lock(m_lock);
    HashTable<const InodeVMObject*> vmobjects;
    for (auto const& region : m_regions) {
        if (region->vmobject().is_inode())
            TRY(vmobjects.try_set(&static_cast<const InodeVMObject&>(region->vmobject())));
    }
    size_t amount = 0;
    for (auto& vmobject : vmobjects)
        amount += vmobject->amount_clean();
    return amount;
}

size_t AddressSpace::amount_virtual() const
{
    SpinlockLocker lock(m_lock);
    size_t amount = 0;
    for (auto const& region : m_regions) {
        amount += region->size();
    }
    return amount;
}

size_t AddressSpace::amount_resident() const
{
    SpinlockLocker lock(m_lock);
    // FIXME: This will double count if multiple regions use the same physical page.
    size_t amount = 0;
    for (auto const& region : m_regions) {
        amount += region->amount_resident();
    }
    return amount;
}

size_t AddressSpace::amount_shared() const
{
    SpinlockLocker lock(m_lock);
    // FIXME: This will double count if multiple regions use the same physical page.
    // FIXME: It doesn't work at the moment, since it relies on PhysicalPage ref counts,
    //        and each PhysicalPage is only reffed by its VMObject. This needs to be refactored
    //        so that every Region contributes +1 ref to each of its PhysicalPages.
    size_t amount = 0;
    for (auto const& region : m_regions) {
        amount += region->amount_shared();
    }
    return amount;
}

size_t AddressSpace::amount_purgeable_volatile() const
{
    SpinlockLocker lock(m_lock);
    size_t amount = 0;
    for (auto const& region : m_regions) {
        if (!region->vmobject().is_anonymous())
            continue;
        auto const& vmobject = static_cast<AnonymousVMObject const&>(region->vmobject());
        if (vmobject.is_purgeable() && vmobject.is_volatile())
            amount += region->amount_resident();
    }
    return amount;
}

size_t AddressSpace::amount_purgeable_nonvolatile() const
{
    SpinlockLocker lock(m_lock);
    size_t amount = 0;
    for (auto const& region : m_regions) {
        if (!region->vmobject().is_anonymous())
            continue;
        auto const& vmobject = static_cast<AnonymousVMObject const&>(region->vmobject());
        if (vmobject.is_purgeable() && !vmobject.is_volatile())
            amount += region->amount_resident();
    }
    return amount;
}

}
