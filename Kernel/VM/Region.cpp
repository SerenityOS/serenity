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

#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/InodeVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/Region.h>

//#define MM_DEBUG
//#define PAGE_FAULT_DEBUG

Region::Region(const Range& range, const String& name, u8 access, bool cacheable)
    : m_range(range)
    , m_vmobject(AnonymousVMObject::create_with_size(size()))
    , m_name(name)
    , m_access(access)
    , m_cacheable(cacheable)
{
    MM.register_region(*this);
}

Region::Region(const Range& range, NonnullRefPtr<Inode> inode, const String& name, u8 access, bool cacheable)
    : m_range(range)
    , m_vmobject(InodeVMObject::create_with_inode(*inode))
    , m_name(name)
    , m_access(access)
    , m_cacheable(cacheable)
{
    MM.register_region(*this);
}

Region::Region(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const String& name, u8 access, bool cacheable)
    : m_range(range)
    , m_offset_in_vmobject(offset_in_vmobject)
    , m_vmobject(move(vmobject))
    , m_name(name)
    , m_access(access)
    , m_cacheable(cacheable)
{
    MM.register_region(*this);
}

Region::~Region()
{
    // Make sure we disable interrupts so we don't get interrupted between unmapping and unregistering.
    // Unmapping the region will give the VM back to the RangeAllocator, so an interrupt handler would
    // find the address<->region mappings in an invalid state there.
    InterruptDisabler disabler;
    if (m_page_directory) {
        unmap(ShouldDeallocateVirtualMemoryRange::Yes);
        ASSERT(!m_page_directory);
    }
    MM.unregister_region(*this);
}

NonnullOwnPtr<Region> Region::clone()
{
    ASSERT(current);

    // FIXME: What should we do for privately mapped InodeVMObjects?
    if (m_shared || vmobject().is_inode()) {
        ASSERT(!m_stack);
#ifdef MM_DEBUG
        dbgprintf("%s<%u> Region::clone(): sharing %s (V%p)\n",
            current->process().name().characters(),
            current->pid(),
            m_name.characters(),
            vaddr().get());
#endif
        // Create a new region backed by the same VMObject.
        auto region = Region::create_user_accessible(m_range, m_vmobject, m_offset_in_vmobject, m_name, m_access);
        region->set_mmap(m_mmap);
        region->set_shared(m_shared);
        return region;
    }

#ifdef MM_DEBUG
    dbgprintf("%s<%u> Region::clone(): cowing %s (V%p)\n",
        current->process().name().characters(),
        current->pid(),
        m_name.characters(),
        vaddr().get());
#endif
    // Set up a COW region. The parent (this) region becomes COW as well!
    ensure_cow_map().fill(true);
    remap();
    auto clone_region = Region::create_user_accessible(m_range, m_vmobject->clone(), m_offset_in_vmobject, m_name, m_access);
    clone_region->ensure_cow_map();
    if (m_stack) {
        ASSERT(is_readable());
        ASSERT(is_writable());
        ASSERT(!is_shared());
        ASSERT(vmobject().is_anonymous());
        clone_region->set_stack(true);
    }
    clone_region->set_mmap(m_mmap);
    return clone_region;
}

bool Region::commit()
{
    InterruptDisabler disabler;
#ifdef MM_DEBUG
    dbgprintf("MM: commit %u pages in Region %p (VMO=%p) at V%p\n", vmobject().page_count(), this, &vmobject(), vaddr().get());
#endif
    for (size_t i = 0; i < page_count(); ++i) {
        if (!commit(i))
            return false;
    }
    return true;
}

