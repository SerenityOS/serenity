/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Benoit Lormeau <blormeau@outlook.com>
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
#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

class URL {
public:
    class Payload : public RefCounted<Payload> {
    public:
        enum class Encoding {
            UrlEncoded,
            Base64,
        };

        const String& mime_type() const { return m_mime_type; }
        void set_mime_type(const String& mime_type) { m_mime_type = mime_type; }
        Encoding encoding() const { return m_encoding; }
        void set_encoding(Encoding encoding) { m_encoding = encoding; }
        const ByteBuffer& data() const { return m_data; }
        void set_data(const ByteBuffer& data) { m_data = data; }

        String m_mime_type;
        Encoding m_encoding { Encoding::UrlEncoded };
        ByteBuffer m_data;
    };

    URL() {}
    URL(const StringView&);

    URL(const char* string)
        : URL(StringView(string))
    {
    }

    URL(const String& string)
        : URL(string.view())
    {
    }

    bool is_valid() const { return m_valid; }
    String scheme() const { return m_scheme; }
    String username() const { return m_username; }
    String password() const { return m_password; }
    String host() const { return m_host; }
    u16 port() const { return m_port; }
    String path() const { return m_path; }
    String query() const { return m_query; }
    String fragment() const { return m_fragment; }

    void set_scheme(const String& scheme);
    void set_username(const String& username);
    void set_password(const String& password);
    void set_host(const String& host);
    void set_port(u16 port) { m_port = port; }
    void set_path(const String& path);
    void set_query(const String& query);
    void set_fragment(const String& fragment);

    HashMap<String, String> parse_query_fields() const;
    void set_query_fields(const HashMap<String, String>&);

    String to_string() const;
    URL complete_url(const String&) const;
    String basename() const;

    bool has_payload() const { return !m_payload.is_null(); }
    void set_payload(RefPtr<Payload> payload) { m_payload = payload; }
    const Payload& payload() const
    {
        ASSERT(has_payload());
        return *m_payload;
    }

    static URL create_with_url_or_path(const String& url_or_path);
    static URL create_with_file_protocol(const String& path);
    static u16 get_protocol_default_port(const String&);

    static String encode(const StringView&);
    static String decode(const StringView&);
    static String query_encode(const StringView&);
    static String query_decode(const StringView&);

    bool operator==(const URL& other) const
    {
        if (this == &other)
            return true;
        return to_string() == other.to_string();
    }

private:
    bool parse(const StringView&);
    bool compute_validity() const;

    bool m_valid { false };
    String m_scheme;
    String m_username;
    String m_password;
    String m_host;
    u16 m_port { 80 };
    String m_path;
    String m_query;
    String m_fragment;

    RefPtr<Payload> m_payload;
};

inline const LogStream& operator<<(const LogStream& stream, const URL& value)
{
    return stream << value.to_string();
}

template<>
struct Traits<URL> : public GenericTraits<URL> {
    static unsigned hash(const URL& url) { return url.to_string().hash(); }
};

}

using AK::URL;
