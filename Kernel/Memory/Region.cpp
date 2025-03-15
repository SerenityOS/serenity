/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <Kernel/Arch/PageDirectory.h>
#include <Kernel/Arch/PageFault.h>
#include <Kernel/Debug.h>
#include <Kernel/FileSystem/Inode.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MMIOVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/Region.h>
#include <Kernel/Memory/SharedInodeVMObject.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/Thread.h>

namespace Kernel::Memory {

Region::Region()
    : m_range(VirtualRange({}, 0))
{
}

Region::Region(NonnullLockRefPtr<VMObject> vmobject, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, MemoryType memory_type, bool shared)
    : m_range(VirtualRange({}, 0))
    , m_offset_in_vmobject(offset_in_vmobject)
    , m_vmobject(move(vmobject))
    , m_name(move(name))
    , m_access(access | ((access & 0x7) << 4))
    , m_shared(shared)
    , m_memory_type(memory_type)
{
    m_vmobject->add_region(*this);
}

Region::Region(VirtualRange const& range, NonnullLockRefPtr<VMObject> vmobject, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, MemoryType memory_type, bool shared)
    : m_range(range)
    , m_offset_in_vmobject(offset_in_vmobject)
    , m_vmobject(move(vmobject))
    , m_name(move(name))
    , m_access(access | ((access & 0x7) << 4))
    , m_shared(shared)
    , m_memory_type(memory_type)
{
    VERIFY(m_range.base().is_page_aligned());
    VERIFY(m_range.size());
    VERIFY((m_range.size() % PAGE_SIZE) == 0);

    m_vmobject->add_region(*this);
}

Region::~Region()
{
    if (is_writable() && vmobject().is_shared_inode())
        (void)static_cast<SharedInodeVMObject&>(vmobject()).sync_before_destroying();

    m_vmobject->remove_region(*this);

    if (m_page_directory) {
        SpinlockLocker pd_locker(m_page_directory->get_lock());
        if (!is_readable() && !is_writable() && !is_executable()) {
            // If the region is "PROT_NONE", we didn't map it in the first place.
        } else {
            unmap_with_locks_held(ShouldFlushTLB::Yes, pd_locker);
            VERIFY(!m_page_directory);
        }
    }

    if (is_kernel())
        MM.unregister_kernel_region(*this);

    // Extend the lifetime of the region if there are any page faults in progress for this region's pages.
    // Both the removal of regions from the region trees and the fetching of the regions from the tree
    // during the start of page fault handling are serialized under the address space spinlock. This means
    // that once the region is removed no more page faults on this region can start, so this counter will
    // eventually reach 0. And similarly since we can only reach the region destructor once the region was
    // removed from the appropriate region tree, it is guaranteed that any page faults that are still being
    // handled have already increased this counter, and will be allowed to finish before deallocation.
    while (m_in_progress_page_faults)
        Processor::wait_check();
}

ErrorOr<NonnullOwnPtr<Region>> Region::create_unbacked()
{
    return adopt_nonnull_own_or_enomem(new (nothrow) Region);
}

ErrorOr<NonnullOwnPtr<Region>> Region::create_unplaced(NonnullLockRefPtr<VMObject> vmobject, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, MemoryType memory_type, bool shared)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) Region(move(vmobject), offset_in_vmobject, move(name), access, memory_type, shared));
}

ErrorOr<NonnullOwnPtr<Region>> Region::try_clone()
{
    VERIFY(Process::has_current());

    if (m_shared) {
        VERIFY(!m_stack);
        if (vmobject().is_inode())
            VERIFY(vmobject().is_shared_inode());

        // Create a new region backed by the same VMObject.

        OwnPtr<KString> region_name;
        if (m_name)
            region_name = TRY(m_name->try_clone());

        auto region = TRY(Region::try_create_user_accessible(
            m_range, vmobject(), m_offset_in_vmobject, move(region_name), access(), m_memory_type, m_shared));
        region->set_mmap(m_mmap, m_mmapped_from_readable, m_mmapped_from_writable);
        region->set_shared(m_shared);
        region->set_syscall_region(is_syscall_region());
        return region;
    }

    if (vmobject().is_inode())
        VERIFY(vmobject().is_private_inode());

    auto vmobject_clone = TRY(vmobject().try_clone());

    // Set up a COW region. The parent (this) region becomes COW as well!
    if (is_writable())
        remap();

    OwnPtr<KString> clone_region_name;
    if (m_name)
        clone_region_name = TRY(m_name->try_clone());

    auto clone_region = TRY(Region::try_create_user_accessible(
        m_range, move(vmobject_clone), m_offset_in_vmobject, move(clone_region_name), access(), m_memory_type, m_shared));

    if (m_stack) {
        VERIFY(vmobject().is_anonymous());
        clone_region->set_stack(true);
    }
    clone_region->set_syscall_region(is_syscall_region());
    clone_region->set_mmap(m_mmap, m_mmapped_from_readable, m_mmapped_from_writable);
    return clone_region;
}

