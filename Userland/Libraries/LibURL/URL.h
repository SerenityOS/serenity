/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 * Copyright (c) 2023-2024, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/CopyOnWrite.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibURL/Host.h>
#include <LibURL/Origin.h>

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

// https://w3c.github.io/FileAPI/#blob-url-entry
// NOTE: This represents the raw bytes behind a 'Blob' (and does not yet support a MediaSourceQuery).
struct BlobURLEntry {
    String type;
    ByteBuffer byte_buffer;
    Origin environment_origin;
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
String percent_encode(StringView input, PercentEncodeSet set = PercentEncodeSet::Userinfo, SpaceAsPlus = SpaceAsPlus::No);
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

    bool is_valid() const { return m_data->valid; }

    String const& scheme() const { return m_data->scheme; }
    String const& username() const { return m_data->username; }
    String const& password() const { return m_data->password; }
    Host const& host() const { return m_data->host; }
    ErrorOr<String> serialized_host() const;
    ByteString basename() const;
    Optional<String> const& query() const { return m_data->query; }
    Optional<String> const& fragment() const { return m_data->fragment; }
    Optional<u16> port() const { return m_data->port; }
    ByteString path_segment_at_index(size_t index) const;
    size_t path_segment_count() const { return m_data->paths.size(); }

    u16 port_or_default() const { return m_data->port.value_or(default_port_for_scheme(m_data->scheme).value_or(0)); }
    bool cannot_be_a_base_url() const { return m_data->cannot_be_a_base_url; }
    bool cannot_have_a_username_or_password_or_port() const;

    bool includes_credentials() const { return !m_data->username.is_empty() || !m_data->password.is_empty(); }
    bool is_special() const { return is_special_scheme(m_data->scheme); }

    void set_scheme(String);
    void set_username(StringView);
    void set_password(StringView);
    void set_host(Host);
    void set_port(Optional<u16>);
    void set_paths(Vector<ByteString> const&);
    Vector<String> const& paths() const { return m_data->paths; }
    void set_query(Optional<String> query) { m_data->query = move(query); }
    void set_fragment(Optional<String> fragment) { m_data->fragment = move(fragment); }
    void set_cannot_be_a_base_url(bool value) { m_data->cannot_be_a_base_url = value; }
    void append_path(StringView);
    void append_slash()
    {
        // NOTE: To indicate that we want to end the path with a slash, we have to append an empty path segment.
        m_data->paths.append(String {});
    }

    String serialize_path() const;
    ByteString serialize(ExcludeFragment = ExcludeFragment::No) const;
    ByteString serialize_for_display() const;
    ByteString to_byte_string() const { return serialize(); }
    ErrorOr<String> to_string() const;

    Origin origin() const;

    bool equals(URL const& other, ExcludeFragment = ExcludeFragment::No) const;

    URL complete_url(StringView) const;

    [[nodiscard]] bool operator==(URL const& other) const
    {
        if (m_data.ptr() == other.m_data.ptr())
            return true;
        return equals(other, ExcludeFragment::No);
    }

    Optional<BlobURLEntry> const& blob_url_entry() const { return m_data->blob_url_entry; }
    void set_blob_url_entry(Optional<BlobURLEntry> entry) { m_data->blob_url_entry = move(entry); }

private:
    bool compute_validity() const;

    struct Data : public RefCounted<Data> {
        NonnullRefPtr<Data> clone()
        {
            auto clone = adopt_ref(*new Data);
            clone->valid = valid;
            clone->scheme = scheme;
            clone->username = username;
            clone->password = password;
            clone->host = host;
            clone->port = port;
            clone->paths = paths;
            clone->query = query;
            clone->fragment = fragment;
            clone->cannot_be_a_base_url = cannot_be_a_base_url;
            clone->blob_url_entry = blob_url_entry;
            return clone;
        }

        bool valid { false };

        // A URL’s scheme is an ASCII string that identifies the type of URL and can be used to dispatch a URL for further processing after parsing. It is initially the empty string.
        String scheme;

        // A URL’s username is an ASCII string identifying a username. It is initially the empty string.
        String username;

        // A URL’s password is an ASCII string identifying a password. It is initially the empty string.
        String password;

        // A URL’s host is null or a host. It is initially null.
        Host host;

        // A URL’s port is either null or a 16-bit unsigned integer that identifies a networking port. It is initially null.
        Optional<u16> port;

        // A URL’s path is either a URL path segment or a list of zero or more URL path segments, usually identifying a location. It is initially « ».
        // A URL path segment is an ASCII string. It commonly refers to a directory or a file, but has no predefined meaning.
        Vector<String> paths;

        // A URL’s query is either null or an ASCII string. It is initially null.
        Optional<String> query;

        // A URL’s fragment is either null or an ASCII string that can be used for further processing on the resource the URL’s other components identify. It is initially null.
        Optional<String> fragment;

        bool cannot_be_a_base_url { false };

        // https://url.spec.whatwg.org/#concept-url-blob-entry
        // A URL also has an associated blob URL entry that is either null or a blob URL entry. It is initially null.
        Optional<BlobURLEntry> blob_url_entry;
    };
    AK::CopyOnWrite<Data> m_data;
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
