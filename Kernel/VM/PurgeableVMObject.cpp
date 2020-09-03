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
        m_ranges.span(), add_range, &nearby_index, [](auto& a, auto& b) {
            if (a.intersects_or_adjacent(b))
                return 0;
            return (signed)(a.base - (b.base + b.count - 1));
        });

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
        m_ranges.span(), remove_range, &nearby_index, [](auto& a, auto& b) {
            if (a.intersects(b))
                return 0;
            return (signed)(a.base - (b.base + b.count - 1));
        });
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
        m_ranges.span(), range, nullptr, [](auto& a, auto& b) {
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
    ScopedSpinLock lock(m_volatile_ranges_lock);
    return m_volatile_ranges.add(range);
}

bool PurgeablePageRanges::remove_volatile_range(const VolatilePageRange& range, bool& was_purged)
{
    if (range.is_empty())
        return false;
    ScopedSpinLock lock(m_volatile_ranges_lock);
    return m_volatile_ranges.remove(range, was_purged);
}

bool PurgeablePageRanges::is_volatile_range(const VolatilePageRange& range) const
{
    if (range.is_empty())
        return false;
    ScopedSpinLock lock(m_volatile_ranges_lock);
    return m_volatile_ranges.intersects(range);
}

void PurgeablePageRanges::set_was_purged(const VolatilePageRange& range)
{
    ScopedSpinLock lock(m_volatile_ranges_lock);
    m_volatile_ranges.add({range.base, range.count, true});
}

NonnullRefPtr<PurgeableVMObject> PurgeableVMObject::create_with_size(size_t size)
{
    return adopt(*new PurgeableVMObject(size));
}

PurgeableVMObject::PurgeableVMObject(size_t size)
    : AnonymousVMObject(size)
{
}

PurgeableVMObject::PurgeableVMObject(const PurgeableVMObject& other)
    : AnonymousVMObject(other)
    , m_purgeable_ranges() // do *not* clone this
{
    // TODO: what about m_lock?
}

PurgeableVMObject::~PurgeableVMObject()
{
}

NonnullRefPtr<VMObject> PurgeableVMObject::clone()
{
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
            if (phys_page && !phys_page->is_shared_zero_page())
                ++purged_in_range;
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
    ASSERT(!m_purgeable_ranges.contains_slow(&purgeable_page_ranges));
    m_purgeable_ranges.append(&purgeable_page_ranges);
}

void PurgeableVMObject::unregister_purgeable_page_ranges(PurgeablePageRanges& purgeable_page_ranges)
{
    ScopedSpinLock lock(m_lock);
    for (size_t i = 0; i < m_purgeable_ranges.size(); i++) {
        if (m_purgeable_ranges[i] != &purgeable_page_ranges)
            continue;
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

}
