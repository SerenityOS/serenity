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

#include <AK/Bitmap.h>
#include <AK/RefCounted.h>
#include <Kernel/SpinLock.h>

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
    void clear() { m_ranges.clear_with_capacity(); }

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
    bool contains(size_t index) const
    {
        return intersects({ index, 1 });
    }

    bool add(const VolatilePageRange&);
    void add_unchecked(const VolatilePageRange&);
    bool remove(const VolatilePageRange&, bool&);

    template<typename F>
    IterationDecision for_each_intersecting_range(const VolatilePageRange& range, F f)
    {
        auto r = m_total_range.intersected(range);
        if (r.is_empty())
            return IterationDecision::Continue;

        size_t nearby_index = 0;
        auto* existing_range = binary_search(
            m_ranges.span(), r, &nearby_index, [](auto& a, auto& b) {
                if (a.intersects(b))
                    return 0;
                return (signed)(a.base - (b.base + b.count - 1));
            });
        if (!existing_range)
            return IterationDecision::Continue;

        if (existing_range->range_equals(r))
            return f(r);
        ASSERT(existing_range == &m_ranges[nearby_index]); // sanity check
        while (nearby_index < m_ranges.size()) {
            existing_range = &m_ranges[nearby_index];
            if (!existing_range->intersects(range))
                break;

            IterationDecision decision = f(existing_range->intersected(r));
            if (decision != IterationDecision::Continue)
                return decision;

            nearby_index++;
        }
        return IterationDecision::Continue;
    }

    template<typename F>
    IterationDecision for_each_nonvolatile_range(F f) const
    {
        size_t base = m_total_range.base;
        for (const auto& volatile_range : m_ranges) {
            if (volatile_range.base == base)
                continue;
            IterationDecision decision = f({ base, volatile_range.base - base });
            if (decision != IterationDecision::Continue)
                return decision;
            base = volatile_range.base + volatile_range.count;
        }
        if (base < m_total_range.base + m_total_range.count)
            return f({ base, (m_total_range.base + m_total_range.count) - base });
        return IterationDecision::Continue;
    }

    Vector<VolatilePageRange>& ranges() { return m_ranges; }
    const Vector<VolatilePageRange>& ranges() const { return m_ranges; }

private:
    Vector<VolatilePageRange> m_ranges;
    VolatilePageRange m_total_range;
};

class AnonymousVMObject;

class PurgeablePageRanges {
    friend class AnonymousVMObject;

public:
    PurgeablePageRanges(const VMObject&);

    void copy_purgeable_page_ranges(const PurgeablePageRanges& other)
    {
        if (this == &other)
            return;
        ScopedSpinLock lock(m_volatile_ranges_lock);
        ScopedSpinLock other_lock(other.m_volatile_ranges_lock);
        m_volatile_ranges = other.m_volatile_ranges;
    }

    bool add_volatile_range(const VolatilePageRange& range);
    enum class RemoveVolatileError {
        Success = 0,
        SuccessNoChange,
        OutOfMemory
    };
    RemoveVolatileError remove_volatile_range(const VolatilePageRange& range, bool& was_purged);
    bool is_volatile_range(const VolatilePageRange& range) const;
    bool is_volatile(size_t) const;

    bool is_empty() const { return m_volatile_ranges.is_empty(); }

    void set_was_purged(const VolatilePageRange&);

    const VolatilePageRanges& volatile_ranges() const { return m_volatile_ranges; }

protected:
    void set_vmobject(AnonymousVMObject*);

    VolatilePageRanges m_volatile_ranges;
    mutable RecursiveSpinLock m_volatile_ranges_lock;
    AnonymousVMObject* m_vmobject { nullptr };
};

class CommittedCowPages : public RefCounted<CommittedCowPages> {
    AK_MAKE_NONCOPYABLE(CommittedCowPages);

public:
    CommittedCowPages() = delete;

    CommittedCowPages(size_t);
    ~CommittedCowPages();

    NonnullRefPtr<PhysicalPage> allocate_one();
    bool return_one();

private:
    size_t m_committed_pages;
};

}