void Region::set_vmobject(NonnullLockRefPtr<VMObject>&& obj)
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
        auto page = physical_page(i);
        if (page && !page->is_shared_zero_page() && !page->is_lazy_committed_page())
            bytes += PAGE_SIZE;
    }
    return bytes;
}

size_t Region::amount_shared() const
{
    size_t bytes = 0;
    for (size_t i = 0; i < page_count(); ++i) {
        auto page = physical_page(i);
        if (page && page->ref_count() > 1 && !page->is_shared_zero_page() && !page->is_lazy_committed_page())
            bytes += PAGE_SIZE;
    }
    return bytes;
}

ErrorOr<NonnullOwnPtr<Region>> Region::try_create_user_accessible(VirtualRange const& range, NonnullLockRefPtr<VMObject> vmobject, size_t offset_in_vmobject, OwnPtr<KString> name, Region::Access access, MemoryType memory_type, bool shared)
{
    return adopt_nonnull_own_or_enomem(new (nothrow) Region(range, move(vmobject), offset_in_vmobject, move(name), access, memory_type, shared));
}

bool Region::should_cow(size_t page_index) const
{
    if (!vmobject().is_anonymous())
        return false;
    return static_cast<AnonymousVMObject const&>(vmobject()).should_cow(first_page_index() + page_index, m_shared);
}

bool Region::should_dirty_on_write(size_t page_index) const
{
    if (!vmobject().is_inode())
        return false;
    SpinlockLocker locker(vmobject().m_lock);
    return !static_cast<InodeVMObject const&>(vmobject()).is_page_dirty(first_page_index() + page_index);
}

bool Region::map_individual_page_impl(size_t page_index, RefPtr<PhysicalRAMPage> page)
{
    if (!page)
        return map_individual_page_impl(page_index, {}, false, false);
    return map_individual_page_impl(page_index, page->paddr(), is_readable(), is_writable() && !page->is_shared_zero_page() && !page->is_lazy_committed_page());
}

bool Region::map_individual_page_impl(size_t page_index, PhysicalAddress paddr)
{
    return map_individual_page_impl(page_index, paddr, is_readable(), is_writable());
}

bool Region::map_individual_page_impl(size_t page_index, PhysicalAddress paddr, bool readable, bool writable)
{
    VERIFY(m_page_directory->get_lock().is_locked_by_current_processor());

    auto page_vaddr = vaddr_from_page_index(page_index);

    bool user_allowed = page_vaddr.get() >= USER_RANGE_BASE && is_user_address(page_vaddr);
    if (is_mmap() && !user_allowed) {
        PANIC("About to map mmap'ed page at a kernel address");
    }

    auto* pte = MM.ensure_pte(*m_page_directory, page_vaddr);
    if (!pte)
        return false;

    if (!readable && !writable) {
        pte->clear();
        return true;
    }

    bool is_writable = writable && !(should_cow(page_index) || should_dirty_on_write(page_index));

    pte->set_memory_type(m_memory_type);
    pte->set_physical_page_base(paddr.get());
    pte->set_present(true);
    pte->set_writable(is_writable);
    if (Processor::current().has_nx())
        pte->set_execute_disabled(!is_executable());
    pte->set_user_allowed(user_allowed);

    return true;
}

