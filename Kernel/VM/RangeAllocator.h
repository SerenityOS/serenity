#pragma once

#include <AK/Vector.h>
#include <Kernel/LinearAddress.h>

class Range {
    friend class RangeAllocator;

public:
    Range() {}
    Range(LinearAddress base, size_t size)
        : m_base(base)
        , m_size(size)
    {
    }

    LinearAddress base() const { return m_base; }
    size_t size() const { return m_size; }
    bool is_valid() const { return !m_base.is_null(); }

    bool contains(LinearAddress laddr) const { return laddr >= base() && laddr < end(); }

    LinearAddress end() const { return m_base.offset(m_size); }

    bool operator==(const Range& other) const
    {
        return m_base == other.m_base && m_size == other.m_size;
    }

    bool contains(LinearAddress base, size_t size) const
    {
        return base >= m_base && base.offset(size) <= end();
    }

    bool contains(const Range& other) const
    {
        return contains(other.base(), other.size());
    }

    Vector<Range, 2> carve(const Range&);

private:
    LinearAddress m_base;
    size_t m_size { 0 };
};

class RangeAllocator {
public:
    RangeAllocator(LinearAddress, size_t);
    RangeAllocator(const RangeAllocator&);
    ~RangeAllocator();

    Range allocate_anywhere(size_t);
    Range allocate_specific(LinearAddress, size_t);
    void deallocate(Range);

    void dump() const;

private:
    void carve_at_index(int, const Range&);

    Vector<Range> m_available_ranges;
};
