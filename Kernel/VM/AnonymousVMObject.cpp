/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/SmapDisabler.h>
#include <Kernel/Debug.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

RefPtr<VMObject> AnonymousVMObject::try_clone()
{
    // We need to acquire our lock so we copy a sane state
    ScopedSpinLock lock(m_lock);

    if (is_purgeable() && is_volatile()) {
        // If this object is purgeable+volatile, create a new zero-filled purgeable+volatile
        // object, effectively "pre-purging" it in the child process.
        auto clone = try_create_purgeable_with_size(size(), AllocationStrategy::None);
        if (!clone)
            return {};
        clone->m_volatile = true;
        return clone;
    }

    // We're the parent. Since we're about to become COW we need to
    // commit the number of pages that we need to potentially allocate
    // so that the parent is still guaranteed to be able to have all
    // non-volatile memory available.
    size_t new_cow_pages_needed = page_count();

    dbgln_if(COMMIT_DEBUG, "Cloning {:p}, need {} committed cow pages", this, new_cow_pages_needed);

    if (!MM.commit_user_physical_pages(new_cow_pages_needed))
        return {};

    // Create or replace the committed cow pages. When cloning a previously
    // cloned vmobject, we want to essentially "fork", leaving us and the
    // new clone with one set of shared committed cow pages, and the original
    // one would keep the one it still has. This ensures that the original
    // one and this one, as well as the clone have sufficient resources
    // to cow all pages as needed
    m_shared_committed_cow_pages = try_create<CommittedCowPages>(new_cow_pages_needed);

    if (!m_shared_committed_cow_pages) {
        MM.uncommit_user_physical_pages(new_cow_pages_needed);
        return {};
    }

    // Both original and clone become COW. So create a COW map for ourselves
    // or reset all pages to be copied again if we were previously cloned
    ensure_or_reset_cow_map();

    // FIXME: If this allocation fails, we need to rollback all changes.
    return adopt_ref_if_nonnull(new (nothrow) AnonymousVMObject(*this));
}

RefPtr<AnonymousVMObject> AnonymousVMObject::try_create_with_size(size_t size, AllocationStrategy commit)
{
    if (commit == AllocationStrategy::Reserve || commit == AllocationStrategy::AllocateNow) {
        // We need to attempt to commit before actually creating the object
        if (!MM.commit_user_physical_pages(ceil_div(size, static_cast<size_t>(PAGE_SIZE))))
            return {};
    }
    return adopt_ref_if_nonnull(new (nothrow) AnonymousVMObject(size, commit));
}

RefPtr<AnonymousVMObject> AnonymousVMObject::try_create_physically_contiguous_with_size(size_t size)
{
    auto contiguous_physical_pages = MM.allocate_contiguous_supervisor_physical_pages(size);
    if (contiguous_physical_pages.is_empty())
        return {};
    return adopt_ref_if_nonnull(new (nothrow) AnonymousVMObject(contiguous_physical_pages.span()));
}

RefPtr<AnonymousVMObject> AnonymousVMObject::try_create_purgeable_with_size(size_t size, AllocationStrategy commit)
{
    if (commit == AllocationStrategy::Reserve || commit == AllocationStrategy::AllocateNow) {
        // We need to attempt to commit before actually creating the object
        if (!MM.commit_user_physical_pages(ceil_div(size, static_cast<size_t>(PAGE_SIZE))))
            return {};
    }
    auto vmobject = adopt_ref_if_nonnull(new (nothrow) AnonymousVMObject(size, commit));
    if (!vmobject)
        return {};
    vmobject->m_purgeable = true;
    return vmobject;
}

RefPtr<AnonymousVMObject> AnonymousVMObject::try_create_with_physical_pages(Span<NonnullRefPtr<PhysicalPage>> physical_pages)
{
    return adopt_ref_if_nonnull(new (nothrow) AnonymousVMObject(physical_pages));
}

RefPtr<AnonymousVMObject> AnonymousVMObject::try_create_for_physical_range(PhysicalAddress paddr, size_t size)
{
    if (paddr.offset(size) < paddr) {
        dbgln("Shenanigans! try_create_for_physical_range({}, {}) would wrap around", paddr, size);
        return nullptr;
    }
    return adopt_ref_if_nonnull(new (nothrow) AnonymousVMObject(paddr, size));
}

AnonymousVMObject::AnonymousVMObject(size_t size, AllocationStrategy strategy)
    : VMObject(size)
    , m_unused_committed_pages(strategy == AllocationStrategy::Reserve ? page_count() : 0)
{
    if (strategy == AllocationStrategy::AllocateNow) {
        // Allocate all pages right now. We know we can get all because we committed the amount needed
        for (size_t i = 0; i < page_count(); ++i)
            physical_pages()[i] = MM.allocate_committed_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
    } else {
        auto& initial_page = (strategy == AllocationStrategy::Reserve) ? MM.lazy_committed_page() : MM.shared_zero_page();
        for (size_t i = 0; i < page_count(); ++i)
            physical_pages()[i] = initial_page;
    }
}

