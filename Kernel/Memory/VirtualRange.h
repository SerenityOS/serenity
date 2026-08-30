/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Vector.h>
#include <Kernel/Memory/VirtualAddress.h>

namespace Kernel::Memory {

class VirtualRange {
public:
    VirtualRange() = delete;
    VirtualRange(VirtualAddress base, size_t size)
        : m_base(base)
        , m_size(size)
    {
    }

    VirtualAddress base() const { return m_base; }
    size_t size() const { return m_size; }
    bool is_valid() const { return !m_base.is_null(); }

    bool contains(VirtualAddress vaddr) const { return vaddr >= base() && vaddr < end(); }

    VirtualAddress end() const { return m_base.offset(m_size); }

    bool operator==(VirtualRange const& other) const
    {
        return m_base == other.m_base && m_size == other.m_size;
    }

    bool contains(VirtualAddress base, size_t size) const
    {
        if (base.offset(size) < base)
            return false;
        return base >= m_base && base.offset(size) <= end();
    }

    bool contains(VirtualRange const& other) const
    {
        return contains(other.base(), other.size());
    }

    Vector<VirtualRange, 2> carve(VirtualRange const& taken) const
    {
        VERIFY((taken.size() % PAGE_SIZE) == 0);

        Vector<VirtualRange, 2> parts;
        if (taken == *this)
            return {};
        if (taken.base() > base())
            parts.unchecked_append({ base(), taken.base().get() - base().get() });
        if (taken.end() < end())
            parts.unchecked_append({ taken.end(), end().get() - taken.end().get() });
        return parts;
    }

    VirtualRange intersect(VirtualRange const& other) const
    {
        if (*this == other) {
            return *this;
        }
        auto new_base = max(base(), other.base());
        auto new_end = min(end(), other.end());
        VERIFY(new_base < new_end);
        return VirtualRange(new_base, (new_end - new_base).get());
    }

    bool intersects(VirtualRange const& other) const
    {
        auto a = *this;
        auto b = other;

        if (a.base() > b.base())
            swap(a, b);

        return a.base() < b.end() && b.base() < a.end();
    }

    static ErrorOr<VirtualRange> expand_to_page_boundaries(FlatPtr address, size_t size);

private:
    VirtualAddress m_base;
    size_t m_size { 0 };
};

}

template<>
struct AK::Formatter<Kernel::Memory::VirtualRange> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::Memory::VirtualRange value)
    {
        return Formatter<FormatString>::format(builder, "{} - {} (size {:p})"sv, value.base().as_ptr(), value.base().offset(value.size() - 1).as_ptr(), value.size());
    }
};
