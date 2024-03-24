/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2023-2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

// On Linux distros that use mlibc `basename` is defined as a macro that expands to `__mlibc_gnu_basename` or `__mlibc_gnu_basename_c`, so we undefine it.
#if defined(AK_OS_LINUX) && defined(basename)
#    undef basename
#endif

namespace URL {

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

enum class ApplyPercentDecoding {
    Yes,
    No
};

void append_percent_encoded_if_necessary(StringBuilder&, u32 code_point, PercentEncodeSet set = PercentEncodeSet::Userinfo);
void append_percent_encoded(StringBuilder&, u32 code_point);
bool code_point_is_in_percent_encode_set(u32 code_point, PercentEncodeSet);
Optional<u16> default_port_for_scheme(StringView);
bool is_special_scheme(StringView);

enum class SpaceAsPlus {
    No,
    Yes,
};
ByteString percent_encode(StringView input, PercentEncodeSet set = PercentEncodeSet::Userinfo, SpaceAsPlus = SpaceAsPlus::No);
ByteString percent_decode(StringView input);

// https://url.spec.whatwg.org/#url-representation
// A URL is a struct that represents a universal identifier. To disambiguate from a valid URL string it can also be referred to as a URL record.
class URL {
    friend class Parser;

public:
    URL() = default;
    URL(StringView);
    URL(ByteString const& string)
        : URL(string.view())
    {
    }
    URL(String const& string)
        : URL(string.bytes_as_string_view())
    {
    }

    bool is_valid() const { return m_valid; }

    String const& scheme() const { return m_scheme; }
    ErrorOr<String> username() const;
    ErrorOr<String> password() const;
    Host const& host() const { return m_host; }
    ErrorOr<String> serialized_host() const;
    ByteString basename() const;
    Optional<String> const& query() const { return m_query; }
    Optional<String> const& fragment() const { return m_fragment; }
    Optional<u16> port() const { return m_port; }
    ByteString path_segment_at_index(size_t index) const;
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
    void set_paths(Vector<ByteString> const&);
    void set_query(Optional<String> query) { m_query = move(query); }
    void set_fragment(Optional<String> fragment) { m_fragment = move(fragment); }
    void set_cannot_be_a_base_url(bool value) { m_cannot_be_a_base_url = value; }
    void append_path(StringView);
    void append_slash()
    {
        // NOTE: To indicate that we want to end the path with a slash, we have to append an empty path segment.
        m_paths.append(String {});
    }

    ByteString serialize_path(ApplyPercentDecoding = ApplyPercentDecoding::Yes) const;
    ByteString serialize(ExcludeFragment = ExcludeFragment::No) const;
    ByteString serialize_for_display() const;
    ByteString to_byte_string() const { return serialize(); }
    ErrorOr<String> to_string() const;

    // HTML origin
    ByteString serialize_origin() const;

    bool equals(URL const& other, ExcludeFragment = ExcludeFragment::No) const;

    URL complete_url(StringView) const;

    bool operator==(URL const& other) const { return equals(other, ExcludeFragment::No); }

    String const& raw_username() const { return m_username; }
    String const& raw_password() const { return m_password; }

private:
    bool compute_validity() const;

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
    Vector<String> m_paths;

    // A URL’s query is either null or an ASCII string. It is initially null.
    Optional<String> m_query;

    // A URL’s fragment is either null or an ASCII string that can be used for further processing on the resource the URL’s other components identify. It is initially null.
    Optional<String> m_fragment;

    bool m_cannot_be_a_base_url { false };
};

URL create_with_url_or_path(ByteString const&);
URL create_with_file_scheme(ByteString const& path, ByteString const& fragment = {}, ByteString const& hostname = {});
URL create_with_help_scheme(ByteString const& path, ByteString const& fragment = {}, ByteString const& hostname = {});
URL create_with_data(StringView mime_type, StringView payload, bool is_base64 = false);

}

template<>
struct AK::Formatter<URL::URL> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, URL::URL const& value)
    {
        return Formatter<StringView>::format(builder, value.serialize());
    }
};

template<>
struct AK::Traits<URL::URL> : public AK::DefaultTraits<URL::URL> {
    static unsigned hash(URL::URL const& url) { return url.to_byte_string().hash(); }
};
