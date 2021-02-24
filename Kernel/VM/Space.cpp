/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/QuickSort.h>
#include <Kernel/Process.h>
#include <Kernel/SpinLock.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Space.h>

namespace Kernel {

OwnPtr<Space> Space::create(Process& process, const Space* parent)
{
    auto page_directory = PageDirectory::create_for_userspace(parent ? &parent->page_directory().range_allocator() : nullptr);
    if (!page_directory)
        return {};
    auto space = adopt_own(*new Space(process, page_directory.release_nonnull()));
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

Optional<Range> Space::allocate_range(VirtualAddress vaddr, size_t size, size_t alignment)
{
    vaddr.mask(PAGE_MASK);
    size = page_round_up(size);
    if (vaddr.is_null())
        return page_directory().range_allocator().allocate_anywhere(size, alignment);
    return page_directory().range_allocator().allocate_specific(vaddr, size);
}

Region& Space::allocate_split_region(const Region& source_region, const Range& range, size_t offset_in_vmobject)
{
    auto& region = add_region(Region::create_user_accessible(
        m_process, range, source_region.vmobject(), offset_in_vmobject, source_region.name(), source_region.access(), source_region.is_cacheable() ? Region::Cacheable::Yes : Region::Cacheable::No, source_region.is_shared()));
    region.set_syscall_region(source_region.is_syscall_region());
    region.set_mmap(source_region.is_mmap());
    region.set_stack(source_region.is_stack());
    size_t page_offset_in_source_region = (offset_in_vmobject - source_region.offset_in_vmobject()) / PAGE_SIZE;
    for (size_t i = 0; i < region.page_count(); ++i) {
        if (source_region.should_cow(page_offset_in_source_region + i))
            region.set_should_cow(i, true);
    }
    return region;
}

KResultOr<Region*> Space::allocate_region(const Range& range, const String& name, int prot, AllocationStrategy strategy)
{
    VERIFY(range.is_valid());
    auto vmobject = AnonymousVMObject::create_with_size(range.size(), strategy);
    if (!vmobject)
        return ENOMEM;
    auto region = Region::create_user_accessible(m_process, range, vmobject.release_nonnull(), 0, name, prot_to_region_access_flags(prot), Region::Cacheable::Yes, false);
    if (!region->map(page_directory()))
        return ENOMEM;
    return &add_region(move(region));
}

KResultOr<Region*> Space::allocate_region_with_vmobject(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const String& name, int prot, bool shared)
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
    auto& region = add_region(Region::create_user_accessible(m_process, range, move(vmobject), offset_in_vmobject, name, prot_to_region_access_flags(prot), Region::Cacheable::Yes, shared));
    if (!region.map(page_directory())) {
        // FIXME: What is an appropriate error code here, really?
        return ENOMEM;
    }
    return &region;
}

bool Space::deallocate_region(Region& region)
{
    OwnPtr<Region> region_protector;
    ScopedSpinLock lock(m_lock);

    if (m_region_lookup_cache.region.unsafe_ptr() == &region)
        m_region_lookup_cache.region = nullptr;
    for (size_t i = 0; i < m_regions.size(); ++i) {
        if (&m_regions[i] == &region) {
            region_protector = m_regions.unstable_take(i);
            return true;
        }
    }
    return false;
}

Region* Space::find_region_from_range(const Range& range)
{
    ScopedSpinLock lock(m_lock);
    if (m_region_lookup_cache.range.has_value() && m_region_lookup_cache.range.value() == range && m_region_lookup_cache.region)
        return m_region_lookup_cache.region.unsafe_ptr();

    size_t size = page_round_up(range.size());
    for (auto& region : m_regions) {
        if (region.vaddr() == range.base() && region.size() == size) {
            m_region_lookup_cache.range = range;
            m_region_lookup_cache.region = region;
            return &region;
        }
    }
    return nullptr;
}

Region* Space::find_region_containing(const Range& range)
{
    ScopedSpinLock lock(m_lock);
    for (auto& region : m_regions) {
        if (region.contains(range))
            return &region;
    }
    return nullptr;
}

Vector<Region*> Space::find_regions_intersecting(const Range& range)
{
    Vector<Region*> regions = {};
    size_t total_size_collected = 0;

    ScopedSpinLock lock(m_lock);

    // FIXME: Maybe take the cache from the single lookup?
    for (auto& region : m_regions) {
        if (region.range().base() < range.end() && region.range().end() > range.base()) {
            regions.append(&region);

            total_size_collected += region.size() - region.range().intersect(range).size();
            if (total_size_collected == range.size())
                break;
        }
    }

    return regions;
}

Region& Space::add_region(NonnullOwnPtr<Region> region)
{
    auto* ptr = region.ptr();
    ScopedSpinLock lock(m_lock);
    m_regions.append(move(region));
    return *ptr;
}

// Carve out a virtual address range from a region and return the two regions on either side
Vector<Region*, 2> Space::split_region_around_range(const Region& source_region, const Range& desired_range)
{
    Range old_region_range = source_region.range();
    auto remaining_ranges_after_unmap = old_region_range.carve(desired_range);

    VERIFY(!remaining_ranges_after_unmap.is_empty());
    auto make_replacement_region = [&](const Range& new_range) -> Region& {
        VERIFY(old_region_range.contains(new_range));
        size_t new_range_offset_in_vmobject = source_region.offset_in_vmobject() + (new_range.base().get() - old_region_range.base().get());
        return allocate_split_region(source_region, new_range, new_range_offset_in_vmobject);
    };
    Vector<Region*, 2> new_regions;
    for (auto& new_range : remaining_ranges_after_unmap) {
        new_regions.unchecked_append(&make_replacement_region(new_range));
    }
    return new_regions;
}

void Space::dump_regions()
{
    dbgln("Process regions:");
    dbgln("BEGIN       END         SIZE        ACCESS  NAME");

    ScopedSpinLock lock(m_lock);

    Vector<Region*> sorted_regions;
    sorted_regions.ensure_capacity(m_regions.size());
    for (auto& region : m_regions)
        sorted_regions.append(&region);
    quick_sort(sorted_regions, [](auto& a, auto& b) {
        return a->vaddr() < b->vaddr();
    });

    for (auto& sorted_region : sorted_regions) {
        auto& region = *sorted_region;
        dbgln("{:08x} -- {:08x} {:08x} {:c}{:c}{:c}{:c}{:c}{:c} {}", region.vaddr().get(), region.vaddr().offset(region.size() - 1).get(), region.size(),
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
        if (!region.is_shared())
            amount += region.amount_dirty();
    }
    return amount;
}

size_t Space::amount_clean_inode() const
{
    ScopedSpinLock lock(m_lock);
    HashTable<const InodeVMObject*> vmobjects;
    for (auto& region : m_regions) {
        if (region.vmobject().is_inode())
            vmobjects.set(&static_cast<const InodeVMObject&>(region.vmobject()));
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
        amount += region.size();
    }
    return amount;
}

size_t Space::amount_resident() const
{
    ScopedSpinLock lock(m_lock);
    // FIXME: This will double count if multiple regions use the same physical page.
    size_t amount = 0;
    for (auto& region : m_regions) {
        amount += region.amount_resident();
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
        amount += region.amount_shared();
    }
    return amount;
}

size_t Space::amount_purgeable_volatile() const
{
    ScopedSpinLock lock(m_lock);
    size_t amount = 0;
    for (auto& region : m_regions) {
        if (region.vmobject().is_anonymous() && static_cast<const AnonymousVMObject&>(region.vmobject()).is_any_volatile())
            amount += region.amount_resident();
    }
    return amount;
}

size_t Space::amount_purgeable_nonvolatile() const
{
    ScopedSpinLock lock(m_lock);
    size_t amount = 0;
    for (auto& region : m_regions) {
        if (region.vmobject().is_anonymous() && !static_cast<const AnonymousVMObject&>(region.vmobject()).is_any_volatile())
            amount += region.amount_resident();
    }
    return amount;
}

}
