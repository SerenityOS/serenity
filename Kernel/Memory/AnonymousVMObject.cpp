/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/SafeMem.h>
#include <Kernel/Arch/SmapDisabler.h>
#include <Kernel/Debug.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Memory/PhysicalRAMPage.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel::Memory {

ErrorOr<NonnullLockRefPtr<VMObject>> AnonymousVMObject::try_clone()
{
    // We need to acquire our lock so we copy a sane state
    SpinlockLocker lock(m_lock);

    if (is_purgeable() && is_volatile()) {
        // If this object is purgeable+volatile, create a new zero-filled purgeable+volatile
        // object, effectively "pre-purging" it in the child process.
        auto clone = TRY(try_create_purgeable_with_size(size(), AllocationStrategy::None));
        clone->m_volatile = true;
        clone->m_was_purged = true;
        return clone;
    }

    // We're the parent. Since we're about to become COW we need to
    // commit the number of pages that we need to potentially allocate
    // so that the parent is still guaranteed to be able to have all
    // non-volatile memory available.
    size_t new_cow_pages_needed = 0;
    for (auto const& page : m_physical_pages) {
        if (!page->is_shared_zero_page() && !page->is_lazy_committed_page())
            ++new_cow_pages_needed;
    }

    if (new_cow_pages_needed == 0)
        return TRY(try_create_with_size(size(), AllocationStrategy::None));

    dbgln_if(COMMIT_DEBUG, "Cloning {:p}, need {} committed cow pages", this, new_cow_pages_needed);

    auto committed_pages = TRY(MM.commit_physical_pages(new_cow_pages_needed));

    // Create or replace the committed cow pages. When cloning a previously
    // cloned vmobject, we want to essentially "fork", leaving us and the
    // new clone with one set of shared committed cow pages, and the original
    // one would keep the one it still has. This ensures that the original
    // one and this one, as well as the clone have sufficient resources
    // to cow all pages as needed
    auto new_shared_committed_cow_pages = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) SharedCommittedCowPages(move(committed_pages))));
    auto new_physical_pages = TRY(this->try_clone_physical_pages());
    auto clone = TRY(try_create_with_shared_cow(*this, *new_shared_committed_cow_pages, move(new_physical_pages)));

    // Both original and clone become COW. So create a COW map for ourselves
    // or reset all pages to be copied again if we were previously cloned
    TRY(ensure_or_reset_cow_map());

    m_shared_committed_cow_pages = move(new_shared_committed_cow_pages);

    if (m_unused_committed_pages.has_value() && !m_unused_committed_pages->is_empty()) {
        // The parent vmobject didn't use up all committed pages. When
        // cloning (fork) we will overcommit. For this purpose we drop all
        // lazy-commit references and replace them with shared zero pages.
        for (size_t i = 0; i < page_count(); i++) {
            auto& page = clone->m_physical_pages[i];
            if (page && page->is_lazy_committed_page()) {
                page = MM.shared_zero_page();
            }
        }
    }

    return clone;
}

ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> AnonymousVMObject::try_create_with_size(size_t size, AllocationStrategy strategy)
{
    Optional<CommittedPhysicalPageSet> committed_pages;
    if (strategy == AllocationStrategy::Reserve || strategy == AllocationStrategy::AllocateNow) {
        committed_pages = TRY(MM.commit_physical_pages(ceil_div(size, static_cast<size_t>(PAGE_SIZE))));
    }

    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(size));

    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) AnonymousVMObject(move(new_physical_pages), strategy, move(committed_pages)));
}

ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> AnonymousVMObject::try_create_physically_contiguous_with_size(size_t size, MemoryType memory_type_for_zero_fill)
{
    auto contiguous_physical_pages = TRY(MM.allocate_contiguous_physical_pages(size, memory_type_for_zero_fill));

    auto new_physical_pages = TRY(FixedArray<RefPtr<PhysicalRAMPage>>::create(contiguous_physical_pages.span()));

    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) AnonymousVMObject(move(new_physical_pages)));
}

ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> AnonymousVMObject::try_create_purgeable_with_size(size_t size, AllocationStrategy strategy)
{
    auto vmobject = TRY(try_create_with_size(size, strategy));
    vmobject->m_purgeable = true;
    return vmobject;
}

ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> AnonymousVMObject::try_create_with_physical_pages(Span<NonnullRefPtr<PhysicalRAMPage>> physical_pages)
{
    auto new_physical_pages = TRY(FixedArray<RefPtr<PhysicalRAMPage>>::create(physical_pages));
    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) AnonymousVMObject(move(new_physical_pages)));
}

ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> AnonymousVMObject::try_create_for_physical_range(PhysicalAddress paddr, size_t size)
{
    if (paddr.offset(size) < paddr) {
        dbgln("Shenanigans! try_create_for_physical_range({}, {}) would wrap around", paddr, size);
        // Since we can't wrap around yet, let's pretend to OOM.
        return ENOMEM;
    }

    auto new_physical_pages = TRY(VMObject::try_create_physical_pages(size));

    return adopt_nonnull_lock_ref_or_enomem(new (nothrow) AnonymousVMObject(paddr, move(new_physical_pages)));
}

