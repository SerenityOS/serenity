/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/PerformanceManager.h>
#include <Kernel/Process.h>
#include <Kernel/SpinLock.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Space.h>

namespace Kernel {

OwnPtr<Space> Space::try_create(Process& process, Space const* parent)
{
    auto page_directory = PageDirectory::create_for_userspace(parent ? &parent->page_directory().range_allocator() : nullptr);
    if (!page_directory)
        return {};
    auto space = adopt_own_if_nonnull(new (nothrow) Space(process, page_directory.release_nonnull()));
    if (!space)
        return {};
    space->page_directory().set_space({}, *space);
    return space;
}

Space::Space(Process& process, NonnullRefPtr<PageDirectory> page_directory)
    : m_process(&process)
    , m_page_directory(move(page_directory))
{
}

Space::~Space()
{
}

KResult Space::unmap_mmap_range(VirtualAddress addr, size_t size)
{
    if (!size)
        return EINVAL;

    auto range_or_error = Range::expand_to_page_boundaries(addr.get(), size);
    if (range_or_error.is_error())
        return range_or_error.error();

    auto range_to_unmap = range_or_error.value();

    if (!is_user_range(range_to_unmap))
        return EFAULT;

    if (auto* whole_region = find_region_from_range(range_to_unmap)) {
        if (!whole_region->is_mmap())
            return EPERM;

        PerformanceManager::add_unmap_perf_event(*Process::current(), whole_region->range());

        deallocate_region(*whole_region);
        return KSuccess;
    }

    if (auto* old_region = find_region_containing(range_to_unmap)) {
        if (!old_region->is_mmap())
            return EPERM;

        // Remove the old region from our regions tree, since were going to add another region
        // with the exact same start address, but don't deallocate it yet.
        auto region = take_region(*old_region);

        // We manually unmap the old region here, specifying that we *don't* want the VM deallocated.
        region->unmap(Region::ShouldDeallocateVirtualMemoryRange::No);

        auto new_regions_or_error = try_split_region_around_range(*region, range_to_unmap);
        if (new_regions_or_error.is_error())
            return new_regions_or_error.error();
        auto& new_regions = new_regions_or_error.value();

        // Instead we give back the unwanted VM manually.
        page_directory().range_allocator().deallocate(range_to_unmap);

        // And finally we map the new region(s) using our page directory (they were just allocated and don't have one).
        for (auto* new_region : new_regions) {
            new_region->map(page_directory());
        }

        PerformanceManager::add_unmap_perf_event(*Process::current(), range_to_unmap);

        return KSuccess;
    }

    // Try again while checking multiple regions at a time.
    auto const& regions = find_regions_intersecting(range_to_unmap);

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
        region->unmap(Region::ShouldDeallocateVirtualMemoryRange::No);

        // Otherwise, split the regions and collect them for future mapping.
        auto split_regions_or_error = try_split_region_around_range(*region, range_to_unmap);
        if (split_regions_or_error.is_error())
            return split_regions_or_error.error();

        if (new_regions.try_extend(split_regions_or_error.value()))
            return ENOMEM;
    }

    // Give back any unwanted VM to the range allocator.
    page_directory().range_allocator().deallocate(range_to_unmap);

    // And finally map the new region(s) into our page directory.
    for (auto* new_region : new_regions) {
        new_region->map(page_directory());
    }

    PerformanceManager::add_unmap_perf_event(*Process::current(), range_to_unmap);

    return KSuccess;
}

Optional<Range> Space::allocate_range(VirtualAddress vaddr, size_t size, size_t alignment)
{
    vaddr.mask(PAGE_MASK);
    size = page_round_up(size);
    if (vaddr.is_null())
        return page_directory().range_allocator().allocate_anywhere(size, alignment);
    return page_directory().range_allocator().allocate_specific(vaddr, size);
}

