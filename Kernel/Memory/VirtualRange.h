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

template<typename VirtualAddress>
class VirtualRangeBase {
public:
    VirtualRangeBase() = delete;
    VirtualRangeBase(VirtualAddress base, size_t size)
        : m_base(base)
        , m_size(size)
    {
    }

    VirtualAddress base() const { return m_base; }
    size_t size() const { return m_size; }
    bool is_valid() const { return !m_base.is_null(); }

    bool contains(VirtualAddress vaddr) const { return vaddr >= base() && vaddr < end(); }

    VirtualAddress end() const { return m_base.offset(m_size); }

    template<typename Self>
    bool operator==(this Self const& self, Self const& other)
    {
        return self.m_base == other.m_base && self.m_size == other.m_size;
    }

    bool contains(VirtualAddress base, size_t size) const
    {
        if (base.offset(size) < base)
            return false;
        return base >= m_base && base.offset(size) <= end();
    }

    template<typename Self>
    bool contains(this Self const& self, Self const& other)
    {
        return self.contains(other.base(), other.size());
    }

    template<typename Self>
    Vector<Self, 2> carve(this Self const& self, Self const& taken)
    {
        VERIFY((taken.size() % PAGE_SIZE) == 0);

        Vector<Self, 2> parts;
        if (taken == self)
            return {};
        if (taken.base() > self.base())
            parts.unchecked_append({ self.base(), taken.base().get() - self.base().get() });
        if (taken.end() < self.end())
            parts.unchecked_append({ taken.end(), self.end().get() - taken.end().get() });
        return parts;
    }

    template<typename Self>
    Self intersect(this Self const& self, Self const& other)
    {
        if (self == other) {
            return self;
        }
        auto new_base = max(self.base(), other.base());
        auto new_end = min(self.end(), other.end());
        VERIFY(new_base < new_end);
        return Self(new_base, (new_end - new_base).get());
    }

    template<typename Self>
    bool intersects(this Self const& self, Self const& other)
    {
        auto a = self;
        auto b = other;

        if (a.base() > b.base())
            swap(a, b);

        return a.base() < b.end() && b.base() < a.end();
    }

private:
    VirtualAddress m_base;
    size_t m_size { 0 };
};

class VirtualRange : public VirtualRangeBase<VirtualAddress> {
    using VirtualRangeBase::VirtualRangeBase;

public:
    static ErrorOr<VirtualRange> expand_to_page_boundaries(FlatPtr address, size_t size);
};

}

template<>
struct AK::Formatter<Kernel::Memory::VirtualRange> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Kernel::Memory::VirtualRange value)
    {
        return Formatter<FormatString>::format(builder, "{} - {} (size {:p})"sv, value.base().as_ptr(), value.base().offset(value.size() - 1).as_ptr(), value.size());
    }
};
