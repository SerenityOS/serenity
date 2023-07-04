/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/DeprecatedString.h>
#include <AK/Optional.h>
#include <AK/SourceLocation.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <AK/URLParser.h>
#include <AK/Utf8View.h>

namespace AK {

// NOTE: This is similar to the LibC macro EOF = -1.
constexpr u32 end_of_file = 0xFFFFFFFF;

static bool is_url_code_point(u32 code_point)
{
    // FIXME: [...] and code points in the range U+00A0 to U+10FFFD, inclusive, excluding surrogates and noncharacters.
    return is_ascii_alphanumeric(code_point) || code_point >= 0xA0 || "!$&'()*+,-./:;=?@_~"sv.contains(code_point);
}

static void report_validation_error(SourceLocation const& location = SourceLocation::current())
{
    dbgln_if(URL_PARSER_DEBUG, "URLParser::parse: Validation error! {}", location);
}

static Optional<DeprecatedString> parse_opaque_host(StringView input)
{
    auto forbidden_host_characters_excluding_percent = "\0\t\n\r #/:<>?@[\\]^|"sv;
    for (auto character : forbidden_host_characters_excluding_percent) {
        if (input.contains(character)) {
            report_validation_error();
            return {};
        }
    }
    // FIXME: If input contains a code point that is not a URL code point and not U+0025 (%), validation error.
    // FIXME: If input contains a U+0025 (%) and the two code points following it are not ASCII hex digits, validation error.
    return URL::percent_encode(input, URL::PercentEncodeSet::C0Control);
}

static Optional<DeprecatedString> parse_ipv4_address(StringView input)
{
    // FIXME: Implement the correct IPv4 parser as specified by https://url.spec.whatwg.org/#concept-ipv4-parser.
    return input;
}

// https://url.spec.whatwg.org/#concept-host-parser
// NOTE: This is a very bare-bones implementation.
static Optional<DeprecatedString> parse_host(StringView input, bool is_not_special = false)
{
    if (input.starts_with('[')) {
        if (!input.ends_with(']')) {
            report_validation_error();
            return {};
        }
        // FIXME: Return the result of IPv6 parsing input with its leading U+005B ([) and trailing U+005D (]) removed.
        TODO();
    }

    if (is_not_special)
        return parse_opaque_host(input);
    VERIFY(!input.is_empty());

    // FIXME: Let domain be the result of running UTF-8 decode without BOM on the percent-decoding of input.
    auto domain = URL::percent_decode(input);
    // FIXME: Let asciiDomain be the result of running domain to ASCII on domain.
    auto& ascii_domain = domain;

    auto forbidden_host_characters = "\0\t\n\r #%/:<>?@[\\]^|"sv;
    for (auto character : forbidden_host_characters) {
        if (ascii_domain.view().contains(character)) {
            report_validation_error();
            return {};
        }
    }

    auto ipv4_host = parse_ipv4_address(ascii_domain);
    return ipv4_host;
}

// https://url.spec.whatwg.org/#start-with-a-windows-drive-letter
constexpr bool starts_with_windows_drive_letter(StringView input)
{
    if (input.length() < 2)
        return false;
    if (!is_ascii_alpha(input[0]) || !(input[1] == ':' || input[1] == '|'))
        return false;
    if (input.length() == 2)
        return true;
    return "/\\?#"sv.contains(input[2]);
}

constexpr bool is_windows_drive_letter(StringView input)
{
    return input.length() == 2 && is_ascii_alpha(input[0]) && (input[1] == ':' || input[1] == '|');
}

constexpr bool is_normalized_windows_drive_letter(StringView input)
{
    return input.length() == 2 && is_ascii_alpha(input[0]) && input[1] == ':';
}

constexpr bool is_single_dot_path_segment(StringView input)
{
    return input == "."sv || input.equals_ignoring_ascii_case("%2e"sv);
}

constexpr bool is_double_dot_path_segment(StringView input)
{
    return input == ".."sv || input.equals_ignoring_ascii_case(".%2e"sv) || input.equals_ignoring_ascii_case("%2e."sv) || input.equals_ignoring_ascii_case("%2e%2e"sv);
}

// https://url.spec.whatwg.org/#string-percent-encode-after-encoding
DeprecatedString URLParser::percent_encode_after_encoding(StringView input, URL::PercentEncodeSet percent_encode_set, bool space_as_plus)
{
    // NOTE: This is written somewhat ad-hoc since we don't yet implement the Encoding spec.

    StringBuilder output;

    // 3. For each byte of encodeOutput converted to a byte sequence:
    for (auto byte : input) {
        // 1. If spaceAsPlus is true and byte is 0x20 (SP), then append U+002B (+) to output and continue.
        if (space_as_plus && byte == ' ') {
            output.append('+');
            continue;
        }

        // 2. Let isomorph be a code point whose value is byte’s value.
        u32 isomorph = byte;

        // 3. Assert: percentEncodeSet includes all non-ASCII code points.

        // 4. If isomorphic is not in percentEncodeSet, then append isomorph to output.
        if (!URL::code_point_is_in_percent_encode_set(isomorph, percent_encode_set)) {
            output.append_code_point(isomorph);
        }

        // 5. Otherwise, percent-encode byte and append the result to output.
        else {
            output.appendff("%{:02X}", byte);
        }
    }

    // 6. Return output.
    return output.to_deprecated_string();
}

// https://fetch.spec.whatwg.org/#data-urls
// FIXME: This only loosely follows the spec, as we use the same class for "regular" and data URLs, unlike the spec.
Optional<URL> URLParser::parse_data_url(StringView raw_input)
{
    dbgln_if(URL_PARSER_DEBUG, "URLParser::parse_data_url: Parsing '{}'.", raw_input);
    VERIFY(raw_input.starts_with("data:"sv));
    auto input = raw_input.substring_view(5);
    auto comma_offset = input.find(',');
    if (!comma_offset.has_value())
        return {};
    auto mime_type = StringUtils::trim(input.substring_view(0, comma_offset.value()), "\t\n\f\r "sv, TrimMode::Both);
    auto encoded_body = input.substring_view(comma_offset.value() + 1);
    auto body = URL::percent_decode(encoded_body);
    bool is_base64_encoded = false;
    if (mime_type.ends_with("base64"sv, CaseSensitivity::CaseInsensitive)) {
        auto substring_view = mime_type.substring_view(0, mime_type.length() - 6);
        auto trimmed_substring_view = StringUtils::trim(substring_view, " "sv, TrimMode::Right);
        if (trimmed_substring_view.ends_with(';')) {
            is_base64_encoded = true;
            mime_type = trimmed_substring_view.substring_view(0, trimmed_substring_view.length() - 1);
        }
    }

    StringBuilder builder;
    if (mime_type.starts_with(";"sv) || mime_type.is_empty()) {
        builder.append("text/plain"sv);
        builder.append(mime_type);
        mime_type = builder.string_view();
    }

    // FIXME: Parse the MIME type's components according to https://mimesniff.spec.whatwg.org/#parse-a-mime-type
    URL url { StringUtils::trim(mime_type, "\n\r\t "sv, TrimMode::Both), move(body), is_base64_encoded };
    dbgln_if(URL_PARSER_DEBUG, "URLParser::parse_data_url: Parsed data URL to be '{}'.", url.serialize());
    return url;
}

// https://url.spec.whatwg.org/#concept-basic-url-parser
// NOTE: This parser assumes a UTF-8 encoding.
// NOTE: Refrain from using the URL classes setters inside this algorithm. Rather, set the values directly. This bypasses the setters' built-in
//       validation, which is strictly unnecessary since we set m_valid=true at the end anyways. Furthermore, this algorithm may be used in the
//       future for validation of URLs, which would then lead to infinite recursion.
//       The same goes for base_url, because e.g. the port() getter does not always return m_port, and we are interested in the underlying member
//       variables' values here, not what the URL class presents to its users.
URL URLParser::parse(StringView raw_input, Optional<URL> const& base_url, Optional<URL> url, Optional<State> state_override)
{
    dbgln_if(URL_PARSER_DEBUG, "URLParser::parse: Parsing '{}'", raw_input);
    if (raw_input.is_empty())
        return base_url.has_value() ? *base_url : URL {};

    if (raw_input.starts_with("data:"sv)) {
        auto maybe_url = parse_data_url(raw_input);
        if (!maybe_url.has_value())
            return {};
        return maybe_url.release_value();
    }

    size_t start_index = 0;
    size_t end_index = raw_input.length();

    // 1. If url is not given:
    if (!url.has_value()) {
        // 1. Set url to a new URL.
        url = URL();

        // 2. If input contains any leading or trailing C0 control or space, invalid-URL-unit validation error.
        // 3. Remove any leading and trailing C0 control or space from input.
        //
        // FIXME: We aren't checking exactly for 'trailing C0 control or space' here.

        bool has_validation_error = false;
        for (size_t i = 0; i < raw_input.length(); ++i) {
            i8 ch = raw_input[i];
            if (0 <= ch && ch <= 0x20) {
                ++start_index;
                has_validation_error = true;
            } else {
                break;
            }
        }
        for (ssize_t i = raw_input.length() - 1; i >= 0; --i) {
            i8 ch = raw_input[i];
            if (0 <= ch && ch <= 0x20) {
                --end_index;
                has_validation_error = true;
            } else {
                break;
            }
        }
        if (has_validation_error)
            report_validation_error();
    }
    if (start_index >= end_index)
        return {};

    DeprecatedString processed_input = raw_input.substring_view(start_index, end_index - start_index);

    // 2. If input contains any ASCII tab or newline, invalid-URL-unit validation error.
    // 3. Remove all ASCII tab or newline from input.
    if (processed_input.contains("\t"sv) || processed_input.contains("\n"sv)) {
        report_validation_error();
        processed_input = processed_input.replace("\t"sv, ""sv, ReplaceMode::All).replace("\n"sv, ""sv, ReplaceMode::All);
    }

    // 4. Let state be state override if given, or scheme start state otherwise.
    State state = state_override.value_or(State::SchemeStart);

    // FIXME: 5. Set encoding to the result of getting an output encoding from encoding.

    // 6. Let buffer be the empty string.
    StringBuilder buffer;

    // 7. Let atSignSeen, insideBrackets, and passwordTokenSeen be false.
    bool at_sign_seen = false;
    bool inside_brackets = false;
    bool password_token_seen = false;

    Utf8View input(processed_input);

    // 8. Let pointer be a pointer for input.
    Utf8CodePointIterator iterator = input.begin();

    auto get_remaining = [&input, &iterator] {
        return input.substring_view(iterator - input.begin() + iterator.underlying_code_point_length_in_bytes()).as_string();
    };

    // 9. Keep running the following state machine by switching on state. If after a run pointer points to the EOF code point, go to the next step. Otherwise, increase pointer by 1 and continue with the state machine.
    // NOTE: "continue" should only be used to prevent incrementing the iterator, as this is done at the end of the loop.
    //       ++iterator : "increase pointer by 1"
    //       continue   : "decrease pointer by 1"
    for (;;) {
        u32 code_point = end_of_file;
        if (!iterator.done())
            code_point = *iterator;

        if constexpr (URL_PARSER_DEBUG) {
            if (code_point == end_of_file)
                dbgln("URLParser::parse: {} state with EOF.", state_name(state));
            else if (is_ascii_printable(code_point))
                dbgln("URLParser::parse: {} state with code point U+{:04X} ({:c}).", state_name(state), code_point, code_point);
            else
                dbgln("URLParser::parse: {} state with code point U+{:04X}.", state_name(state), code_point);
        }

        switch (state) {
        // -> scheme start state, https://url.spec.whatwg.org/#scheme-start-state
        case State::SchemeStart:
            // 1. If c is an ASCII alpha, append c, lowercased, to buffer, and set state to scheme state.
            if (is_ascii_alpha(code_point)) {
                buffer.append_as_lowercase(code_point);
                state = State::Scheme;
            }
            // 2. Otherwise, if state override is not given, set state to no scheme state and decrease pointer by 1.
            else if (!state_override.has_value()) {
                state = State::NoScheme;
                continue;
            }
            // 3. Otherwise, return failure.
            else {
                return {};
            }
            break;
        // -> scheme state, https://url.spec.whatwg.org/#scheme-state
        case State::Scheme:
            // 1. If c is an ASCII alphanumeric, U+002B (+), U+002D (-), or U+002E (.), append c, lowercased, to buffer.
            if (is_ascii_alphanumeric(code_point) || code_point == '+' || code_point == '-' || code_point == '.') {
                buffer.append_as_lowercase(code_point);
            }
            // 2. Otherwise, if c is U+003A (:), then:
            else if (code_point == ':') {
                // FIXME: 1. If state override is given, then:
                if (false) {
                    // FIXME: 1. If url’s scheme is a special scheme and buffer is not a special scheme, then return.
                    // FIXME: 2. If url’s scheme is a special scheme and buffer is not a special scheme, then return.
                    // FIXME: 3. If url includes credentials or has a non-null port, and buffer is "file", then return.
                    // FIXME: 4. If url’s scheme is "file" and its host is an empty host, then return.
                }

                // 2. Set url’s scheme to buffer.
                url->m_scheme = buffer.to_deprecated_string();

                // FIXME: 3. If state override is given, then:
                if (false) {
                    // FIXME: 1. If url’s port is url’s scheme’s default port, then set url’s port to null.
                    // FIXME: 2. Return.
                }

                // 4. Set buffer to the empty string.
                buffer.clear();

                // 5. If url’s scheme is "file", then:
                if (url->scheme() == "file") {
                    // 1. If remaining does not start with "//", special-scheme-missing-following-solidus validation error.
                    if (!get_remaining().starts_with("//"sv)) {
                        report_validation_error();
                    }
                    // 2. Set state to file state.
                    state = State::File;
                }
                // 6. Otherwise, if url is special, base is non-null, and base’s scheme is url’s scheme:
                // 7. Otherwise, if url is special, set state to special authority slashes state.
                // FIXME: Write this block closer to spec text.
                else if (url->is_special()) {
                    // FIXME: 1. Assert: base is is special (and therefore does not have an opaque path).

                    // 2. Set state to special relative or authority state.
                    if (base_url.has_value() && base_url->m_scheme == url->m_scheme)
                        state = State::SpecialRelativeOrAuthority;
                    else
                        state = State::SpecialAuthoritySlashes;
                }

                // 8. Otherwise, if remaining starts with an U+002F (/), set state to path or authority state and increase pointer by 1.
                else if (get_remaining().starts_with("/"sv)) {
                    state = State::PathOrAuthority;
                    ++iterator;
                }
                // 9. Otherwise, set url’s path to the empty string and set state to opaque path state.
                else {
                    url->m_cannot_be_a_base_url = true;
                    url->append_slash();
                    state = State::CannotBeABaseUrlPath;
                }
            }
            // 3. Otherwise, if state override is not given, set buffer to the empty string, state to no scheme state, and start over (from the first code point in input).
            else if (!state_override.has_value()) {
                buffer.clear();
                state = State::NoScheme;
                iterator = input.begin();
                continue;
            }
            // 4. Otherwise, return failure.
            else {
                return {};
            }
            break;
        // -> no scheme state, https://url.spec.whatwg.org/#no-scheme-state
        case State::NoScheme:
            // 1. If base is null, or base has an opaque path and c is not U+0023 (#), missing-scheme-non-relative-URL validation error, return failure.
            if (!base_url.has_value() || (base_url->m_cannot_be_a_base_url && code_point != '#')) {
                report_validation_error();
                return {};
            }
            // 2. Otherwise, if base has an opaque path and c is U+0023 (#), set url’s scheme to base’s scheme, url’s path to base’s path, url’s query
            //    to base’s query,url’s fragment to the empty string, and set state to fragment state.
            else if (base_url->m_cannot_be_a_base_url && code_point == '#') {
                url->m_scheme = base_url->m_scheme;
                url->m_paths = base_url->m_paths;
                url->m_query = base_url->m_query;
                url->m_fragment = "";
                url->m_cannot_be_a_base_url = true;
                state = State::Fragment;
            }
            // 3. Otherwise, if base’s scheme is not "file", set state to relative state and decrease pointer by 1.
            else if (base_url->m_scheme != "file") {
                state = State::Relative;
                continue;
            }
            // 4. Otherwise, set state to file state and decrease pointer by 1.
            else {
                state = State::File;
                continue;
            }
            break;
        // -> special relative or authority state, https://url.spec.whatwg.org/#special-relative-or-authority-state
        case State::SpecialRelativeOrAuthority:
            // 1. If c is U+002F (/) and remaining starts with U+002F (/), then set state to special authority ignore slashes state and increase pointer by 1.
            if (code_point == '/' && get_remaining().starts_with("/"sv)) {
                state = State::SpecialAuthorityIgnoreSlashes;
                ++iterator;
            }
            // 2. Otherwise, special-scheme-missing-following-solidus validation error, set state to relative state and decrease pointer by 1.
            else {
                report_validation_error();
                state = State::Relative;
                continue;
            }
            break;
        // -> path or authority state, https://url.spec.whatwg.org/#path-or-authority-state
        case State::PathOrAuthority:
            // 1. If c is U+002F (/), then set state to authority state.
            if (code_point == '/') {
                state = State::Authority;
            }
            // 2. Otherwise, set state to path state, and decrease pointer by 1.
            else {
                state = State::Path;
                continue;
            }
            break;
        // -> relative state, https://url.spec.whatwg.org/#relative-state
        case State::Relative:
            // FIXME: 1. Assert: base’s scheme is not "file".

            // 2. Set url’s scheme to base’s scheme.
            url->m_scheme = base_url->m_scheme;

            // 3. If c is U+002F (/), then set state to relative slash state.
            if (code_point == '/') {
                state = State::RelativeSlash;
            }
            // 4. Otherwise, if url is special and c is U+005C (\), invalid-reverse-solidus validation error, set state to relative slash state.
            else if (url->is_special() && code_point == '\\') {
                report_validation_error();
                state = State::RelativeSlash;
            }
            // 5. Otherwise:
            else {
                // 1. Set url’s username to base’s username, url’s password to base’s password, url’s host to base’s host, url’s port to base’s port, url’s path to a clone of base’s path, and url’s query to base’s query.
                url->m_username = base_url->m_username;
                url->m_password = base_url->m_password;
                url->m_host = base_url->m_host;
                url->m_port = base_url->m_port;
                url->m_paths = base_url->m_paths;
                url->m_query = base_url->m_query;

                // 2. If c is U+003F (?), then set url’s query to the empty string, and state to query state.
                if (code_point == '?') {
                    url->m_query = "";
                    state = State::Query;
                }
                // 3. Otherwise, if c is U+0023 (#), set url’s fragment to the empty string and state to fragment state.
                else if (code_point == '#') {
                    url->m_fragment = "";
                    state = State::Fragment;
                }
                // 4. Otherwise, if c is not the EOF code point:
                else if (code_point != end_of_file) {
                    // 1. Set url’s query to null.
                    url->m_query = {};

                    // 2. Shorten url’s path.
                    if (url->m_paths.size())
                        url->m_paths.remove(url->m_paths.size() - 1);

                    // 3. Set state to path state and decrease pointer by 1.
                    state = State::Path;
                    continue;
                }
            }
            break;
        // -> relative slash state, https://url.spec.whatwg.org/#relative-slash-state
        case State::RelativeSlash:
            // 1. If url is special and c is U+002F (/) or U+005C (\), then:
            if (url->is_special() && (code_point == '/' || code_point == '\\')) {
                // 1. If c is U+005C (\), invalid-reverse-solidus validation error.
                if (code_point == '\\')
                    report_validation_error();

                // 2. Set state to special authority ignore slashes state.
                state = State::SpecialAuthorityIgnoreSlashes;
            }
            // 2. Otherwise, if c is U+002F (/), then set state to authority state.
            else if (code_point == '/') {
                state = State::Authority;
            }
            // 3. Otherwise, set url’s username to base’s username, url’s password to base’s password, url’s host to base’s host, url’s port to base’s port, state to path state, and then, decrease pointer by 1.
            else {
                url->m_username = base_url->m_username;
                url->m_password = base_url->m_password;
                url->m_host = base_url->m_host;
                url->m_port = base_url->m_port;
                state = State::Path;
                continue;
            }
            break;
        // -> special authority slashes state, https://url.spec.whatwg.org/#special-authority-slashes-state
        case State::SpecialAuthoritySlashes:
            // 1. If c is U+002F (/) and remaining starts with U+002F (/), then set state to special authority ignore slashes state and increase pointer by 1.
            if (code_point == '/' && get_remaining().starts_with("/"sv)) {
                state = State::SpecialAuthorityIgnoreSlashes;
                ++iterator;
            }
            // 2. Otherwise, special-scheme-missing-following-solidus validation error, set state to special authority ignore slashes state and decrease pointer by 1.
            else {
                report_validation_error();
                state = State::SpecialAuthorityIgnoreSlashes;
                continue;
            }
            break;
        // -> special authority ignore slashes state, https://url.spec.whatwg.org/#special-authority-ignore-slashes-state
        case State::SpecialAuthorityIgnoreSlashes:
            // 1. If c is neither U+002F (/) nor U+005C (\), then set state to authority state and decrease pointer by 1.
            if (code_point != '/' && code_point != '\\') {
                state = State::Authority;
                continue;
            }
            // 2. Otherwise, special-scheme-missing-following-solidus validation error.
            else {
                report_validation_error();
            }
            break;
        // -> authority state, https://url.spec.whatwg.org/#authority-state
        case State::Authority:
            // 1. If c is U+0040 (@), then:
            if (code_point == '@') {
                // 1. Invalid-credentials validation error.
                report_validation_error();

                // 2. If atSignSeen is true, then prepend "%40" to buffer.
                if (at_sign_seen) {
                    auto content = buffer.to_deprecated_string();
                    buffer.clear();
                    buffer.append("%40"sv);
                    buffer.append(content);
                }

                // 3. Set atSignSeen to true.
                at_sign_seen = true;

                StringBuilder builder;

                // FIXME: 4. For each codePoint in buffer:
                for (auto c : Utf8View(builder.string_view())) {
                    // 1. If codePoint is U+003A (:) and passwordTokenSeen is false, then set passwordTokenSeen to true and continue.
                    if (c == ':' && !password_token_seen) {
                        password_token_seen = true;
                        continue;
                    }

                    // 2. Let encodedCodePoints be the result of running UTF-8 percent-encode codePoint using the userinfo percent-encode set.
                    // NOTE: This is done inside of step 3 and 4 implementation

                    builder.clear();
                    // 3. If passwordTokenSeen is true, then append encodedCodePoints to url’s password.
                    if (password_token_seen) {
                        builder.append(url->password());
                        URL::append_percent_encoded_if_necessary(builder, c, URL::PercentEncodeSet::Userinfo);
                        url->m_password = builder.string_view();
                    }
                    // 4. Otherwise, append encodedCodePoints to url’s username.
                    else {
                        builder.append(url->username());
                        URL::append_percent_encoded_if_necessary(builder, c, URL::PercentEncodeSet::Userinfo);
                        url->m_username = builder.string_view();
                    }
                }

                // 5. Set buffer to the empty string.
                buffer.clear();

            }
            // 2. Otherwise, if one of the following is true:
            //    * c is the EOF code point, U+002F (/), U+003F (?), or U+0023 (#)
            //    * url is special and c is U+005C (\)
            else if ((code_point == end_of_file || code_point == '/' || code_point == '?' || code_point == '#')
                || (url->is_special() && code_point == '\\')) {
                // then:

                // 1. If atSignSeen is true and buffer is the empty string, invalid-credentials validation error, return failure.
                if (at_sign_seen && buffer.is_empty()) {
                    report_validation_error();
                    return {};
                }

                // 2. Decrease pointer by buffer’s code point length + 1, set buffer to the empty string, and set state to host state.
                iterator = input.iterator_at_byte_offset(iterator - input.begin() - buffer.length() - 1);
                buffer.clear();
                state = State::Host;
            }
            // 3. Otherwise, append c to buffer.
            else {
                buffer.append_code_point(code_point);
            }
            break;
        // -> host state, https://url.spec.whatwg.org/#host-state
        // -> hostname state, https://url.spec.whatwg.org/#hostname-state
        case State::Host:
        case State::Hostname:
            // 1. If state override is given and url’s scheme is "file", then decrease pointer by 1 and set state to file host state.
            if (state_override.has_value() && url->scheme() == "file") {
                state = State::FileHost;
                continue;
            }

            // 2. Otherwise, if c is U+003A (:) and insideBrackets is false, then:
            if (code_point == ':' && !inside_brackets) {
                // 1. If buffer is the empty string, host-missing validation error, return failure.
                if (buffer.is_empty()) {
                    report_validation_error();
                    return {};
                }

                // FIXME: 2. If state override is given and state override is hostname state, then return.

                // 3. Let host be the result of host parsing buffer with url is not special.
                auto host = parse_host(buffer.string_view(), !url->is_special());

                // 4. If host is failure, then return failure.
                if (!host.has_value())
                    return {};

                // 5. Set url’s host to host, buffer to the empty string, and state to port state.
                url->m_host = host.release_value();
                buffer.clear();
                state = State::Port;
            }
            // 3. Otherwise, if one of the following is true:
            //    * c is the EOF code point, U+002F (/), U+003F (?), or U+0023 (#)
            //    * url is special and c is U+005C (\)
            else if ((code_point == end_of_file || code_point == '/' || code_point == '?' || code_point == '#')
                || (url->is_special() && code_point == '\\')) {
                // then decrease pointer by 1, and then:
                // NOTE: pointer decrement is done by the continue below

                // 1. If url is special and buffer is the empty string, host-missing validation error, return failure.
                if (url->is_special() && buffer.is_empty()) {
                    report_validation_error();
                    return {};
                }

                // FIXME: 2. Otherwise, if state override is given, buffer is the empty string, and either url includes credentials or url’s port is non-null, return.

                // 3. Let host be the result of host parsing buffer with url is not special.
                auto host = parse_host(buffer.string_view(), !url->is_special());

                // 4. If host is failure, then return failure.
                if (!host.has_value())
                    return {};

                // 5. Set url’s host to host, buffer to the empty string, and state to path start state.
                url->m_host = host.value();
                buffer.clear();
                state = State::Port;

                // FIXME: 6. If state override is given, then return.
                continue;

            }
            // 4. Otherwise:
            // FIXME: Implement closer to spec text. From reading it, shouldn't we be appending [ or ] to buffer as well? Step 3. below does not have an 'otherwise'.
            //
            // 1. If c is U+005B ([), then set insideBrackets to true.
            else if (code_point == '[') {
                inside_brackets = true;
            }
            // 2. If c is U+005D (]), then set insideBrackets to false.
            else if (code_point == ']') {
                inside_brackets = false;
            }
            // 3. Append c to buffer.
            else {
                buffer.append_code_point(code_point);
            }
            break;
        // -> port state, https://url.spec.whatwg.org/#port-state
        case State::Port:
            // 1. If c is an ASCII digit, append c to buffer.
            if (is_ascii_digit(code_point)) {
                buffer.append_code_point(code_point);
            }

            // 2. Otherwise, if one of the following is true:
            //    * c is the EOF code point, U+002F (/), U+003F (?), or U+0023 (#)
            //    * url is special and c is U+005C (\)
            //    * state override is given
            else if ((code_point == end_of_file || code_point == '/' || code_point == '?' || code_point == '#')
                || (url->is_special() && code_point == '\\')
                || state_override.has_value()) {
                // then:

                // 1. If buffer is not the empty string, then:
                if (!buffer.is_empty()) {
                    // 1. Let port be the mathematical integer value that is represented by buffer in radix-10 using ASCII digits for digits with values 0 through 9.
                    auto port = buffer.string_view().to_uint();

                    // 2. If port is greater than 2^16 − 1, port-out-of-range validation error, return failure.
                    if (!port.has_value() || port.value() > 65535) {
                        report_validation_error();
                        return {};
                    }

                    // 3. Set url’s port to null, if port is url’s scheme’s default port; otherwise to port.
                    if (port.value() == URL::default_port_for_scheme(url->scheme()))
                        url->m_port = {};
                    else
                        url->m_port = port.value();

                    // 4. Set buffer to the empty string.
                    buffer.clear();
                }

                // FIXME: 2. If state override is given, then return.

                // 3. Set state to path start state and decrease pointer by 1.
                state = State::PathStart;
                continue;
            }
            // 3. Otherwise, port-invalid validation error, return failure.
            else {
                report_validation_error();
                return {};
            }
            break;
        // -> file state, https://url.spec.whatwg.org/#file-state
        case State::File:
            // 1. Set url’s scheme to "file".
            url->m_scheme = "file";

            // 2. Set url’s host to the empty string.
            url->m_host = "";

            // 3. If c is U+002F (/) or U+005C (\), then:
            if (code_point == '/' || code_point == '\\') {
                // 1. If c is U+005C (\), invalid-reverse-solidus validation error.
                if (code_point == '\\')
                    report_validation_error();

                // 2. Set state to file slash state.
                state = State::FileSlash;
            }
            // 4. Otherwise, if base is non-null and base’s scheme is "file":
            else if (base_url.has_value() && base_url->m_scheme == "file") {
                // 1. Set url’s host to base’s host, url’s path to a clone of base’s path, and url’s query to base’s query.
                url->m_host = base_url->m_host;
                url->m_paths = base_url->m_paths;
                url->m_query = base_url->m_query;

                // 2. If c is U+003F (?), then set url’s query to the empty string and state to query state.
                if (code_point == '?') {
                    url->m_query = "";
                    state = State::Query;
                }
                // 3. Otherwise, if c is U+0023 (#), set url’s fragment to the empty string and state to fragment state.
                else if (code_point == '#') {
                    url->m_fragment = "";
                    state = State::Fragment;
                }
                // 4. Otherwise, if c is not the EOF code point:
                else if (code_point != end_of_file) {
                    // 1. Set url’s query to null.
                    url->m_query = {};

                    // 2. If the code point substring from pointer to the end of input does not start with a Windows drive letter, then shorten url’s path.
                    auto substring_from_pointer = input.substring_view(iterator - input.begin()).as_string();
                    if (!starts_with_windows_drive_letter(substring_from_pointer)) {
                        if (!url->m_paths.is_empty() && !(url->scheme() == "file" && url->m_paths.size() == 1 && is_normalized_windows_drive_letter(url->m_paths[0])))
                            url->m_paths.remove(url->m_paths.size() - 1);
                    }
                    // 3. Otherwise:
                    else {
                        // 1. File-invalid-Windows-drive-letter validation error.
                        report_validation_error();

                        // 2. Set url’s path to « ».
                        url->m_paths.clear();
                    }

                    // 4. Set state to path state and decrease pointer by 1.
                    state = State::Path;
                    continue;
                }
            }
            // 5. Otherwise, set state to path state, and decrease pointer by 1.
            else {
                state = State::Path;
                continue;
            }

            break;
        // -> file slash state, https://url.spec.whatwg.org/#file-slash-state
        case State::FileSlash:
            // 1. If c is U+002F (/) or U+005C (\), then:
            if (code_point == '/' || code_point == '\\') {
                // 1. If c is U+005C (\), invalid-reverse-solidus validation error.
                if (code_point == '\\')
                    report_validation_error();

                // 2. Set state to file host state.
                state = State::FileHost;
            }
            // 2. Otherwise:
            // 1. If base is non-null and base’s scheme is "file", then:
            else if (base_url.has_value() && base_url->m_scheme == "file") {
                // 1. Set url’s host to base’s host.
                url->m_paths = base_url->m_paths;
                url->m_paths.remove(url->m_paths.size() - 1);

                // 2. If the code point substring from pointer to the end of input does not start with a Windows drive letter and base’s path[0] is a normalized Windows drive letter, then append base’s path[0] to url’s path.
                auto substring_from_pointer = input.substring_view(iterator - input.begin()).as_string();
                if (!starts_with_windows_drive_letter(substring_from_pointer) && is_normalized_windows_drive_letter(base_url->m_paths[0]))
                    url->append_path(base_url->m_paths[0], URL::ApplyPercentEncoding::No);

                // FIXME: This should be done outside of this file block, see below.
                state = State::Path;
                continue;
            }
            // FIXME: 2. Set state to path state, and decrease pointer by 1.
            break;
        // -> file host state, https://url.spec.whatwg.org/#file-host-state
        case State::FileHost:
            // 1. If c is the EOF code point, U+002F (/), U+005C (\), U+003F (?), or U+0023 (#), then decrease pointer by 1 and then:
            //    NOTE: decreasing the pointer is done at the bottom of this block.
            if (code_point == end_of_file || code_point == '/' || code_point == '\\' || code_point == '?' || code_point == '#') {
                // 1. If state override is not given and buffer is a Windows drive letter, file-invalid-Windows-drive-letter-host validation error, set state to path state.
                if (!state_override.has_value() && is_windows_drive_letter(buffer.string_view())) {
                    report_validation_error();
                    state = State::Path;
                }
                // 2. Otherwise, if buffer is the empty string, then:
                else if (buffer.is_empty()) {
                    // 1. Set url’s host to the empty string.
                    url->m_host = "";

                    // FIXME: 2. If state override is given, then return.

                    // 3. Set state to path start state.
                    state = State::PathStart;
                }
                // 3. Otherwise, run these steps:
                else {
                    // 1. Let host be the result of host parsing buffer with url is not special.
                    // FIXME: It seems we are not passing through url is not special through here
                    auto host = parse_host(buffer.string_view(), true);

                    // 2. If host is failure, then return failure.
                    if (!host.has_value())
                        return {};

                    // 3. If host is "localhost", then set host to the empty string.
                    if (host.value() == "localhost")
                        host = "";

                    // 4. Set url’s host to host.
                    url->m_host = host.release_value();

                    // FIXME: 5. If state override is given, then return.

                    // 6. Set buffer to the empty string and state to path start state.
                    buffer.clear();
                    state = State::PathStart;
                }

                // NOTE: Decrement specified at the top of this 'if' statement.
                continue;
            } else {
                buffer.append_code_point(code_point);
            }
            break;
        // -> path start state, https://url.spec.whatwg.org/#path-start-state
        case State::PathStart:
            // 1. If url is special, then:
            if (url->is_special()) {
                // 1. If c is U+005C (\), invalid-reverse-solidus validation error.
                if (code_point == '\\')
                    report_validation_error();

                // 2. Set state to path state.
                state = State::Path;

                // 3. If c is neither U+002F (/) nor U+005C (\), then decrease pointer by 1.
                if (code_point != '/' && code_point != '\\')
                    continue;
            }
            // 2. Otherwise, if state override is not given and c is U+003F (?), set url’s query to the empty string and state to query state.
            else if (!state_override.has_value() && code_point == '?') {
                url->m_query = "";
                state = State::Query;
            }
            // 3. Otherwise, if state override is not given and c is U+0023 (#), set url’s fragment to the empty string and state to fragment state.
            else if (!state_override.has_value() && code_point == '#') {
                url->m_fragment = "";
                state = State::Fragment;
            }
            // 4. Otherwise, if c is not the EOF code point:
            else if (code_point != end_of_file) {
                // 1. Set state to path state.
                state = State::Path;

                // 2. If c is not U+002F (/), then decrease pointer by 1.
                if (code_point != '/')
                    continue;
            }
            // 5. Otherwise, if state override is given and url’s host is null, append the empty string to url’s path.
            else if (state_override.has_value() && url->host().is_empty()) {
                url->append_slash();
            }
            break;
        // -> path state, https://url.spec.whatwg.org/#path-state
        case State::Path:
            // 1. If one of the following is true:
            //    * c is the EOF code point or U+002F (/)
            //    * url is special and c is U+005C (\)
            //    * state override is not given and c is U+003F (?) or U+0023 (#)
            if ((code_point == end_of_file || code_point == '/')
                || (url->is_special() && code_point == '\\')
                || (!state_override.has_value() && (code_point == '?' || code_point == '#'))) {
                // then:

                // 1. If url is special and c is U+005C (\), invalid-reverse-solidus validation error.
                if (url->is_special() && code_point == '\\')
                    report_validation_error();

                // 2. If buffer is a double-dot URL path segment, then:
                if (is_double_dot_path_segment(buffer.string_view())) {
                    // FIXME: 1. Shorten url’s path.
                    if (!url->m_paths.is_empty() && !(url->m_scheme == "file" && url->m_paths.size() == 1 && is_normalized_windows_drive_letter(url->m_paths[0])))
                        url->m_paths.remove(url->m_paths.size() - 1);

                    // 2. If neither c is U+002F (/), nor url is special and c is U+005C (\), append the empty string to url’s path.
                    if (code_point != '/' && !(url->is_special() && code_point == '\\'))
                        url->append_slash();
                }
                // 3. Otherwise, if buffer is a single-dot URL path segment and if neither c is U+002F (/), nor url is special and c is U+005C (\), append the empty string to url’s path.
                else if (is_single_dot_path_segment(buffer.string_view()) && code_point != '/' && !(url->is_special() && code_point == '\\')) {
                    url->append_slash();
                }
                // 4. Otherwise, if buffer is not a single-dot URL path segment, then:
                else if (!is_single_dot_path_segment(buffer.string_view())) {
                    // 1. If url’s scheme is "file", url’s path is empty, and buffer is a Windows drive letter, then replace the second code point in buffer with U+003A (:).
                    if (url->m_scheme == "file" && url->m_paths.is_empty() && is_windows_drive_letter(buffer.string_view())) {
                        auto drive_letter = buffer.string_view()[0];
                        buffer.clear();
                        buffer.append(drive_letter);
                        buffer.append(':');
                    }
                    // 2. Append buffer to url’s path.
                    //    FIXME: It would be nicer (and closer to spec) if URLParser could just directly append the path.
                    url->append_path(buffer.string_view(), URL::ApplyPercentEncoding::No);
                }

                // 5. Set buffer to the empty string.
                buffer.clear();

                // 6. If c is U+003F (?), then set url’s query to the empty string and state to query state.
                if (code_point == '?') {
                    url->m_query = "";
                    state = State::Query;
                }
                // 7. If c is U+0023 (#), then set url’s fragment to the empty string and state to fragment state.
                else if (code_point == '#') {
                    url->m_fragment = "";
                    state = State::Fragment;
                }
            }
            // 2. Otherwise, run these steps
            else {
                // 1. If c is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
                if (!is_url_code_point(code_point) && code_point != '%')
                    report_validation_error();

                // FIXME: 2. If c is U+0025 (%) and remaining does not start with two ASCII hex digits, validation error.

                // 3. UTF-8 percent-encode c using the path percent-encode set and append the result to buffer.
                URL::append_percent_encoded_if_necessary(buffer, code_point, URL::PercentEncodeSet::Path);
            }
            break;
        // -> opaque path state, https://url.spec.whatwg.org/#cannot-be-a-base-url-path-state
        case State::CannotBeABaseUrlPath:
            // NOTE: This does not follow the spec exactly but rather uses the buffer and only sets the path on EOF.
            // NOTE: Verify that the assumptions required for this simplification are correct.
            VERIFY(url->m_paths.size() == 1 && url->m_paths[0].is_empty());

            // 1. If c is U+003F (?), then set url’s query to the empty string and state to query state.
            if (code_point == '?') {
                url->m_paths[0] = buffer.string_view();
                url->m_query = "";
                state = State::Query;
            }
            // 2. Otherwise, if c is U+0023 (#), then set url’s fragment to the empty string and state to fragment state.
            else if (code_point == '#') {
                // NOTE: This needs to be percent decoded since the member variables contain decoded data.
                url->m_paths[0] = buffer.string_view();
                url->m_fragment = "";
                state = State::Fragment;
            }
            // 3. Otherwise:
            else {
                // 1. If c is not the EOF code point, not a URL code point, and not U+0025 (%), invalid-URL-unit validation error.
                if (code_point != end_of_file && !is_url_code_point(code_point) && code_point != '%')
                    report_validation_error();

                // FIXME: 2. If c is U+0025 (%) and remaining does not start with two ASCII hex digits, validation error.

                // 3. If c is not the EOF code point, UTF-8 percent-encode c using the C0 control percent-encode set and append the result to url’s path.
                if (code_point != end_of_file) {
                    URL::append_percent_encoded_if_necessary(buffer, code_point, URL::PercentEncodeSet::C0Control);
                } else {
                    url->m_paths[0] = buffer.string_view();
                }
            }
            break;
        // -> query state, https://url.spec.whatwg.org/#query-state
        case State::Query:
            // FIXME: 1. If encoding is not UTF-8 and one of the following is true:
            //           * url is not special
            //           * url’s scheme is "ws" or "wss"
            //        then set encoding to UTF-8.

            // 2. If one of the following is true:
            //    * state override is not given and c is U+0023 (#)
            //    * c is the EOF code point
            if ((!state_override.has_value() && code_point == '#')
                || code_point == end_of_file) {
                VERIFY(url->m_query == "");
                // then:

                // 1. Let queryPercentEncodeSet be the special-query percent-encode set if url is special; otherwise the query percent-encode set.
                auto query_percent_encode_set = url->is_special() ? URL::PercentEncodeSet::SpecialQuery : URL::PercentEncodeSet::Query;

                // 2. Percent-encode after encoding, with encoding, buffer, and queryPercentEncodeSet, and append the result to url’s query.
                url->m_query = percent_encode_after_encoding(buffer.string_view(), query_percent_encode_set);

                // 3. Set buffer to the empty string.
                buffer.clear();

                // 4. If c is U+0023 (#), then set url’s fragment to the empty string and state to fragment state.
                if (code_point == '#') {
                    url->m_fragment = "";
                    state = State::Fragment;
                }
            }
            // 3. Otherwise, if c is not the EOF code point:
            else if (code_point != end_of_file) {
                // 1. If c is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
                if (!is_url_code_point(code_point) && code_point != '%')
                    report_validation_error();

                // FIXME: 2. If c is U+0025 (%) and remaining does not start with two ASCII hex digits, validation error.

                // 3. Append c to buffer.
                buffer.append_code_point(code_point);
            }
            break;
        // -> fragment state, https://url.spec.whatwg.org/#fragment-state
        case State::Fragment:
            // NOTE: This does not follow the spec exactly but rather uses the buffer and only sets the fragment on EOF.
            // 1. If c is not the EOF code point, then:
            if (code_point != end_of_file) {
                // 1. If c is not a URL code point and not U+0025 (%), invalid-URL-unit validation error.
                if (!is_url_code_point(code_point) && code_point != '%')
                    report_validation_error();

                // FIXME: 2. If c is U+0025 (%) and remaining does not start with two ASCII hex digits, validation error.

                // FIXME: 3. UTF-8 percent-encode c using the fragment percent-encode set and append the result to url’s fragment.
                buffer.append_code_point(code_point);
            } else {
                url->m_fragment = buffer.string_view();
                buffer.clear();
            }
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        if (iterator.done())
            break;
        ++iterator;
    }

    url->m_valid = true;
    dbgln_if(URL_PARSER_DEBUG, "URLParser::parse: Parsed URL to be '{}'.", url->serialize());

    // 10. Return url.
    return url.release_value();
}

}