KResultOr<Region*> Space::try_allocate_split_region(Region const& source_region, Range const& range, size_t offset_in_vmobject)
{
    auto new_region = Region::try_create_user_accessible(
        range, source_region.vmobject(), offset_in_vmobject, KString::try_create(source_region.name()), source_region.access(), source_region.is_cacheable() ? Region::Cacheable::Yes : Region::Cacheable::No, source_region.is_shared());
    if (!new_region)
        return ENOMEM;
    auto* region = add_region(new_region.release_nonnull());
    if (!region)
        return ENOMEM;
    region->set_syscall_region(source_region.is_syscall_region());
    region->set_mmap(source_region.is_mmap());
    region->set_stack(source_region.is_stack());
    size_t page_offset_in_source_region = (offset_in_vmobject - source_region.offset_in_vmobject()) / PAGE_SIZE;
    for (size_t i = 0; i < region->page_count(); ++i) {
        if (source_region.should_cow(page_offset_in_source_region + i))
            region->set_should_cow(i, true);
    }
    return region;
}

KResultOr<Region*> Space::allocate_region(Range const& range, StringView name, int prot, AllocationStrategy strategy)
{
    VERIFY(range.is_valid());
    auto vmobject = AnonymousVMObject::try_create_with_size(range.size(), strategy);
    if (!vmobject)
        return ENOMEM;
    auto region = Region::try_create_user_accessible(range, vmobject.release_nonnull(), 0, KString::try_create(name), prot_to_region_access_flags(prot), Region::Cacheable::Yes, false);
    if (!region)
        return ENOMEM;
    if (!region->map(page_directory()))
        return ENOMEM;
    auto* added_region = add_region(region.release_nonnull());
    if (!added_region)
        return ENOMEM;
    return added_region;
}

KResultOr<Region*> Space::allocate_region_with_vmobject(Range const& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, StringView name, int prot, bool shared)
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
    auto region = Region::try_create_user_accessible(range, move(vmobject), offset_in_vmobject, KString::try_create(name), prot_to_region_access_flags(prot), Region::Cacheable::Yes, shared);
    if (!region) {
        dbgln("allocate_region_with_vmobject: Unable to allocate Region");
        return ENOMEM;
    }
    auto* added_region = add_region(region.release_nonnull());
    if (!added_region)
        return ENOMEM;
    if (!added_region->map(page_directory()))
        return ENOMEM;
    return added_region;
}

void Space::deallocate_region(Region& region)
{
    take_region(region);
}

NonnullOwnPtr<Region> Space::take_region(Region& region)
{
    ScopedSpinLock lock(m_lock);

    if (m_region_lookup_cache.region.unsafe_ptr() == &region)
        m_region_lookup_cache.region = nullptr;

    auto found_region = m_regions.unsafe_remove(region.vaddr().get());
    VERIFY(found_region.ptr() == &region);
    return found_region;
}

Region* Space::find_region_from_range(const Range& range)
{
    ScopedSpinLock lock(m_lock);
    if (m_region_lookup_cache.range.has_value() && m_region_lookup_cache.range.value() == range && m_region_lookup_cache.region)
        return m_region_lookup_cache.region.unsafe_ptr();

    auto found_region = m_regions.find(range.base().get());
    if (!found_region)
        return nullptr;
    auto& region = *found_region;
    size_t size = page_round_up(range.size());
    if (region->size() != size)
        return nullptr;
    m_region_lookup_cache.range = range;
    m_region_lookup_cache.region = *region;
    return region;
}

Region* Space::find_region_containing(const Range& range)
{
    ScopedSpinLock lock(m_lock);
    auto candidate = m_regions.find_largest_not_above(range.base().get());
    if (!candidate)
        return nullptr;
    return (*candidate)->range().contains(range) ? candidate->ptr() : nullptr;
}

Vector<Region*> Space::find_regions_intersecting(const Range& range)
{
    Vector<Region*> regions = {};
    size_t total_size_collected = 0;

    ScopedSpinLock lock(m_lock);

    auto found_region = m_regions.find_largest_not_above(range.base().get());
    if (!found_region)
        return regions;
    for (auto iter = m_regions.begin_from((*found_region)->vaddr().get()); !iter.is_end(); ++iter) {
        if ((*iter)->range().base() < range.end() && (*iter)->range().end() > range.base()) {
            regions.append(*iter);

            total_size_collected += (*iter)->size() - (*iter)->range().intersect(range).size();
            if (total_size_collected == range.size())
                break;
        }
    }

    return regions;
}

Region* Space::add_region(NonnullOwnPtr<Region> region)
{
    auto* ptr = region.ptr();
    ScopedSpinLock lock(m_lock);
    auto success = m_regions.try_insert(region->vaddr().get(), move(region));
    return success ? ptr : nullptr;
}