bool Region::map_individual_page_impl(size_t page_index)
{
    RefPtr<PhysicalRAMPage> page;
    {
        SpinlockLocker vmobject_locker(vmobject().m_lock);
        page = physical_page(page_index);
    }

    return map_individual_page_impl(page_index, page);
}

bool Region::remap_vmobject_page(size_t page_index, NonnullRefPtr<PhysicalRAMPage> physical_page)
{
    SpinlockLocker page_lock(m_page_directory->get_lock());

    // NOTE: `page_index` is a VMObject page index, so first we convert it to a Region page index.
    if (!translate_vmobject_page(page_index))
        return false;

    bool success = map_individual_page_impl(page_index, physical_page);
    MemoryManager::flush_tlb(m_page_directory, vaddr_from_page_index(page_index));
    return success;
}

void Region::unmap(ShouldFlushTLB should_flush_tlb)
{
    if (!m_page_directory)
        return;
    SpinlockLocker pd_locker(m_page_directory->get_lock());
    unmap_with_locks_held(should_flush_tlb, pd_locker);
}

void Region::unmap_with_locks_held(ShouldFlushTLB should_flush_tlb, SpinlockLocker<RecursiveSpinlock<LockRank::None>>&)
{
    if (!m_page_directory)
        return;
    size_t count = page_count();
    for (size_t i = 0; i < count; ++i) {
        auto vaddr = vaddr_from_page_index(i);
        MM.release_pte(*m_page_directory, vaddr, i == count - 1 ? MemoryManager::IsLastPTERelease::Yes : MemoryManager::IsLastPTERelease::No);
    }
    if (should_flush_tlb == ShouldFlushTLB::Yes)
        MemoryManager::flush_tlb(m_page_directory, vaddr(), page_count());
    m_page_directory = nullptr;
}

void Region::set_page_directory(PageDirectory& page_directory)
{
    VERIFY(!m_page_directory || m_page_directory == &page_directory);
    m_page_directory = page_directory;
}

ErrorOr<void> Region::map(PageDirectory& page_directory, ShouldFlushTLB should_flush_tlb)
{
    SpinlockLocker page_lock(page_directory.get_lock());

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
            MemoryManager::flush_tlb(m_page_directory, vaddr(), page_index);
        if (page_index == page_count())
            return {};
    }
    return ENOMEM;
}

ErrorOr<void> Region::map(PageDirectory& page_directory, PhysicalAddress paddr, ShouldFlushTLB should_flush_tlb)
{
    SpinlockLocker page_lock(page_directory.get_lock());
    set_page_directory(page_directory);
    size_t page_index = 0;
    while (page_index < page_count()) {
        if (!map_individual_page_impl(page_index, paddr))
            break;
        ++page_index;
        paddr = paddr.offset(PAGE_SIZE);
    }
    if (page_index > 0) {
        if (should_flush_tlb == ShouldFlushTLB::Yes)
            MemoryManager::flush_tlb(m_page_directory, vaddr(), page_index);
        if (page_index == page_count())
            return {};
    }
    return ENOMEM;
}

void Region::remap()
{
    VERIFY(m_page_directory);
    ErrorOr<void> result;
    if (m_vmobject->is_mmio())
        result = map(*m_page_directory, static_cast<MMIOVMObject const&>(*m_vmobject).base_address());
    else
        result = map(*m_page_directory);
    if (result.is_error())
        TODO();
}

void Region::clear_to_zero()
{
    VERIFY(vmobject().is_anonymous());
    SpinlockLocker locker(vmobject().m_lock);
    for (auto i = 0u; i < page_count(); ++i) {
        auto& page = physical_page_slot(i);
        VERIFY(page);
        if (page->is_shared_zero_page())
            continue;
        page = MM.shared_zero_page();
    }
}

