/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
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

    Vector<VirtualRange, 2> carve(VirtualRange const&) const;
    VirtualRange intersect(VirtualRange const&) const;

    bool intersects(VirtualRange const&) const;

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
