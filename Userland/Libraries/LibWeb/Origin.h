/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace Web {

class Origin {
public:
    Origin() { }
    Origin(const String& protocol, const String& host, u16 port)
        : m_protocol(protocol)
        , m_host(host)
        , m_port(port)
    {
    }

    // https://html.spec.whatwg.org/multipage/origin.html#concept-origin-opaque
    bool is_opaque() const { return m_protocol.is_null() && m_host.is_null() && m_port == 0; }

    const String& protocol() const { return m_protocol; }
    const String& host() const { return m_host; }
    u16 port() const { return m_port; }

    // https://html.spec.whatwg.org/multipage/origin.html#same-origin
    bool is_same_origin(Origin const& other) const
    {
        // 1. If A and B are the same opaque origin, then return true.
        if (is_opaque() && other.is_opaque())
            return true;

        // 2. If A and B are both tuple origins and their schemes, hosts, and port are identical, then return true.
        // 3. Return false.
        return protocol() == other.protocol()
            && host() == other.host()
            && port() == other.port();
    }

    bool operator==(Origin const& other) const { return is_same_origin(other); }
    bool operator!=(Origin const& other) const { return !is_same_origin(other); }

private:
    String m_protocol;
    String m_host;
    u16 m_port { 0 };
};

}

namespace AK {
template<>
struct Traits<Web::Origin> : public GenericTraits<Web::Origin> {
    static unsigned hash(Web::Origin const& origin)
    {
        return pair_int_hash(origin.protocol().hash(), pair_int_hash(int_hash(origin.port()), origin.host().hash()));
    }
};
} // namespace AK
