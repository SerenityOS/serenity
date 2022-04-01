/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/VirtualAddress.h>

namespace UserspaceEmulator {

class Range {
    friend class RangeAllocator;

public:
    Range() = delete;
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

    bool operator==(Range const& other) const
    {
        return m_base == other.m_base && m_size == other.m_size;
    }

    bool contains(VirtualAddress base, size_t size) const
    {
        if (base.offset(size) < base)
            return false;
        return base >= m_base && base.offset(size) <= end();
    }

    bool contains(Range const& other) const
    {
        return contains(other.base(), other.size());
    }

    Vector<Range, 2> carve(Range const&) const;

    Range split_at(VirtualAddress address)
    {
        VERIFY(address.is_page_aligned());
        VERIFY(m_base < address);
        size_t new_size = (address - m_base).get();
        VERIFY(new_size < m_size);
        size_t other_size = m_size - new_size;
        m_size = new_size;
        return { address, other_size };
    }

private:
    VirtualAddress m_base;
    size_t m_size { 0 };
};

}

namespace AK {
template<>
struct Traits<UserspaceEmulator::Range> : public GenericTraits<UserspaceEmulator::Range> {
    static constexpr bool is_trivial() { return true; }
};
}
