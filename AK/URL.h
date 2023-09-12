/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

// On Linux distros that use mlibc `basename` is defined as a macro that expands to `__mlibc_gnu_basename` or `__mlibc_gnu_basename_c`, so we undefine it.
#if defined(AK_OS_LINUX) && defined(basename)
#    undef basename
#endif

namespace AK {

// https://url.spec.whatwg.org/#url-representation
// A URL is a struct that represents a universal identifier. To disambiguate from a valid URL string it can also be referred to as a URL record.
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
    URL(String const& string)
        : URL(string.bytes_as_string_view())
    {
    }

    // https://url.spec.whatwg.org/#concept-ipv4
    // An IPv4 address is a 32-bit unsigned integer that identifies a network address. [RFC791]
    // FIXME: It would be nice if this were an AK::IPv4Address
    using IPv4Address = u32;

    // https://url.spec.whatwg.org/#concept-ipv6
    // An IPv6 address is a 128-bit unsigned integer that identifies a network address. For the purposes of this standard
    // it is represented as a list of eight 16-bit unsigned integers, also known as IPv6 pieces. [RFC4291]
    // FIXME: It would be nice if this were an AK::IPv6Address
    using IPv6Address = Array<u16, 8>;

    // https://url.spec.whatwg.org/#concept-host
    // A host is a domain, an IP address, an opaque host, or an empty host. Typically a host serves as a network address,
    // but it is sometimes used as opaque identifier in URLs where a network address is not necessary.
    using Host = Variant<IPv4Address, IPv6Address, String, Empty>;

    bool is_valid() const { return m_valid; }

    String const& scheme() const { return m_scheme; }
    ErrorOr<String> username() const;
    ErrorOr<String> password() const;
    Host const& host() const { return m_host; }
    ErrorOr<String> serialized_host() const;
    DeprecatedString basename() const;
    Optional<String> const& query() const { return m_query; }
    Optional<String> const& fragment() const { return m_fragment; }
    Optional<u16> port() const { return m_port; }
    DeprecatedString path_segment_at_index(size_t index) const;
    size_t path_segment_count() const { return m_paths.size(); }

    u16 port_or_default() const { return m_port.value_or(default_port_for_scheme(m_scheme).value_or(0)); }
    bool cannot_be_a_base_url() const { return m_cannot_be_a_base_url; }
    bool cannot_have_a_username_or_password_or_port() const;

    bool includes_credentials() const { return !m_username.is_empty() || !m_password.is_empty(); }
    bool is_special() const { return is_special_scheme(m_scheme); }

    void set_scheme(String);
    ErrorOr<void> set_username(StringView);
    ErrorOr<void> set_password(StringView);
    void set_host(Host);
    void set_port(Optional<u16>);
    void set_paths(Vector<DeprecatedString> const&);
    void set_query(Optional<String> query) { m_query = move(query); }
    void set_fragment(Optional<String> fragment) { m_fragment = move(fragment); }
    void set_cannot_be_a_base_url(bool value) { m_cannot_be_a_base_url = value; }
    void append_path(StringView);
    void append_slash()
    {
        // NOTE: To indicate that we want to end the path with a slash, we have to append an empty path segment.
        m_paths.append("");
    }

    enum class ApplyPercentDecoding {
        Yes,
        No
    };
    DeprecatedString serialize_path(ApplyPercentDecoding = ApplyPercentDecoding::Yes) const;
    DeprecatedString serialize(ExcludeFragment = ExcludeFragment::No) const;
    DeprecatedString serialize_for_display() const;
    DeprecatedString to_deprecated_string() const { return serialize(); }
    ErrorOr<String> to_string() const;

    // HTML origin
    DeprecatedString serialize_origin() const;

    bool equals(URL const& other, ExcludeFragment = ExcludeFragment::No) const;

    URL complete_url(StringView) const;

    struct DataURL {
        String mime_type;
        ByteBuffer body;
    };
    ErrorOr<DataURL> process_data_url() const;

    static URL create_with_url_or_path(DeprecatedString const&);
    static URL create_with_file_scheme(DeprecatedString const& path, DeprecatedString const& fragment = {}, DeprecatedString const& hostname = {});
    static URL create_with_help_scheme(DeprecatedString const& path, DeprecatedString const& fragment = {}, DeprecatedString const& hostname = {});
    static URL create_with_data(StringView mime_type, StringView payload, bool is_base64 = false);

    static Optional<u16> default_port_for_scheme(StringView);
    static bool is_special_scheme(StringView);

    enum class SpaceAsPlus {
        No,
        Yes,
    };
    static DeprecatedString percent_encode(StringView input, PercentEncodeSet set = PercentEncodeSet::Userinfo, SpaceAsPlus = SpaceAsPlus::No);
    static DeprecatedString percent_decode(StringView input);

    bool operator==(URL const& other) const { return equals(other, ExcludeFragment::No); }

    static bool code_point_is_in_percent_encode_set(u32 code_point, URL::PercentEncodeSet);

    String const& raw_username() const { return m_username; }
    String const& raw_password() const { return m_password; }

private:
    bool compute_validity() const;

    static void append_percent_encoded_if_necessary(StringBuilder&, u32 code_point, PercentEncodeSet set = PercentEncodeSet::Userinfo);
    static void append_percent_encoded(StringBuilder&, u32 code_point);

    bool m_valid { false };

    // A URL’s scheme is an ASCII string that identifies the type of URL and can be used to dispatch a URL for further processing after parsing. It is initially the empty string.
    String m_scheme;

    // A URL’s username is an ASCII string identifying a username. It is initially the empty string.
    String m_username;

    // A URL’s password is an ASCII string identifying a password. It is initially the empty string.
    String m_password;

    // A URL’s host is null or a host. It is initially null.
    Host m_host;

    // A URL’s port is either null or a 16-bit unsigned integer that identifies a networking port. It is initially null.
    Optional<u16> m_port;

    // A URL’s path is either a URL path segment or a list of zero or more URL path segments, usually identifying a location. It is initially « ».
    // A URL path segment is an ASCII string. It commonly refers to a directory or a file, but has no predefined meaning.
    Vector<DeprecatedString> m_paths;

    // A URL’s query is either null or an ASCII string. It is initially null.
    Optional<String> m_query;

    // A URL’s fragment is either null or an ASCII string that can be used for further processing on the resource the URL’s other components identify. It is initially null.
    Optional<String> m_fragment;

    bool m_cannot_be_a_base_url { false };
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
