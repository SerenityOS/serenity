/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

    static LoadRequest create_for_url_on_page(URL const& url, Page* page);

    bool is_valid() const { return m_url.is_valid(); }

    URL const& url() const { return m_url; }
    void set_url(URL const& url) { m_url = url; }

    String const& method() const { return m_method; }
    void set_method(String const& method) { m_method = method; }

    ByteBuffer const& body() const { return m_body; }
    void set_body(ByteBuffer const& body) { m_body = body; }

    unsigned hash() const
    {
        // FIXME: Include headers in the hash as well
        return pair_int_hash(pair_int_hash(m_url.to_string().hash(), m_method.hash()), string_hash((char const*)m_body.data(), m_body.size()));
    }

    bool operator==(LoadRequest const& other) const
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

    void set_header(String const& name, String const& value) { m_headers.set(name, value); }
    String header(String const& name) const { return m_headers.get(name).value_or({}); }

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
