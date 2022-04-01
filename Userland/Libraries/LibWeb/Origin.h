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
    Origin() = default;
    Origin(String const& protocol, String const& host, u16 port)
        : m_protocol(protocol)
        , m_host(host)
        , m_port(port)
    {
    }

    // https://html.spec.whatwg.org/multipage/origin.html#concept-origin-opaque
    bool is_opaque() const { return m_protocol.is_null() && m_host.is_null() && m_port == 0; }

    String const& protocol() const { return m_protocol; }
    String const& host() const { return m_host; }
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

    // https://html.spec.whatwg.org/multipage/origin.html#same-origin-domain
    bool is_same_origin_domain(Origin const& other) const
    {
        // 1. If A and B are the same opaque origin, then return true.
        if (is_opaque() && other.is_opaque())
            return true;

        // 2. If A and B are both tuple origins, run these substeps:
        if (!is_opaque() && !other.is_opaque()) {
            // 1. If A and B's schemes are identical, and their domains are identical and non-null, then return true.
            // FIXME: Check domains once supported.
            if (protocol() == other.protocol())
                return true;

            // 2. Otherwise, if A and B are same origin and their domains are identical and null, then return true.
            // FIXME: Check domains once supported.
            if (is_same_origin(other))
                return true;
        }

        // 3. Return false.
        return false;
    }

    // https://html.spec.whatwg.org/multipage/origin.html#ascii-serialisation-of-an-origin
    String serialize()
    {
        // 1. If origin is an opaque origin, then return "null"
        if (is_opaque())
            return "null";

        // 2. Otherwise, let result be origin's scheme.
        StringBuilder result;
        result.append(protocol());

        // 3. Append "://" to result.
        result.append("://");

        // 4. Append origin's host, serialized, to result.
        result.append(host());

        // 5. If origin's port is non-null, append a U+003A COLON character (:), and origin's port, serialized, to result.
        if (port() != 0) {
            result.append(":");
            result.append(String::number(port()));
        }
        // 6. Return result
        return result.to_string();
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
