/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Types.h>

class VirtualAddress {
public:
    VirtualAddress() = default;
    constexpr explicit VirtualAddress(FlatPtr address)
        : m_address(address)
    {
    }

    explicit VirtualAddress(const void* address)
        : m_address((FlatPtr)address)
    {
    }

    [[nodiscard]] constexpr bool is_null() const { return m_address == 0; }
    [[nodiscard]] constexpr bool is_page_aligned() const { return (m_address & 0xfff) == 0; }

    [[nodiscard]] constexpr VirtualAddress offset(FlatPtr o) const { return VirtualAddress(m_address + o); }
    [[nodiscard]] constexpr FlatPtr get() const { return m_address; }
    void set(FlatPtr address) { m_address = address; }
    void mask(FlatPtr m) { m_address &= m; }

    bool operator<=(const VirtualAddress& other) const { return m_address <= other.m_address; }
    bool operator>=(const VirtualAddress& other) const { return m_address >= other.m_address; }
    bool operator>(const VirtualAddress& other) const { return m_address > other.m_address; }
    bool operator<(const VirtualAddress& other) const { return m_address < other.m_address; }
    bool operator==(const VirtualAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const VirtualAddress& other) const { return m_address != other.m_address; }

    // NOLINTNEXTLINE(readability-make-member-function-const) const VirtualAddress shouldn't be allowed to modify the underlying memory
    [[nodiscard]] u8* as_ptr() { return reinterpret_cast<u8*>(m_address); }
    [[nodiscard]] const u8* as_ptr() const { return reinterpret_cast<const u8*>(m_address); }

    [[nodiscard]] VirtualAddress page_base() const { return VirtualAddress(m_address & ~(FlatPtr)0xfffu); }

private:
    FlatPtr m_address { 0 };
};

inline VirtualAddress operator-(const VirtualAddress& a, const VirtualAddress& b)
{
    return VirtualAddress(a.get() - b.get());
}

template<>
struct AK::Formatter<VirtualAddress> : AK::Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, const VirtualAddress& value)
    {
        return AK::Formatter<FormatString>::format(builder, "V{}", value.as_ptr());
    }
};
