/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <AK/URLParser.h>
#include <AK/Utf8View.h>

namespace AK {

// FIXME: It could make sense to force users of URL to use URLParser::parse() explicitly instead of using a constructor.
URL::URL(StringView string)
    : URL(URLParser::parse(string))
{
    if constexpr (URL_PARSER_DEBUG) {
        if (m_valid)
            dbgln("URL constructor: Parsed URL to be '{}'.", serialize());
        else
            dbgln("URL constructor: Parsed URL to be invalid.");
    }
}

String URL::path() const
{
    if (cannot_be_a_base_url())
        return paths()[0];
    StringBuilder builder;
    for (auto& path : m_paths) {
        builder.append('/');
        builder.append(path);
    }
    return builder.to_string();
}

URL URL::complete_url(String const& string) const
{
    if (!is_valid())
        return {};

    return URLParser::parse(string, this);
}

void URL::set_scheme(String scheme)
{
    m_scheme = move(scheme);
    m_valid = compute_validity();
}

void URL::set_username(String username)
{
    m_username = move(username);
    m_valid = compute_validity();
}

void URL::set_password(String password)
{
    m_password = move(password);
    m_valid = compute_validity();
}

void URL::set_host(String host)
{
    m_host = move(host);
    m_valid = compute_validity();
}

void URL::set_port(Optional<u16> port)
{
    if (port == default_port_for_scheme(m_scheme)) {
        m_port = {};
        return;
    }
    m_port = move(port);
    m_valid = compute_validity();
}

void URL::set_paths(Vector<String> paths)
{
    m_paths = move(paths);
    m_valid = compute_validity();
}

void URL::set_query(String query)
{
    m_query = move(query);
}

void URL::set_fragment(String fragment)
{
    m_fragment = move(fragment);
}

// FIXME: This is by no means complete.
// NOTE: This relies on some assumptions about how the spec-defined URL parser works that may turn out to be wrong.
bool URL::compute_validity() const
{
    if (m_scheme.is_empty())
        return false;

    if (m_scheme == "data") {
        if (m_data_mime_type.is_empty())
            return false;
        if (m_data_payload_is_base64) {
            if (m_data_payload.length() % 4 != 0)
                return false;
            for (auto character : m_data_payload) {
                if (!is_ascii_alphanumeric(character) || character == '+' || character == '/' || character == '=')
                    return false;
            }
        }
    } else if (m_cannot_be_a_base_url) {
        if (m_paths.size() != 1)
            return false;
        if (m_paths[0].is_empty())
            return false;
    } else {
        if (m_scheme.is_one_of("about", "mailto"))
            return false;
        // NOTE: Maybe it is allowed to have a zero-segment path.
        if (m_paths.size() == 0)
            return false;
    }

    // NOTE: A file URL's host should be the empty string for localhost, not null.
    if (m_scheme == "file" && m_host.is_null())
        return false;

    return true;
}

bool URL::scheme_requires_port(StringView scheme)
{
    return (default_port_for_scheme(scheme) != 0);
}

u16 URL::default_port_for_scheme(StringView scheme)
{
    if (scheme == "http")
        return 80;
    if (scheme == "https")
        return 443;
    if (scheme == "gemini")
        return 1965;
    if (scheme == "irc")
        return 6667;
    if (scheme == "ircs")
        return 6697;
    if (scheme == "ws")
        return 80;
    if (scheme == "wss")
        return 443;
    return 0;
}

URL URL::create_with_file_scheme(String const& path, String const& fragment, String const& hostname)
{
    LexicalPath lexical_path(path);
    if (!lexical_path.is_absolute())
        return {};

    URL url;
    url.set_scheme("file");
    // NOTE: If the hostname is localhost (or null, which implies localhost), it should be set to the empty string.
    //       This is because a file URL always needs a non-null hostname.
    url.set_host(hostname.is_null() || hostname == "localhost" ? String::empty() : hostname);
    url.set_paths(lexical_path.parts());
    // NOTE: To indicate that we want to end the path with a slash, we have to append an empty path segment.
    if (path.ends_with('/'))
        url.append_path("");
    url.set_fragment(fragment);
    return url;
}

URL URL::create_with_url_or_path(String const& url_or_path)
{
    URL url = url_or_path;
    if (url.is_valid())
        return url;

    String path = LexicalPath::canonicalized_path(url_or_path);
    return URL::create_with_file_scheme(path);
}

// https://url.spec.whatwg.org/#special-scheme
bool URL::is_special_scheme(StringView scheme)
{
    return scheme.is_one_of("ftp", "file", "http", "https", "ws", "wss");
}

String URL::serialize_data_url() const
{
    VERIFY(m_scheme == "data");
    VERIFY(!m_data_mime_type.is_null());
    VERIFY(!m_data_payload.is_null());
    StringBuilder builder;
    builder.append(m_scheme);
    builder.append(':');
    builder.append(m_data_mime_type);
    if (m_data_payload_is_base64)
        builder.append(";base64");
    builder.append(',');
    // NOTE: The specification does not say anything about encoding this, but we should encode at least control and non-ASCII
    //       characters (since this is also a valid representation of the same data URL).
    builder.append(URL::percent_encode(m_data_payload, PercentEncodeSet::C0Control));
    return builder.to_string();
}

// https://url.spec.whatwg.org/#concept-url-serializer
String URL::serialize(ExcludeFragment exclude_fragment) const
{
    if (m_scheme == "data")
        return serialize_data_url();
    StringBuilder builder;
    builder.append(m_scheme);
    builder.append(':');

    if (!m_host.is_null()) {
        builder.append("//");

        if (includes_credentials()) {
            builder.append(percent_encode(m_username, PercentEncodeSet::Userinfo));
            if (!m_password.is_empty()) {
                builder.append(':');
                builder.append(percent_encode(m_password, PercentEncodeSet::Userinfo));
            }
            builder.append('@');
        }

        builder.append(m_host);
        if (m_port.has_value())
            builder.appendff(":{}", *m_port);
    }

    if (cannot_be_a_base_url()) {
        builder.append(percent_encode(m_paths[0], PercentEncodeSet::Path));
    } else {
        if (m_host.is_null() && m_paths.size() > 1 && m_paths[0].is_empty())
            builder.append("/.");
        for (auto& segment : m_paths) {
            builder.append('/');
            builder.append(percent_encode(segment, PercentEncodeSet::Path));
        }
    }

    if (!m_query.is_null()) {
        builder.append('?');
        builder.append(percent_encode(m_query, is_special() ? URL::PercentEncodeSet::SpecialQuery : URL::PercentEncodeSet::Query));
    }

    if (exclude_fragment == ExcludeFragment::No && !m_fragment.is_null()) {
        builder.append('#');
        builder.append(percent_encode(m_fragment, PercentEncodeSet::Fragment));
    }

    return builder.to_string();
}

// https://url.spec.whatwg.org/#url-rendering
// NOTE: This does e.g. not display credentials.
// FIXME: Parts of the URL other than the host should have their sequences of percent-encoded bytes replaced with code points
//        resulting from percent-decoding those sequences converted to bytes, unless that renders those sequences invisible.
String URL::serialize_for_display() const
{
    VERIFY(m_valid);
    if (m_scheme == "data")
        return serialize_data_url();
    StringBuilder builder;
    builder.append(m_scheme);
    builder.append(':');

    if (!m_host.is_null()) {
        builder.append("//");
        builder.append(m_host);
        if (m_port.has_value())
            builder.appendff(":{}", *m_port);
    }

    if (cannot_be_a_base_url()) {
        builder.append(percent_encode(m_paths[0], PercentEncodeSet::Path));
    } else {
        if (m_host.is_null() && m_paths.size() > 1 && m_paths[0].is_empty())
            builder.append("/.");
        for (auto& segment : m_paths) {
            builder.append('/');
            builder.append(percent_encode(segment, PercentEncodeSet::Path));
        }
    }

    if (!m_query.is_null()) {
        builder.append('?');
        builder.append(percent_encode(m_query, is_special() ? URL::PercentEncodeSet::SpecialQuery : URL::PercentEncodeSet::Query));
    }

    if (!m_fragment.is_null()) {
        builder.append('#');
        builder.append(percent_encode(m_fragment, PercentEncodeSet::Fragment));
    }

    return builder.to_string();
}

// https://html.spec.whatwg.org/multipage/origin.html#ascii-serialisation-of-an-origin
// https://url.spec.whatwg.org/#concept-url-origin
String URL::serialize_origin() const
{
    VERIFY(m_valid);

    if (m_scheme == "blob"sv) {
        // TODO: 1. If URL’s blob URL entry is non-null, then return URL’s blob URL entry’s environment’s origin.
        // 2. Let url be the result of parsing URL’s path[0].
        VERIFY(!m_paths.is_empty());
        URL url = m_paths[0];
        // 3. Return a new opaque origin, if url is failure, and url’s origin otherwise.
        if (!url.is_valid())
            return "null";
        return url.serialize_origin();
    } else if (!m_scheme.is_one_of("ftp"sv, "http"sv, "https"sv, "ws"sv, "wss"sv)) { // file: "Unfortunate as it is, this is left as an exercise to the reader. When in doubt, return a new opaque origin."
        return "null";
    }

    StringBuilder builder;
    builder.append(m_scheme);
    builder.append("://"sv);
    builder.append(m_host);
    if (m_port.has_value())
        builder.append(":{}", *m_port);
    return builder.build();
}

bool URL::equals(URL const& other, ExcludeFragment exclude_fragments) const
{
    if (this == &other)
        return true;
    if (!m_valid || !other.m_valid)
        return false;
    return serialize(exclude_fragments) == other.serialize(exclude_fragments);
}

String URL::basename() const
{
    if (!m_valid)
        return {};
    if (m_paths.is_empty())
        return {};
    return m_paths.last();
}

void URL::append_percent_encoded(StringBuilder& builder, u32 code_point)
{
    if (code_point <= 0x7f)
        builder.appendff("%{:02X}", code_point);
    else if (code_point <= 0x07ff)
        builder.appendff("%{:02X}%{:02X}", ((code_point >> 6) & 0x1f) | 0xc0, (code_point & 0x3f) | 0x80);
    else if (code_point <= 0xffff)
        builder.appendff("%{:02X}%{:02X}%{:02X}", ((code_point >> 12) & 0x0f) | 0xe0, ((code_point >> 6) & 0x3f) | 0x80, (code_point & 0x3f) | 0x80);
    else if (code_point <= 0x10ffff)
        builder.appendff("%{:02X}%{:02X}%{:02X}%{:02X}", ((code_point >> 18) & 0x07) | 0xf0, ((code_point >> 12) & 0x3f) | 0x80, ((code_point >> 6) & 0x3f) | 0x80, (code_point & 0x3f) | 0x80);
    else
        VERIFY_NOT_REACHED();
}

// https://url.spec.whatwg.org/#c0-control-percent-encode-set
constexpr bool code_point_is_in_percent_encode_set(u32 code_point, URL::PercentEncodeSet set)
{
    switch (set) {
    case URL::PercentEncodeSet::C0Control:
        return code_point < 0x20 || code_point > 0x7E;
    case URL::PercentEncodeSet::Fragment:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::C0Control) || " \"<>`"sv.contains(code_point);
    case URL::PercentEncodeSet::Query:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::C0Control) || " \"#<>"sv.contains(code_point);
    case URL::PercentEncodeSet::SpecialQuery:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::Query) || code_point == '\'';
    case URL::PercentEncodeSet::Path:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::Query) || "?`{}"sv.contains(code_point);
    case URL::PercentEncodeSet::Userinfo:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::Path) || "/:;=@[\\]^|"sv.contains(code_point);
    case URL::PercentEncodeSet::Component:
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::Userinfo) || "$%&+,"sv.contains(code_point);
    case URL::PercentEncodeSet::ApplicationXWWWFormUrlencoded:
        return code_point >= 0x7E || !(is_ascii_alphanumeric(code_point) || "!'()~"sv.contains(code_point));
    case URL::PercentEncodeSet::EncodeURI:
        // NOTE: This is the same percent encode set that JS encodeURI() uses.
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURI
        return code_point >= 0x7E || (!is_ascii_alphanumeric(code_point) && !";,/?:@&=+$-_.!~*'()#"sv.contains(code_point));
    default:
        VERIFY_NOT_REACHED();
    }
}

void URL::append_percent_encoded_if_necessary(StringBuilder& builder, u32 code_point, URL::PercentEncodeSet set)
{
    if (code_point_is_in_percent_encode_set(code_point, set))
        append_percent_encoded(builder, code_point);
    else
        builder.append_code_point(code_point);
}

String URL::percent_encode(StringView input, URL::PercentEncodeSet set)
{
    StringBuilder builder;
    for (auto code_point : Utf8View(input)) {
        append_percent_encoded_if_necessary(builder, code_point, set);
    }
    return builder.to_string();
}

String URL::percent_decode(StringView input)
{
    if (!input.contains('%'))
        return input;
    StringBuilder builder;
    Utf8View utf8_view(input);
    for (auto it = utf8_view.begin(); !it.done(); ++it) {
        if (*it != '%') {
            builder.append_code_point(*it);
        } else if (!is_ascii_hex_digit(it.peek(1).value_or(0)) || !is_ascii_hex_digit(it.peek(2).value_or(0))) {
            builder.append_code_point(*it);
        } else {
            ++it;
            u8 byte = parse_ascii_hex_digit(*it) << 4;
            ++it;
            byte += parse_ascii_hex_digit(*it);
            builder.append(byte);
        }
    }
    return builder.to_string();
}

}
