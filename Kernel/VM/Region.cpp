/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/SharedInodeVMObject.h>

//#define MM_DEBUG
//#define PAGE_FAULT_DEBUG

namespace Kernel {

Region::Region(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const String& name, u8 access, bool cacheable, bool kernel)
    : m_range(range)
    , m_offset_in_vmobject(offset_in_vmobject)
    , m_vmobject(move(vmobject))
    , m_name(name)
    , m_access(access)
    , m_cacheable(cacheable)
    , m_kernel(kernel)
{
    MM.register_region(*this);
}

Region::~Region()
{
    // Make sure we disable interrupts so we don't get interrupted between unmapping and unregistering.
    // Unmapping the region will give the VM back to the RangeAllocator, so an interrupt handler would
    // find the address<->region mappings in an invalid state there.
    ScopedSpinLock lock(s_mm_lock);
    if (m_page_directory) {
        unmap(ShouldDeallocateVirtualMemoryRange::Yes);
        ASSERT(!m_page_directory);
    }
    MM.unregister_region(*this);
}

NonnullOwnPtr<Region> Region::clone()
{
    ASSERT(Process::current());

    ScopedSpinLock lock(s_mm_lock);
    if (m_inherit_mode == InheritMode::ZeroedOnFork) {
        ASSERT(m_mmap);
        ASSERT(!m_shared);
        ASSERT(vmobject().is_anonymous());
        auto zeroed_region = Region::create_user_accessible(m_range, AnonymousVMObject::create_with_size(size()), 0, m_name, m_access);
        zeroed_region->set_mmap(m_mmap);
        zeroed_region->set_inherit_mode(m_inherit_mode);
        return zeroed_region;
    }

    if (m_shared) {
        ASSERT(!m_stack);
#ifdef MM_DEBUG
        dbg() << "Region::clone(): Sharing " << name() << " (" << vaddr() << ")";
#endif
        if (vmobject().is_inode())
            ASSERT(vmobject().is_shared_inode());

        // Create a new region backed by the same VMObject.
        auto region = Region::create_user_accessible(m_range, m_vmobject, m_offset_in_vmobject, m_name, m_access);
        region->set_mmap(m_mmap);
        region->set_shared(m_shared);
        return region;
    }

    if (vmobject().is_inode())
        ASSERT(vmobject().is_private_inode());

#ifdef MM_DEBUG
    dbg() << "Region::clone(): CoWing " << name() << " (" << vaddr() << ")";
#endif
    // Set up a COW region. The parent (this) region becomes COW as well!
    ensure_cow_map().fill(true);
    remap();
    auto clone_region = Region::create_user_accessible(m_range, m_vmobject->clone(), m_offset_in_vmobject, m_name, m_access);
    clone_region->ensure_cow_map();
    if (m_stack) {
        ASSERT(is_readable());
        ASSERT(is_writable());
        ASSERT(vmobject().is_anonymous());
        clone_region->set_stack(true);
    }
    clone_region->set_mmap(m_mmap);
    return clone_region;
}

bool Region::commit()
{
    ScopedSpinLock lock(s_mm_lock);
#ifdef MM_DEBUG
    dbg() << "MM: Commit " << page_count() << " pages in Region " << this << " (VMO=" << &vmobject() << ") at " << vaddr();
#endif
    for (size_t i = 0; i < page_count(); ++i) {
        if (!commit(i)) {
            // Flush what we did commit
            if (i > 0)
                MM.flush_tlb(vaddr(), i + 1);
            return false;
        }
    }
    MM.flush_tlb(vaddr(), page_count());
    return true;
}

bool Region::commit(size_t page_index)
{
    ASSERT(vmobject().is_anonymous() || vmobject().is_purgeable());
    ASSERT(s_mm_lock.own_lock());
    auto& vmobject_physical_page_entry = physical_page_slot(page_index);
    if (!vmobject_physical_page_entry.is_null() && !vmobject_physical_page_entry->is_shared_zero_page())
        return true;
    auto physical_page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
    if (!physical_page) {
        klog() << "MM: commit was unable to allocate a physical page";
        return false;
    }
    vmobject_physical_page_entry = move(physical_page);
    remap_page(page_index, false); // caller is in charge of flushing tlb
    return true;
}

u32 Region::cow_pages() const
{
    if (!m_cow_map)
        return 0;
    u32 count = 0;
    for (size_t i = 0; i < m_cow_map->size(); ++i)
        count += m_cow_map->get(i);
    return count;
}

size_t Region::amount_dirty() const
{
    if (!vmobject().is_inode())
        return amount_resident();
    return static_cast<const InodeVMObject&>(vmobject()).amount_dirty();
}

size_t Region::amount_resident() const
{
    size_t bytes = 0;
    for (size_t i = 0; i < page_count(); ++i) {
        auto* page = physical_page(i);
        if (page && !page->is_shared_zero_page())
            bytes += PAGE_SIZE;
    }
    return bytes;
}

size_t Region::amount_shared() const
{
    size_t bytes = 0;
    for (size_t i = 0; i < page_count(); ++i) {
        auto* page = physical_page(i);
        if (page && page->ref_count() > 1 && !page->is_shared_zero_page())
            bytes += PAGE_SIZE;
    }
    return bytes;
}

NonnullOwnPtr<Region> Region::create_user_accessible(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const StringView& name, u8 access, bool cacheable)
{
    auto region = make<Region>(range, move(vmobject), offset_in_vmobject, name, access, cacheable, false);
    region->m_user_accessible = true;
    return region;
}

NonnullOwnPtr<Region> Region::create_kernel_only(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const StringView& name, u8 access, bool cacheable)
{
    auto region = make<Region>(range, move(vmobject), offset_in_vmobject, name, access, cacheable, true);
    region->m_user_accessible = false;
    return region;
}

bool Region::should_cow(size_t page_index) const
{
    auto* page = physical_page(page_index);
    if (page && page->is_shared_zero_page())
        return true;
    if (m_shared)
        return false;
    return m_cow_map && m_cow_map->get(page_index);
}

void Region::set_should_cow(size_t page_index, bool cow)
{
    ASSERT(!m_shared);
    ensure_cow_map().set(page_index, cow);
}

Bitmap& Region::ensure_cow_map() const
{
    if (!m_cow_map)
        m_cow_map = make<Bitmap>(page_count(), true);
    return *m_cow_map;
}

bool Region::map_individual_page_impl(size_t page_index)
{
    auto page_vaddr = vaddr_from_page_index(page_index);
    auto* pte = MM.ensure_pte(*m_page_directory, page_vaddr);
    if (!pte) {
#ifdef MM_DEBUG
        dbg() << "MM: >> region map (PD=" << m_page_directory->cr3() << " " << name() << " cannot create PTE for " << page_vaddr;
#endif
        return false;
    }
    auto* page = physical_page(page_index);
    if (!page || (!is_readable() && !is_writable())) {
        pte->clear();
    } else {
        pte->set_cache_disabled(!m_cacheable);
        pte->set_physical_page_base(page->paddr().get());
        pte->set_present(true);
        if (should_cow(page_index))
            pte->set_writable(false);
        else
            pte->set_writable(is_writable());
        if (Processor::current().has_feature(CPUFeature::NX))
            pte->set_execute_disabled(!is_executable());
        pte->set_user_allowed(is_user_accessible());
#ifdef MM_DEBUG
        dbg() << "MM: >> region map (PD=" << m_page_directory->cr3() << ", PTE=" << (void*)pte->raw() << "{" << pte << "}) " << name() << " " << page_vaddr << " => " << page->paddr() << " (@" << page << ")";
#endif
    }
    return true;
}

bool Region::remap_page(size_t page_index, bool with_flush)
{
    ASSERT(m_page_directory);
    ScopedSpinLock lock(s_mm_lock);
    ASSERT(physical_page(page_index));
    bool success = map_individual_page_impl(page_index);
    if (with_flush)
        MM.flush_tlb(vaddr_from_page_index(page_index));
    return success;
}

void Region::unmap(ShouldDeallocateVirtualMemoryRange deallocate_range)
{
    ScopedSpinLock lock(s_mm_lock);
    ASSERT(m_page_directory);
    size_t count = page_count();
    for (size_t i = 0; i < count; ++i) {
        auto vaddr = vaddr_from_page_index(i);
        MM.release_pte(*m_page_directory, vaddr, i == count - 1);
#ifdef MM_DEBUG
        auto* page = physical_page(i);
        dbg() << "MM: >> Unmapped " << vaddr << " => P" << String::format("%p", page ? page->paddr().get() : 0) << " <<";
#endif
    }
    MM.flush_tlb(vaddr(), page_count());
    if (deallocate_range == ShouldDeallocateVirtualMemoryRange::Yes) {
        if (m_page_directory->range_allocator().contains(range()))
            m_page_directory->range_allocator().deallocate(range());
        else
            m_page_directory->identity_range_allocator().deallocate(range());
    }
    m_page_directory = nullptr;
}

void Region::set_page_directory(PageDirectory& page_directory)
{
    ASSERT(!m_page_directory || m_page_directory == &page_directory);
    ASSERT(s_mm_lock.own_lock());
    m_page_directory = page_directory;
}

bool Region::map(PageDirectory& page_directory)
{
    ScopedSpinLock lock(s_mm_lock);
    set_page_directory(page_directory);
#ifdef MM_DEBUG
    dbg() << "MM: Region::map() will map VMO pages " << first_page_index() << " - " << last_page_index() << " (VMO page count: " << vmobject().page_count() << ")";
#endif
    size_t page_index = 0;
    while (page_index < page_count()) {
        if (!map_individual_page_impl(page_index))
            break;
        ++page_index;
    }
    if (page_index > 0) {
        MM.flush_tlb(vaddr(), page_index);
        return page_index == page_count();
    }
    return false;
}

void Region::remap()
{
    ASSERT(m_page_directory);
    map(*m_page_directory);
}

PageFaultResponse Region::handle_fault(const PageFault& fault)
{
    auto page_index_in_region = page_index_from_address(fault.vaddr());
    if (fault.type() == PageFault::Type::PageNotPresent) {
        if (fault.is_read() && !is_readable()) {
            dbg() << "NP(non-readable) fault in Region{" << this << "}[" << page_index_in_region << "]";
            return PageFaultResponse::ShouldCrash;
        }
        if (fault.is_write() && !is_writable()) {
            dbg() << "NP(non-writable) write fault in Region{" << this << "}[" << page_index_in_region << "] at " << fault.vaddr();
            return PageFaultResponse::ShouldCrash;
        }
        if (vmobject().is_inode()) {
#ifdef PAGE_FAULT_DEBUG
            dbg() << "NP(inode) fault in Region{" << this << "}[" << page_index_in_region << "]";
#endif
            return handle_inode_fault(page_index_in_region);
        }
#ifdef MAP_SHARED_ZERO_PAGE_LAZILY
        if (fault.is_read()) {
            physical_page_slot(page_index_in_region) = MM.shared_zero_page();
            remap_page(page_index_in_region);
            return PageFaultResponse::Continue;
        }
        return handle_zero_fault(page_index_in_region);
#else
        dbg() << "BUG! Unexpected NP fault at " << fault.vaddr();
        return PageFaultResponse::ShouldCrash;
#endif
    }
    ASSERT(fault.type() == PageFault::Type::ProtectionViolation);
    if (fault.access() == PageFault::Access::Write && is_writable() && should_cow(page_index_in_region)) {
#ifdef PAGE_FAULT_DEBUG
        dbg() << "PV(cow) fault in Region{" << this << "}[" << page_index_in_region << "]";
#endif
        if (physical_page(page_index_in_region)->is_shared_zero_page()) {
#ifdef PAGE_FAULT_DEBUG
            dbg() << "NP(zero) fault in Region{" << this << "}[" << page_index_in_region << "]";
#endif
            return handle_zero_fault(page_index_in_region);
        }
        return handle_cow_fault(page_index_in_region);
    }
    dbg() << "PV(error) fault in Region{" << this << "}[" << page_index_in_region << "] at " << fault.vaddr();
    return PageFaultResponse::ShouldCrash;
}

PageFaultResponse Region::handle_zero_fault(size_t page_index_in_region)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(vmobject().is_anonymous());

