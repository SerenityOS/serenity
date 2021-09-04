/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace Web {

class Origin {
public:
    Origin() { }
    Origin(String const& protocol, String const& host, u16 port)
        : m_protocol(protocol)
        , m_host(host)
        , m_port(port)
    {
    }

    bool is_null() const { return m_protocol.is_null() && m_host.is_null() && !m_port; }

    String const& protocol() const { return m_protocol; }
    String const& host() const { return m_host; }
    u16 port() const { return m_port; }

    bool is_same(Origin const& other) const
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
