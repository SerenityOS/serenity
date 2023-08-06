/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/EventReceiver.h>
#include <LibTLS/TLSv12.h>
#include <LibWebSocket/Message.h>

namespace WebSocket {

class ConnectionInfo final {
public:
    ConnectionInfo(URL);

    URL const& url() const { return m_url; }

    DeprecatedString const& origin() const { return m_origin; }
    void set_origin(DeprecatedString origin) { m_origin = move(origin); }

    Vector<DeprecatedString> const& protocols() const { return m_protocols; }
    void set_protocols(Vector<DeprecatedString> protocols) { m_protocols = move(protocols); }

    Vector<DeprecatedString> const& extensions() const { return m_extensions; }
    void set_extensions(Vector<DeprecatedString> extensions) { m_extensions = move(extensions); }

    struct Header {
        DeprecatedString name;
        DeprecatedString value;
    };
    Vector<Header> const& headers() const { return m_headers; }
    void set_headers(Vector<Header> headers) { m_headers = move(headers); }

    // secure flag - defined in RFC 6455 Section 3
    bool is_secure() const;

    // "resource-name" or "/resource name/" - defined in RFC 6455 Section 3
    DeprecatedString resource_name() const;

private:
    URL m_url;
    DeprecatedString m_origin;
    Vector<DeprecatedString> m_protocols {};
    Vector<DeprecatedString> m_extensions {};
    Vector<Header> m_headers {};
};

}
