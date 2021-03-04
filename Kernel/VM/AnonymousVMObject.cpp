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

#include <Kernel/Arch/x86/SmapDisabler.h>
#include <Kernel/Debug.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>

namespace Kernel {

RefPtr<VMObject> AnonymousVMObject::clone()
{
    // We need to acquire our lock so we copy a sane state
    ScopedSpinLock lock(m_lock);

    // We're the parent. Since we're about to become COW we need to
    // commit the number of pages that we need to potentially allocate
    // so that the parent is still guaranteed to be able to have all
    // non-volatile memory available.
    size_t need_cow_pages = 0;
    {
        // We definitely need to commit non-volatile areas
        for_each_nonvolatile_range([&](const VolatilePageRange& nonvolatile_range) {
            need_cow_pages += nonvolatile_range.count;
            return IterationDecision::Continue;
        });
    }

#if COMMIT_DEBUG
    klog() << "Cloning " << this << ", need " << need_cow_pages << " committed cow pages";
#endif
    if (!MM.commit_user_physical_pages(need_cow_pages))
        return {};
    // Create or replace the committed cow pages. When cloning a previously
    // cloned vmobject, we want to essentially "fork", leaving us and the
    // new clone with one set of shared committed cow pages, and the original
    // one would keep the one it still has. This ensures that the original
    // one and this one, as well as the clone have sufficient resources
    // to cow all pages as needed
    m_shared_committed_cow_pages = adopt(*new CommittedCowPages(need_cow_pages));

    // Both original and clone become COW. So create a COW map for ourselves
    // or reset all pages to be copied again if we were previously cloned
    ensure_or_reset_cow_map();

    return adopt(*new AnonymousVMObject(*this));
}

RefPtr<AnonymousVMObject> AnonymousVMObject::create_with_size(size_t size, AllocationStrategy commit)
{
    if (commit == AllocationStrategy::Reserve || commit == AllocationStrategy::AllocateNow) {
        // We need to attempt to commit before actually creating the object
        if (!MM.commit_user_physical_pages(ceil_div(size, static_cast<size_t>(PAGE_SIZE))))
            return {};
    }
    return adopt(*new AnonymousVMObject(size, commit));
}

NonnullRefPtr<AnonymousVMObject> AnonymousVMObject::create_with_physical_page(PhysicalPage& page)
{
    return adopt(*new AnonymousVMObject(page));
}

RefPtr<AnonymousVMObject> AnonymousVMObject::create_for_physical_range(PhysicalAddress paddr, size_t size)
{
    if (paddr.offset(size) < paddr) {
        dbgln("Shenanigans! create_for_physical_range({}, {}) would wrap around", paddr, size);
        return nullptr;
    }
    return adopt(*new AnonymousVMObject(paddr, size));
}

AnonymousVMObject::AnonymousVMObject(size_t size, AllocationStrategy strategy)
    : VMObject(size)
    , m_volatile_ranges_cache({ 0, page_count() })
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
    , m_volatile_ranges_cache({ 0, page_count() })
{
    VERIFY(paddr.page_base() == paddr);
    for (size_t i = 0; i < page_count(); ++i)
        physical_pages()[i] = PhysicalPage::create(paddr.offset(i * PAGE_SIZE), false, false);
}

AnonymousVMObject::AnonymousVMObject(PhysicalPage& page)
    : VMObject(PAGE_SIZE)
    , m_volatile_ranges_cache({ 0, page_count() })
{
    physical_pages()[0] = page;
}

AnonymousVMObject::AnonymousVMObject(const AnonymousVMObject& other)
    : VMObject(other)
    , m_volatile_ranges_cache({ 0, page_count() }) // do *not* clone this
    , m_volatile_ranges_cache_dirty(true)          // do *not* clone this
    , m_purgeable_ranges()                         // do *not* clone this
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

int AnonymousVMObject::purge()
{
    LOCKER(m_paging_lock);
    return purge_impl();
}

int AnonymousVMObject::purge_with_interrupts_disabled(Badge<MemoryManager>)
{
    VERIFY_INTERRUPTS_DISABLED();
    if (m_paging_lock.is_locked())
        return 0;
    return purge_impl();
}

void AnonymousVMObject::set_was_purged(const VolatilePageRange& range)
{
    VERIFY(m_lock.is_locked());
    for (auto* purgeable_ranges : m_purgeable_ranges)
        purgeable_ranges->set_was_purged(range);
}

int AnonymousVMObject::purge_impl()
{
    int purged_page_count = 0;
    ScopedSpinLock lock(m_lock);
    for_each_volatile_range([&](const auto& range) {
        int purged_in_range = 0;
        auto range_end = range.base + range.count;
        for (size_t i = range.base; i < range_end; i++) {
            auto& phys_page = m_physical_pages[i];
            if (phys_page && !phys_page->is_shared_zero_page()) {
                VERIFY(!phys_page->is_lazy_committed_page());
                ++purged_in_range;
            }
            phys_page = MM.shared_zero_page();
        }

        if (purged_in_range > 0) {
            purged_page_count += purged_in_range;
            set_was_purged(range);
            for_each_region([&](auto& region) {
                if (&region.vmobject() == this) {
                    if (auto owner = region.get_owner()) {
                        // we need to hold a reference the process here (if there is one) as we may not own this region
                        klog() << "Purged " << purged_in_range << " pages from region " << region.name() << " owned by " << *owner << " at " << region.vaddr_from_page_index(range.base) << " - " << region.vaddr_from_page_index(range.base + range.count);
                    } else {
                        klog() << "Purged " << purged_in_range << " pages from region " << region.name() << " (no ownership) at " << region.vaddr_from_page_index(range.base) << " - " << region.vaddr_from_page_index(range.base + range.count);
                    }
                    region.remap_vmobject_page_range(range.base, range.count);
                }
            });
        }
        return IterationDecision::Continue;
    });
    return purged_page_count;
}

void AnonymousVMObject::register_purgeable_page_ranges(PurgeablePageRanges& purgeable_page_ranges)
{
    ScopedSpinLock lock(m_lock);
    purgeable_page_ranges.set_vmobject(this);
    VERIFY(!m_purgeable_ranges.contains_slow(&purgeable_page_ranges));
    m_purgeable_ranges.append(&purgeable_page_ranges);
}

void AnonymousVMObject::unregister_purgeable_page_ranges(PurgeablePageRanges& purgeable_page_ranges)
{
    ScopedSpinLock lock(m_lock);
    for (size_t i = 0; i < m_purgeable_ranges.size(); i++) {
        if (m_purgeable_ranges[i] != &purgeable_page_ranges)
            continue;
        purgeable_page_ranges.set_vmobject(nullptr);
        m_purgeable_ranges.remove(i);
        return;
    }
    VERIFY_NOT_REACHED();
}

bool AnonymousVMObject::is_any_volatile() const
{
    ScopedSpinLock lock(m_lock);
    for (auto& volatile_ranges : m_purgeable_ranges) {
        ScopedSpinLock lock(volatile_ranges->m_volatile_ranges_lock);
        if (!volatile_ranges->is_empty())
            return true;
    }
    return false;
}

size_t AnonymousVMObject::remove_lazy_commit_pages(const VolatilePageRange& range)
{
    VERIFY(m_lock.is_locked());

    size_t removed_count = 0;
    auto range_end = range.base + range.count;
    for (size_t i = range.base; i < range_end; i++) {
        auto& phys_page = m_physical_pages[i];
        if (phys_page && phys_page->is_lazy_committed_page()) {
            phys_page = MM.shared_zero_page();
            removed_count++;
            VERIFY(m_unused_committed_pages > 0);
            if (--m_unused_committed_pages == 0)
                break;
        }
    }
    return removed_count;
}

void AnonymousVMObject::update_volatile_cache()
{
    VERIFY(m_lock.is_locked());
    VERIFY(m_volatile_ranges_cache_dirty);

    m_volatile_ranges_cache.clear();
    for_each_nonvolatile_range([&](const VolatilePageRange& range) {
        m_volatile_ranges_cache.add_unchecked(range);
        return IterationDecision::Continue;
    });

    m_volatile_ranges_cache_dirty = false;
}

void AnonymousVMObject::range_made_volatile(const VolatilePageRange& range)
{
    VERIFY(m_lock.is_locked());

    if (m_unused_committed_pages == 0)
        return;

    // We need to check this range for any pages that are marked for
    // lazy committed allocation and turn them into shared zero pages
    // and also adjust the m_unused_committed_pages for each such page.
    // Take into account all the other views as well.
    size_t uncommit_page_count = 0;
    for_each_volatile_range([&](const auto& r) {
        auto intersected = range.intersected(r);
        if (!intersected.is_empty()) {
            uncommit_page_count += remove_lazy_commit_pages(intersected);
            if (m_unused_committed_pages == 0)
                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    // Return those committed pages back to the system
    if (uncommit_page_count > 0) {
#if COMMIT_DEBUG
        klog() << "Uncommit " << uncommit_page_count << " lazy-commit pages from " << this;
#endif
        MM.uncommit_user_physical_pages(uncommit_page_count);
    }

    m_volatile_ranges_cache_dirty = true;
}

void AnonymousVMObject::range_made_nonvolatile(const VolatilePageRange&)
{
    VERIFY(m_lock.is_locked());
    m_volatile_ranges_cache_dirty = true;
}

size_t AnonymousVMObject::count_needed_commit_pages_for_nonvolatile_range(const VolatilePageRange& range)
{
    VERIFY(m_lock.is_locked());
    VERIFY(!range.is_empty());

    size_t need_commit_pages = 0;
    auto range_end = range.base + range.count;
    for (size_t page_index = range.base; page_index < range_end; page_index++) {
        // COW pages are accounted for in m_shared_committed_cow_pages
        if (!m_cow_map.is_null() && m_cow_map.get(page_index))
            continue;
        auto& phys_page = m_physical_pages[page_index];
        if (phys_page && phys_page->is_shared_zero_page())
            need_commit_pages++;
    }
    return need_commit_pages;
}

size_t AnonymousVMObject::mark_committed_pages_for_nonvolatile_range(const VolatilePageRange& range, size_t mark_total)
{
    VERIFY(m_lock.is_locked());
    VERIFY(!range.is_empty());
    VERIFY(mark_total > 0);

    size_t pages_updated = 0;
    auto range_end = range.base + range.count;
    for (size_t page_index = range.base; page_index < range_end; page_index++) {
        // COW pages are accounted for in m_shared_committed_cow_pages
        if (!m_cow_map.is_null() && m_cow_map.get(page_index))
            continue;
        auto& phys_page = m_physical_pages[page_index];
        if (phys_page && phys_page->is_shared_zero_page()) {
            phys_page = MM.lazy_committed_page();
            if (++pages_updated == mark_total)
                break;
        }
    }

#if COMMIT_DEBUG
    klog() << "Added " << pages_updated << " lazy-commit pages to " << this;
#endif
    m_unused_committed_pages += pages_updated;
    return pages_updated;
}

RefPtr<PhysicalPage> AnonymousVMObject::allocate_committed_page(size_t page_index)
{
    {
        ScopedSpinLock lock(m_lock);

        VERIFY(m_unused_committed_pages > 0);

        // We shouldn't have any committed page tags in volatile regions
        VERIFY([&]() {
            for (auto* purgeable_ranges : m_purgeable_ranges) {
                if (purgeable_ranges->is_volatile(page_index))
                    return false;
            }
            return true;
        }());

        m_unused_committed_pages--;
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

bool AnonymousVMObject::is_nonvolatile(size_t page_index)
{
    if (m_volatile_ranges_cache_dirty)
        update_volatile_cache();
    return !m_volatile_ranges_cache.contains(page_index);
}

PageFaultResponse AnonymousVMObject::handle_cow_fault(size_t page_index, VirtualAddress vaddr)
{
    VERIFY_INTERRUPTS_DISABLED();
    ScopedSpinLock lock(m_lock);
    auto& page_slot = physical_pages()[page_index];
    bool have_committed = m_shared_committed_cow_pages && is_nonvolatile(page_index);
    if (page_slot->ref_count() == 1) {
#if PAGE_FAULT_DEBUG
        dbgln("    >> It's a COW page but nobody is sharing it anymore. Remap r/w");
#endif
        set_should_cow(page_index, false);
        if (have_committed) {
            if (m_shared_committed_cow_pages->return_one())
                m_shared_committed_cow_pages = nullptr;
        }
        return PageFaultResponse::Continue;
    }

    RefPtr<PhysicalPage> page;
    if (have_committed) {
#if PAGE_FAULT_DEBUG
        dbgln("    >> It's a committed COW page and it's time to COW!");
#endif
        page = m_shared_committed_cow_pages->allocate_one();
    } else {
#if PAGE_FAULT_DEBUG
        dbgln("    >> It's a COW page and it's time to COW!");
#endif
        page = MM.allocate_user_physical_page(MemoryManager::ShouldZeroFill::No);
        if (page.is_null()) {
            klog() << "MM: handle_cow_fault was unable to allocate a physical page";
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

}
