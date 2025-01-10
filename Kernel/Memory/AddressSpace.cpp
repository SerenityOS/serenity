/*
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/MemoryLayout.h>
#include <Kernel/Arch/CPU.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/AddressSpace.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/InodeVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/PowerStateSwitchTask.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>

namespace Kernel::Memory {

ErrorOr<NonnullOwnPtr<AddressSpace>> AddressSpace::try_create(Process& process, AddressSpace const* parent)
{
    auto page_directory = TRY(PageDirectory::try_create_for_userspace(process));

    VirtualRange total_range = [&]() -> VirtualRange {
        if (parent)
            return parent->m_region_tree.total_range();
        constexpr FlatPtr userspace_range_base = USER_RANGE_BASE;
        FlatPtr const userspace_range_ceiling = USER_RANGE_CEILING;
        size_t random_offset = (get_fast_random<u8>() % 2 * MiB) & PAGE_MASK;
        FlatPtr base = userspace_range_base + random_offset;
        return VirtualRange(VirtualAddress { base }, userspace_range_ceiling - base);
    }();

    return adopt_nonnull_own_or_enomem(new (nothrow) AddressSpace(move(page_directory), total_range));
}

AddressSpace::AddressSpace(NonnullLockRefPtr<PageDirectory> page_directory, VirtualRange total_range)
    : m_page_directory(move(page_directory))
    , m_region_tree(total_range)
{
}

AddressSpace::~AddressSpace() = default;

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
        if (whole_region->is_immutable())
            return EPERM;

        PerformanceManager::add_unmap_perf_event(Process::current(), whole_region->range());

        deallocate_region(*whole_region);
        return {};
    }

    if (auto* old_region = find_region_containing(range_to_unmap)) {
        if (!old_region->is_mmap())
            return EPERM;
        if (old_region->is_immutable())
            return EPERM;

        // Remove the old region from our regions tree, since were going to add another region
        // with the exact same start address.
        auto region = take_region(*old_region);
        region->unmap();

        auto new_regions = TRY(try_split_region_around_range(*region, range_to_unmap));

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
        if (region->is_immutable())
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
        // with the exact same start address.
        auto region = take_region(*old_region);
        region->unmap();

        // Otherwise, split the regions and collect them for future mapping.
        auto split_regions = TRY(try_split_region_around_range(*region, range_to_unmap));
        TRY(new_regions.try_extend(split_regions));
    }

    // And finally map the new region(s) into our page directory.
    for (auto* new_region : new_regions) {
        // TODO: Ideally we should do this in a way that can be rolled back on failure, as failing here
        // leaves the caller in an undefined state.
        TRY(new_region->map(page_directory()));
    }

    PerformanceManager::add_unmap_perf_event(Process::current(), range_to_unmap);

    return {};
}

ErrorOr<Region*> AddressSpace::try_allocate_split_region(Region const& source_region, VirtualRange const& range, size_t offset_in_vmobject)
{
    OwnPtr<KString> region_name;
    if (!source_region.name().is_null())
        region_name = TRY(KString::try_create(source_region.name()));

    auto new_region = TRY(Region::create_unplaced(
        source_region.vmobject(), offset_in_vmobject, move(region_name), source_region.access(), source_region.memory_type(), source_region.is_shared()));
    new_region->set_syscall_region(source_region.is_syscall_region());
    new_region->set_mmap(source_region.is_mmap(), source_region.mmapped_from_readable(), source_region.mmapped_from_writable());
    new_region->set_stack(source_region.is_stack());
    TRY(m_region_tree.place_specifically(*new_region, range));
    return new_region.leak_ptr();
}

ErrorOr<Region*> AddressSpace::allocate_region(RandomizeVirtualAddress randomize_virtual_address, VirtualAddress requested_address, size_t requested_size, size_t requested_alignment, StringView name, int prot, AllocationStrategy strategy)
{
    if (!requested_address.is_page_aligned())
        return EINVAL;
    auto size = TRY(Memory::page_round_up(requested_size));
    auto alignment = TRY(Memory::page_round_up(requested_alignment));
    OwnPtr<KString> region_name;
    if (!name.is_null())
        region_name = TRY(KString::try_create(name));
    auto vmobject = TRY(AnonymousVMObject::try_create_with_size(size, strategy));
    auto region = TRY(Region::create_unplaced(move(vmobject), 0, move(region_name), prot_to_region_access_flags(prot)));
    if (requested_address.is_null()) {
        TRY(m_region_tree.place_anywhere(*region, randomize_virtual_address, size, alignment));
    } else {
        TRY(m_region_tree.place_specifically(*region, VirtualRange { requested_address, size }));
    }
    TRY(region->map(page_directory(), ShouldFlushTLB::No));
    return region.leak_ptr();
}

ErrorOr<Region*> AddressSpace::allocate_region_with_vmobject(VirtualRange requested_range, NonnullLockRefPtr<VMObject> vmobject, size_t offset_in_vmobject, StringView name, int prot, bool shared, MemoryType memory_type)
{
    return allocate_region_with_vmobject(RandomizeVirtualAddress::Yes, requested_range.base(), requested_range.size(), PAGE_SIZE, move(vmobject), offset_in_vmobject, name, prot, shared, memory_type);
}

ErrorOr<Region*> AddressSpace::allocate_region_with_vmobject(RandomizeVirtualAddress randomize_virtual_address, VirtualAddress requested_address, size_t requested_size, size_t requested_alignment, NonnullLockRefPtr<VMObject> vmobject, size_t offset_in_vmobject, StringView name, int prot, bool shared, MemoryType memory_type)
{
    if (!requested_address.is_page_aligned())
        return EINVAL;
    auto size = TRY(page_round_up(requested_size));
    auto alignment = TRY(page_round_up(requested_alignment));

    if (Checked<size_t>::addition_would_overflow(offset_in_vmobject, requested_size))
        return EOVERFLOW;

    size_t end_in_vmobject = offset_in_vmobject + requested_size;
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

    auto region = TRY(Region::create_unplaced(move(vmobject), offset_in_vmobject, move(region_name), prot_to_region_access_flags(prot), memory_type, shared));

    if (requested_address.is_null())
        TRY(m_region_tree.place_anywhere(*region, randomize_virtual_address, size, alignment));
    else
        TRY(m_region_tree.place_specifically(*region, VirtualRange { VirtualAddress { requested_address }, size }));

    ArmedScopeGuard remove_region_from_tree_on_failure = [&] {
        // At this point the region is already part of the Process region tree, so we have to make sure
        // we remove it from the tree before returning an error, or else the Region tree will contain
        // a dangling pointer to the free'd Region instance
        m_region_tree.remove(*region);
    };

    if (prot == PROT_NONE) {
        // For PROT_NONE mappings, we don't have to set up any page table mappings.
        // We do still need to attach the region to the page_directory though.
        region->set_page_directory(page_directory());
    } else {
        TRY(region->map(page_directory(), ShouldFlushTLB::No));
    }
    remove_region_from_tree_on_failure.disarm();
    return region.leak_ptr();
}

void AddressSpace::deallocate_region(Region& region)
{
    (void)take_region(region);
}

NonnullOwnPtr<Region> AddressSpace::take_region(Region& region)
{
    auto did_remove = m_region_tree.remove(region);
    VERIFY(did_remove);
    return NonnullOwnPtr { NonnullOwnPtr<Region>::Adopt, region };
}

Region* AddressSpace::find_region_from_range(VirtualRange const& range)
{
    auto* found_region = m_region_tree.regions().find(range.base().get());
    if (!found_region)
        return nullptr;
    auto& region = *found_region;
    auto rounded_range_size = page_round_up(range.size());
    if (rounded_range_size.is_error() || region.size() != rounded_range_size.value())
        return nullptr;
    return &region;
}

Region* AddressSpace::find_region_containing(VirtualRange const& range)
{
    return m_region_tree.find_region_containing(range);
}

ErrorOr<Vector<Region*, 4>> AddressSpace::find_regions_intersecting(VirtualRange const& range)
{
    Vector<Region*, 4> regions = {};
    size_t total_size_collected = 0;

    auto* found_region = m_region_tree.regions().find_largest_not_above(range.base().get());
    if (!found_region)
        return regions;
    for (auto iter = m_region_tree.regions().begin_from(*found_region); !iter.is_end(); ++iter) {
        auto const& iter_range = (*iter).range();
        if (iter_range.base() < range.end() && iter_range.end() > range.base()) {
            TRY(regions.try_append(&*iter));

            total_size_collected += (*iter).size() - iter_range.intersect(range).size();
            if (total_size_collected == range.size())
                break;
        }
    }

    return regions;
}

// Carve out a virtual address range from a region and return the two regions on either side
ErrorOr<Vector<Region*, 2>> AddressSpace::try_split_region_around_range(Region const& source_region, VirtualRange const& desired_range)
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
    char const* addr_padding = "        ";
    dbgln("BEGIN{}         END{}        SIZE{}       ACCESS NAME",
        addr_padding, addr_padding, addr_padding);

    for (auto const& region : m_region_tree.regions()) {
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
    if (!g_in_system_shutdown)
        VERIFY(Thread::current() == g_finalizer);

    {
        SpinlockLocker pd_locker(m_page_directory->get_lock());
        for (auto& region : m_region_tree.regions())
            region.unmap_with_locks_held(ShouldFlushTLB::No, pd_locker);
    }

    m_region_tree.delete_all_regions_assuming_they_are_unmapped();
}

size_t AddressSpace::amount_dirty_private() const
{
    // FIXME: This gets a bit more complicated for Regions sharing the same underlying VMObject.
    //        The main issue I'm thinking of is when the VMObject has physical pages that none of the Regions are mapping.
    //        That's probably a situation that needs to be looked at in general.
    size_t amount = 0;
    for (auto const& region : m_region_tree.regions()) {
        if (!region.is_shared())
            amount += region.amount_dirty();
    }
    return amount;
}

ErrorOr<size_t> AddressSpace::amount_clean_inode() const
{
    HashTable<LockRefPtr<InodeVMObject>> vmobjects;
    for (auto const& region : m_region_tree.regions()) {
        if (region.vmobject().is_inode())
            TRY(vmobjects.try_set(&static_cast<InodeVMObject const&>(region.vmobject())));
    }
    size_t amount = 0;
    for (auto& vmobject : vmobjects)
        amount += vmobject->amount_clean();
    return amount;
}

size_t AddressSpace::amount_virtual() const
{
    size_t amount = 0;
    for (auto const& region : m_region_tree.regions()) {
        amount += region.size();
    }
    return amount;
}

size_t AddressSpace::amount_resident() const
{
    // FIXME: This will double count if multiple regions use the same physical page.
    size_t amount = 0;
    for (auto const& region : m_region_tree.regions()) {
        amount += region.amount_resident();
    }
    return amount;
}

size_t AddressSpace::amount_shared() const
{
    // FIXME: This will double count if multiple regions use the same physical page.
    // FIXME: It doesn't work at the moment, since it relies on PhysicalPage ref counts,
    //        and each PhysicalPage is only reffed by its VMObject. This needs to be refactored
    //        so that every Region contributes +1 ref to each of its PhysicalPages.
    size_t amount = 0;
    for (auto const& region : m_region_tree.regions()) {
        amount += region.amount_shared();
    }
    return amount;
}

size_t AddressSpace::amount_purgeable_volatile() const
{
    size_t amount = 0;
    for (auto const& region : m_region_tree.regions()) {
        if (!region.vmobject().is_anonymous())
            continue;
        auto const& vmobject = static_cast<AnonymousVMObject const&>(region.vmobject());
        if (vmobject.is_purgeable() && vmobject.is_volatile())
            amount += region.amount_resident();
    }
    return amount;
}

size_t AddressSpace::amount_purgeable_nonvolatile() const
{
    size_t amount = 0;
    for (auto const& region : m_region_tree.regions()) {
        if (!region.vmobject().is_anonymous())
            continue;
        auto const& vmobject = static_cast<AnonymousVMObject const&>(region.vmobject());
        if (vmobject.is_purgeable() && !vmobject.is_volatile())
            amount += region.amount_resident();
    }
    return amount;
}

}
