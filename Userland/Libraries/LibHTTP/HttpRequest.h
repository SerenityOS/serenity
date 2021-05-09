/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/Forward.h>

namespace HTTP {

class HttpRequest {
public:
    enum Method {
        Invalid,
        HEAD,
        GET,
        POST
    };

    struct Header {
        String name;
        String value;
    };

    HttpRequest();
    ~HttpRequest();

    const String& resource() const { return m_resource; }
    const Vector<Header>& headers() const { return m_headers; }

    const URL& url() const { return m_url; }
    void set_url(const URL& url) { m_url = url; }

    Method method() const { return m_method; }
    void set_method(Method method) { m_method = method; }

    const ByteBuffer& body() const { return m_body; }
    void set_body(ReadonlyBytes body) { m_body = ByteBuffer::copy(body); }
    void set_body(ByteBuffer&& body) { m_body = move(body); }

    String method_name() const;
    ByteBuffer to_raw_request() const;

    void set_headers(const HashMap<String, String>&);

    static Optional<HttpRequest> from_raw_request(ReadonlyBytes);

private:
    URL m_url;
    String m_resource;
    Method m_method { GET };
    Vector<Header> m_headers;
    ByteBuffer m_body;
};

}
