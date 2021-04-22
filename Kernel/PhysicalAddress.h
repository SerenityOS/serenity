/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Types.h>

class PhysicalAddress {
public:
    PhysicalAddress() = default;
    explicit PhysicalAddress(FlatPtr address)
        : m_address(address)
    {
    }

    [[nodiscard]] PhysicalAddress offset(FlatPtr o) const { return PhysicalAddress(m_address + o); }
    [[nodiscard]] FlatPtr get() const { return m_address; }
    void set(FlatPtr address) { m_address = address; }
    void mask(FlatPtr m) { m_address &= m; }

    [[nodiscard]] bool is_null() const { return m_address == 0; }

    [[nodiscard]] u8* as_ptr() { return reinterpret_cast<u8*>(m_address); }
    [[nodiscard]] const u8* as_ptr() const { return reinterpret_cast<const u8*>(m_address); }

    [[nodiscard]] PhysicalAddress page_base() const { return PhysicalAddress(m_address & 0xfffff000); }
    [[nodiscard]] FlatPtr offset_in_page() const { return PhysicalAddress(m_address & 0xfff).get(); }

    bool operator==(const PhysicalAddress& other) const { return m_address == other.m_address; }
    bool operator!=(const PhysicalAddress& other) const { return m_address != other.m_address; }
    bool operator>(const PhysicalAddress& other) const { return m_address > other.m_address; }
    bool operator>=(const PhysicalAddress& other) const { return m_address >= other.m_address; }
    bool operator<(const PhysicalAddress& other) const { return m_address < other.m_address; }
    bool operator<=(const PhysicalAddress& other) const { return m_address <= other.m_address; }

private:
    FlatPtr m_address { 0 };
};

template<>
struct AK::Formatter<PhysicalAddress> : AK::Formatter<FormatString> {
    void format(FormatBuilder& builder, PhysicalAddress value)
    {
        return AK::Formatter<FormatString>::format(builder, "P{}", value.as_ptr());
    }
};
