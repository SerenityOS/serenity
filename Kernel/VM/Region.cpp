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
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageDirectory.h>
#include <Kernel/VM/Region.h>
#include <Kernel/VM/SharedInodeVMObject.h>

namespace Kernel {

Region::Region(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, String name, u8 access, Cacheable cacheable, bool shared)
    : PurgeablePageRanges(vmobject)
    , m_range(range)
    , m_offset_in_vmobject(offset_in_vmobject)
    , m_vmobject(move(vmobject))
    , m_name(move(name))
    , m_access(access | ((access & 0x7) << 4))
    , m_shared(shared)
    , m_cacheable(cacheable == Cacheable::Yes)
{
    VERIFY(m_range.base().is_page_aligned());
    VERIFY(m_range.size());
    VERIFY((m_range.size() % PAGE_SIZE) == 0);

    m_vmobject->ref_region();
    register_purgeable_page_ranges();
    MM.register_region(*this);
}

Region::~Region()
{
    m_vmobject->unref_region();
    unregister_purgeable_page_ranges();

    // Make sure we disable interrupts so we don't get interrupted between unmapping and unregistering.
    // Unmapping the region will give the VM back to the RangeAllocator, so an interrupt handler would
    // find the address<->region mappings in an invalid state there.
    ScopedSpinLock lock(s_mm_lock);
    if (m_page_directory) {
        unmap(ShouldDeallocateVirtualMemoryRange::Yes);
        VERIFY(!m_page_directory);
    }

    MM.unregister_region(*this);
}

void Region::register_purgeable_page_ranges()
{
    if (m_vmobject->is_anonymous()) {
        auto& vmobject = static_cast<AnonymousVMObject&>(*m_vmobject);
        vmobject.register_purgeable_page_ranges(*this);
    }
}

void Region::unregister_purgeable_page_ranges()
{
    if (m_vmobject->is_anonymous()) {
        auto& vmobject = static_cast<AnonymousVMObject&>(*m_vmobject);
        vmobject.unregister_purgeable_page_ranges(*this);
    }
}

OwnPtr<Region> Region::clone(Process& new_owner)
{
    VERIFY(Process::current());

    ScopedSpinLock lock(s_mm_lock);

    if (m_shared) {
        VERIFY(!m_stack);
        if (vmobject().is_inode())
            VERIFY(vmobject().is_shared_inode());

        // Create a new region backed by the same VMObject.
        auto region = Region::create_user_accessible(
            &new_owner, m_range, m_vmobject, m_offset_in_vmobject, m_name, m_access, m_cacheable ? Cacheable::Yes : Cacheable::No, m_shared);
        if (m_vmobject->is_anonymous())
            region->copy_purgeable_page_ranges(*this);
        region->set_mmap(m_mmap);
        region->set_shared(m_shared);
        region->set_syscall_region(is_syscall_region());
        return region;
    }

    if (vmobject().is_inode())
        VERIFY(vmobject().is_private_inode());

    auto vmobject_clone = vmobject().clone();
    if (!vmobject_clone)
        return {};

    // Set up a COW region. The parent (this) region becomes COW as well!
    remap();
    auto clone_region = Region::create_user_accessible(
        &new_owner, m_range, vmobject_clone.release_nonnull(), m_offset_in_vmobject, m_name, m_access, m_cacheable ? Cacheable::Yes : Cacheable::No, m_shared);
    if (m_vmobject->is_anonymous())
        clone_region->copy_purgeable_page_ranges(*this);
    if (m_stack) {
        VERIFY(is_readable());
        VERIFY(is_writable());
        VERIFY(vmobject().is_anonymous());
        clone_region->set_stack(true);
    }
    clone_region->set_syscall_region(is_syscall_region());
    clone_region->set_mmap(m_mmap);
    return clone_region;
}

void Region::set_vmobject(NonnullRefPtr<VMObject>&& obj)
{
    if (m_vmobject.ptr() == obj.ptr())
        return;
    unregister_purgeable_page_ranges();
    m_vmobject->unref_region();
    m_vmobject = move(obj);
    m_vmobject->ref_region();
    register_purgeable_page_ranges();
}

bool Region::is_volatile(VirtualAddress vaddr, size_t size) const
{
    if (!m_vmobject->is_anonymous())
        return false;

    auto offset_in_vmobject = vaddr.get() - (this->vaddr().get() - m_offset_in_vmobject);
    size_t first_page_index = page_round_down(offset_in_vmobject) / PAGE_SIZE;
    size_t last_page_index = page_round_up(offset_in_vmobject + size) / PAGE_SIZE;
    return is_volatile_range({ first_page_index, last_page_index - first_page_index });
}

auto Region::set_volatile(VirtualAddress vaddr, size_t size, bool is_volatile, bool& was_purged) -> SetVolatileError
{
    was_purged = false;
    if (!m_vmobject->is_anonymous())
        return SetVolatileError::NotPurgeable;

    auto offset_in_vmobject = vaddr.get() - (this->vaddr().get() - m_offset_in_vmobject);
    if (is_volatile) {
        // If marking pages as volatile, be prudent by not marking
        // partial pages volatile to prevent potentially non-volatile
        // data to be discarded. So rund up the first page and round
        // down the last page.
        size_t first_page_index = page_round_up(offset_in_vmobject) / PAGE_SIZE;
        size_t last_page_index = page_round_down(offset_in_vmobject + size) / PAGE_SIZE;
        if (first_page_index != last_page_index)
            add_volatile_range({ first_page_index, last_page_index - first_page_index });
    } else {
        // If marking pages as non-volatile, round down the first page
        // and round up the last page to make sure the beginning and
        // end of the range doesn't inadvertedly get discarded.
        size_t first_page_index = page_round_down(offset_in_vmobject) / PAGE_SIZE;
        size_t last_page_index = page_round_up(offset_in_vmobject + size) / PAGE_SIZE;
        switch (remove_volatile_range({ first_page_index, last_page_index - first_page_index }, was_purged)) {
        case PurgeablePageRanges::RemoveVolatileError::Success:
        case PurgeablePageRanges::RemoveVolatileError::SuccessNoChange:
            break;
        case PurgeablePageRanges::RemoveVolatileError::OutOfMemory:
            return SetVolatileError::OutOfMemory;
        }
    }
    return SetVolatileError::Success;
}

size_t Region::cow_pages() const
{
    if (!vmobject().is_anonymous())
        return 0;
    return static_cast<const AnonymousVMObject&>(vmobject()).cow_pages();
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
        if (page && !page->is_shared_zero_page() && !page->is_lazy_committed_page())
            bytes += PAGE_SIZE;
    }
    return bytes;
}