    sti();
    LOCKER(vmobject().m_paging_lock);
    cli();

    auto& page_slot = physical_page_slot(page_index_in_region);

    if (!page_slot.is_null() && !page_slot->is_shared_zero_page()) {
#ifdef PAGE_FAULT_DEBUG
        dbg() << "MM: zero_page() but page already present. Fine with me!";
#endif
        if (!remap_page(page_index_in_region))
            return PageFaultResponse::OutOfMemory;
        return PageFaultResponse::Continue;
    }

    auto current_thread = Thread::current();
    if (current_thread != nullptr)
        current_thread->did_zero_fault();

    auto page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
    if (page.is_null()) {
        klog() << "MM: handle_zero_fault was unable to allocate a physical page";
        return PageFaultResponse::OutOfMemory;
    }

#ifdef PAGE_FAULT_DEBUG
    dbg() << "      >> ZERO " << page->paddr();
#endif
    page_slot = move(page);
    if (!remap_page(page_index_in_region)) {
        klog() << "MM: handle_zero_fault was unable to allocate a page table to map " << page_slot;
        return PageFaultResponse::OutOfMemory;
    }
    return PageFaultResponse::Continue;
}

PageFaultResponse Region::handle_cow_fault(size_t page_index_in_region)
{
    ASSERT_INTERRUPTS_DISABLED();
    auto& page_slot = physical_page_slot(page_index_in_region);
    if (page_slot->ref_count() == 1) {
#ifdef PAGE_FAULT_DEBUG
        dbg() << "    >> It's a COW page but nobody is sharing it anymore. Remap r/w";
#endif
        set_should_cow(page_index_in_region, false);
        if (!remap_page(page_index_in_region))
            return PageFaultResponse::OutOfMemory;
        return PageFaultResponse::Continue;
    }

    auto current_thread = Thread::current();
    if (current_thread)
        current_thread->did_cow_fault();

#ifdef PAGE_FAULT_DEBUG
    dbg() << "    >> It's a COW page and it's time to COW!";
#endif
    auto page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::No);
    if (page.is_null()) {
        klog() << "MM: handle_cow_fault was unable to allocate a physical page";
        return PageFaultResponse::OutOfMemory;
    }

    u8* dest_ptr = MM.quickmap_page(*page);
    const u8* src_ptr = vaddr().offset(page_index_in_region * PAGE_SIZE).as_ptr();