PageFaultResponse Region::handle_fault(PageFault const& fault)
{
#if !ARCH(RISCV64)
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

        SpinlockLocker vmobject_locker(vmobject().m_lock);
        auto& page_slot = physical_page_slot(page_index_in_region);
        if (page_slot->is_lazy_committed_page()) {
            auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
            VERIFY(m_vmobject->is_anonymous());
            page_slot = static_cast<AnonymousVMObject&>(*m_vmobject).allocate_committed_page({});
            if (!remap_vmobject_page(page_index_in_vmobject, *page_slot))
                return PageFaultResponse::OutOfMemory;
            return PageFaultResponse::Continue;
        }
        dbgln("BUG! Unexpected NP fault at {}", fault.vaddr());
        dbgln("     - Physical page slot pointer: {:p}", page_slot.ptr());
        if (page_slot) {
            dbgln("     - Physical page: {}", page_slot->paddr());
            dbgln("     - Lazy committed: {}", page_slot->is_lazy_committed_page());
            dbgln("     - Shared zero: {}", page_slot->is_shared_zero_page());
        }
        return PageFaultResponse::ShouldCrash;
    }
    VERIFY(fault.type() == PageFault::Type::ProtectionViolation);
    if (fault.access() == PageFault::Access::Write && is_writable()) {
        if (should_cow(page_index_in_region)) {
            dbgln_if(PAGE_FAULT_DEBUG, "PV(cow) fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
            auto phys_page = physical_page(page_index_in_region);
            if (phys_page->is_shared_zero_page() || phys_page->is_lazy_committed_page()) {
                dbgln_if(PAGE_FAULT_DEBUG, "NP(zero) fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
                return handle_zero_fault(page_index_in_region, *phys_page);
            }
            return handle_cow_fault(page_index_in_region);
        }
        // Write faults to InodeVMObjects should always be treated as a dirty-on-write fault
        if (vmobject().is_inode()) {
            dbgln_if(PAGE_FAULT_DEBUG, "PV(dirty_on_write) fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
            return handle_dirty_on_write_fault(page_index_in_region);
        }
    }
    dbgln("PV(error) fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
    return PageFaultResponse::ShouldCrash;
#else
    // FIXME: Consider to merge the RISC-V page fault handling code with the x86_64/aarch64 implementation.
    //        RISC-V doesn't tell you *why* a memory access failed, only the original access type (r/w/x).
    //        We probably should take the page fault reason into account, if the processor provides it.

    auto page_index_in_region = page_index_from_address(fault.vaddr());

    if (fault.is_read() && !is_readable()) {
        dbgln("Read page fault in non-readable Region({})[{}]", this, page_index_in_region);
        return PageFaultResponse::ShouldCrash;
    }

    if (fault.is_write() && !is_writable()) {
        dbgln("Write page fault in non-writable Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
        return PageFaultResponse::ShouldCrash;
    }

    if (fault.is_instruction_fetch() && !is_executable()) {
        dbgln("Instruction fetch page fault in non-executable Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
        return PageFaultResponse::ShouldCrash;
    }

    if (fault.is_write() && is_writable()) {
        if (should_cow(page_index_in_region)) {
            dbgln_if(PAGE_FAULT_DEBUG, "CoW page fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
            auto phys_page = physical_page(page_index_in_region);
            if (phys_page->is_shared_zero_page() || phys_page->is_lazy_committed_page()) {
                dbgln_if(PAGE_FAULT_DEBUG, "Zero page fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
                return handle_zero_fault(page_index_in_region, *phys_page);
            }
            return handle_cow_fault(page_index_in_region);
        }
        if (should_dirty_on_write(page_index_in_region)) {
            dbgln_if(PAGE_FAULT_DEBUG, "PV(dirty_on_write) fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
            return handle_dirty_on_write_fault(page_index_in_region);
        }
    }

    if (vmobject().is_inode()) {
        dbgln_if(PAGE_FAULT_DEBUG, "Inode page fault in Region({})[{}]", this, page_index_in_region);
        return handle_inode_fault(page_index_in_region);
    }

    SpinlockLocker vmobject_locker(vmobject().m_lock);
    auto& page_slot = physical_page_slot(page_index_in_region);
    if (page_slot->is_lazy_committed_page()) {
        auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
        VERIFY(m_vmobject->is_anonymous());
        page_slot = static_cast<AnonymousVMObject&>(*m_vmobject).allocate_committed_page({});
        if (!remap_vmobject_page(page_index_in_vmobject, *page_slot))
            return PageFaultResponse::OutOfMemory;
        return PageFaultResponse::Continue;
    }

    dbgln("Unexpected page fault in Region({})[{}] at {}", this, page_index_in_region, fault.vaddr());
    return PageFaultResponse::ShouldCrash;
#endif
}

PageFaultResponse Region::handle_zero_fault(size_t page_index_in_region, PhysicalRAMPage& page_in_slot_at_time_of_fault)
{
    VERIFY(vmobject().is_anonymous());

    auto& anonymous_vmobject = static_cast<AnonymousVMObject&>(vmobject());
    auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);

    auto current_thread = Thread::current();
    if (current_thread != nullptr)
        current_thread->did_zero_fault();

    RefPtr<PhysicalRAMPage> new_physical_page;

    if (page_in_slot_at_time_of_fault.is_lazy_committed_page()) {
        new_physical_page = anonymous_vmobject.allocate_committed_page({});
        dbgln_if(PAGE_FAULT_DEBUG, "      >> ALLOCATED COMMITTED {}", new_physical_page->paddr());
    } else {
        auto page_or_error = MM.allocate_physical_page(MemoryManager::ShouldZeroFill::Yes);
        if (page_or_error.is_error()) {
            dmesgln("MM: handle_zero_fault was unable to allocate a physical page");
            return PageFaultResponse::OutOfMemory;
        }
        new_physical_page = page_or_error.release_value();
        dbgln_if(PAGE_FAULT_DEBUG, "      >> ALLOCATED {}", new_physical_page->paddr());
    }

    {
        SpinlockLocker locker(anonymous_vmobject.m_lock);
        auto& page_slot = physical_page_slot(page_index_in_region);
        bool already_handled = !page_slot.is_null() && !page_slot->is_shared_zero_page() && !page_slot->is_lazy_committed_page();
        if (already_handled) {
            // Someone else already faulted in a new page in this slot. That's fine, we'll just remap with their page.
            new_physical_page = page_slot;
        } else {
            // Install the newly allocated page into the VMObject.
            page_slot = new_physical_page;
        }
    }

    if (m_shared) {
        if (!anonymous_vmobject.remap_regions_one_page(page_index_in_vmobject, *new_physical_page)) {
            dmesgln("MM: handle_zero_fault was unable to allocate a physical page");
            return PageFaultResponse::OutOfMemory;
        }
    } else {
        if (!remap_vmobject_page(page_index_in_vmobject, *new_physical_page)) {
            dmesgln("MM: handle_zero_fault was unable to allocate a physical page");
            return PageFaultResponse::OutOfMemory;
        }
    }
    return PageFaultResponse::Continue;
}

PageFaultResponse Region::handle_cow_fault(size_t page_index_in_region)
{
    auto current_thread = Thread::current();
    if (current_thread)
        current_thread->did_cow_fault();

    if (!vmobject().is_anonymous())
        return PageFaultResponse::ShouldCrash;

    VERIFY(!m_shared);

    auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
    auto response = reinterpret_cast<AnonymousVMObject&>(vmobject()).handle_cow_fault(page_index_in_vmobject, vaddr().offset(page_index_in_region * PAGE_SIZE));
    if (!remap_vmobject_page(page_index_in_vmobject, *vmobject().physical_pages()[page_index_in_vmobject]))
        return PageFaultResponse::OutOfMemory;
    return response;
}

PageFaultResponse Region::handle_inode_fault(size_t page_index_in_region, bool mark_page_dirty)
{
    VERIFY(vmobject().is_inode());
    VERIFY(!g_scheduler_lock.is_locked_by_current_processor());

    auto& inode_vmobject = static_cast<InodeVMObject&>(vmobject());
    auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
    auto& physical_page_slot = inode_vmobject.physical_pages()[page_index_in_vmobject];

    {
        // NOTE: The VMObject lock is required when manipulating the VMObject's physical page slot.
        SpinlockLocker locker(inode_vmobject.m_lock);

        if (!physical_page_slot.is_null()) {
            dbgln_if(PAGE_FAULT_DEBUG, "handle_inode_fault: Page faulted in by someone else before reading, remapping.");
            if (mark_page_dirty)
                inode_vmobject.set_page_dirty(page_index_in_vmobject, true);
            if (!remap_vmobject_page(page_index_in_vmobject, *physical_page_slot))
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
    // Note: If we received 0, it means we are at the end of file or after it,
    // which means we should return bus error.
    if (nread == 0)
        return PageFaultResponse::BusError;

    // If we read less than a page, zero out the rest to avoid leaking uninitialized data.
    if (nread < PAGE_SIZE)
        memset(page_buffer + nread, 0, PAGE_SIZE - nread);

    // Allocate a new physical page, and copy the read inode contents into it.
    auto new_physical_page_or_error = MM.allocate_physical_page(MemoryManager::ShouldZeroFill::No);
    if (new_physical_page_or_error.is_error()) {
        dmesgln("MM: handle_inode_fault was unable to allocate a physical page");
        return PageFaultResponse::OutOfMemory;
    }
    auto new_physical_page = new_physical_page_or_error.release_value();
    {
        InterruptDisabler disabler;
        u8* dest_ptr = MM.quickmap_page(*new_physical_page);
        memcpy(dest_ptr, page_buffer, PAGE_SIZE);

        if (is_executable()) {
            // Some architectures require an explicit synchronization operation after writing to memory that will be executed.
            // This is required even if no instructions were previously fetched from that (physical) memory location,
            // because some systems have an I-cache that is not coherent with the D-cache,
            // resulting in the I-cache being filled with old values if the contents of the D-cache aren't written back yet.
            Processor::flush_instruction_cache(VirtualAddress { dest_ptr }, PAGE_SIZE);
        }

        MM.unquickmap_page();
    }

    {
        SpinlockLocker locker(inode_vmobject.m_lock);

        // Someone else can assign a new page before we get here, so check if physical_page_slot is still null.
        if (physical_page_slot.is_null()) {
            physical_page_slot = new_physical_page;
            // Something went wrong if a newly loaded page is already marked dirty
            VERIFY(!inode_vmobject.is_page_dirty(page_index_in_vmobject));
        } else {
            dbgln_if(PAGE_FAULT_DEBUG, "handle_inode_fault: Page faulted in by someone else, remapping.");
        }

        if (mark_page_dirty)
            inode_vmobject.set_page_dirty(page_index_in_vmobject, true);
        if (!remap_vmobject_page(page_index_in_vmobject, *physical_page_slot))
            return PageFaultResponse::OutOfMemory;
        return PageFaultResponse::Continue;
    }
}

PageFaultResponse Region::handle_dirty_on_write_fault(size_t page_index_in_region)
{
    VERIFY(vmobject().is_inode());
    auto& inode_vmobject = static_cast<InodeVMObject&>(vmobject());
    auto page_index_in_vmobject = translate_to_vmobject_page(page_index_in_region);
    auto& physical_page_slot = inode_vmobject.physical_pages()[page_index_in_vmobject];

    {
        SpinlockLocker locker(inode_vmobject.m_lock);

        if (!physical_page_slot.is_null()) {
            dbgln_if(PAGE_FAULT_DEBUG, "handle_dirty_on_write_fault: Marking page dirty and remapping.");
            inode_vmobject.set_page_dirty(page_index_in_vmobject, true);
            if (!remap_vmobject_page(page_index_in_vmobject, *physical_page_slot))
                return PageFaultResponse::OutOfMemory;
            return PageFaultResponse::Continue;
        }
    }

    // Clean page was purged before we held the lock.
    // Handle this like a page-not-present fault, except we mark the page as dirty when we remap it
    dbgln_if(PAGE_FAULT_DEBUG, "handle_dirty_on_write_fault: Page was purged by someone else, calling handle_inode_fault to load the page and mark it dirty.");
    return handle_inode_fault(page_index_in_region, true);
}

RefPtr<PhysicalRAMPage> Region::physical_page(size_t index) const
{
    SpinlockLocker vmobject_locker(vmobject().m_lock);
    VERIFY(index < page_count());
    return vmobject().physical_pages()[first_page_index() + index];
}

RefPtr<PhysicalRAMPage>& Region::physical_page_slot(size_t index)
{
    VERIFY(vmobject().m_lock.is_locked_by_current_processor());
    VERIFY(index < page_count());
    return vmobject().physical_pages()[first_page_index() + index];
}

}
