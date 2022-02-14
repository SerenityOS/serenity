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

    bool is_same(const Origin& other) const
    {
        return protocol() == other.protocol()
            && host() == other.host()
            && port() == other.port();
    }

    bool operator==(Origin const& other) const { return is_same(other); }
    bool operator!=(Origin const& other) const { return !is_same(other); }

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