size_t Region::amount_shared() const
{
    size_t bytes = 0;
    for (size_t i = 0; i < page_count(); ++i) {
        auto* page = physical_page(i);
        if (page && page->ref_count() > 1 && !page->is_shared_zero_page() && !page->is_lazy_committed_page())
            bytes += PAGE_SIZE;
    }
    return bytes;
}

NonnullOwnPtr<Region> Region::create_user_accessible(Process* owner, const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, String name, u8 access, Cacheable cacheable, bool shared)
{
    auto region = adopt_own(*new Region(range, move(vmobject), offset_in_vmobject, move(name), access, cacheable, shared));
    if (owner)
        region->m_owner = owner->make_weak_ptr();
    return region;
}

NonnullOwnPtr<Region> Region::create_kernel_only(const Range& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, String name, u8 access, Cacheable cacheable)
{
    return adopt_own(*new Region(range, move(vmobject), offset_in_vmobject, move(name), access, cacheable, false));
}

bool Region::should_cow(size_t page_index) const
{
    if (!vmobject().is_anonymous())
        return false;
    return static_cast<const AnonymousVMObject&>(vmobject()).should_cow(first_page_index() + page_index, m_shared);
}

void Region::set_should_cow(size_t page_index, bool cow)
{
    VERIFY(!m_shared);
    if (vmobject().is_anonymous())
        static_cast<AnonymousVMObject&>(vmobject()).set_should_cow(first_page_index() + page_index, cow);
}

