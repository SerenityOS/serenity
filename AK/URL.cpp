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

// FIXME: It could make sense to force users of URL to use URLParser::basic_parse() explicitly instead of using a constructor.
URL::URL(StringView string)
    : URL(URLParser::basic_parse(string))
{
    if constexpr (URL_PARSER_DEBUG) {
        if (m_valid)
            dbgln("URL constructor: Parsed URL to be '{}'.", serialize());
        else
            dbgln("URL constructor: Parsed URL to be invalid.");
    }
}

URL URL::complete_url(StringView relative_url) const
{
    if (!is_valid())
        return {};

    return URLParser::basic_parse(relative_url, *this);
}

DeprecatedString URL::username(ApplyPercentDecoding apply_percent_decoding) const
{
    return apply_percent_decoding == ApplyPercentDecoding::Yes ? percent_decode(m_username) : m_username;
}

DeprecatedString URL::password(ApplyPercentDecoding apply_percent_decoding) const
{
    return apply_percent_decoding == ApplyPercentDecoding::Yes ? percent_decode(m_password) : m_password;
}

DeprecatedString URL::path_segment_at_index(size_t index, ApplyPercentDecoding apply_percent_decoding) const
{
    VERIFY(index < path_segment_count());
    return apply_percent_decoding == ApplyPercentDecoding::Yes ? percent_decode(m_paths[index]) : m_paths[index];
}

DeprecatedString URL::basename(ApplyPercentDecoding apply_percent_decoding) const
{
    if (!m_valid)
        return {};
    if (m_paths.is_empty())
        return {};
    auto& last_segment = m_paths.last();
    return apply_percent_decoding == ApplyPercentDecoding::Yes ? percent_decode(last_segment) : last_segment;
}

DeprecatedString URL::query(ApplyPercentDecoding apply_percent_decoding) const
{
    return apply_percent_decoding == ApplyPercentDecoding::Yes ? percent_decode(m_query) : m_query;
}

DeprecatedString URL::fragment(ApplyPercentDecoding apply_percent_decoding) const
{
    return apply_percent_decoding == ApplyPercentDecoding::Yes ? percent_decode(m_fragment) : m_fragment;
}

// NOTE: This only exists for compatibility with the existing URL tests which check for both .is_null() and .is_empty().
static DeprecatedString deprecated_string_percent_encode(DeprecatedString const& input, URL::PercentEncodeSet set = URL::PercentEncodeSet::Userinfo, URL::SpaceAsPlus space_as_plus = URL::SpaceAsPlus::No)
{
    if (input.is_null() || input.is_empty())
        return input;
    return URL::percent_encode(input.view(), set, space_as_plus);
}

void URL::set_scheme(DeprecatedString scheme)
{
    m_scheme = move(scheme);
    m_valid = compute_validity();
}

void URL::set_username(DeprecatedString username, ApplyPercentEncoding apply_percent_encoding)
{
    if (apply_percent_encoding == ApplyPercentEncoding::Yes)
        username = deprecated_string_percent_encode(username, PercentEncodeSet::Userinfo);
    m_username = move(username);
    m_valid = compute_validity();
}

void URL::set_password(DeprecatedString password, ApplyPercentEncoding apply_percent_encoding)
{
    if (apply_percent_encoding == ApplyPercentEncoding::Yes)
        password = deprecated_string_percent_encode(password, PercentEncodeSet::Userinfo);
    m_password = move(password);
    m_valid = compute_validity();
}

void URL::set_host(DeprecatedString host)
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

void URL::set_paths(Vector<DeprecatedString> paths, ApplyPercentEncoding apply_percent_encoding)
{
    if (apply_percent_encoding == ApplyPercentEncoding::Yes) {
        Vector<DeprecatedString> encoded_paths;
        encoded_paths.ensure_capacity(paths.size());
        for (auto& segment : paths)
            encoded_paths.unchecked_append(deprecated_string_percent_encode(segment, PercentEncodeSet::Path));
        m_paths = move(encoded_paths);
    } else {
        m_paths = move(paths);
    }
    m_valid = compute_validity();
}

void URL::append_path(DeprecatedString path, ApplyPercentEncoding apply_percent_encoding)
{
    if (apply_percent_encoding == ApplyPercentEncoding::Yes)
        path = deprecated_string_percent_encode(path, PercentEncodeSet::Path);
    m_paths.append(path);
}

void URL::set_query(DeprecatedString query, ApplyPercentEncoding apply_percent_encoding)
{
    if (apply_percent_encoding == ApplyPercentEncoding::Yes)
        query = deprecated_string_percent_encode(query, is_special() ? PercentEncodeSet::SpecialQuery : PercentEncodeSet::Query);
    m_query = move(query);
}

