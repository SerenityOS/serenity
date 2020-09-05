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

#include <AK/BinarySearch.h>
#include <AK/ScopeGuard.h>
#include <Kernel/Process.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PurgeableVMObject.h>

//#define VOLATILE_PAGE_RANGES_DEBUG

namespace Kernel {

#ifdef VOLATILE_PAGE_RANGES_DEBUG
inline LogStream& operator<<(const LogStream& stream, const VolatilePageRange& range)
{
    stream << "{" << range.base << " (" << range.count << ") purged: " << range.was_purged << "}";
    return const_cast<LogStream&>(stream);
}

static void dump_volatile_page_ranges(const Vector<VolatilePageRange>& ranges)
{
   for (size_t i = 0; i < ranges.size(); i++) {
       const auto& range = ranges[i];
       klog() << "  [" << i << "] " << range;
   }
}
#endif

bool VolatilePageRanges::add(const VolatilePageRange& range)
{
    auto add_range = m_total_range.intersected(range);
    if (add_range.is_empty())
        return false;
    add_range.was_purged = range.was_purged;

#ifdef VOLATILE_PAGE_RANGES_DEBUG
    klog() << "ADD " << range << " (total range: " << m_total_range << ") -->";
    dump_volatile_page_ranges(m_ranges);
    ScopeGuard debug_guard([&]() {
        klog() << "After adding " << range << " (total range: " << m_total_range << ")";
        dump_volatile_page_ranges(m_ranges);
        klog() << "<-- ADD " << range << " (total range: " << m_total_range << ")";
    });
#endif

    size_t nearby_index = 0;
    auto* existing_range = binary_search(
        m_ranges.span(), add_range, [](auto& a, auto& b) {
            if (a.intersects_or_adjacent(b))
                return 0;
            return (signed)(a.base - (b.base + b.count - 1));
        },
        &nearby_index);

    size_t inserted_index = 0;
    if (existing_range) {
        if (*existing_range == add_range)
            return false;
        
        if (existing_range->was_purged != add_range.was_purged) {
            // Found an intersecting or adjacent range, but the purge flag
            // doesn't match. Subtract what we're adding from it, and
            existing_range->subtract_intersecting(add_range);
            if (existing_range->is_empty()) {
                *existing_range = add_range;
            } else {
                m_ranges.insert(++nearby_index, add_range);
                existing_range = &m_ranges[nearby_index];
            }
        } else {
            // Found an intersecting or adjacent range that can be merged
            existing_range->combine_intersecting_or_adjacent(add_range);
        }
        inserted_index = nearby_index;
    } else {
        // Insert into the sorted list
        m_ranges.insert_before_matching(
            VolatilePageRange(add_range), [&](auto& entry) {
                return entry.base >= add_range.base + add_range.count;
            },
            nearby_index, &inserted_index);
        existing_range = &m_ranges[inserted_index];
    }

    // See if we can merge any of the following ranges
    inserted_index++;
    while (inserted_index < m_ranges.size()) {
        auto& next_range = m_ranges[inserted_index];
        if (!next_range.intersects_or_adjacent(*existing_range))
            break;
        if (next_range.was_purged != existing_range->was_purged) {
            // The purged flag of following range is not the same.
            // Subtract the added/combined range from it
            next_range.subtract_intersecting(*existing_range);
            if (next_range.is_empty())
                m_ranges.remove(inserted_index);
        } else {
            existing_range->combine_intersecting_or_adjacent(next_range);
            m_ranges.remove(inserted_index);
        }
    }
    return true;
}

bool VolatilePageRanges::remove(const VolatilePageRange& range, bool& was_purged)
{
    auto remove_range = m_total_range.intersected(range);
    if (remove_range.is_empty())
        return false;

#ifdef VOLATILE_PAGE_RANGES_DEBUG
    klog() << "REMOVE " << range << " (total range: " << m_total_range << ") -->";
    dump_volatile_page_ranges(m_ranges);
    ScopeGuard debug_guard([&]() {
        klog() << "After removing " << range << " (total range: " << m_total_range << ")";
        dump_volatile_page_ranges(m_ranges);
        klog() << "<-- REMOVE " << range << " (total range: " << m_total_range << ") was_purged: " << was_purged;
    });
#endif

    size_t nearby_index = 0;
    auto* existing_range = binary_search(
        m_ranges.span(), remove_range, [](auto& a, auto& b) {
            if (a.intersects(b))
                return 0;
            return (signed)(a.base - (b.base + b.count - 1));
        },
        &nearby_index);
    if (!existing_range)
        return false;

    was_purged = existing_range->was_purged;
    if (existing_range->range_equals(remove_range)) {
        m_ranges.remove(nearby_index);
    } else {
        // See if we need to remove any of the following ranges
        ASSERT(existing_range == &m_ranges[nearby_index]); // sanity check
        while (nearby_index < m_ranges.size()) {
            existing_range = &m_ranges[nearby_index];
            if (!existing_range->intersects(range))
                break;
            was_purged |= existing_range->was_purged;
            existing_range->subtract_intersecting(remove_range);
            if (existing_range->is_empty()) {
                m_ranges.remove(nearby_index);
                break;
            }
        }
    }
    return true;
}

bool VolatilePageRanges::intersects(const VolatilePageRange& range) const
{
    auto* existing_range = binary_search(
        m_ranges.span(), range, [](auto& a, auto& b) {
            if (a.intersects(b))
                return 0;
            return (signed)(a.base - (b.base + b.count - 1));
        });
    return existing_range != nullptr;
}

PurgeablePageRanges::PurgeablePageRanges(const VMObject& vmobject)
    : m_volatile_ranges({0, vmobject.is_purgeable() ? static_cast<const PurgeableVMObject&>(vmobject).page_count() : 0})
{
}

bool PurgeablePageRanges::add_volatile_range(const VolatilePageRange& range)
{
    if (range.is_empty())
        return false;

    // Since we may need to call into PurgeableVMObject we need to acquire
    // its lock as well, and acquire it first. This is important so that
    // we don't deadlock when a page fault (e.g. on another processor)
    // happens that is meant to lazy-allocate a committed page. It would
    // call into PurgeableVMObject::range_made_volatile, which then would
    // also call into this object and need to acquire m_lock. By acquiring
    // the vmobject lock first in both cases, we avoid deadlocking.
    // We can access m_vmobject without any locks for that purpose because
    // add_volatile_range and remove_volatile_range can only be called
    // by same object that calls set_vmobject.
    ScopedSpinLock vmobject_lock(m_vmobject->m_lock);
    ScopedSpinLock lock(m_volatile_ranges_lock);
    bool added = m_volatile_ranges.add(range);
    if (added)
        m_vmobject->range_made_volatile(range);
    return added;
}

bool PurgeablePageRanges::remove_volatile_range(const VolatilePageRange& range, bool& was_purged)
{
    if (range.is_empty())
        return false;
    ScopedSpinLock lock(m_volatile_ranges_lock);
    ASSERT(m_vmobject);
    return m_volatile_ranges.remove(range, was_purged);
}

bool PurgeablePageRanges::is_volatile_range(const VolatilePageRange& range) const
{
    if (range.is_empty())
        return false;
    ScopedSpinLock lock(m_volatile_ranges_lock);
    return m_volatile_ranges.intersects(range);
}

bool PurgeablePageRanges::is_volatile(size_t index) const
{
    ScopedSpinLock lock(m_volatile_ranges_lock);
    return m_volatile_ranges.contains(index);
}

void PurgeablePageRanges::set_was_purged(const VolatilePageRange& range)
{
    ScopedSpinLock lock(m_volatile_ranges_lock);
    m_volatile_ranges.add({range.base, range.count, true});
}

void PurgeablePageRanges::set_vmobject(PurgeableVMObject* vmobject)
{
    // No lock needed here
    if (vmobject) {
        ASSERT(!m_vmobject);
        m_vmobject = vmobject;
    } else {
        ASSERT(m_vmobject);
        m_vmobject = nullptr;
    }
}

RefPtr<PurgeableVMObject> PurgeableVMObject::create_with_size(size_t size)
{
    // We need to attempt to commit before actually creating the object
    if (!MM.commit_user_physical_pages(ceil_div(size, PAGE_SIZE)))
        return {};
    return adopt(*new PurgeableVMObject(size));
}

PurgeableVMObject::PurgeableVMObject(size_t size)
    : AnonymousVMObject(size, false)
    , m_unused_committed_pages(page_count())
{
    for (size_t i = 0; i < page_count(); ++i)
        physical_pages()[i] = MM.lazy_committed_page();
}

PurgeableVMObject::PurgeableVMObject(const PurgeableVMObject& other)
    : AnonymousVMObject(other)
    , m_purgeable_ranges() // do *not* clone this
    , m_unused_committed_pages(other.m_unused_committed_pages)
{
    // We can't really "copy" a spinlock. But we're holding it. Clear in the clone
    ASSERT(other.m_lock.is_locked());
    m_lock.initialize();
}

PurgeableVMObject::~PurgeableVMObject()
{
    if (m_unused_committed_pages > 0)
        MM.uncommit_user_physical_pages(m_unused_committed_pages);
}

RefPtr<VMObject> PurgeableVMObject::clone()
{
    // We need to acquire our lock so we copy a sane state
    ScopedSpinLock lock(m_lock);
    if (m_unused_committed_pages > 0) {
        // We haven't used up all committed pages. In order to be able
        // to clone ourselves, we need to be able to commit the same number
        // of pages first
        if (!MM.commit_user_physical_pages(m_unused_committed_pages))
            return {};
    }
    return adopt(*new PurgeableVMObject(*this));
}

int PurgeableVMObject::purge()
{
    LOCKER(m_paging_lock);
    return purge_impl();
}

int PurgeableVMObject::purge_with_interrupts_disabled(Badge<MemoryManager>)
{
    ASSERT_INTERRUPTS_DISABLED();
    if (m_paging_lock.is_locked())
        return 0;
    return purge_impl();
}

void PurgeableVMObject::set_was_purged(const VolatilePageRange& range)
{
    ASSERT(m_lock.is_locked());
    for (auto* purgeable_ranges : m_purgeable_ranges)
        purgeable_ranges->set_was_purged(range);
}

int PurgeableVMObject::purge_impl()
{
    int purged_page_count = 0;
    ScopedSpinLock lock(m_lock);
    for_each_volatile_range([&](const auto& range) {
        int purged_in_range = 0;
        auto range_end = range.base + range.count;
        for (size_t i = range.base; i < range_end; i++) {
            auto& phys_page = m_physical_pages[i];
            if (phys_page && !phys_page->is_shared_zero_page()) {
                ASSERT(!phys_page->is_lazy_committed_page());
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
                    region.remap_page_range(range.base, range.count);
                }
            });
        }
        return IterationDecision::Continue;
    });
    return purged_page_count;
}