ErrorOr<NonnullLockRefPtr<AnonymousVMObject>> AnonymousVMObject::try_create_with_shared_cow(AnonymousVMObject const& other, NonnullLockRefPtr<SharedCommittedCowPages> shared_committed_cow_pages, FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages)
{
    auto weak_parent = TRY(other.try_make_weak_ptr<AnonymousVMObject>());
    auto vmobject = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) AnonymousVMObject(move(weak_parent), move(shared_committed_cow_pages), move(new_physical_pages))));

    TRY(vmobject->ensure_cow_map());

    return vmobject;
}

AnonymousVMObject::AnonymousVMObject(FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages, AllocationStrategy strategy, Optional<CommittedPhysicalPageSet> committed_pages)
    : VMObject(move(new_physical_pages))
    , m_unused_committed_pages(move(committed_pages))
{
    if (strategy == AllocationStrategy::AllocateNow) {
        // Allocate all pages right now. We know we can get all because we committed the amount needed
        for (size_t i = 0; i < page_count(); ++i)
            physical_pages()[i] = m_unused_committed_pages->take_one();
    } else {
        auto& initial_page = (strategy == AllocationStrategy::Reserve) ? MM.lazy_committed_page() : MM.shared_zero_page();
        for (size_t i = 0; i < page_count(); ++i)
            physical_pages()[i] = initial_page;
    }
}

AnonymousVMObject::AnonymousVMObject(PhysicalAddress paddr, FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages)
    : VMObject(move(new_physical_pages))
{
    VERIFY(paddr.page_base() == paddr);
    for (size_t i = 0; i < page_count(); ++i)
        physical_pages()[i] = PhysicalRAMPage::create(paddr.offset(i * PAGE_SIZE), MayReturnToFreeList::No);
}

AnonymousVMObject::AnonymousVMObject(FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages)
    : VMObject(move(new_physical_pages))
{
}

AnonymousVMObject::AnonymousVMObject(LockWeakPtr<AnonymousVMObject> other, NonnullLockRefPtr<SharedCommittedCowPages> shared_committed_cow_pages, FixedArray<RefPtr<PhysicalRAMPage>>&& new_physical_pages)
    : VMObject(move(new_physical_pages))
    , m_cow_parent(move(other))
    , m_shared_committed_cow_pages(move(shared_committed_cow_pages))
    , m_purgeable(m_cow_parent.strong_ref()->m_purgeable)
{
}

AnonymousVMObject::~AnonymousVMObject()
{
    if (!m_shared_committed_cow_pages || m_shared_committed_cow_pages->is_empty())
        return;
    auto cow_parent = m_cow_parent.strong_ref();
    if (!cow_parent)
        return;
    SpinlockLocker lock(cow_parent->m_lock);
    if (cow_parent->m_shared_committed_cow_pages == m_shared_committed_cow_pages)
        cow_parent->m_shared_committed_cow_pages.clear();
}

size_t AnonymousVMObject::purge()
{
    SpinlockLocker lock(m_lock);

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

    remap_regions();

    return total_pages_purged;
}

ErrorOr<void> AnonymousVMObject::set_volatile(bool is_volatile, bool& was_purged)
{
    VERIFY(is_purgeable());

    SpinlockLocker locker(m_lock);

    was_purged = m_was_purged;
    if (m_volatile == is_volatile)
        return {};

    if (is_volatile) {
        // When a VMObject is made volatile, it gives up all of its committed memory.
        // Any physical pages already allocated remain in the VMObject for now, but the kernel is free to take them at any moment.
        for (auto& page : m_physical_pages) {
            if (page && page->is_lazy_committed_page())
                page = MM.shared_zero_page();
        }

        m_unused_committed_pages = {};
        m_shared_committed_cow_pages = nullptr;

        if (!m_cow_map.is_null())
            m_cow_map = {};

        m_volatile = true;
        m_was_purged = false;

        remap_regions();
        return {};
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
        return {};
    }

    m_unused_committed_pages = TRY(MM.commit_physical_pages(committed_pages_needed));

    for (auto& page : m_physical_pages) {
        if (page->is_shared_zero_page())
            page = MM.lazy_committed_page();
    }

    m_volatile = false;
    m_was_purged = false;
    remap_regions();
    return {};
}