#ifdef PAGE_FAULT_DEBUG
    dbg() << "      >> COW " << page->paddr() << " <- " << page_slot->paddr();
#endif
    {
        SmapDisabler disabler;
        void* fault_at;
        if (!safe_memcpy(dest_ptr, src_ptr, PAGE_SIZE, fault_at)) {
            if ((u8*)fault_at >= dest_ptr && (u8*)fault_at <= dest_ptr + PAGE_SIZE)
                dbg() << "      >> COW: error copying page " << page_slot->paddr() << "/" << VirtualAddress(src_ptr) << " to " << page->paddr() << "/" << VirtualAddress(dest_ptr) << ": failed to write to page at " << VirtualAddress(fault_at);
            else if ((u8*)fault_at >= src_ptr && (u8*)fault_at <= src_ptr + PAGE_SIZE)
                dbg() << "      >> COW: error copying page " << page_slot->paddr() << "/" << VirtualAddress(src_ptr) << " to " << page->paddr() << "/" << VirtualAddress(dest_ptr) << ": failed to read from page at " << VirtualAddress(fault_at);
            else
                ASSERT_NOT_REACHED();
        }
    }
    page_slot = move(page);
    MM.unquickmap_page();
    set_should_cow(page_index_in_region, false);
    if (!remap_page(page_index_in_region))
        return PageFaultResponse::OutOfMemory;
    return PageFaultResponse::Continue;
}

