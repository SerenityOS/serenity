/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AllOf.h>
#include <AK/Array.h>
#include <AK/Assertions.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>

class [[gnu::packed]] MACAddress {
    static constexpr size_t s_mac_address_length = 6u;

public:
    constexpr MACAddress() = default;

    constexpr MACAddress(u8 a, u8 b, u8 c, u8 d, u8 e, u8 f)
    {
        m_data[0] = a;
        m_data[1] = b;
        m_data[2] = c;
        m_data[3] = d;
        m_data[4] = e;
        m_data[5] = f;
    }

    constexpr ~MACAddress() = default;

    constexpr const u8& operator[](unsigned i) const
    {
        VERIFY(i < s_mac_address_length);
        return m_data[i];
    }

    constexpr u8& operator[](unsigned i)
    {
        VERIFY(i < s_mac_address_length);
        return m_data[i];
    }

    constexpr bool operator==(const MACAddress& other) const
    {
        for (auto i = 0u; i < m_data.size(); ++i) {
            if (m_data[i] != other.m_data[i]) {
                return false;
            }
        }
        return true;
    }

    String to_string() const
    {
        return String::formatted("{:02x}:{:02x}:{:02x}:{:02x}:{:02x}:{:02x}", m_data[0], m_data[1], m_data[2], m_data[3], m_data[4], m_data[5]);
    }

    static Optional<MACAddress> from_string(StringView string)
    {
        if (string.is_null())
            return {};

        const auto parts = string.split_view(":");
        if (parts.size() != 6)
            return {};

        auto a = AK::StringUtils::convert_to_uint_from_hex(parts[0]).value_or(256);
        auto b = AK::StringUtils::convert_to_uint_from_hex(parts[1]).value_or(256);
        auto c = AK::StringUtils::convert_to_uint_from_hex(parts[2]).value_or(256);
        auto d = AK::StringUtils::convert_to_uint_from_hex(parts[3]).value_or(256);
        auto e = AK::StringUtils::convert_to_uint_from_hex(parts[4]).value_or(256);
        auto f = AK::StringUtils::convert_to_uint_from_hex(parts[5]).value_or(256);

        if (a > 255 || b > 255 || c > 255 || d > 255 || e > 255 || f > 255)
            return {};

        return MACAddress(a, b, c, d, e, f);
    }

    constexpr bool is_zero() const
    {
        return all_of(m_data, [](const auto octet) { return octet == 0; });
    }

    void copy_to(Bytes destination) const
    {
        m_data.span().copy_to(destination);
    }

private:
    Array<u8, s_mac_address_length> m_data {};
};

static_assert(sizeof(MACAddress) == 6u);

namespace AK {

template<>
struct Traits<MACAddress> : public GenericTraits<MACAddress> {
    static unsigned hash(const MACAddress& address) { return string_hash((const char*)&address, sizeof(address)); }
};

}
