/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>

namespace AK {

// NOTE: The member variables cannot contain any percent encoded sequences.
//       The URL parser automatically decodes those sequences and the the serialize() method will re-encode them as necessary.
class URL {
public:
    enum class PercentEncodeSet {
        C0Control,
        Fragment,
        Query,
        SpecialQuery,
        Path,
        Userinfo,
        Component,
        ApplicationXWWWFormUrlencoded,
        EncodeURI
    };

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

    String scheme() const { return m_scheme; }
    String protocol() const { return m_scheme; }
    String username() const { return m_username; }
    String password() const { return m_password; }
    String host() const { return m_host; }
    const Vector<String>& paths() const { return m_paths; }
    String query() const { return m_query; }
    String fragment() const { return m_fragment; }
    u16 port() const { return m_port ? m_port : default_port_for_scheme(m_scheme); }
    bool cannot_be_a_base_url() const { return m_cannot_be_a_base_url; }

    void set_scheme(const String&);
    void set_protocol(const String& protocol) { set_scheme(protocol); }
    void set_username(const String&);
    void set_password(const String&);
    void set_host(const String&);
    void set_port(const u16);
    void set_path(const String&);
    void set_paths(const Vector<String>&);
    void set_query(const String&);
    void set_fragment(const String&);
    void set_cannot_be_a_base_url(const bool value) { m_cannot_be_a_base_url = value; }
    void append_path(const String& path) { m_paths.append(path); }

    String path() const;
    String basename() const;
    String to_string() const;
    String to_string_encoded() const
    {
        return percent_encode(to_string(), PercentEncodeSet::EncodeURI);
    }

    URL complete_url(const String&) const;

    bool data_payload_is_base64() const { return m_data_payload_is_base64; }
    const String& data_mime_type() const { return m_data_mime_type; }
    const String& data_payload() const { return m_data_payload; }

    static URL create_with_url_or_path(const String&);
    static URL create_with_file_scheme(const String& path, const String& fragment = {});
    static URL create_with_file_protocol(const String& path, const String& fragment = {}) { return create_with_file_scheme(path, fragment); }
    static URL create_with_data(const StringView& mime_type, const StringView& payload, bool is_base64 = false);
    static bool scheme_requires_port(const StringView&);
    static u16 default_port_for_scheme(const StringView&);

    static String percent_encode(const StringView& input, PercentEncodeSet set = PercentEncodeSet::Userinfo);
    static String percent_decode(const StringView& input);

    bool operator==(const URL& other) const
    {
        if (this == &other)
            return true;
        return to_string() == other.to_string();
    }

private:
    bool parse(const StringView&);
    bool compute_validity() const;

    static void append_percent_encoded_if_necessary(StringBuilder&, u32 code_point, PercentEncodeSet set = PercentEncodeSet::Userinfo);
    static void append_percent_encoded(StringBuilder&, u32 code_point);

    bool m_valid { false };

    String m_scheme;
    String m_username;
    String m_password;
    String m_host;
    // NOTE: If the port is the default port for the scheme, m_port should be 0.
    u16 m_port { 0 };
    String m_path;
    Vector<String> m_paths;
    String m_query;
    String m_fragment;

    bool m_cannot_be_a_base_url { false };

    bool m_data_payload_is_base64 { false };
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
