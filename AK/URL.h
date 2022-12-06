/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

// On Linux distros that use mlibc `basename` is defined as a macro that expands to `__mlibc_gnu_basename` or `__mlibc_gnu_basename_c`, so we undefine it.
#if defined(AK_OS_LINUX) && defined(basename)
#    undef basename
#endif

namespace AK {

// NOTE: The member variables cannot contain any percent encoded sequences.
//       The URL parser automatically decodes those sequences and the serialize() method will re-encode them as necessary.
class URL {
    friend class URLParser;

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

    enum class ExcludeFragment {
        No,
        Yes
    };

    URL() = default;
    URL(StringView);
    URL(DeprecatedString const& string)
        : URL(string.view())
    {
    }

    bool is_valid() const { return m_valid; }

    DeprecatedString const& scheme() const { return m_scheme; }
    DeprecatedString const& username() const { return m_username; }
    DeprecatedString const& password() const { return m_password; }
    DeprecatedString const& host() const { return m_host; }
    Vector<DeprecatedString> const& paths() const { return m_paths; }
    DeprecatedString const& query() const { return m_query; }
    DeprecatedString const& fragment() const { return m_fragment; }
    Optional<u16> port() const { return m_port; }
    u16 port_or_default() const { return m_port.value_or(default_port_for_scheme(m_scheme)); }
    bool cannot_be_a_base_url() const { return m_cannot_be_a_base_url; }
    bool cannot_have_a_username_or_password_or_port() const { return m_host.is_null() || m_host.is_empty() || m_cannot_be_a_base_url || m_scheme == "file"sv; }

    bool includes_credentials() const { return !m_username.is_empty() || !m_password.is_empty(); }
    bool is_special() const { return is_special_scheme(m_scheme); }

    void set_scheme(DeprecatedString);
    void set_username(DeprecatedString);
    void set_password(DeprecatedString);
    void set_host(DeprecatedString);
    void set_port(Optional<u16>);
    void set_paths(Vector<DeprecatedString>);
    void set_query(DeprecatedString);
    void set_fragment(DeprecatedString);
    void set_cannot_be_a_base_url(bool value) { m_cannot_be_a_base_url = value; }
    void append_path(DeprecatedString path) { m_paths.append(move(path)); }

    DeprecatedString path() const;
    DeprecatedString basename() const;

    DeprecatedString serialize(ExcludeFragment = ExcludeFragment::No) const;
    DeprecatedString serialize_for_display() const;
    DeprecatedString to_deprecated_string() const { return serialize(); }

    // HTML origin
    DeprecatedString serialize_origin() const;

    bool equals(URL const& other, ExcludeFragment = ExcludeFragment::No) const;

    URL complete_url(DeprecatedString const&) const;

    bool data_payload_is_base64() const { return m_data_payload_is_base64; }
    DeprecatedString const& data_mime_type() const { return m_data_mime_type; }
    DeprecatedString const& data_payload() const { return m_data_payload; }

    static URL create_with_url_or_path(DeprecatedString const&);
    static URL create_with_file_scheme(DeprecatedString const& path, DeprecatedString const& fragment = {}, DeprecatedString const& hostname = {});
    static URL create_with_help_scheme(DeprecatedString const& path, DeprecatedString const& fragment = {}, DeprecatedString const& hostname = {});
    static URL create_with_data(DeprecatedString mime_type, DeprecatedString payload, bool is_base64 = false) { return URL(move(mime_type), move(payload), is_base64); };

    static bool scheme_requires_port(StringView);
    static u16 default_port_for_scheme(StringView);
    static bool is_special_scheme(StringView);

    enum class SpaceAsPlus {
        No,
        Yes,
    };
    static DeprecatedString percent_encode(StringView input, PercentEncodeSet set = PercentEncodeSet::Userinfo, SpaceAsPlus = SpaceAsPlus::No);
    static DeprecatedString percent_decode(StringView input);

    bool operator==(URL const& other) const { return equals(other, ExcludeFragment::No); }

    static bool code_point_is_in_percent_encode_set(u32 code_point, URL::PercentEncodeSet);

private:
    URL(DeprecatedString&& data_mime_type, DeprecatedString&& data_payload, bool payload_is_base64)
        : m_valid(true)
        , m_scheme("data")
        , m_data_payload_is_base64(payload_is_base64)
        , m_data_mime_type(move(data_mime_type))
        , m_data_payload(move(data_payload))
    {
    }

    bool compute_validity() const;
    DeprecatedString serialize_data_url() const;

    static void append_percent_encoded_if_necessary(StringBuilder&, u32 code_point, PercentEncodeSet set = PercentEncodeSet::Userinfo);
    static void append_percent_encoded(StringBuilder&, u32 code_point);

    bool m_valid { false };

    DeprecatedString m_scheme;
    DeprecatedString m_username;
    DeprecatedString m_password;
    DeprecatedString m_host;
    // NOTE: If the port is the default port for the scheme, m_port should be empty.
    Optional<u16> m_port;
    DeprecatedString m_path;
    Vector<DeprecatedString> m_paths;
    DeprecatedString m_query;
    DeprecatedString m_fragment;

    bool m_cannot_be_a_base_url { false };

    bool m_data_payload_is_base64 { false };
    DeprecatedString m_data_mime_type;
    DeprecatedString m_data_payload;
};

template<>
struct Formatter<URL> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, URL const& value)
    {
        return Formatter<StringView>::format(builder, value.serialize());
    }
};

template<>
struct Traits<URL> : public GenericTraits<URL> {
    static unsigned hash(URL const& url) { return url.to_deprecated_string().hash(); }
};

}
