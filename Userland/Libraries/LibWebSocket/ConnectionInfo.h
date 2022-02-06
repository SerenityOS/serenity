/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/Object.h>
#include <LibTLS/TLSv12.h>
#include <LibWebSocket/Message.h>

namespace WebSocket {

class ConnectionInfo final {
public:
    ConnectionInfo(URL);

    URL const& url() const { return m_url; }

    String const& origin() const { return m_origin; }
    void set_origin(String origin) { m_origin = move(origin); }

    Vector<String> const& protocols() const { return m_protocols; }
    void set_protocols(Vector<String> protocols) { m_protocols = move(protocols); }

    Vector<String> const& extensions() const { return m_extensions; }
    void set_extensions(Vector<String> extensions) { m_extensions = move(extensions); }

    struct Header {
        String name;
        String value;
    };
    Vector<Header> const& headers() const { return m_headers; }
    void set_headers(Vector<Header> headers) { m_headers = move(headers); }

    // secure flag - defined in RFC 6455 Section 3
    bool is_secure() const;

    // "resource-name" or "/resource name/" - defined in RFC 6455 Section 3
    String resource_name() const;

private:
    URL m_url;
    String m_origin;
    Vector<String> m_protocols {};
    Vector<String> m_extensions {};
    Vector<Header> m_headers {};
};

}
