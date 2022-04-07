/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/IPv4Address.h>
#include <AK/Types.h>
#include <AK/URL.h>
#include <LibIPC/Forward.h>

namespace Core {
// FIXME: Username/password support.
struct ProxyData {
    enum Type {
        Direct,
        SOCKS5,
    } type { Type::Direct };

    u32 host_ipv4 { 0 };
    int port { 0 };

    bool operator==(ProxyData const& other) const = default;

    static ErrorOr<ProxyData> parse_url(URL const& url)
    {
        if (!url.is_valid())
            return Error::from_string_literal("Invalid proxy URL");

        ProxyData proxy_data;
        if (url.scheme() != "socks5")
            return Error::from_string_literal("Unsupported proxy type");

        proxy_data.type = ProxyData::Type::SOCKS5;

        auto host_ipv4 = IPv4Address::from_string(url.host());
        if (!host_ipv4.has_value())
            return Error::from_string_literal("Invalid proxy host, must be an IPv4 address");
        proxy_data.host_ipv4 = host_ipv4->to_u32();

        auto port = url.port();
        if (!port.has_value())
            return Error::from_string_literal("Invalid proxy, must have a port");
        proxy_data.port = *port;

        return proxy_data;
    }
};
}

namespace IPC {
bool encode(Encoder&, Core::ProxyData const&);
ErrorOr<void> decode(Decoder&, Core::ProxyData&);
}
