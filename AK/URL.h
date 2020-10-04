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

#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

// FIXME: URL needs query string parsing.

class URL {
public:
    URL() { }
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
    String protocol() const { return m_protocol; }
    String host() const { return m_host; }
    String path() const { return m_path; }
    String query() const { return m_query; }
    String fragment() const { return m_fragment; }
    u16 port() const { return m_port; }

    void set_protocol(const String& protocol);
    void set_host(const String& host);
    void set_path(const String& path);
    void set_query(const String& query);
    void set_fragment(const String& fragment);
    void set_port(u16 port) { m_port = port; }

    String basename() const;
    String to_string() const;

    URL complete_url(const String&) const;

    bool data_payload_is_base64() const { return m_data_payload_is_base64; }
    const String& data_mime_type() const { return m_data_mime_type; }
    const String& data_payload() const { return m_data_payload; }

    static URL create_with_url_or_path(const String& url_or_path);
    static URL create_with_file_protocol(const String& path);
    static URL create_with_data(const StringView& mime_type, const StringView& payload, bool is_base64 = false);

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
    u16 m_port { 80 };
    bool m_data_payload_is_base64 { false };
    String m_protocol;
    String m_host;
    String m_path;
    String m_query;
    String m_fragment;
    String m_data_mime_type;
    String m_data_payload;
};

inline const LogStream& operator<<(const LogStream& stream, const URL& value)
{
    return stream << value.to_string();
}

template<>
struct Formatter<URL> : Formatter<StringView> {
    void format(TypeErasedFormatParams& params, FormatBuilder& builder, const URL& value)
    {
        Formatter<StringView>::format(params, builder, value.to_string());
    }
};

template<>
struct Traits<URL> : public GenericTraits<URL> {
    static unsigned hash(const URL& url) { return url.to_string().hash(); }
};

}
