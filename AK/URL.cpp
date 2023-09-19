/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
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

ErrorOr<String> URL::username() const
{
    return String::from_deprecated_string(percent_decode(m_username));
}

ErrorOr<String> URL::password() const
{
    return String::from_deprecated_string(percent_decode(m_password));
}

DeprecatedString URL::path_segment_at_index(size_t index) const
{
    VERIFY(index < path_segment_count());
    return percent_decode(m_paths[index]);
}

DeprecatedString URL::basename() const
{
    if (!m_valid)
        return {};
    if (m_paths.is_empty())
        return {};
    auto& last_segment = m_paths.last();
    return percent_decode(last_segment);
}

// NOTE: This only exists for compatibility with the existing URL tests which check for both .is_null() and .is_empty().
static DeprecatedString deprecated_string_percent_encode(DeprecatedString const& input, URL::PercentEncodeSet set = URL::PercentEncodeSet::Userinfo, URL::SpaceAsPlus space_as_plus = URL::SpaceAsPlus::No)
{
    if (input.is_null() || input.is_empty())
        return input;
    return URL::percent_encode(input.view(), set, space_as_plus);
}

void URL::set_scheme(String scheme)
{
    m_scheme = move(scheme);
    m_valid = compute_validity();
}

// https://url.spec.whatwg.org/#set-the-username
ErrorOr<void> URL::set_username(StringView username)
{
    // To set the username given a url and username, set url’s username to the result of running UTF-8 percent-encode on username using the userinfo percent-encode set.
    m_username = TRY(String::from_deprecated_string(deprecated_string_percent_encode(username, PercentEncodeSet::Userinfo)));
    m_valid = compute_validity();
    return {};
}

// https://url.spec.whatwg.org/#set-the-password
ErrorOr<void> URL::set_password(StringView password)
{
    // To set the password given a url and password, set url’s password to the result of running UTF-8 percent-encode on password using the userinfo percent-encode set.
    m_password = TRY(String::from_deprecated_string(deprecated_string_percent_encode(password, PercentEncodeSet::Userinfo)));
    m_valid = compute_validity();
    return {};
}

void URL::set_host(Host host)
{
    m_host = move(host);
    m_valid = compute_validity();
}