AnonymousVMObject::AnonymousVMObject(PhysicalAddress paddr, size_t size)
    : VMObject(size)
{
    VERIFY(paddr.page_base() == paddr);
    for (size_t i = 0; i < page_count(); ++i)
        physical_pages()[i] = PhysicalPage::create(paddr.offset(i * PAGE_SIZE), MayReturnToFreeList::No);
}

AnonymousVMObject::AnonymousVMObject(Span<NonnullRefPtr<PhysicalPage>> physical_pages)
    : VMObject(physical_pages.size() * PAGE_SIZE)
{
    for (size_t i = 0; i < physical_pages.size(); ++i) {
        m_physical_pages[i] = physical_pages[i];
    }
}

AnonymousVMObject::AnonymousVMObject(AnonymousVMObject const& other)
    : VMObject(other)
    , m_unused_committed_pages(other.m_unused_committed_pages)
    , m_cow_map()                                                      // do *not* clone this
    , m_shared_committed_cow_pages(other.m_shared_committed_cow_pages) // share the pool
{
    // We can't really "copy" a spinlock. But we're holding it. Clear in the clone
    VERIFY(other.m_lock.is_locked());
    m_lock.initialize();

    // The clone also becomes COW
    ensure_or_reset_cow_map();

    if (m_unused_committed_pages > 0) {
        // The original vmobject didn't use up all committed pages. When
        // cloning (fork) we will overcommit. For this purpose we drop all
        // lazy-commit references and replace them with shared zero pages.
        for (size_t i = 0; i < page_count(); i++) {
            auto& phys_page = m_physical_pages[i];
            if (phys_page && phys_page->is_lazy_committed_page()) {
                phys_page = MM.shared_zero_page();
                if (--m_unused_committed_pages == 0)
                    break;
            }
        }
        VERIFY(m_unused_committed_pages == 0);
    }
}

AnonymousVMObject::~AnonymousVMObject()
{
    // Return any unused committed pages
    if (m_unused_committed_pages > 0)
        MM.uncommit_user_physical_pages(m_unused_committed_pages);
}

size_t AnonymousVMObject::purge()
{
    ScopedSpinLock lock(m_lock);

    if (!is_purgeable() || !is_volatile())
        return 0;

    size_t total_pages_purged = 0;

    for (auto& page : m_physical_pages) {
        VERIFY(page);
        if (page->is_shared_zero_page())
            continue;
        page = MM.shared_zero_page();
        ++total_pages_purged;
    }

    m_was_purged = true;

    for_each_region([](Region& region) {
        region.remap();
    });

    return total_pages_purged;
}

KResult AnonymousVMObject::set_volatile(bool is_volatile, bool& was_purged)
{
    VERIFY(is_purgeable());

    ScopedSpinLock locker(m_lock);

    was_purged = m_was_purged;
    if (m_volatile == is_volatile)
        return KSuccess;

    if (is_volatile) {
        // When a VMObject is made volatile, it gives up all of its committed memory.
        // Any physical pages already allocated remain in the VMObject for now, but the kernel is free to take them at any moment.
        for (auto& page : m_physical_pages) {
            if (page && page->is_lazy_committed_page())
                page = MM.shared_zero_page();
        }

        if (m_unused_committed_pages) {
            MM.uncommit_user_physical_pages(m_unused_committed_pages);
            m_unused_committed_pages = 0;
        }

        m_shared_committed_cow_pages = nullptr;

        if (!m_cow_map.is_null())
            m_cow_map = {};

        m_volatile = true;
        m_was_purged = false;
        return KSuccess;
    }
    // When a VMObject is made non-volatile, we try to commit however many pages are not currently available.
    // If that fails, we return false to indicate that memory allocation failed.
    size_t committed_pages_needed = 0;
    for (auto& page : m_physical_pages) {
        VERIFY(page);
        if (page->is_shared_zero_page())
            ++committed_pages_needed;
    }

    if (!committed_pages_needed) {
        m_volatile = false;
        return KSuccess;
    }

    if (!MM.commit_user_physical_pages(committed_pages_needed))
        return ENOMEM;

    m_unused_committed_pages = committed_pages_needed;

    for (auto& page : m_physical_pages) {
        if (page->is_shared_zero_page())
            page = MM.lazy_committed_page();
    }

    m_volatile = false;
    m_was_purged = false;
    return KSuccess;
}

