/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/URLParser.h>

namespace AK {

// FIXME: URL needs query string parsing.

class URL {
public:
    URL() = default;
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
    void set_port(const u16 port);
    void set_path(const String& path);
    void set_query(const String& query);
    void set_fragment(const String& fragment);

    String basename() const;
    String to_string() const;
    String to_string_encoded() const
    {
        // Exclusion character set is the same JS's encodeURI() uses
        return urlencode(to_string(), "#$&+,/:;=?@");
    }

    URL complete_url(const String&) const;

    bool data_payload_is_base64() const { return m_data_payload_is_base64; }
    const String& data_mime_type() const { return m_data_mime_type; }
    const String& data_payload() const { return m_data_payload; }

    static URL create_with_url_or_path(const String& url_or_path);
    static URL create_with_file_protocol(const String& path, const String& fragment = {});
    static URL create_with_data(const StringView& mime_type, const StringView& payload, bool is_base64 = false);
    static bool protocol_requires_port(const String& protocol);
    static u16 default_port_for_protocol(const String& protocol);

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
    u16 m_port { 0 };
    bool m_data_payload_is_base64 { false };
    String m_protocol;
    String m_host;
    String m_path;
    String m_query;
    String m_fragment;
    String m_data_mime_type;
    String m_data_payload;
};

template<>
struct Formatter<URL> : Formatter<StringView> {
    void format(FormatBuilder& builder, const URL& value)
    {
        Formatter<StringView>::format(builder, value.to_string());
    }
};

template<>
struct Traits<URL> : public GenericTraits<URL> {
    static unsigned hash(const URL& url) { return url.to_string().hash(); }
};

}
