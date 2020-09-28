/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    void set_body(const ByteBuffer& body) { m_body = body; }

    String method_name() const;
    ByteBuffer to_raw_request() const;

    RefPtr<Core::NetworkJob> schedule();

    void set_headers(const HashMap<String, String>&);

    static Optional<HttpRequest> from_raw_request(const ByteBuffer&);

private:
    URL m_url;
    String m_resource;
    Method m_method { GET };
    Vector<Header> m_headers;
    ByteBuffer m_body;
};

}