bool Region::map_individual_page_impl(size_t page_index)
{
    VERIFY(m_page_directory->get_lock().own_lock());
    auto page_vaddr = vaddr_from_page_index(page_index);

    bool user_allowed = page_vaddr.get() >= 0x00800000 && is_user_address(page_vaddr);
    if (is_mmap() && !user_allowed) {
        PANIC("About to map mmap'ed page at a kernel address");
    }

    auto* pte = MM.ensure_pte(*m_page_directory, page_vaddr);
    if (!pte)
        return false;
    auto* page = physical_page(page_index);
    if (!page || (!is_readable() && !is_writable())) {
        pte->clear();
    } else {
        pte->set_cache_disabled(!m_cacheable);
        pte->set_physical_page_base(page->paddr().get());
        pte->set_present(true);
        if (page->is_shared_zero_page() || page->is_lazy_committed_page() || should_cow(page_index))
            pte->set_writable(false);
        else
            pte->set_writable(is_writable());
        if (Processor::current().has_feature(CPUFeature::NX))
            pte->set_execute_disabled(!is_executable());
        pte->set_user_allowed(user_allowed);
    }
    return true;
}

bool Region::do_remap_vmobject_page_range(size_t page_index, size_t page_count)
{
    bool success = true;
    VERIFY(s_mm_lock.own_lock());
    if (!m_page_directory)
        return success; // not an error, region may have not yet mapped it
    if (!translate_vmobject_page_range(page_index, page_count))
        return success; // not an error, region doesn't map this page range
    ScopedSpinLock page_lock(m_page_directory->get_lock());
    size_t index = page_index;
    while (index < page_index + page_count) {
        if (!map_individual_page_impl(index)) {
            success = false;
            break;
        }
        index++;
    }
    if (index > page_index)
        MM.flush_tlb(m_page_directory, vaddr_from_page_index(page_index), index - page_index);
    return success;
}

bool Region::remap_vmobject_page_range(size_t page_index, size_t page_count)
{
    bool success = true;
    ScopedSpinLock lock(s_mm_lock);
    auto& vmobject = this->vmobject();
    if (vmobject.is_shared_by_multiple_regions()) {
        vmobject.for_each_region([&](auto& region) {
            if (!region.do_remap_vmobject_page_range(page_index, page_count))
                success = false;
        });
    } else {
        if (!do_remap_vmobject_page_range(page_index, page_count))
            success = false;
    }
    return success;
}

bool Region::do_remap_vmobject_page(size_t page_index, bool with_flush)
{
    ScopedSpinLock lock(s_mm_lock);
    if (!m_page_directory)
        return true; // not an error, region may have not yet mapped it
    if (!translate_vmobject_page(page_index))
        return true; // not an error, region doesn't map this page
    ScopedSpinLock page_lock(m_page_directory->get_lock());
    VERIFY(physical_page(page_index));
    bool success = map_individual_page_impl(page_index);
    if (with_flush)
        MM.flush_tlb(m_page_directory, vaddr_from_page_index(page_index));
    return success;
}

bool Region::remap_vmobject_page(size_t page_index, bool with_flush)
{
    bool success = true;
    ScopedSpinLock lock(s_mm_lock);
    auto& vmobject = this->vmobject();
    if (vmobject.is_shared_by_multiple_regions()) {
        vmobject.for_each_region([&](auto& region) {
            if (!region.do_remap_vmobject_page(page_index, with_flush))
                success = false;
        });
    } else {
        if (!do_remap_vmobject_page(page_index, with_flush))
            success = false;
    }
    return success;
}

void Region::unmap(ShouldDeallocateVirtualMemoryRange deallocate_range)
{
    ScopedSpinLock lock(s_mm_lock);
    if (!m_page_directory)
        return;
    ScopedSpinLock page_lock(m_page_directory->get_lock());
    size_t count = page_count();
    for (size_t i = 0; i < count; ++i) {
        auto vaddr = vaddr_from_page_index(i);
        MM.release_pte(*m_page_directory, vaddr, i == count - 1);
    }
    MM.flush_tlb(m_page_directory, vaddr(), page_count());
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
    VERIFY(!m_page_directory || m_page_directory == &page_directory);
    VERIFY(s_mm_lock.own_lock());
    m_page_directory = page_directory;
}