void PurgeableVMObject::register_purgeable_page_ranges(PurgeablePageRanges& purgeable_page_ranges)
{
    ScopedSpinLock lock(m_lock);
    purgeable_page_ranges.set_vmobject(this);
    ASSERT(!m_purgeable_ranges.contains_slow(&purgeable_page_ranges));
    m_purgeable_ranges.append(&purgeable_page_ranges);
}

void PurgeableVMObject::unregister_purgeable_page_ranges(PurgeablePageRanges& purgeable_page_ranges)
{
    ScopedSpinLock lock(m_lock);
    for (size_t i = 0; i < m_purgeable_ranges.size(); i++) {
        if (m_purgeable_ranges[i] != &purgeable_page_ranges)
            continue;
        purgeable_page_ranges.set_vmobject(nullptr);
        m_purgeable_ranges.remove(i);
        return;
    }
    ASSERT_NOT_REACHED();
}

bool PurgeableVMObject::is_any_volatile() const
{
    ScopedSpinLock lock(m_lock);
    for (auto& volatile_ranges : m_purgeable_ranges) {
        ScopedSpinLock lock(volatile_ranges->m_volatile_ranges_lock);
        if (!volatile_ranges->is_empty())
            return true;
    }
    return false;
}

size_t PurgeableVMObject::remove_lazy_commit_pages(const VolatilePageRange& range)
{
    ASSERT(m_lock.is_locked());

    size_t removed_count = 0;
    auto range_end = range.base + range.count;
    for (size_t i = range.base; i < range_end; i++) {
        auto& phys_page = m_physical_pages[i];
        if (phys_page && phys_page->is_lazy_committed_page()) {
            phys_page = MM.shared_zero_page();
            removed_count++;
            ASSERT(m_unused_committed_pages > 0);
            m_unused_committed_pages--;
//            if (--m_unused_committed_pages == 0)
//                break;
        }
    }
    return removed_count;
}

void PurgeableVMObject::range_made_volatile(const VolatilePageRange& range)
{
    ASSERT(m_lock.is_locked());

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
//            if (m_unused_committed_pages == 0)
//                return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });

    // Return those committed pages back to the system
    if (uncommit_page_count > 0)
        MM.uncommit_user_physical_pages(uncommit_page_count);
}

RefPtr<PhysicalPage> PurgeableVMObject::allocate_committed_page(size_t page_index)
{
    {
        ScopedSpinLock lock(m_lock);

        ASSERT(m_unused_committed_pages > 0);

        // We should't have any committed page tags in volatile regions
        ASSERT([&]() {
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

}