// https://url.spec.whatwg.org/#concept-host-serializer
ErrorOr<String> URL::serialized_host() const
{
    return URLParser::serialize_host(m_host);
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

void URL::set_paths(Vector<DeprecatedString> const& paths)
{
    m_paths.clear_with_capacity();
    m_paths.ensure_capacity(paths.size());
    for (auto const& segment : paths)
        m_paths.unchecked_append(deprecated_string_percent_encode(segment, PercentEncodeSet::Path));
    m_valid = compute_validity();
}

void URL::append_path(StringView path)
{
    m_paths.append(deprecated_string_percent_encode(path, PercentEncodeSet::Path));
}

// https://url.spec.whatwg.org/#cannot-have-a-username-password-port
bool URL::cannot_have_a_username_or_password_or_port() const
{
    // A URL cannot have a username/password/port if its host is null or the empty string, or its scheme is "file".
    // FIXME: The spec does not mention anything to do with 'cannot be a base URL'.
    return m_host.has<Empty>() || m_host == String {} || m_cannot_be_a_base_url || m_scheme == "file"sv;
}

// FIXME: This is by no means complete.
// NOTE: This relies on some assumptions about how the spec-defined URL parser works that may turn out to be wrong.
bool URL::compute_validity() const
{
    if (m_scheme.is_empty())
        return false;

    if (m_cannot_be_a_base_url) {
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
    if (m_scheme == "file" && m_host.has<Empty>())
        return false;

    return true;
}

// https://url.spec.whatwg.org/#default-port
Optional<u16> URL::default_port_for_scheme(StringView scheme)
{
    // Spec defined mappings with port:
    if (scheme == "ftp")
        return 21;
    if (scheme == "http")
        return 80;
    if (scheme == "https")
        return 443;
    if (scheme == "ws")
        return 80;
    if (scheme == "wss")
        return 443;

    // NOTE: not in spec, but we support these too
    if (scheme == "gemini")
        return 1965;
    if (scheme == "irc")
        return 6667;
    if (scheme == "ircs")
        return 6697;

    return {};
}

URL URL::create_with_file_scheme(DeprecatedString const& path, DeprecatedString const& fragment, DeprecatedString const& hostname)
{
    LexicalPath lexical_path(path);
    if (!lexical_path.is_absolute())
        return {};

    URL url;
    url.set_scheme("file"_string);
    // NOTE: If the hostname is localhost (or null, which implies localhost), it should be set to the empty string.
    //       This is because a file URL always needs a non-null hostname.
    url.set_host(hostname.is_null() || hostname == "localhost" ? String {} : String::from_deprecated_string(hostname).release_value_but_fixme_should_propagate_errors());
    url.set_paths(lexical_path.parts());
    if (path.ends_with('/'))
        url.append_slash();
    if (!fragment.is_null())
        url.set_fragment(String::from_deprecated_string(fragment).release_value_but_fixme_should_propagate_errors());
    return url;
}

URL URL::create_with_help_scheme(DeprecatedString const& path, DeprecatedString const& fragment, DeprecatedString const& hostname)
{
    LexicalPath lexical_path(path);

    URL url;
    url.set_scheme("help"_string);
    // NOTE: If the hostname is localhost (or null, which implies localhost), it should be set to the empty string.
    //       This is because a file URL always needs a non-null hostname.
    url.set_host(hostname.is_null() || hostname == "localhost" ? String {} : String::from_deprecated_string(hostname).release_value_but_fixme_should_propagate_errors());

    url.set_paths(lexical_path.parts());
    if (path.ends_with('/'))
        url.append_slash();
    if (!fragment.is_null())
        url.set_fragment(String::from_deprecated_string(fragment).release_value_but_fixme_should_propagate_errors());
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

URL URL::create_with_data(StringView mime_type, StringView payload, bool is_base64)
{
    URL url;
    url.set_cannot_be_a_base_url(true);
    url.set_scheme("data"_string);

    StringBuilder builder;
    builder.append(mime_type);
    if (is_base64)
        builder.append(";base64"sv);
    builder.append(',');
    builder.append(payload);
    url.set_paths({ builder.to_deprecated_string() });
    return url;
}

// https://url.spec.whatwg.org/#special-scheme
bool URL::is_special_scheme(StringView scheme)
{
    return scheme.is_one_of("ftp", "file", "http", "https", "ws", "wss");
}

// https://url.spec.whatwg.org/#url-path-serializer
DeprecatedString URL::serialize_path(ApplyPercentDecoding apply_percent_decoding) const
{
    // 1. If url has an opaque path, then return url’s path.
    // FIXME: Reimplement this step once we modernize the URL implementation to meet the spec.
    if (cannot_be_a_base_url())
        return m_paths[0];

    // 2. Let output be the empty string.
    StringBuilder output;

    // 3. For each segment of url’s path: append U+002F (/) followed by segment to output.
    for (auto const& segment : m_paths) {
        output.append('/');
        output.append(apply_percent_decoding == ApplyPercentDecoding::Yes ? percent_decode(segment) : segment);
    }

    // 4. Return output.
    return output.to_deprecated_string();
}

// https://url.spec.whatwg.org/#concept-url-serializer
DeprecatedString URL::serialize(ExcludeFragment exclude_fragment) const
{
    // 1. Let output be url’s scheme and U+003A (:) concatenated.
    StringBuilder output;
    output.append(m_scheme);
    output.append(':');

    // 2. If url’s host is non-null:
    if (!m_host.has<Empty>()) {
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
        output.append(serialized_host().release_value_but_fixme_should_propagate_errors());

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
        if (m_host.has<Empty>() && m_paths.size() > 1 && m_paths[0].is_empty())
            output.append("/."sv);
        for (auto& segment : m_paths) {
            output.append('/');
            output.append(segment);
        }
    }

    // 5. If url’s query is non-null, append U+003F (?), followed by url’s query, to output.
    if (m_query.has_value()) {
        output.append('?');
        output.append(*m_query);
    }

    // 6. If exclude fragment is false and url’s fragment is non-null, then append U+0023 (#), followed by url’s fragment, to output.
    if (exclude_fragment == ExcludeFragment::No && m_fragment.has_value()) {
        output.append('#');
        output.append(*m_fragment);
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

    StringBuilder builder;
    builder.append(m_scheme);
    builder.append(':');

    if (!m_host.has<Empty>()) {
        builder.append("//"sv);
        builder.append(serialized_host().release_value_but_fixme_should_propagate_errors());
        if (m_port.has_value())
            builder.appendff(":{}", *m_port);
    }

    if (cannot_be_a_base_url()) {
        builder.append(m_paths[0]);
    } else {
        if (m_host.has<Empty>() && m_paths.size() > 1 && m_paths[0].is_empty())
            builder.append("/."sv);
        for (auto& segment : m_paths) {
            builder.append('/');
            builder.append(segment);
        }
    }

    if (m_query.has_value()) {
        builder.append('?');
        builder.append(*m_query);
    }

    if (m_fragment.has_value()) {
        builder.append('#');
        builder.append(*m_fragment);
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
    builder.append(serialized_host().release_value_but_fixme_should_propagate_errors());
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

// https://fetch.spec.whatwg.org/#data-url-processor
ErrorOr<URL::DataURL> URL::process_data_url() const
{
    // 1. Assert: dataURL’s scheme is "data".
    VERIFY(scheme() == "data");

    // 2. Let input be the result of running the URL serializer on dataURL with exclude fragment set to true.
    auto input = serialize(URL::ExcludeFragment::Yes);

    // 3. Remove the leading "data:" from input.
    input = input.substring("data:"sv.length());

    // 4. Let position point at the start of input.

    // 5. Let mimeType be the result of collecting a sequence of code points that are not equal to U+002C (,), given position.
    auto position = input.find(',');
    auto mime_type = input.substring_view(0, position.value_or(input.length()));

    // 6. Strip leading and trailing ASCII whitespace from mimeType.
    mime_type = mime_type.trim_whitespace(TrimMode::Both);

    // 7. If position is past the end of input, then return failure.
    if (!position.has_value())
        return Error::from_string_literal("Missing a comma character");

    // 8. Advance position by 1.
    position = position.value() + 1;

    // 9. Let encodedBody be the remainder of input.
    auto encoded_body = input.substring_view(position.value());

    // 10. Let body be the percent-decoding of encodedBody.
    auto body = URL::percent_decode(encoded_body).to_byte_buffer();

    // 11. If mimeType ends with U+003B (;), followed by zero or more U+0020 SPACE, followed by an ASCII case-insensitive match for "base64", then:
    if (mime_type.ends_with("base64"sv, CaseSensitivity::CaseInsensitive)) {
        auto trimmed_substring_view = mime_type.substring_view(0, mime_type.length() - 6);
        trimmed_substring_view = trimmed_substring_view.trim(" "sv, TrimMode::Right);
        if (trimmed_substring_view.ends_with(';')) {
            // 1. Let stringBody be the isomorphic decode of body.
            auto string_body = StringView(body);

            // 2. Set body to the forgiving-base64 decode of stringBody.
            //    FIXME: Check if it's really forgiving.
            // 3. If body is failure, then return failure.
            body = TRY(decode_base64(string_body));

            // 4. Remove the last 6 code points from mimeType.
            // 5. Remove trailing U+0020 SPACE code points from mimeType, if any.
            // 6. Remove the last U+003B (;) from mimeType.
            mime_type = trimmed_substring_view.substring_view(0, trimmed_substring_view.length() - 1);
        }
    }

    // 12. If mimeType starts with ";", then prepend "text/plain" to mimeType.
    StringBuilder builder;
    if (mime_type.starts_with(';')) {
        builder.append("text/plain"sv);
        builder.append(mime_type);
        mime_type = builder.string_view();
    }

    // FIXME: Parse the MIME type's components according to https://mimesniff.spec.whatwg.org/#parse-a-mime-type
    // FIXME: 13. Let mimeTypeRecord be the result of parsing mimeType.
    auto mime_type_record = mime_type.trim("\n\r\t "sv, TrimMode::Both);

    // 14. If mimeTypeRecord is failure, then set mimeTypeRecord to text/plain;charset=US-ASCII.
    if (mime_type_record.is_empty())
        mime_type_record = "text/plain;charset=US-ASCII"sv;

    // 15. Return a new data: URL struct whose MIME type is mimeTypeRecord and body is body.
    return URL::DataURL { TRY(String::from_utf8(mime_type_record)), body };
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
