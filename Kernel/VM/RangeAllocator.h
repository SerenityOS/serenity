#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <Kernel/VM/VirtualAddress.h>

class Range {
    friend class RangeAllocator;

public:
    Range() {}
    Range(VirtualAddress base, size_t size)
        : m_base(base)
        , m_size(size)
    {
    }

    VirtualAddress base() const { return m_base; }
    size_t size() const { return m_size; }
    bool is_valid() const { return !m_base.is_null(); }

    bool contains(VirtualAddress vaddr) const { return vaddr >= base() && vaddr < end(); }

    VirtualAddress end() const { return m_base.offset(m_size); }

    bool operator==(const Range& other) const
    {
        return m_base == other.m_base && m_size == other.m_size;
    }

    bool contains(VirtualAddress base, size_t size) const
    {
        return base >= m_base && base.offset(size) <= end();
    }

    bool contains(const Range& other) const
    {
        return contains(other.base(), other.size());
    }

    Vector<Range, 2> carve(const Range&);

private:
    VirtualAddress m_base;
    size_t m_size { 0 };
};

class RangeAllocator {
public:
    RangeAllocator();
    ~RangeAllocator();

    void initialize_with_range(VirtualAddress, size_t);
    void initialize_from_parent(const RangeAllocator&);

    Range allocate_anywhere(size_t);
    Range allocate_specific(VirtualAddress, size_t);
    void deallocate(Range);

    void dump() const;

private:
    void carve_at_index(int, const Range&);

    Vector<Range> m_available_ranges;
};

inline const LogStream& operator<<(const LogStream& stream, const Range& value)
{
    return stream << String::format("Range(%x-%x)", value.base().get(), value.end().get() - 1);
}