NonnullRefPtr<PhysicalPage> AnonymousVMObject::allocate_committed_page(Badge<Region>)
{
    {
        ScopedSpinLock lock(m_lock);
        VERIFY(m_unused_committed_pages > 0);
        --m_unused_committed_pages;
    }
    return MM.allocate_committed_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
}

Bitmap& AnonymousVMObject::ensure_cow_map()
{
    if (m_cow_map.is_null())
        m_cow_map = Bitmap { page_count(), true };
    return m_cow_map;
}

void AnonymousVMObject::ensure_or_reset_cow_map()
{
    if (m_cow_map.is_null())
        ensure_cow_map();
    else
        m_cow_map.fill(true);
}

bool AnonymousVMObject::should_cow(size_t page_index, bool is_shared) const
{
    auto& page = physical_pages()[page_index];
    if (page && (page->is_shared_zero_page() || page->is_lazy_committed_page()))
        return true;
    if (is_shared)
        return false;
    return !m_cow_map.is_null() && m_cow_map.get(page_index);
}

void AnonymousVMObject::set_should_cow(size_t page_index, bool cow)
{
    ensure_cow_map().set(page_index, cow);
}

size_t AnonymousVMObject::cow_pages() const
{
    if (m_cow_map.is_null())
        return 0;
    return m_cow_map.count_slow(true);
}

PageFaultResponse AnonymousVMObject::handle_cow_fault(size_t page_index, VirtualAddress vaddr)
{
    VERIFY_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(m_lock);

    if (is_volatile()) {
        // A COW fault in a volatile region? Userspace is writing to volatile memory, this is a bug. Crash.
        dbgln("COW fault in volatile region, will crash.");
        return PageFaultResponse::ShouldCrash;
    }

    auto& page_slot = physical_pages()[page_index];

    if (page_slot->ref_count() == 1) {
        dbgln_if(PAGE_FAULT_DEBUG, "    >> It's a COW page but nobody is sharing it anymore. Remap r/w");
        set_should_cow(page_index, false);

        // If we were sharing committed COW pages with another process, and the other process
        // has exhausted the supply, we can stop counting the shared pages.
        if (m_shared_committed_cow_pages && m_shared_committed_cow_pages->is_empty())
            m_shared_committed_cow_pages = nullptr;

        return PageFaultResponse::Continue;
    }

    RefPtr<PhysicalPage> page;
    if (m_shared_committed_cow_pages) {
        dbgln_if(PAGE_FAULT_DEBUG, "    >> It's a committed COW page and it's time to COW!");
        page = m_shared_committed_cow_pages->allocate_one();
    } else {
        dbgln_if(PAGE_FAULT_DEBUG, "    >> It's a COW page and it's time to COW!");
        page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::No);
        if (page.is_null()) {
            dmesgln("MM: handle_cow_fault was unable to allocate a physical page");
            return PageFaultResponse::OutOfMemory;
        }
    }

    u8* dest_ptr = MM.quickmap_page(*page);
    dbgln_if(PAGE_FAULT_DEBUG, "      >> COW {} <- {}", page->paddr(), page_slot->paddr());
    {
        SmapDisabler disabler;
        void* fault_at;
        if (!safe_memcpy(dest_ptr, vaddr.as_ptr(), PAGE_SIZE, fault_at)) {
            if ((u8*)fault_at >= dest_ptr && (u8*)fault_at <= dest_ptr + PAGE_SIZE)
                dbgln("      >> COW: error copying page {}/{} to {}/{}: failed to write to page at {}",
                    page_slot->paddr(), vaddr, page->paddr(), VirtualAddress(dest_ptr), VirtualAddress(fault_at));
            else if ((u8*)fault_at >= vaddr.as_ptr() && (u8*)fault_at <= vaddr.as_ptr() + PAGE_SIZE)
                dbgln("      >> COW: error copying page {}/{} to {}/{}: failed to read from page at {}",
                    page_slot->paddr(), vaddr, page->paddr(), VirtualAddress(dest_ptr), VirtualAddress(fault_at));
            else
                VERIFY_NOT_REACHED();
        }
    }
    page_slot = move(page);
    MM.unquickmap_page();
    set_should_cow(page_index, false);
    return PageFaultResponse::Continue;
}

CommittedCowPages::CommittedCowPages(size_t committed_pages)
    : m_committed_pages(committed_pages)
{
}

CommittedCowPages::~CommittedCowPages()
{
    // Return unused committed pages
    if (m_committed_pages > 0)
        MM.uncommit_user_physical_pages(m_committed_pages);
}

NonnullRefPtr<PhysicalPage> CommittedCowPages::allocate_one()
{
    VERIFY(m_committed_pages > 0);
    m_committed_pages--;

    return MM.allocate_committed_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
}

}
