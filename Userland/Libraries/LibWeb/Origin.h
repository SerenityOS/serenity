/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/String.h>

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

    bool is_null() const { return m_protocol.is_null() && m_host.is_null() && !m_port; }

    const String& protocol() const { return m_protocol; }
    const String& host() const { return m_host; }
    u16 port() const { return m_port; }

    bool is_same(const Origin& other) const
    {
        return protocol() == other.protocol()
            && host() == other.host()
            && port() == other.port();
    }

private:
    String m_protocol;
    String m_host;
    u16 m_port { 0 };
};

}
