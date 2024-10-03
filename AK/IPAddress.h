/*
 * Copyright (c) 2024, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IPv4Address.h>
#include <AK/IPv6Address.h>
#include <AK/Variant.h>

namespace AK {

enum class IPVersion : bool {
    IPv6,
    IPv4,
};

class IPAddress {
    AK_MAKE_DEFAULT_COPYABLE(IPAddress);
    AK_MAKE_DEFAULT_MOVABLE(IPAddress);

public:
    constexpr IPAddress()
        : m_variant(IPv4Address {})
    {
    }

    IPAddress(IPv4Address const addr)
        : m_variant(addr)
    {
    }
    IPAddress(IPv6Address const addr)
        : m_variant(addr)
    {
    }

    constexpr bool is_ipv4() { return m_variant.has<IPv4Address>(); }
    constexpr bool is_ipv6() { return m_variant.has<IPv6Address>(); }

    template<typename... Fs>
    ALWAYS_INLINE decltype(auto) visit(Fs&&... functions) const
    {
        return m_variant.visit(forward<Fs&&>(functions)...);
    }

    constexpr bool is_zero() const
    {
        return visit([](auto const& address) { return address.is_zero(); });
    }

    constexpr IPv4Address as_v4() const { return m_variant.get<IPv4Address>(); }
    constexpr IPv6Address as_v6() const { return m_variant.get<IPv6Address>(); }

    constexpr IPVersion version() const
    {
        return visit([](IPv6Address const&) { return IPVersion::IPv6; },
            [](IPv4Address const&) { return IPVersion::IPv4; });
    }

private:
    Variant<IPv4Address, IPv6Address> m_variant;
};

template<>
struct Formatter<IPAddress> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, IPAddress const& ip_address)
    {
        return ip_address.visit(
            [&](IPv4Address const& contained_address) {
                Formatter<IPv4Address> formatter { *this };
                return formatter.format(builder, contained_address);
            },
            [&](IPv6Address const& contained_address) {
                Formatter<IPv6Address> formatter { *this };
                return formatter.format(builder, contained_address);
            });
    }
};

}

#if USING_AK_GLOBALLY
using AK::IPAddress;
using AK::IPVersion;
#endif