bool Region::commit(size_t page_index)
{
    ASSERT(vmobject().is_anonymous() || vmobject().is_purgeable());
    InterruptDisabler disabler;
#ifdef MM_DEBUG
    dbgprintf("MM: commit single page (%zu) in Region %p (VMO=%p) at V%p\n", page_index, vmobject().page_count(), this, &vmobject(), vaddr().get());
#endif
    auto& vmobject_physical_page_entry = vmobject().physical_pages()[first_page_index() + page_index];
    if (!vmobject_physical_page_entry.is_null())
        return true;
    auto physical_page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
    if (!physical_page) {
        kprintf("MM: commit was unable to allocate a physical page\n");
        return false;
    }
    vmobject_physical_page_entry = move(physical_page);
    remap_page(page_index);
    return true;
}

u32 Region::cow_pages() const
{
    if (!m_cow_map)
        return 0;
    u32 count = 0;
    for (int i = 0; i < m_cow_map->size(); ++i)
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
        if (m_vmobject->physical_pages()[first_page_index() + i])
            bytes += PAGE_SIZE;
    }
    return bytes;
}

size_t Region::amount_shared() const
{
    size_t bytes = 0;
    for (size_t i = 0; i < page_count(); ++i) {
        auto& physical_page = m_vmobject->physical_pages()[first_page_index() + i];
        if (physical_page && physical_page->ref_count() > 1)
            bytes += PAGE_SIZE;
    }
    return bytes;
}

NonnullOwnPtr<Region> Region::create_user_accessible(const Range& range, const StringView& name, u8 access, bool cacheable)
{
    auto region = make<Region>(range, name, access, cacheable);
    region->m_user_accessible = true;
    return region;
}

NonnullOwnPtr<Region> Region::create_user_accessible(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const StringView& name, u8 access, bool cacheable)
{
    auto region = make<Region>(range, move(vmobject), offset_in_vmobject, name, access, cacheable);
    region->m_user_accessible = true;
    return region;
}

NonnullOwnPtr<Region> Region::create_user_accessible(const Range& range, NonnullRefPtr<Inode> inode, const StringView& name, u8 access, bool cacheable)
{
    auto region = make<Region>(range, move(inode), name, access, cacheable);
    region->m_user_accessible = true;
    return region;
}

NonnullOwnPtr<Region> Region::create_kernel_only(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, const StringView& name, u8 access, bool cacheable)
{
    auto region = make<Region>(range, move(vmobject), offset_in_vmobject, name, access, cacheable);
    region->m_user_accessible = false;
    return region;
}

NonnullOwnPtr<Region> Region::create_kernel_only(const Range& range, const StringView& name, u8 access, bool cacheable)
{
    auto region = make<Region>(range, name, access, cacheable);
    region->m_user_accessible = false;
    return region;
}