void URL::set_fragment(DeprecatedString fragment, ApplyPercentEncoding apply_percent_encoding)
{
    if (apply_percent_encoding == ApplyPercentEncoding::Yes)
        fragment = deprecated_string_percent_encode(fragment, PercentEncodeSet::Fragment);
    m_fragment = move(fragment);
}

// https://url.spec.whatwg.org/#cannot-have-a-username-password-port
bool URL::cannot_have_a_username_or_password_or_port() const
{
    // A URL cannot have a username/password/port if its host is null or the empty string, or its scheme is "file".
    // FIXME: The spec does not mention anything to do with 'cannot be a base URL'.
    return m_host.is_null() || m_host.is_empty() || m_cannot_be_a_base_url || m_scheme == "file"sv;
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

URL URL::create_with_file_scheme(DeprecatedString const& path, DeprecatedString const& fragment, DeprecatedString const& hostname)
{
    LexicalPath lexical_path(path);
    if (!lexical_path.is_absolute())
        return {};

    URL url;
    url.set_scheme("file");
    // NOTE: If the hostname is localhost (or null, which implies localhost), it should be set to the empty string.
    //       This is because a file URL always needs a non-null hostname.
    url.set_host(hostname.is_null() || hostname == "localhost" ? DeprecatedString::empty() : hostname);
    url.set_paths(lexical_path.parts());
    if (path.ends_with('/'))
        url.append_slash();
    url.set_fragment(fragment);
    return url;
}

URL URL::create_with_help_scheme(DeprecatedString const& path, DeprecatedString const& fragment, DeprecatedString const& hostname)
{
    LexicalPath lexical_path(path);

    URL url;
    url.set_scheme("help");
    // NOTE: If the hostname is localhost (or null, which implies localhost), it should be set to the empty string.
    //       This is because a file URL always needs a non-null hostname.
    url.set_host(hostname.is_null() || hostname == "localhost" ? DeprecatedString::empty() : hostname);
    url.set_paths(lexical_path.parts());
    if (path.ends_with('/'))
        url.append_slash();
    url.set_fragment(fragment);
    return url;
}

URL URL::create_with_url_or_path(DeprecatedString const& url_or_path)
{
    URL url = url_or_path;
    if (url.is_valid())
        return url;

    DeprecatedString path = LexicalPath::canonicalized_path(url_or_path);
    return URL::create_with_file_scheme(path);
}

// https://url.spec.whatwg.org/#special-scheme
bool URL::is_special_scheme(StringView scheme)
{
    return scheme.is_one_of("ftp", "file", "http", "https", "ws", "wss");
}

DeprecatedString URL::serialize_path(ApplyPercentDecoding apply_percent_decoding) const
{
    if (cannot_be_a_base_url())
        return m_paths[0];
    StringBuilder builder;
    for (auto& path : m_paths) {
        builder.append('/');
        builder.append(apply_percent_decoding == ApplyPercentDecoding::Yes ? percent_decode(path) : path);
    }
    return builder.to_deprecated_string();
}

DeprecatedString URL::serialize_data_url() const
{
    VERIFY(m_scheme == "data");
    VERIFY(!m_data_mime_type.is_null());
    VERIFY(!m_data_payload.is_null());
    StringBuilder builder;
    builder.append(m_scheme);
    builder.append(':');
    builder.append(m_data_mime_type);
    if (m_data_payload_is_base64)
        builder.append(";base64"sv);
    builder.append(',');
    // NOTE: The specification does not say anything about encoding this, but we should encode at least control and non-ASCII
    //       characters (since this is also a valid representation of the same data URL).
    builder.append(URL::percent_encode(m_data_payload, PercentEncodeSet::C0Control));
    return builder.to_deprecated_string();
}

// https://url.spec.whatwg.org/#concept-url-serializer
DeprecatedString URL::serialize(ExcludeFragment exclude_fragment) const
{
    if (m_scheme == "data")
        return serialize_data_url();

    // 1. Let output be url’s scheme and U+003A (:) concatenated.
    StringBuilder output;
    output.append(m_scheme);
    output.append(':');

    // 2. If url’s host is non-null:
    if (!m_host.is_null()) {
        // 1. Append "//" to output.
        output.append("//"sv);

        // 2. If url includes credentials, then:
        if (includes_credentials()) {
            // 1. Append url’s username to output.
            output.append(m_username);

            // 2. If url’s password is not the empty string, then append U+003A (:), followed by url’s password, to output.
            if (!m_password.is_empty()) {
                output.append(':');
                output.append(m_password);
            }

            // 3. Append U+0040 (@) to output.
            output.append('@');
        }

        // 3. Append url’s host, serialized, to output.
        output.append(m_host);

        // 4. If url’s port is non-null, append U+003A (:) followed by url’s port, serialized, to output.
        if (m_port.has_value())
            output.appendff(":{}", *m_port);
    }

    // 3. If url’s host is null, url does not have an opaque path, url’s path’s size is greater than 1, and url’s path[0] is the empty string, then append U+002F (/) followed by U+002E (.) to output.
    // 4. Append the result of URL path serializing url to output.
    // FIXME: Implement this closer to spec steps.
    if (cannot_be_a_base_url()) {
        output.append(m_paths[0]);
    } else {
        if (m_host.is_null() && m_paths.size() > 1 && m_paths[0].is_empty())
            output.append("/."sv);
        for (auto& segment : m_paths) {
            output.append('/');
            output.append(segment);
        }
    }

    // 5. If url’s query is non-null, append U+003F (?), followed by url’s query, to output.
    if (!m_query.is_null()) {
        output.append('?');
        output.append(m_query);
    }

    // 6. If exclude fragment is false and url’s fragment is non-null, then append U+0023 (#), followed by url’s fragment, to output.
    if (exclude_fragment == ExcludeFragment::No && !m_fragment.is_null()) {
        output.append('#');
        output.append(m_fragment);
    }

    // 7. Return output.
    return output.to_deprecated_string();
}

// https://url.spec.whatwg.org/#url-rendering
// NOTE: This does e.g. not display credentials.
// FIXME: Parts of the URL other than the host should have their sequences of percent-encoded bytes replaced with code points
//        resulting from percent-decoding those sequences converted to bytes, unless that renders those sequences invisible.
DeprecatedString URL::serialize_for_display() const
{
    VERIFY(m_valid);
    if (m_scheme == "data")
        return serialize_data_url();
    StringBuilder builder;
    builder.append(m_scheme);
    builder.append(':');

    if (!m_host.is_null()) {
        builder.append("//"sv);
        builder.append(m_host);
        if (m_port.has_value())
            builder.appendff(":{}", *m_port);
    }

    if (cannot_be_a_base_url()) {
        builder.append(m_paths[0]);
    } else {
        if (m_host.is_null() && m_paths.size() > 1 && m_paths[0].is_empty())
            builder.append("/."sv);
        for (auto& segment : m_paths) {
            builder.append('/');
            builder.append(segment);
        }
    }

    if (!m_query.is_null()) {
        builder.append('?');
        builder.append(m_query);
    }

    if (!m_fragment.is_null()) {
        builder.append('#');
        builder.append(m_fragment);
    }

    return builder.to_deprecated_string();
}

ErrorOr<String> URL::to_string() const
{
    return String::from_deprecated_string(serialize());
}

// https://html.spec.whatwg.org/multipage/origin.html#ascii-serialisation-of-an-origin
// https://url.spec.whatwg.org/#concept-url-origin
DeprecatedString URL::serialize_origin() const
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
        builder.appendff(":{}", *m_port);
    return builder.to_deprecated_string();
}