// Carve out a virtual address range from a region and return the two regions on either side
KResultOr<Vector<Region*, 2>> Space::try_split_region_around_range(const Region& source_region, const Range& desired_range)
{
    Range old_region_range = source_region.range();
    auto remaining_ranges_after_unmap = old_region_range.carve(desired_range);

    VERIFY(!remaining_ranges_after_unmap.is_empty());
    auto try_make_replacement_region = [&](const Range& new_range) -> KResultOr<Region*> {
        VERIFY(old_region_range.contains(new_range));
        size_t new_range_offset_in_vmobject = source_region.offset_in_vmobject() + (new_range.base().get() - old_region_range.base().get());
        return try_allocate_split_region(source_region, new_range, new_range_offset_in_vmobject);
    };
    Vector<Region*, 2> new_regions;
    for (auto& new_range : remaining_ranges_after_unmap) {
        auto new_region_or_error = try_make_replacement_region(new_range);
        if (new_region_or_error.is_error())
            return new_region_or_error.error();
        new_regions.unchecked_append(new_region_or_error.value());
    }
    return new_regions;
}

void Space::dump_regions()
{
    dbgln("Process regions:");
#if ARCH(I386)
    auto addr_padding = "";
#else
    auto addr_padding = "        ";
#endif
    dbgln("BEGIN{}         END{}        SIZE{}       ACCESS NAME",
        addr_padding, addr_padding, addr_padding);

    ScopedSpinLock lock(m_lock);

    for (auto& sorted_region : m_regions) {
        auto& region = *sorted_region;
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

void Space::remove_all_regions(Badge<Process>)
{
    ScopedSpinLock lock(m_lock);
    m_regions.clear();
}

size_t Space::amount_dirty_private() const
{
    ScopedSpinLock lock(m_lock);
    // FIXME: This gets a bit more complicated for Regions sharing the same underlying VMObject.
    //        The main issue I'm thinking of is when the VMObject has physical pages that none of the Regions are mapping.
    //        That's probably a situation that needs to be looked at in general.
    size_t amount = 0;
    for (auto& region : m_regions) {
        if (!region->is_shared())
            amount += region->amount_dirty();
    }
    return amount;
}

size_t Space::amount_clean_inode() const
{
    ScopedSpinLock lock(m_lock);
    HashTable<const InodeVMObject*> vmobjects;
    for (auto& region : m_regions) {
        if (region->vmobject().is_inode())
            vmobjects.set(&static_cast<const InodeVMObject&>(region->vmobject()));
    }
    size_t amount = 0;
    for (auto& vmobject : vmobjects)
        amount += vmobject->amount_clean();
    return amount;
}

size_t Space::amount_virtual() const
{
    ScopedSpinLock lock(m_lock);
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region->size();
    }
    return amount;
}

size_t Space::amount_resident() const
{
    ScopedSpinLock lock(m_lock);
    // FIXME: This will double count if multiple regions use the same physical page.
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region->amount_resident();
    }
    return amount;
}

size_t Space::amount_shared() const
{
    ScopedSpinLock lock(m_lock);
    // FIXME: This will double count if multiple regions use the same physical page.
    // FIXME: It doesn't work at the moment, since it relies on PhysicalPage ref counts,
    //        and each PhysicalPage is only reffed by its VMObject. This needs to be refactored
    //        so that every Region contributes +1 ref to each of its PhysicalPages.
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region->amount_shared();
    }
    return amount;
}

size_t Space::amount_purgeable_volatile() const
{
    ScopedSpinLock lock(m_lock);
    size_t amount = 0;
    for (auto& region : m_regions) {
        if (!region->vmobject().is_anonymous())
            continue;
        auto const& vmobject = static_cast<AnonymousVMObject const&>(region->vmobject());
        if (vmobject.is_purgeable() && vmobject.is_volatile())
            amount += region->amount_resident();
    }
    return amount;
}

size_t Space::amount_purgeable_nonvolatile() const
{
    ScopedSpinLock lock(m_lock);
    size_t amount = 0;
    for (auto& region : m_regions) {
        if (!region->vmobject().is_anonymous())
            continue;
        auto const& vmobject = static_cast<AnonymousVMObject const&>(region->vmobject());
        if (vmobject.is_purgeable() && !vmobject.is_volatile())
            amount += region->amount_resident();
    }
    return amount;
}

}
