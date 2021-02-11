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
#include <Kernel/Debug.h>
#include <Kernel/Process.h>
#include <Kernel/VM/AnonymousVMObject.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/VM/PurgeablePageRanges.h>

namespace Kernel {

#if VOLATILE_PAGE_RANGES_DEBUG
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

void VolatilePageRanges::add_unchecked(const VolatilePageRange& range)
{
    auto add_range = m_total_range.intersected(range);
    if (add_range.is_empty())
        return;
    m_ranges.append(range);
}

bool VolatilePageRanges::add(const VolatilePageRange& range)
{
    auto add_range = m_total_range.intersected(range);
    if (add_range.is_empty())
        return false;
    add_range.was_purged = range.was_purged;

#if VOLATILE_PAGE_RANGES_DEBUG
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

#if VOLATILE_PAGE_RANGES_DEBUG
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
    : m_volatile_ranges({ 0, vmobject.is_anonymous() ? vmobject.page_count() : 0 })
{
}

bool PurgeablePageRanges::add_volatile_range(const VolatilePageRange& range)
{
    if (range.is_empty())
        return false;

    // Since we may need to call into AnonymousVMObject we need to acquire
    // its lock as well, and acquire it first. This is important so that
    // we don't deadlock when a page fault (e.g. on another processor)
    // happens that is meant to lazy-allocate a committed page. It would
    // call into AnonymousVMObject::range_made_volatile, which then would
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

auto PurgeablePageRanges::remove_volatile_range(const VolatilePageRange& range, bool& was_purged) -> RemoveVolatileError
{
    if (range.is_empty()) {
        was_purged = false;
        return RemoveVolatileError::Success;
    }
    ScopedSpinLock vmobject_lock(m_vmobject->m_lock); // see comment in add_volatile_range
    ScopedSpinLock lock(m_volatile_ranges_lock);
    ASSERT(m_vmobject);

    // Before we actually remove this range, we need to check if we need
    // to commit any pages, which may fail. If it fails, we don't actually
    // want to make any modifications. COW pages are already accounted for
    // in m_shared_committed_cow_pages
    size_t need_commit_pages = 0;
    m_volatile_ranges.for_each_intersecting_range(range, [&](const VolatilePageRange& intersected_range) {
        need_commit_pages += m_vmobject->count_needed_commit_pages_for_nonvolatile_range(intersected_range);
        return IterationDecision::Continue;
    });
    if (need_commit_pages > 0) {
        // See if we can grab enough pages for what we're marking non-volatile
        if (!MM.commit_user_physical_pages(need_commit_pages))
            return RemoveVolatileError::OutOfMemory;

        // Now that we are committed to these pages, mark them for lazy-commit allocation
        auto pages_to_mark = need_commit_pages;
        m_volatile_ranges.for_each_intersecting_range(range, [&](const VolatilePageRange& intersected_range) {
            auto pages_marked = m_vmobject->mark_committed_pages_for_nonvolatile_range(intersected_range, pages_to_mark);
            pages_to_mark -= pages_marked;
            return IterationDecision::Continue;
        });
    }

    // Now actually remove the range
    if (m_volatile_ranges.remove(range, was_purged)) {
        m_vmobject->range_made_nonvolatile(range);
        return RemoveVolatileError::Success;
    }

    ASSERT(need_commit_pages == 0); // We should have not touched anything
    return RemoveVolatileError::SuccessNoChange;
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
    m_volatile_ranges.add({ range.base, range.count, true });
}

void PurgeablePageRanges::set_vmobject(AnonymousVMObject* vmobject)
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
    ASSERT(m_committed_pages > 0);
    m_committed_pages--;

    return MM.allocate_committed_user_physical_page(MemoryManager::ShouldZeroFill::Yes);
}

bool CommittedCowPages::return_one()
{
    ASSERT(m_committed_pages > 0);
    m_committed_pages--;

    MM.uncommit_user_physical_pages(1);
    return m_committed_pages == 0;
}

}
