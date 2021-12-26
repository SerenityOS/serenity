/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/PhysicalAddress.h>
#include <Kernel/VM/AllocationStrategy.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/PageFaultResponse.h>
#include <Kernel/VM/PurgeablePageRanges.h>
#include <Kernel/VM/VMObject.h>

namespace Kernel {

class AnonymousVMObject final : public VMObject {
    friend class PurgeablePageRanges;

public:
    virtual ~AnonymousVMObject() override;

    static RefPtr<AnonymousVMObject> try_create_with_size(size_t, AllocationStrategy);
    static RefPtr<AnonymousVMObject> try_create_for_physical_range(PhysicalAddress paddr, size_t size);
    static RefPtr<AnonymousVMObject> try_create_with_physical_page(PhysicalPage& page);
    static RefPtr<AnonymousVMObject> try_create_with_physical_pages(NonnullRefPtrVector<PhysicalPage>);
    virtual RefPtr<VMObject> try_clone() override;

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

    template<IteratorFunction<const VolatilePageRange&> F>
    IterationDecision for_each_volatile_range(F f) const
    {
        VERIFY(m_lock.is_locked());
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

    template<IteratorFunction<const VolatilePageRange&> F>
    IterationDecision for_each_nonvolatile_range(F f) const
    {
        size_t base = 0;
        for_each_volatile_range([&](const VolatilePageRange& volatile_range) {
            if (volatile_range.base == base)
                return IterationDecision::Continue;
            IterationDecision decision = f(VolatilePageRange { base, volatile_range.base - base });
            if (decision != IterationDecision::Continue)
                return decision;
            base = volatile_range.base + volatile_range.count;
            return IterationDecision::Continue;
        });
        if (base < page_count())
            return f(VolatilePageRange { base, page_count() - base });
        return IterationDecision::Continue;
    }

    template<VoidFunction<const VolatilePageRange&> F>
    IterationDecision for_each_volatile_range(F f) const
    {
        return for_each_volatile_range([&](auto& range) {
            f(range);
            return IterationDecision::Continue;
        });
    }

    template<VoidFunction<const VolatilePageRange&> F>
    IterationDecision for_each_nonvolatile_range(F f) const
    {
        return for_each_nonvolatile_range([&](auto range) {
            f(move(range));
            return IterationDecision::Continue;
        });
    }

private:
    explicit AnonymousVMObject(size_t, AllocationStrategy);
    explicit AnonymousVMObject(PhysicalAddress, size_t);
    explicit AnonymousVMObject(PhysicalPage&);
    explicit AnonymousVMObject(NonnullRefPtrVector<PhysicalPage>);
    explicit AnonymousVMObject(const AnonymousVMObject&);

    virtual StringView class_name() const override { return "AnonymousVMObject"sv; }

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

    Bitmap m_cow_map;

    // We share a pool of committed cow-pages with clones
    RefPtr<CommittedCowPages> m_shared_committed_cow_pages;
};

}
