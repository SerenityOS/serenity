/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/HashMap.h>
#include <AK/URL.h>
#include <LibWeb/Forward.h>

namespace Web {

class LoadRequest {
public:
    LoadRequest()
    {
    }

    bool is_valid() const { return m_url.is_valid(); }

    const URL& url() const { return m_url; }
    void set_url(const URL& url) { m_url = url; }

    const String& method() const { return m_method; }
    void set_method(const String& method) { m_method = method; }

    const ByteBuffer& body() const { return m_body; }
    void set_body(const ByteBuffer& body) { m_body = body; }

    unsigned hash() const
    {
        // FIXME: Include headers in the hash as well
        return pair_int_hash(pair_int_hash(m_url.to_string().hash(), m_method.hash()), string_hash((const char*)m_body.data(), m_body.size()));
    }

    bool operator==(const LoadRequest& other) const
    {
        if (m_headers.size() != other.m_headers.size())
            return false;
        for (auto& it : m_headers) {
            auto jt = other.m_headers.find(it.key);
            if (jt == other.m_headers.end())
                return false;
            if (it.value != jt->value)
                return false;
        }
        return m_url == other.m_url && m_method == other.m_method && m_body == other.m_body;
    }

    void set_header(const String& name, const String& value) { m_headers.set(name, value); }
    String header(const String& name) const { return m_headers.get(name).value_or({}); }

    const HashMap<String, String>& headers() const { return m_headers; }

private:
    URL m_url;
    String m_method { "GET" };
    HashMap<String, String> m_headers;
    ByteBuffer m_body;
};

}

namespace AK {

template<>
struct Traits<Web::LoadRequest> : public GenericTraits<Web::LoadRequest> {
    static unsigned hash(const Web::LoadRequest& request) { return request.hash(); }
};

}