PageFaultResponse Region::handle_inode_fault(size_t page_index_in_region)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(vmobject().is_inode());

    sti();
    LOCKER(vmobject().m_paging_lock);
    cli();

    auto& inode_vmobject = static_cast<InodeVMObject&>(vmobject());
    auto& vmobject_physical_page_entry = inode_vmobject.physical_pages()[first_page_index() + page_index_in_region];

#ifdef PAGE_FAULT_DEBUG
    dbg() << "Inode fault in " << name() << " page index: " << page_index_in_region;
#endif

    if (!vmobject_physical_page_entry.is_null()) {
#ifdef PAGE_FAULT_DEBUG
        dbg() << ("MM: page_in_from_inode() but page already present. Fine with me!");
#endif
        if (!remap_page(page_index_in_region))
            return PageFaultResponse::OutOfMemory;
        return PageFaultResponse::Continue;
    }

    auto current_thread = Thread::current();
    if (current_thread)
        current_thread->did_inode_fault();

#ifdef MM_DEBUG
    dbg() << "MM: page_in_from_inode ready to read from inode";
#endif
    sti();
    u8 page_buffer[PAGE_SIZE];
    auto& inode = inode_vmobject.inode();
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(page_buffer);
    auto nread = inode.read_bytes((first_page_index() + page_index_in_region) * PAGE_SIZE, PAGE_SIZE, buffer, nullptr);
    if (nread < 0) {
        klog() << "MM: handle_inode_fault had error (" << nread << ") while reading!";
        return PageFaultResponse::ShouldCrash;
    }
    if (nread < PAGE_SIZE) {
        // If we read less than a page, zero out the rest to avoid leaking uninitialized data.
        memset(page_buffer + nread, 0, PAGE_SIZE - nread);
    }
    cli();
    vmobject_physical_page_entry = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::No);
    if (vmobject_physical_page_entry.is_null()) {
        klog() << "MM: handle_inode_fault was unable to allocate a physical page";
        return PageFaultResponse::OutOfMemory;
    }

    u8* dest_ptr = MM.quickmap_page(*vmobject_physical_page_entry);
    {
        void* fault_at;
        if (!safe_memcpy(dest_ptr, page_buffer, PAGE_SIZE, fault_at)) {
            if ((u8*)fault_at >= dest_ptr && (u8*)fault_at <= dest_ptr + PAGE_SIZE)
                dbg() << "      >> inode fault: error copying data to " << vmobject_physical_page_entry->paddr() << "/" << VirtualAddress(dest_ptr) << ", failed at " << VirtualAddress(fault_at);
            else
                ASSERT_NOT_REACHED();
        }
    }
    MM.unquickmap_page();

    remap_page(page_index_in_region);
    return PageFaultResponse::Continue;
}

}