bool Region::should_cow(size_t page_index) const
{
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

void Region::map_individual_page_impl(size_t page_index)
{
    auto page_vaddr = vaddr().offset(page_index * PAGE_SIZE);
    auto& pte = MM.ensure_pte(*m_page_directory, page_vaddr);
    auto& physical_page = vmobject().physical_pages()[first_page_index() + page_index];
    if (!physical_page) {
        pte.set_physical_page_base(0);
        pte.set_present(false);
    } else {
        pte.set_cache_disabled(!m_cacheable);
        pte.set_physical_page_base(physical_page->paddr().get());
        pte.set_present(true);
        if (should_cow(page_index))
            pte.set_writable(false);
        else
            pte.set_writable(is_writable());
        if (g_cpu_supports_nx)
            pte.set_execute_disabled(!is_executable());
        pte.set_user_allowed(is_user_accessible());
#ifdef MM_DEBUG
        dbg() << "MM: >> region map (PD=" << m_page_directory->cr3() << ", PTE=" << (void*)pte.raw() << "{" << &pte << "}) " << name() << " " << page_vaddr << " => " << physical_page->paddr() << " (@" << physical_page.ptr() << ")";
#endif
    }
    MM.flush_tlb(page_vaddr);
}

void Region::remap_page(size_t page_index)
{
    ASSERT(m_page_directory);
    InterruptDisabler disabler;
    ASSERT(vmobject().physical_pages()[first_page_index() + page_index]);
    map_individual_page_impl(page_index);
}

void Region::unmap(ShouldDeallocateVirtualMemoryRange deallocate_range)
{
    InterruptDisabler disabler;
    ASSERT(m_page_directory);
    for (size_t i = 0; i < page_count(); ++i) {
        auto vaddr = this->vaddr().offset(i * PAGE_SIZE);
        auto& pte = MM.ensure_pte(*m_page_directory, vaddr);
        pte.set_physical_page_base(0);
        pte.set_present(false);
        pte.set_writable(false);
        pte.set_user_allowed(false);
        MM.flush_tlb(vaddr);
#ifdef MM_DEBUG
        auto& physical_page = vmobject().physical_pages()[first_page_index() + i];
        dbgprintf("MM: >> Unmapped V%p => P%p <<\n", vaddr.get(), physical_page ? physical_page->paddr().get() : 0);
#endif
    }
    if (deallocate_range == ShouldDeallocateVirtualMemoryRange::Yes)
        m_page_directory->range_allocator().deallocate(range());
    m_page_directory = nullptr;
}

void Region::set_page_directory(PageDirectory& page_directory)
{
    ASSERT(!m_page_directory || m_page_directory == &page_directory);
    InterruptDisabler disabler;
    m_page_directory = page_directory;
}
void Region::map(PageDirectory& page_directory)
{
    set_page_directory(page_directory);
    InterruptDisabler disabler;
#ifdef MM_DEBUG
    dbgprintf("MM: Region::map() will map VMO pages %u - %u (VMO page count: %u)\n", first_page_index(), last_page_index(), vmobject().page_count());
#endif
    for (size_t page_index = 0; page_index < page_count(); ++page_index)
        map_individual_page_impl(page_index);
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
            dbgprintf("NP(non-readable) fault in Region{%p}[%u]\n", this, page_index_in_region);
            return PageFaultResponse::ShouldCrash;
        }

        if (vmobject().is_inode()) {
#ifdef PAGE_FAULT_DEBUG
            dbgprintf("NP(inode) fault in Region{%p}[%u]\n", this, page_index_in_region);
#endif
            return handle_inode_fault(page_index_in_region);
        }
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("NP(zero) fault in Region{%p}[%u]\n", this, page_index_in_region);
#endif
        return handle_zero_fault(page_index_in_region);
    }
    ASSERT(fault.type() == PageFault::Type::ProtectionViolation);
    if (fault.access() == PageFault::Access::Write && is_writable() && should_cow(page_index_in_region)) {
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("PV(cow) fault in Region{%p}[%u]\n", this, page_index_in_region);
#endif
        return handle_cow_fault(page_index_in_region);
    }
    kprintf("PV(error) fault in Region{%p}[%u] at V%p\n", this, page_index_in_region, fault.vaddr().get());
    return PageFaultResponse::ShouldCrash;
}

PageFaultResponse Region::handle_zero_fault(size_t page_index_in_region)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(vmobject().is_anonymous());

    sti();
    LOCKER(vmobject().m_paging_lock);
    cli();

    auto& vmobject_physical_page_entry = vmobject().physical_pages()[first_page_index() + page_index_in_region];

    if (!vmobject_physical_page_entry.is_null()) {
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("MM: zero_page() but page already present. Fine with me!\n");
#endif
        remap_page(page_index_in_region);
        return PageFaultResponse::Continue;
    }

    if (current)
        current->did_zero_fault();

    auto physical_page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
    if (physical_page.is_null()) {
        kprintf("MM: handle_zero_fault was unable to allocate a physical page\n");
        return PageFaultResponse::ShouldCrash;
    }

#ifdef PAGE_FAULT_DEBUG
    dbgprintf("      >> ZERO P%p\n", physical_page->paddr().get());
#endif
    vmobject_physical_page_entry = move(physical_page);
    remap_page(page_index_in_region);
    return PageFaultResponse::Continue;
}