NonnullRefPtr<PhysicalRAMPage> AnonymousVMObject::allocate_committed_page(Badge<Region>)
{
    return m_unused_committed_pages->take_one();
}

void AnonymousVMObject::reset_cow_map()
{
    for (size_t i = 0; i < page_count(); ++i) {
        auto& page = physical_pages()[i];
        bool should_cow = !page->is_shared_zero_page() && !page->is_lazy_committed_page();
        m_cow_map.set(i, should_cow);
    }
}

ErrorOr<void> AnonymousVMObject::ensure_cow_map()
{
    if (m_cow_map.is_null()) {
        m_cow_map = TRY(Bitmap::create(page_count(), true));
        reset_cow_map();
    }
    return {};
}

ErrorOr<void> AnonymousVMObject::ensure_or_reset_cow_map()
{
    if (m_cow_map.is_null())
        TRY(ensure_cow_map());
    else
        reset_cow_map();
    return {};
}

bool AnonymousVMObject::should_cow(size_t page_index, bool is_shared) const
{
    auto const& page = physical_pages()[page_index];
    if (page && (page->is_shared_zero_page() || page->is_lazy_committed_page()))
        return true;
    if (is_shared)
        return false;
    return !m_cow_map.is_null() && m_cow_map.get(page_index);
}

ErrorOr<void> AnonymousVMObject::set_should_cow(size_t page_index, bool cow)
{
    TRY(ensure_cow_map());
    m_cow_map.set(page_index, cow);
    return {};
}

size_t AnonymousVMObject::cow_pages() const
{
    if (m_cow_map.is_null())
        return 0;
    return m_cow_map.count_slow(true);
}

PageFaultResponse AnonymousVMObject::handle_cow_fault(size_t page_index, VirtualAddress vaddr)
{
    SpinlockLocker lock(m_lock);

    if (is_volatile()) {
        // A COW fault in a volatile region? Userspace is writing to volatile memory, this is a bug. Crash.
        dbgln("COW fault in volatile region, will crash.");
        return PageFaultResponse::ShouldCrash;
    }

    auto& page_slot = physical_pages()[page_index];

    // If we were sharing committed COW pages with another process, and the other process
    // has exhausted the supply, we can stop counting the shared pages.
    if (m_shared_committed_cow_pages && m_shared_committed_cow_pages->is_empty())
        m_shared_committed_cow_pages = nullptr;

    if (page_slot->ref_count() == 1) {
        dbgln_if(PAGE_FAULT_DEBUG, "    >> It's a COW page but nobody is sharing it anymore. Remap r/w");
        MUST(set_should_cow(page_index, false)); // If we received a COW fault, we already have a cow map allocated, so this is infallible

        if (m_shared_committed_cow_pages) {
            m_shared_committed_cow_pages->uncommit_one();
            if (m_shared_committed_cow_pages->is_empty())
                m_shared_committed_cow_pages = nullptr;
        }
        return PageFaultResponse::Continue;
    }

    RefPtr<PhysicalRAMPage> page;
    if (m_shared_committed_cow_pages) {
        dbgln_if(PAGE_FAULT_DEBUG, "    >> It's a committed COW page and it's time to COW!");
        page = m_shared_committed_cow_pages->take_one();
    } else {
        dbgln_if(PAGE_FAULT_DEBUG, "    >> It's a COW page and it's time to COW!");
        auto page_or_error = MM.allocate_physical_page(MemoryManager::ShouldZeroFill::No);
        if (page_or_error.is_error()) {
            dmesgln("MM: handle_cow_fault was unable to allocate a physical page");
            return PageFaultResponse::OutOfMemory;
        }
        page = page_or_error.release_value();
    }

    dbgln_if(PAGE_FAULT_DEBUG, "      >> COW {} <- {}", page->paddr(), page_slot->paddr());
    {
        u8* dest_ptr = MM.quickmap_page(*page);
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
        MM.unquickmap_page();
    }
    page_slot = move(page);
    MUST(set_should_cow(page_index, false)); // If we received a COW fault, we already have a cow map allocated, so this is infallible
    return PageFaultResponse::Continue;
}

AnonymousVMObject::SharedCommittedCowPages::SharedCommittedCowPages(CommittedPhysicalPageSet&& committed_pages)
    : m_committed_pages(move(committed_pages))
{
}

AnonymousVMObject::SharedCommittedCowPages::~SharedCommittedCowPages() = default;

NonnullRefPtr<PhysicalRAMPage> AnonymousVMObject::SharedCommittedCowPages::take_one()
{
    SpinlockLocker locker(m_lock);
    return m_committed_pages.take_one();
}

void AnonymousVMObject::SharedCommittedCowPages::uncommit_one()
{
    SpinlockLocker locker(m_lock);
    m_committed_pages.uncommit_one();
}

}
