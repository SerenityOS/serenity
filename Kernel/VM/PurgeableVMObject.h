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

#include <Kernel/SpinLock.h>
#include <Kernel/VM/AnonymousVMObject.h>

namespace Kernel {

struct VolatilePageRange {
    size_t base { 0 };
    size_t count { 0 };
    bool was_purged { false };

    bool is_empty() const { return count == 0; }

    bool intersects(const VolatilePageRange& other) const
    {
        return other.base < base + count || other.base + other.count > base;
    }

    bool intersects_or_adjacent(const VolatilePageRange& other) const
    {
        return other.base <= base + count || other.base + other.count >= base;
    }

    bool contains(const VolatilePageRange& other) const
    {
        return base <= other.base && base + count >= other.base + other.count;
    }

    VolatilePageRange intersected(const VolatilePageRange& other) const
    {
        auto b = max(base, other.base);
        auto e = min(base + count, other.base + other.count);
        if (b >= e)
            return {};
        return { b, e - b, was_purged };
    }

    void combine_intersecting_or_adjacent(const VolatilePageRange& other)
    {
        ASSERT(intersects_or_adjacent(other));
        if (base <= other.base) {
            count = (other.base - base) + other.count;
        } else {
            count = (base - other.base) + count;
            base = other.base;
        }
        was_purged |= other.was_purged;
    }

    void subtract_intersecting(const VolatilePageRange& other)
    {
        if (!intersects(other))
            return;
        if (other.contains(*this)) {
            count = 0;
            return;
        }
        if (base <= other.base) {
            count = (other.base - base);
        } else {
            auto new_base = other.base + other.count;
            count = (base + count) - new_base;
            base = new_base;
        }
    }

    bool range_equals(const VolatilePageRange& other) const
    {
        return base == other.base && count == other.count;
    }
    bool operator==(const VolatilePageRange& other) const
    {
        return base == other.base && count == other.count && was_purged == other.was_purged;
    }
    bool operator!=(const VolatilePageRange& other) const
    {
        return base != other.base || count != other.count || was_purged != other.was_purged;
    }
};

class VolatilePageRanges {
public:
    VolatilePageRanges(const VolatilePageRange& total_range)
        : m_total_range(total_range)
    {
    }
    VolatilePageRanges(const VolatilePageRanges& other)
        : m_ranges(other.m_ranges)
        , m_total_range(other.m_total_range)
    {
    }

    bool is_empty() const { return m_ranges.is_empty(); }
    void clear() { m_ranges.clear(); }

    bool is_all() const
    {
        if (m_ranges.size() != 1)
            return false;
        return m_ranges[0] == m_total_range;
    }

    void set_all()
    {
        if (m_ranges.size() != 1)
            m_ranges = { m_total_range };
        else
            m_ranges[0] = m_total_range;
    }

    bool intersects(const VolatilePageRange&) const;

    bool add(const VolatilePageRange&);
    bool remove(const VolatilePageRange&, bool&);

    Vector<VolatilePageRange>& ranges() { return m_ranges; }
    const Vector<VolatilePageRange>& ranges() const { return m_ranges; }

private:
    Vector<VolatilePageRange> m_ranges;
    VolatilePageRange m_total_range;
};

class PurgeableVMObject;

class PurgeablePageRanges {
    friend class PurgeableVMObject;
public:
    PurgeablePageRanges(const VMObject&);

    void set_purgeable_page_ranges(const PurgeablePageRanges& other)
    {
        if (this == &other)
            return;
        ScopedSpinLock lock(m_volatile_ranges_lock);
        ScopedSpinLock other_lock(other.m_volatile_ranges_lock);
        m_volatile_ranges = other.m_volatile_ranges;
        return;
    }

    bool add_volatile_range(const VolatilePageRange& range);
    bool remove_volatile_range(const VolatilePageRange& range, bool& was_purged);
    bool is_volatile_range(const VolatilePageRange& range) const;

    bool is_empty() const { return m_volatile_ranges.is_empty(); }

    void set_was_purged(const VolatilePageRange&);

    const VolatilePageRanges& volatile_ranges() const { return m_volatile_ranges; }
protected:
    VolatilePageRanges m_volatile_ranges;
    mutable SpinLock<u8> m_volatile_ranges_lock;
};

class PurgeableVMObject final : public AnonymousVMObject {
public:
    virtual ~PurgeableVMObject() override;

    static NonnullRefPtr<PurgeableVMObject> create_with_size(size_t);
    virtual NonnullRefPtr<VMObject> clone() override;

    void register_purgeable_page_ranges(PurgeablePageRanges&);
    void unregister_purgeable_page_ranges(PurgeablePageRanges&);

    int purge();
    int purge_with_interrupts_disabled(Badge<MemoryManager>);

    bool is_any_volatile() const;

    template<typename F>
    IterationDecision for_each_volatile_range(F f)
    {
        ASSERT(m_lock.is_locked());
        // This is a little ugly. Basically, we're trying to find the
        // volatile ranges that all share, because those are the only
        // pages we can actually purge
        for (auto* purgeable_range : m_purgeable_ranges) {
            for (auto& r1 : purgeable_range->volatile_ranges().ranges()) {
                VolatilePageRange range(r1);
                for (auto* purgeable_range2 : m_purgeable_ranges) {
                    if (purgeable_range2 == purgeable_range)
                        continue;
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

private:
    explicit PurgeableVMObject(size_t);
    explicit PurgeableVMObject(const PurgeableVMObject&);

    virtual const char* class_name() const override { return "PurgeableVMObject"; }

    int purge_impl();
    void set_was_purged(const VolatilePageRange&);

    PurgeableVMObject& operator=(const PurgeableVMObject&) = delete;
    PurgeableVMObject& operator=(PurgeableVMObject&&) = delete;
    PurgeableVMObject(PurgeableVMObject&&) = delete;

    virtual bool is_purgeable() const override { return true; }

    Vector<PurgeablePageRanges*> m_purgeable_ranges;
    mutable SpinLock<u8> m_lock;
};

}