bool Region::map(PageDirectory& page_directory, ShouldFlushTLB should_flush_tlb)
{
    ScopedSpinLock lock(s_mm_lock);
    ScopedSpinLock page_lock(page_directory.get_lock());

    // FIXME: Find a better place for this sanity check(?)
    if (is_user() && !is_shared()) {
        VERIFY(!vmobject().is_shared_inode());
    }

    set_page_directory(page_directory);
    size_t page_index = 0;
    while (page_index < page_count()) {
        if (!map_individual_page_impl(page_index))
            break;
        ++page_index;
    }
    if (page_index > 0) {
        if (should_flush_tlb == ShouldFlushTLB::Yes)
            MM.flush_tlb(m_page_directory, vaddr(), page_index);
        return page_index == page_count();
    }
    return false;
}

void Region::remap()
{
    VERIFY(m_page_directory);
    map(*m_page_directory);
}

PageFaultResponse Region::handle_fault(const PageFault& fault, ScopedSpinLock<RecursiveSpinLock>& mm_lock)
{
    auto page_index_in_region = page_index_from_address(fault.vaddr());
    if (fault.type() == PageFault::Type::PageNotPresent) {
        if (fault.is_read() && !is_readable()) {
            dbgln("NP(non-readable) fault in Region({})[{}]", this, page_index_in_region);
            return PageFaultResponse::ShouldCrash;
        }
        if (fault.is_write() && !is_writable()) {
            dbgln("NP(non-writable) write fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
            return PageFaultResponse::ShouldCrash;
        }
        if (vmobject().is_inode()) {
            dbgln_if(PAGE_FAULT_DEBUG, "NP(inode) fault in Region({})[{}]", this, page_index_in_region);
            return handle_inode_fault(page_index_in_region, mm_lock);
        }

        auto& page_slot = physical_page_slot(page_index_in_region);
        if (page_slot->is_lazy_committed_page()) {
            auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
            page_slot = static_cast<AnonymousVMObject&>(*m_vmobject).allocate_committed_page(page_index_in_vmobject);
            remap_vmobject_page(page_index_in_vmobject);
            return PageFaultResponse::Continue;
        }
#ifdef MAP_SHARED_ZERO_PAGE_LAZILY
        if (fault.is_read()) {
            page_slot = MM.shared_zero_page();
            remap_vmobject_page(translate_to_vmobject_page(page_index_in_region));
            return PageFaultResponse::Continue;
        }
        return handle_zero_fault(page_index_in_region);
#else
        dbgln("BUG! Unexpected NP fault at {}", fault.vaddr());
        return PageFaultResponse::ShouldCrash;
#endif
    }
    VERIFY(fault.type() == PageFault::Type::ProtectionViolation);
    if (fault.access() == PageFault::Access::Write && is_writable() && should_cow(page_index_in_region)) {
        dbgln_if(PAGE_FAULT_DEBUG, "PV(cow) fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
        auto* phys_page = physical_page(page_index_in_region);
        if (phys_page->is_shared_zero_page() || phys_page->is_lazy_committed_page()) {
            dbgln_if(PAGE_FAULT_DEBUG, "NP(zero) fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
            return handle_zero_fault(page_index_in_region);
        }
        return handle_cow_fault(page_index_in_region);
    }
    dbgln("PV(error) fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
    return PageFaultResponse::ShouldCrash;
}

PageFaultResponse Region::handle_zero_fault(size_t page_index_in_region)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(vmobject().is_anonymous());

    LOCKER(vmobject().m_paging_lock);

    auto& page_slot = physical_page_slot(page_index_in_region);
    auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);

    if (!page_slot.is_null() && !page_slot->is_shared_zero_page() && !page_slot->is_lazy_committed_page()) {
#if PAGE_FAULT_DEBUG
        dbgln("MM: zero_page() but page already present. Fine with me!");
#endif
        if (!remap_vmobject_page(page_index_in_vmobject))
            return PageFaultResponse::OutOfMemory;
        return PageFaultResponse::Continue;
    }

    auto current_thread = Thread::current();
    if (current_thread != nullptr)
        current_thread->did_zero_fault();

    if (page_slot->is_lazy_committed_page()) {
        page_slot = static_cast<AnonymousVMObject&>(*m_vmobject).allocate_committed_page(page_index_in_vmobject);
        dbgln_if(PAGE_FAULT_DEBUG, "      >> ALLOCATED COMMITTED {}", page_slot->paddr());
    } else {
        page_slot = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
        if (page_slot.is_null()) {
            klog() << "MM: handle_zero_fault was unable to allocate a physical page";
            return PageFaultResponse::OutOfMemory;
        }
        dbgln_if(PAGE_FAULT_DEBUG, "      >> ALLOCATED {}", page_slot->paddr());
    }

    if (!remap_vmobject_page(page_index_in_vmobject)) {
        klog() << "MM: handle_zero_fault was unable to allocate a page table to map " << page_slot;
        return PageFaultResponse::OutOfMemory;
    }
    return PageFaultResponse::Continue;
}

PageFaultResponse Region::handle_cow_fault(size_t page_index_in_region)
{
    VERIFY_INTERRUPTS_DISABLED();
    auto current_thread = Thread::current();
    if (current_thread)
        current_thread->did_cow_fault();

    if (!vmobject().is_anonymous())
        return PageFaultResponse::ShouldCrash;

    auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
    auto response = reinterpret_cast<AnonymousVMObject&>(vmobject()).handle_cow_fault(page_index_in_vmobject, vaddr().offset(page_index_in_region * PAGE_SIZE));
    if (!remap_vmobject_page(page_index_in_vmobject))
        return PageFaultResponse::OutOfMemory;
    return response;
}

PageFaultResponse Region::handle_inode_fault(size_t page_index_in_region, ScopedSpinLock<RecursiveSpinLock>& mm_lock)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(vmobject().is_inode());

    mm_lock.unlock();
    VERIFY(!s_mm_lock.own_lock());
    VERIFY(!g_scheduler_lock.own_lock());

    LOCKER(vmobject().m_paging_lock);

    mm_lock.lock();

    VERIFY_INTERRUPTS_DISABLED();
    auto& inode_vmobject = static_cast<InodeVMObject&>(vmobject());
    auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
    auto& vmobject_physical_page_entry = inode_vmobject.physical_pages()[page_index_in_vmobject];

    dbgln_if(PAGE_FAULT_DEBUG, "Inode fault in {} page index: {}", name(), page_index_in_region);

    if (!vmobject_physical_page_entry.is_null()) {
        dbgln_if(PAGE_FAULT_DEBUG, "MM: page_in_from_inode() but page already present. Fine with me!");
        if (!remap_vmobject_page(page_index_in_vmobject))
            return PageFaultResponse::OutOfMemory;
        return PageFaultResponse::Continue;
    }

    auto current_thread = Thread::current();
    if (current_thread)
        current_thread->did_inode_fault();

    u8 page_buffer[PAGE_SIZE];
    auto& inode = inode_vmobject.inode();

    // Reading the page may block, so release the MM lock temporarily
    mm_lock.unlock();
    auto buffer = UserOrKernelBuffer::for_kernel_buffer(page_buffer);
    auto nread = inode.read_bytes(page_index_in_vmobject * PAGE_SIZE, PAGE_SIZE, buffer, nullptr);
    mm_lock.lock();

    if (nread < 0) {
        klog() << "MM: handle_inode_fault had error (" << nread << ") while reading!";
        return PageFaultResponse::ShouldCrash;
    }
    if (nread < PAGE_SIZE) {
        // If we read less than a page, zero out the rest to avoid leaking uninitialized data.
        memset(page_buffer + nread, 0, PAGE_SIZE - nread);
    }

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
                dbgln("      >> inode fault: error copying data to {}/{}, failed at {}",
                    vmobject_physical_page_entry->paddr(),
                    VirtualAddress(dest_ptr),
                    VirtualAddress(fault_at));
            else
                VERIFY_NOT_REACHED();
        }
    }
    MM.unquickmap_page();

    remap_vmobject_page(page_index_in_vmobject);
    return PageFaultResponse::Continue;
}

RefPtr<Process> Region::get_owner()
{
    return m_owner.strong_ref();
}

}