bool URL::equals(URL const& other, ExcludeFragment exclude_fragments) const
{
    if (this == &other)
        return true;
    if (!m_valid || !other.m_valid)
        return false;
    return serialize(exclude_fragments) == other.serialize(exclude_fragments);
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
bool URL::code_point_is_in_percent_encode_set(u32 code_point, URL::PercentEncodeSet set)
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
        return code_point_is_in_percent_encode_set(code_point, URL::PercentEncodeSet::Component) || "!'()~"sv.contains(code_point);
    case URL::PercentEncodeSet::EncodeURI:
        // NOTE: This is the same percent encode set that JS encodeURI() uses.
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURI
        return code_point > 0x7E || (!is_ascii_alphanumeric(code_point) && !";,/?:@&=+$-_.!~*'()#"sv.contains(static_cast<char>(code_point)));
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

DeprecatedString URL::percent_encode(StringView input, URL::PercentEncodeSet set, SpaceAsPlus space_as_plus)
{
    StringBuilder builder;
    for (auto code_point : Utf8View(input)) {
        if (space_as_plus == SpaceAsPlus::Yes && code_point == ' ')
            builder.append('+');
        else
            append_percent_encoded_if_necessary(builder, code_point, set);
    }
    return builder.to_deprecated_string();
}

DeprecatedString URL::percent_decode(StringView input)
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
    return builder.to_deprecated_string();
}

}
