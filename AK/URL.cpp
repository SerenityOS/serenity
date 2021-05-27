/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <AK/URLParser.h>
#include <AK/Utf8View.h>

namespace AK {

constexpr bool is_ascii_alpha(u32 code_point)
{
    return ('a' <= code_point && code_point <= 'z') || ('A' <= code_point && code_point <= 'Z');
}

constexpr bool is_ascii_digit(u32 code_point)
{
    return '0' <= code_point && code_point <= '9';
}

constexpr bool is_ascii_alphanumeric(u32 code_point)
{
    return is_ascii_alpha(code_point) || is_ascii_digit(code_point);
}

constexpr bool is_ascii_hex_digit(u32 code_point)
{
    return is_ascii_digit(code_point) || (code_point >= 'a' && code_point <= 'f') || (code_point >= 'A' && code_point <= 'F');
}

// FIXME: It could make sense to force users of URL to use URLParser::parse() explicitly instead of using a constructor.
URL::URL(const StringView& string)
    : URL(URLParser::parse({}, string))
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
    if (!m_path.is_null())
        return m_path;
    StringBuilder builder;
    for (auto& path : m_paths) {
        builder.append('/');
        builder.append(path);
    }
    return builder.to_string();
}

String URL::to_string() const
{
    StringBuilder builder;
    builder.append(m_scheme);

    if (m_scheme == "about") {
        builder.append(':');
        builder.append(m_path);
        return builder.to_string();
    }

    if (m_scheme == "data") {
        builder.append(':');
        builder.append(m_data_mime_type);
        if (m_data_payload_is_base64)
            builder.append(";base64");
        builder.append(',');
        builder.append(m_data_payload);
        return builder.to_string();
    }

    builder.append("://");
    builder.append(m_host);
    if (default_port_for_scheme(scheme()) != port()) {
        builder.append(':');
        builder.append(String::number(m_port));
    }

    builder.append(path());
    if (!m_query.is_empty()) {
        builder.append('?');
        builder.append(m_query);
    }
    if (!m_fragment.is_empty()) {
        builder.append('#');
        builder.append(m_fragment);
    }
    return builder.to_string();
}

URL URL::complete_url(const String& string) const
{
    if (!is_valid())
        return {};

    return URLParser::parse({}, string, this);
}

void URL::set_scheme(const String& scheme)
{
    m_scheme = scheme;
    m_valid = compute_validity();
}

void URL::set_username(const String& username)
{
    m_username = username;
    m_valid = compute_validity();
}

void URL::set_password(const String& password)
{
    m_password = password;
    m_valid = compute_validity();
}

void URL::set_host(const String& host)
{
    m_host = host;
    m_valid = compute_validity();
}

void URL::set_port(const u16 port)
{
    if (port == default_port_for_scheme(m_scheme)) {
        m_port = 0;
        return;
    }
    m_port = port;
    m_valid = compute_validity();
}

void URL::set_path(const String& path)
{
    m_path = path;
    m_valid = compute_validity();
}

void URL::set_paths(const Vector<String>& paths)
{
    m_paths = paths;
    m_valid = compute_validity();
}

void URL::set_query(const String& query)
{
    m_query = query;
}

void URL::set_fragment(const String& fragment)
{
    m_fragment = fragment;
}

bool URL::compute_validity() const
{
    // FIXME: This is by no means complete.
    if (m_scheme.is_empty())
        return false;

    if (m_scheme == "about") {
        if (path().is_empty())
            return false;
        return true;
    }

    if (m_scheme == "file") {
        if (path().is_empty())
            return false;
        return true;
    }

    if (m_scheme == "data") {
        if (m_data_mime_type.is_empty())
            return false;
        return true;
    }

    if (m_host.is_empty())
        return false;

    return true;
}

bool URL::scheme_requires_port(const StringView& scheme)
{
    return (default_port_for_scheme(scheme) != 0);
}

u16 URL::default_port_for_scheme(const StringView& scheme)
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

URL URL::create_with_file_scheme(const String& path, const String& fragment)
{
    URL url;
    url.set_scheme("file");
    url.set_path(path);
    url.set_fragment(fragment);
    return url;
}

URL URL::create_with_url_or_path(const String& url_or_path)
{
    URL url = url_or_path;
    if (url.is_valid())
        return url;

    String path = LexicalPath::canonicalized_path(url_or_path);
    return URL::create_with_file_scheme(path);
}

URL URL::create_with_data(const StringView& mime_type, const StringView& payload, bool is_base64)
{
    URL url;
    url.set_scheme("data");
    url.m_valid = true;
    url.m_data_payload = payload;
    url.m_data_mime_type = mime_type;
    url.m_data_payload_is_base64 = is_base64;

    return url;
}

// https://url.spec.whatwg.org/#special-scheme
bool URL::is_special_scheme(const StringView& scheme)
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
        if (m_port != 0)
            builder.appendff(":{}", m_port);
    }

    if (cannot_be_a_base_url()) {
        builder.append(percent_encode(m_paths[0], PercentEncodeSet::Path));
    } else {
        // FIXME: Temporary m_path hack
        if (!m_path.is_null()) {
            builder.append(path());
        } else {
            if (m_host.is_null() && m_paths.size() > 1 && m_paths[0].is_empty())
                builder.append("/.");
            for (auto& segment : m_paths) {
                builder.append('/');
                builder.append(percent_encode(segment, PercentEncodeSet::Path));
            }
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
        if (m_port != 0)
            builder.appendff(":{}", m_port);
    }

    if (cannot_be_a_base_url()) {
        builder.append(percent_encode(m_paths[0], PercentEncodeSet::Path));
    } else {
        // FIXME: Temporary m_path hack
        if (!m_path.is_null()) {
            builder.append(path());
        } else {
            if (m_host.is_null() && m_paths.size() > 1 && m_paths[0].is_empty())
                builder.append("/.");
            for (auto& segment : m_paths) {
                builder.append('/');
                builder.append(percent_encode(segment, PercentEncodeSet::Path));
            }
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

String URL::basename() const
{
    if (!m_valid)
        return {};
    // FIXME: Temporary m_path hack
    if (!m_path.is_null())
        return LexicalPath(m_path).basename();
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

String URL::percent_encode(const StringView& input, URL::PercentEncodeSet set)
{
    StringBuilder builder;
    for (auto code_point : Utf8View(input)) {
        append_percent_encoded_if_necessary(builder, code_point, set);
    }
    return builder.to_string();
}

constexpr u8 parse_hex_digit(u8 digit)
{
    if (digit >= '0' && digit <= '9')
        return digit - '0';
    if (digit >= 'a' && digit <= 'f')
        return digit - 'a' + 10;
    if (digit >= 'A' && digit <= 'F')
        return digit - 'A' + 10;
    VERIFY_NOT_REACHED();
}

String URL::percent_decode(const StringView& input)
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
            u8 byte = parse_hex_digit(*it) << 4;
            ++it;
            byte += parse_hex_digit(*it);
            builder.append(byte);
        }
    }
    return builder.to_string();
}

}
