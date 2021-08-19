/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <AK/StringView.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PageDirectory.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Panic.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

namespace Kernel::Memory {

Region::Region(VirtualRange const& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, Cacheable cacheable, bool shared)
    : m_range(range)
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

    m_vmobject->add_region(*this);
    MM.register_region(*this);
}

Region::~Region()
{
    m_vmobject->remove_region(*this);

    MM.unregister_region(*this);

    if (m_page_directory) {
        ScopedSpinLock page_lock(m_page_directory->get_lock());
        ScopedSpinLock lock(s_mm_lock);
        unmap(ShouldDeallocateVirtualRange::Yes);
        VERIFY(!m_page_directory);
    }
}

KResultOr<NonnullOwnPtr<Region>> Region::try_clone()
{
    VERIFY(Process::has_current());

    if (m_shared) {
        VERIFY(!m_stack);
        if (vmobject().is_inode())
            VERIFY(vmobject().is_shared_inode());

        // Create a new region backed by the same VMObject.
        auto maybe_region = Region::try_create_user_accessible(
            m_range, m_vmobject, m_offset_in_vmobject, m_name ? m_name->try_clone() : OwnPtr<KString> {}, access(), m_cacheable ? Cacheable::Yes : Cacheable::No, m_shared);
        if (maybe_region.is_error()) {
            dbgln("Region::clone: Unable to allocate new Region");
            return maybe_region.error();
        }

        auto region = maybe_region.release_value();
        region->set_mmap(m_mmap);
        region->set_shared(m_shared);
        region->set_syscall_region(is_syscall_region());
        return region;
    }

    if (vmobject().is_inode())
        VERIFY(vmobject().is_private_inode());

    auto maybe_vmobject_clone = vmobject().try_clone();
    if (maybe_vmobject_clone.is_error())
        return maybe_vmobject_clone.error();
    auto vmobject_clone = maybe_vmobject_clone.release_value();

    // Set up a COW region. The parent (this) region becomes COW as well!
    remap();
    auto maybe_clone_region = Region::try_create_user_accessible(
        m_range, vmobject_clone, m_offset_in_vmobject, m_name ? m_name->try_clone() : OwnPtr<KString> {}, access(), m_cacheable ? Cacheable::Yes : Cacheable::No, m_shared);
    if (maybe_clone_region.is_error()) {
        dbgln("Region::clone: Unable to allocate new Region for COW");
        return maybe_clone_region.error();
    }
    auto clone_region = maybe_clone_region.release_value();

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
    m_vmobject->remove_region(*this);
    m_vmobject = move(obj);
    m_vmobject->add_region(*this);
}

size_t Region::cow_pages() const
{
    if (!vmobject().is_anonymous())
        return 0;
    return static_cast<AnonymousVMObject const&>(vmobject()).cow_pages();
}

size_t Region::amount_dirty() const
{
    if (!vmobject().is_inode())
        return amount_resident();
    return static_cast<InodeVMObject const&>(vmobject()).amount_dirty();
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

KResultOr<NonnullOwnPtr<Region>> Region::try_create_user_accessible(VirtualRange const& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, Cacheable cacheable, bool shared)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) Region(range, move(vmobject), offset_in_vmobject, move(name), access, cacheable, shared));
}

KResultOr<NonnullOwnPtr<Region>> Region::try_create_kernel_only(VirtualRange const& range, NonnullRefPtr<VMObject> vmobject, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, Cacheable cacheable)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) Region(range, move(vmobject), offset_in_vmobject, move(name), access, cacheable, false));
}

