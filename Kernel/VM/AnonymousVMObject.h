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

#pragma once

#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/AllocationStrategy.h>
#include <Kernel/VM/PageFaultResponse.h>
#include <Kernel/VM/PurgeablePageRanges.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {

class AnonymousVMObject : public VMObject {
    friend class PurgeablePageRanges;

public:
    virtual ~AnonymousVMObject() override;

    static RefPtr<AnonymousVMObject> create_with_size(size_t, AllocationStrategy);
    static RefPtr<AnonymousVMObject> create_for_physical_range(PhysicalAddress paddr, size_t size);
    static NonnullRefPtr<AnonymousVMObject> create_with_physical_page(PhysicalPage& page);
    virtual RefPtr<VMObject> clone() override;

    RefPtr<PhysicalPage> allocate_committed_page(size_t);
    PageFaultResponse handle_cow_fault(size_t, VirtualAddress);
    size_t cow_pages() const;
    bool should_cow(size_t page_index, bool) const;
    void set_should_cow(size_t page_index, bool);

    void register_purgeable_page_ranges(PurgeablePageRanges&);
    void unregister_purgeable_page_ranges(PurgeablePageRanges&);

    int purge();
    int purge_with_interrupts_disabled(Badge<MemoryManager>);

    bool is_any_volatile() const;

    template<typename F>
    IterationDecision for_each_volatile_range(F f) const
    {
        ASSERT(m_lock.is_locked());
        // This is a little ugly. Basically, we're trying to find the
        // volatile ranges that all share, because those are the only
        // pages we can actually purge
        for (auto* purgeable_range : m_purgeable_ranges) {
            ScopedSpinLock purgeable_lock(purgeable_range->m_volatile_ranges_lock);
            for (auto& r1 : purgeable_range->volatile_ranges().ranges()) {
                VolatilePageRange range(r1);
                for (auto* purgeable_range2 : m_purgeable_ranges) {
                    if (purgeable_range2 == purgeable_range)
                        continue;
                    ScopedSpinLock purgeable2_lock(purgeable_range2->m_volatile_ranges_lock);
                    if (purgeable_range2->is_empty()) {
                        // If just one doesn't allow any purging, we can
                        // immediately bail
                        return IterationDecision::Continue;
                    }
                    for (const auto& r2 : purgeable_range2->volatile_ranges().ranges()) {
                        range = range.intersected(r2);
                        if (range.is_empty())
                            break;
                    }
                    if (range.is_empty())
                        break;
                }
                if (range.is_empty())
                    continue;
                IterationDecision decision = f(range);
                if (decision != IterationDecision::Continue)
                    return decision;
            }
        }
        return IterationDecision::Continue;
    }

    template<typename F>
    IterationDecision for_each_nonvolatile_range(F f) const
    {
        size_t base = 0;
        for_each_volatile_range([&](const VolatilePageRange& volatile_range) {
            if (volatile_range.base == base)
                return IterationDecision::Continue;
            IterationDecision decision = f({ base, volatile_range.base - base });
            if (decision != IterationDecision::Continue)
                return decision;
            base = volatile_range.base + volatile_range.count;
            return IterationDecision::Continue;
        });
        if (base < page_count())
            return f({ base, page_count() - base });
        return IterationDecision::Continue;
    }

private:
    explicit AnonymousVMObject(size_t, AllocationStrategy);
    explicit AnonymousVMObject(PhysicalAddress, size_t);
    explicit AnonymousVMObject(PhysicalPage&);
    explicit AnonymousVMObject(const AnonymousVMObject&);

    virtual const char* class_name() const override { return "AnonymousVMObject"; }

    int purge_impl();
    void update_volatile_cache();
    void set_was_purged(const VolatilePageRange&);
    size_t remove_lazy_commit_pages(const VolatilePageRange&);
    void range_made_volatile(const VolatilePageRange&);
    void range_made_nonvolatile(const VolatilePageRange&);
    size_t count_needed_commit_pages_for_nonvolatile_range(const VolatilePageRange&);
    size_t mark_committed_pages_for_nonvolatile_range(const VolatilePageRange&, size_t);
    bool is_nonvolatile(size_t page_index);

    AnonymousVMObject& operator=(const AnonymousVMObject&) = delete;
    AnonymousVMObject& operator=(AnonymousVMObject&&) = delete;
    AnonymousVMObject(AnonymousVMObject&&) = delete;

    virtual bool is_anonymous() const override { return true; }

    Bitmap& ensure_cow_map();
    void ensure_or_reset_cow_map();

    VolatilePageRanges m_volatile_ranges_cache;
    bool m_volatile_ranges_cache_dirty { true };
    Vector<PurgeablePageRanges*> m_purgeable_ranges;
    size_t m_unused_committed_pages { 0 };

    mutable OwnPtr<Bitmap> m_cow_map;

    // We share a pool of committed cow-pages with clones
    RefPtr<CommittedCowPages> m_shared_committed_cow_pages;
};

}