PageFaultResponse Region::handle_cow_fault(size_t page_index_in_region)
{
    ASSERT_INTERRUPTS_DISABLED();
    auto& vmobject_physical_page_entry = vmobject().physical_pages()[first_page_index() + page_index_in_region];
    if (vmobject_physical_page_entry->ref_count() == 1) {
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("    >> It's a COW page but nobody is sharing it anymore. Remap r/w\n");
#endif
        set_should_cow(page_index_in_region, false);
        remap_page(page_index_in_region);
        return PageFaultResponse::Continue;
    }

    if (current)
        current->did_cow_fault();

#ifdef PAGE_FAULT_DEBUG
    dbgprintf("    >> It's a COW page and it's time to COW!\n");
#endif
    auto physical_page_to_copy = move(vmobject_physical_page_entry);
    auto physical_page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::No);
    if (physical_page.is_null()) {
        kprintf("MM: handle_cow_fault was unable to allocate a physical page\n");
        return PageFaultResponse::ShouldCrash;
    }
    u8* dest_ptr = MM.quickmap_page(*physical_page);
    const u8* src_ptr = vaddr().offset(page_index_in_region * PAGE_SIZE).as_ptr();
#ifdef PAGE_FAULT_DEBUG
    dbgprintf("      >> COW P%p <- P%p\n", physical_page->paddr().get(), physical_page_to_copy->paddr().get());
#endif
    copy_from_user(dest_ptr, src_ptr, PAGE_SIZE);
    vmobject_physical_page_entry = move(physical_page);
    MM.unquickmap_page();
    set_should_cow(page_index_in_region, false);
    remap_page(page_index_in_region);
    return PageFaultResponse::Continue;
}

PageFaultResponse Region::handle_inode_fault(size_t page_index_in_region)
{
    ASSERT_INTERRUPTS_DISABLED();
    ASSERT(vmobject().is_inode());
    auto& inode_vmobject = static_cast<InodeVMObject&>(vmobject());
    auto& vmobject_physical_page_entry = inode_vmobject.physical_pages()[first_page_index() + page_index_in_region];

    sti();
    LOCKER(vmobject().m_paging_lock);
    cli();

#ifdef PAGE_FAULT_DEBUG
    dbg() << "Inode fault in " << name() << " page index: " << page_index_in_region;
#endif

    if (!vmobject_physical_page_entry.is_null()) {
#ifdef PAGE_FAULT_DEBUG
        dbgprintf("MM: page_in_from_inode() but page already present. Fine with me!\n");
#endif
        remap_page(page_index_in_region);
        return PageFaultResponse::Continue;
    }

    if (current)
        current->did_inode_fault();

#ifdef MM_DEBUG
    dbgprintf("MM: page_in_from_inode ready to read from inode\n");
#endif
    sti();
    u8 page_buffer[PAGE_SIZE];
    auto& inode = inode_vmobject.inode();
    auto nread = inode.read_bytes((first_page_index() + page_index_in_region) * PAGE_SIZE, PAGE_SIZE, page_buffer, nullptr);
    if (nread < 0) {
        kprintf("MM: handle_inode_fault had error (%d) while reading!\n", nread);
        return PageFaultResponse::ShouldCrash;
    }
    if (nread < PAGE_SIZE) {
        // If we read less than a page, zero out the rest to avoid leaking uninitialized data.
        memset(page_buffer + nread, 0, PAGE_SIZE - nread);
    }
    cli();
    vmobject_physical_page_entry = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::No);
    if (vmobject_physical_page_entry.is_null()) {
        kprintf("MM: handle_inode_fault was unable to allocate a physical page\n");
        return PageFaultResponse::ShouldCrash;
    }

    u8* dest_ptr = MM.quickmap_page(*vmobject_physical_page_entry);
    memcpy(dest_ptr, page_buffer, PAGE_SIZE);
    MM.unquickmap_page();

    remap_page(page_index_in_region);
    return PageFaultResponse::Continue;
}