bool Region::should_cow(size_t page_index) const
{
    if (!vmobject().is_anonymous())
        return false;
    return static_cast<AnonymousVMObject const&>(vmobject()).should_cow(first_page_index() + page_index, m_shared);
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

    // NOTE: We have to take the MM lock for PTE's to stay valid while we use them.
    ScopedSpinLock mm_locker(s_mm_lock);

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

bool Region::do_remap_vmobject_page(size_t page_index, bool with_flush)
{
    ScopedSpinLock lock(vmobject().m_lock);
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
    auto& vmobject = this->vmobject();
    bool success = true;
    vmobject.for_each_region([&](auto& region) {
        if (!region.do_remap_vmobject_page(page_index, with_flush))
            success = false;
    });
    return success;
}

void Region::unmap(ShouldDeallocateVirtualRange deallocate_range)
{
    if (!m_page_directory)
        return;
    ScopedSpinLock page_lock(m_page_directory->get_lock());
    ScopedSpinLock lock(s_mm_lock);
    size_t count = page_count();
    for (size_t i = 0; i < count; ++i) {
        auto vaddr = vaddr_from_page_index(i);
        MM.release_pte(*m_page_directory, vaddr, i == count - 1);
    }
    MM.flush_tlb(m_page_directory, vaddr(), page_count());
    if (deallocate_range == ShouldDeallocateVirtualRange::Yes) {
        m_page_directory->range_allocator().deallocate(range());
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
    ScopedSpinLock page_lock(page_directory.get_lock());
    ScopedSpinLock lock(s_mm_lock);

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

PageFaultResponse Region::handle_fault(PageFault const& fault)
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
            return handle_inode_fault(page_index_in_region);
        }

        auto& page_slot = physical_page_slot(page_index_in_region);
        if (page_slot->is_lazy_committed_page()) {
            auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
            VERIFY(m_vmobject->is_anonymous());
            page_slot = static_cast<AnonymousVMObject&>(*m_vmobject).allocate_committed_page({});
            remap_vmobject_page(page_index_in_vmobject);
            return PageFaultResponse::Continue;
        }
        dbgln("BUG! Unexpected NP fault at {}", fault.vaddr());
        return PageFaultResponse::ShouldCrash;
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

    auto& page_slot = physical_page_slot(page_index_in_region);
    auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);

    ScopedSpinLock locker(vmobject().m_lock);

    if (!page_slot.is_null() && !page_slot->is_shared_zero_page() && !page_slot->is_lazy_committed_page()) {
        dbgln_if(PAGE_FAULT_DEBUG, "MM: zero_page() but page already present. Fine with me!");
        if (!remap_vmobject_page(page_index_in_vmobject))
            return PageFaultResponse::OutOfMemory;
        return PageFaultResponse::Continue;
    }

    auto current_thread = Thread::current();
    if (current_thread != nullptr)
        current_thread->did_zero_fault();

    if (page_slot->is_lazy_committed_page()) {
        VERIFY(m_vmobject->is_anonymous());
        page_slot = static_cast<AnonymousVMObject&>(*m_vmobject).allocate_committed_page({});
        dbgln_if(PAGE_FAULT_DEBUG, "      >> ALLOCATED COMMITTED {}", page_slot->paddr());
    } else {
        page_slot = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
        if (page_slot.is_null()) {
            dmesgln("MM: handle_zero_fault was unable to allocate a physical page");
            return PageFaultResponse::OutOfMemory;
        }
        dbgln_if(PAGE_FAULT_DEBUG, "      >> ALLOCATED {}", page_slot->paddr());
    }

    if (!remap_vmobject_page(page_index_in_vmobject)) {
        dmesgln("MM: handle_zero_fault was unable to allocate a page table to map {}", page_slot);
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

PageFaultResponse Region::handle_inode_fault(size_t page_index_in_region)
{
    VERIFY_INTERRUPTS_DISABLED();
    VERIFY(vmobject().is_inode());
    VERIFY(!s_mm_lock.own_lock());
    VERIFY(!g_scheduler_lock.own_lock());

    auto& inode_vmobject = static_cast<InodeVMObject&>(vmobject());

    auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
    auto& vmobject_physical_page_entry = inode_vmobject.physical_pages()[page_index_in_vmobject];

    {
        ScopedSpinLock locker(inode_vmobject.m_lock);
        if (!vmobject_physical_page_entry.is_null()) {
            dbgln_if(PAGE_FAULT_DEBUG, "handle_inode_fault: Page faulted in by someone else before reading, remapping.");
            if (!remap_vmobject_page(page_index_in_vmobject))
                return PageFaultResponse::OutOfMemory;
            return PageFaultResponse::Continue;
        }
    }

    dbgln_if(PAGE_FAULT_DEBUG, "Inode fault in {} page index: {}", name(), page_index_in_region);

    auto current_thread = Thread::current();
    if (current_thread)
        current_thread->did_inode_fault();

    u8 page_buffer[PAGE_SIZE];
    auto& inode = inode_vmobject.inode();

    auto buffer = UserOrKernelBuffer::for_kernel_buffer(page_buffer);
    auto result = inode.read_bytes(page_index_in_vmobject * PAGE_SIZE, PAGE_SIZE, buffer, nullptr);

    if (result.is_error()) {
        dmesgln("handle_inode_fault: Error ({}) while reading from inode", result.error());
        return PageFaultResponse::ShouldCrash;
    }

    auto nread = result.value();
    if (nread < PAGE_SIZE) {
        // If we read less than a page, zero out the rest to avoid leaking uninitialized data.
        memset(page_buffer + nread, 0, PAGE_SIZE - nread);
    }

    ScopedSpinLock locker(inode_vmobject.m_lock);

    if (!vmobject_physical_page_entry.is_null()) {
        // Someone else faulted in this page while we were reading from the inode.
        // No harm done (other than some duplicate work), remap the page here and return.
        dbgln_if(PAGE_FAULT_DEBUG, "handle_inode_fault: Page faulted in by someone else, remapping.");
        if (!remap_vmobject_page(page_index_in_vmobject))
            return PageFaultResponse::OutOfMemory;
        return PageFaultResponse::Continue;
    }

    vmobject_physical_page_entry = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::No);

    if (vmobject_physical_page_entry.is_null()) {
        dmesgln("MM: handle_inode_fault was unable to allocate a physical page");
        return PageFaultResponse::OutOfMemory;
    }

    u8* dest_ptr = MM.quickmap_page(*vmobject_physical_page_entry);
    memcpy(dest_ptr, page_buffer, PAGE_SIZE);
    MM.unquickmap_page();

    remap_vmobject_page(page_index_in_vmobject);
    return PageFaultResponse::Continue;
}

}
