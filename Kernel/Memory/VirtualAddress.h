/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Types.h>

template<typename T>
class VirtualAddressBase {
public:
    using UnderlyingType = T;

    VirtualAddressBase() = default;
    constexpr explicit VirtualAddressBase(T address)
        : m_address(address)
    {
    }

    explicit VirtualAddressBase(void const* address)
        : m_address((T)address)
    {
    }

    [[nodiscard]] constexpr bool is_null() const { return m_address == 0; }

    template<typename Self>
    [[nodiscard]] constexpr Self offset(this Self const& self, T o) { return Self(self.m_address + o); }

    [[nodiscard]] constexpr T get() const { return m_address; }

    void set(T address) { m_address = address; }
    void mask(T m) { m_address &= m; }

    template<typename Self>
    int operator<=>(this Self const& self, Self const& other)
    {
        if (self.m_address == other.m_address)
            return 0;
        return self.m_address < other.m_address ? -1 : 1;
    }

    template<typename Self>
    bool operator==(this Self const& self, Self const& other) { return self.m_address == other.m_address; }

    template<typename Self>
    bool operator!=(this Self const& self, Self const& other) { return self.m_address != other.m_address; }

    // NOLINTNEXTLINE(readability-make-member-function-const) const VirtualAddressBase shouldn't be allowed to modify the underlying memory
    [[nodiscard]] u8* as_ptr() { return reinterpret_cast<u8*>(m_address); }
    [[nodiscard]] u8 const* as_ptr() const { return reinterpret_cast<u8 const*>(m_address); }

    template<typename Self>
    [[nodiscard]] Self operator-(this Self const& self, Self const& other) { return Self(self.get() - other.get()); }

protected:
    T m_address { 0 };
};

class VirtualAddress : public VirtualAddressBase<FlatPtr> {
    using VirtualAddressBase::VirtualAddressBase;

public:
    [[nodiscard]] constexpr bool is_page_aligned() const { return (m_address & 0xfff) == 0; }
    [[nodiscard]] VirtualAddress page_base() const { return VirtualAddress(m_address & ~(FlatPtr)0xfffu); }
};

template<>
struct AK::Formatter<VirtualAddress> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, VirtualAddress const& value)
    {
        return AK::Formatter<FormatString>::format(builder, "V{}"sv, value.as_ptr());
    }
};
