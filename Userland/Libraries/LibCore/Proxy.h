/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Types.h>
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
};
}

namespace IPC {
bool encode(Encoder&, Core::ProxyData const&);
ErrorOr<void> decode(Decoder&, Core::ProxyData&);
}
