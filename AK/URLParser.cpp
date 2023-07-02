/*
 * Copyright (c) 2021, Max Wipfli <mail@maxwipfli.ch>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Debug.h>
#include <AK/DeprecatedString.h>
#include <AK/IntegralMath.h>
#include <AK/Optional.h>
#include <AK/SourceLocation.h>
#include <AK/StringBuilder.h>
#include <AK/StringUtils.h>
#include <AK/Tuple.h>
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

// https://url.spec.whatwg.org/#ipv4-number-parser
static Optional<Tuple<size_t, bool>> parse_ipv4_number(StringView input)
{
    // 1. If input is the empty string, then return failure.
    if (input.is_empty())
        return OptionalNone {};

    // 2. Let validationError be false.
    bool validation_error = false;

    // 3. Let R be 10.
    u8 radix = 10;

    // 4. If input contains at least two code points and the first two code points are either "0X" or "0x", then:
    if (input.length() >= 2 && (input.starts_with("0X"sv) || input.starts_with("0x"sv))) {
        // 1. Set validationError to true.
        validation_error = true;

        // 2. Remove the first two code points from input.
        input = input.substring_view(2);

        // 3. Set R to 16.
        radix = 16;
    }

    // 5. Otherwise, if input contains at least two code points and the first code point is U+0030 (0), then:
    if (input.length() >= 2 && input.starts_with('0')) {
        // 1. Set validationError to true.
        validation_error = true;

        // 2. Remove the first code point from input.
        input = input.substring_view(1);

        // 3. Set R to 8.
        radix = 8;
    }

    // 6. If input is the empty string, then return (0, true).
    if (input.is_empty())
        return Tuple<size_t, bool> { 0, true };

    // 7. If input contains a code point that is not a radix-R digit, then return failure.
    if (radix == 8 && !all_of(input, [](auto character) { return is_ascii_octal_digit(character); }))
        return OptionalNone {};
    if (radix == 10 && !all_of(input, [](auto character) { return is_ascii_digit(character); }))
        return OptionalNone {};
    if (radix == 16 && !all_of(input, [](auto character) { return is_ascii_hex_digit(character); }))
        return OptionalNone {};

    // 8. Let output be the mathematical integer value that is represented by input in radix-R notation, using ASCII hex digits for digits with values 0 through 15.
    Optional<u32> output;
    if (radix == 8)
        output = AK::StringUtils::convert_to_uint_from_octal(input);
    else if (radix == 10)
        output = input.to_uint();
    else if (radix == 16)
        output = AK::StringUtils::convert_to_uint_from_hex(input);
    VERIFY(output.has_value());

    // 9. Return (output, validationError).
    return Tuple<size_t, bool> { static_cast<size_t>(*output), validation_error };
}

static Optional<DeprecatedString> parse_ipv4_address(StringView input)
{
    // 1. Let parts be the result of strictly splitting input on U+002E (.).
    auto parts = input.split_view('.');

    // 2. If the last item in parts is the empty string, then:
    if (parts.last().is_empty()) {
        // 1. IPv4-empty-part validation error.
        report_validation_error();

        // 2. If parts’s size is greater than 1, then remove the last item from parts.
        if (parts.size() > 1)
            parts.remove(parts.size() - 1);
    }

    // 3. If parts’s size is greater than 4, IPv4-too-many-parts validation error, return failure.
    if (parts.size() > 4) {
        report_validation_error();
        return OptionalNone {};
    }

    // 4. Let numbers be an empty list.
    Vector<size_t> numbers;

    // 5. For each part of parts:
    for (auto part : parts) {
        // 1. Let result be the result of parsing part.
        auto result = parse_ipv4_number(part);

        // 2. If result is failure, IPv4-non-numeric-part validation error, return failure.
        if (!result.has_value()) {
            report_validation_error();
            return OptionalNone {};
        }

        // 3. If result[1] is true, IPv4-non-decimal-part validation error.
        if (result->get<1>())
            report_validation_error();

        // 4. Append result[0] to numbers.
        numbers.append(result->get<0>());
    }

    // 6. If any item in numbers is greater than 255, IPv4-out-of-range-part validation error.
    if (any_of(numbers, [](auto number) { return number > 255; }))
        report_validation_error();

    // 7. If any but the last item in numbers is greater than 255, then return failure.
    if (any_of(numbers.span().slice(0, numbers.size() - 1), [](auto number) { return number > 255; }))
        return OptionalNone {};

    // 8. If the last item in numbers is greater than or equal to 256^(5 − numbers’s size), then return failure.
    if (numbers.last() >= AK::pow<size_t>(256, 5 - numbers.size()))
        return OptionalNone {};

    // 9. Let ipv4 be the last item in numbers.
    auto ipv4 = numbers.last();

    // 10. Remove the last item from numbers.
    numbers.remove(numbers.size() - 1);

    // 11. Let counter be 0.
    size_t counter = 0;

    // 12. For each n of numbers:
    for (auto number : numbers) {
        // 1. Increment ipv4 by n × 256^(3 − counter).
        ipv4 += number * AK::pow<size_t>(256, 3 - counter);

        // 2. Increment counter by 1.
        counter++;
    }

    // 13. Return ipv4.
    return DeprecatedString::number(ipv4);
}

// https://url.spec.whatwg.org/#ends-in-a-number-checker
static bool ends_in_a_number(StringView input)
{
    // 1. Let parts be the result of strictly splitting input on U+002E (.).
    auto parts = input.split_view('.');

    // 2. If the last item in parts is the empty string, then:
    if (parts.last().is_empty()) {
        // 1. If parts’s size is 1, then return false.
        if (parts.size() == 1)
            return false;

        // 2. Remove the last item from parts.
        parts.remove(parts.size() - 1);
    }

    // 3. Let last be the last item in parts.
    auto last = parts.last();

    // 4. If last is non-empty and contains only ASCII digits, then return true.
    if (!last.is_empty() && all_of(last, [](auto character) { return is_ascii_digit(character); }))
        return true;

    // 5. If parsing last as an IPv4 number does not return failure, then return true.
    if (parse_ipv4_number(last).has_value())
        return true;

    // 6. Return false.
    return false;
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

    if (ends_in_a_number(ascii_domain))
        return parse_ipv4_address(ascii_domain);
    return ascii_domain;
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
    if (!url.has_value()) {
        url = URL();

        // NOTE: This removes all leading and trailing C0 control or space characters.
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

    // NOTE: This replaces all tab and newline characters with nothing.
    if (processed_input.contains("\t"sv) || processed_input.contains("\n"sv)) {
        report_validation_error();
        processed_input = processed_input.replace("\t"sv, ""sv, ReplaceMode::All).replace("\n"sv, ""sv, ReplaceMode::All);
    }

    State state = state_override.value_or(State::SchemeStart);
    StringBuilder buffer;
    bool at_sign_seen = false;
    bool inside_brackets = false;
    bool password_token_seen = false;

    Utf8View input(processed_input);
    Utf8CodePointIterator iterator = input.begin();

    auto get_remaining = [&input, &iterator] {
        return input.substring_view(iterator - input.begin() + iterator.underlying_code_point_length_in_bytes()).as_string();
    };

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
        case State::SchemeStart:
            if (is_ascii_alpha(code_point)) {
                buffer.append_as_lowercase(code_point);
                state = State::Scheme;
            } else {
                state = State::NoScheme;
                continue;
            }
            break;
        case State::Scheme:
            if (is_ascii_alphanumeric(code_point) || code_point == '+' || code_point == '-' || code_point == '.') {
                buffer.append_as_lowercase(code_point);
            } else if (code_point == ':') {
                url->m_scheme = buffer.to_deprecated_string();
                buffer.clear();
                if (url->scheme() == "file") {
                    if (!get_remaining().starts_with("//"sv)) {
                        report_validation_error();
                    }
                    state = State::File;
                } else if (url->is_special()) {
                    if (base_url.has_value() && base_url->m_scheme == url->m_scheme)
                        state = State::SpecialRelativeOrAuthority;
                    else
                        state = State::SpecialAuthoritySlashes;
                } else if (get_remaining().starts_with("/"sv)) {
                    state = State::PathOrAuthority;
                    ++iterator;
                } else {
                    url->m_cannot_be_a_base_url = true;
                    url->append_slash();
                    state = State::CannotBeABaseUrlPath;
                }
            } else {
                buffer.clear();
                state = State::NoScheme;
                iterator = input.begin();
                continue;
            }
            break;
        case State::NoScheme:
            if (!base_url.has_value() || (base_url->m_cannot_be_a_base_url && code_point != '#')) {
                report_validation_error();
                return {};
            } else if (base_url->m_cannot_be_a_base_url && code_point == '#') {
                url->m_scheme = base_url->m_scheme;
                url->m_paths = base_url->m_paths;
                url->m_query = base_url->m_query;
                url->m_fragment = "";
                url->m_cannot_be_a_base_url = true;
                state = State::Fragment;
            } else if (base_url->m_scheme != "file") {
                state = State::Relative;
                continue;
            } else {
                state = State::File;
                continue;
            }
            break;
        case State::SpecialRelativeOrAuthority:
            if (code_point == '/' && get_remaining().starts_with("/"sv)) {
                state = State::SpecialAuthorityIgnoreSlashes;
                ++iterator;
            } else {
                report_validation_error();
                state = State::Relative;
                continue;
            }
            break;
        case State::PathOrAuthority:
            if (code_point == '/') {
                state = State::Authority;
            } else {
                state = State::Path;
                continue;
            }
            break;
        case State::Relative:
            url->m_scheme = base_url->m_scheme;
            if (code_point == '/') {
                state = State::RelativeSlash;
            } else if (url->is_special() && code_point == '\\') {
                report_validation_error();
                state = State::RelativeSlash;
            } else {
                url->m_username = base_url->m_username;
                url->m_password = base_url->m_password;
                url->m_host = base_url->m_host;
                url->m_port = base_url->m_port;
                url->m_paths = base_url->m_paths;
                url->m_query = base_url->m_query;

                if (code_point == '?') {
                    url->m_query = "";
                    state = State::Query;
                } else if (code_point == '#') {
                    url->m_fragment = "";
                    state = State::Fragment;
                } else if (code_point != end_of_file) {
                    url->m_query = {};
                    if (url->m_paths.size())
                        url->m_paths.remove(url->m_paths.size() - 1);
                    state = State::Path;
                    continue;
                }
            }
            break;
        case State::RelativeSlash:
            if (url->is_special() && (code_point == '/' || code_point == '\\')) {
                if (code_point == '\\')
                    report_validation_error();
                state = State::SpecialAuthorityIgnoreSlashes;
            } else if (code_point == '/') {
                state = State::Authority;
            } else {
                url->m_username = base_url->m_username;
                url->m_password = base_url->m_password;
                url->m_host = base_url->m_host;
                url->m_port = base_url->m_port;
                state = State::Path;
                continue;
            }
            break;
        case State::SpecialAuthoritySlashes:
            if (code_point == '/' && get_remaining().starts_with("/"sv)) {
                state = State::SpecialAuthorityIgnoreSlashes;
                ++iterator;
            } else {
                report_validation_error();
                state = State::SpecialAuthorityIgnoreSlashes;
                continue;
            }
            break;
        case State::SpecialAuthorityIgnoreSlashes:
            if (code_point != '/' && code_point != '\\') {
                state = State::Authority;
                continue;
            } else {
                report_validation_error();
            }
            break;
        case State::Authority:
            if (code_point == '@') {
                report_validation_error();
                if (at_sign_seen) {
                    auto content = buffer.to_deprecated_string();
                    buffer.clear();
                    buffer.append("%40"sv);
                    buffer.append(content);
                }
                at_sign_seen = true;
                StringBuilder builder;
                for (auto c : Utf8View(builder.string_view())) {
                    if (c == ':' && !password_token_seen) {
                        password_token_seen = true;
                        continue;
                    }
                    builder.clear();
                    if (password_token_seen) {
                        builder.append(url->password());
                        URL::append_percent_encoded_if_necessary(builder, c, URL::PercentEncodeSet::Userinfo);
                        url->m_password = builder.string_view();
                    } else {
                        builder.append(url->username());
                        URL::append_percent_encoded_if_necessary(builder, c, URL::PercentEncodeSet::Userinfo);
                        url->m_username = builder.string_view();
                    }
                }
                buffer.clear();
            } else if (code_point == end_of_file || code_point == '/' || code_point == '?' || code_point == '#' || (url->is_special() && code_point == '\\')) {
                if (at_sign_seen && buffer.is_empty()) {
                    report_validation_error();
                    return {};
                }
                // NOTE: This decreases the iterator by the number of code points in buffer plus one.
                iterator = input.iterator_at_byte_offset(iterator - input.begin() - buffer.length() - 1);
                buffer.clear();
                state = State::Host;
            } else {
                buffer.append_code_point(code_point);
            }
            break;
        case State::Host:
        case State::Hostname:
            if (code_point == ':' && !inside_brackets) {
                if (buffer.is_empty()) {
                    report_validation_error();
                    return {};
                }
                auto host = parse_host(buffer.string_view(), !url->is_special());
                if (!host.has_value())
                    return {};
                url->m_host = host.release_value();
                buffer.clear();
                state = State::Port;
            } else if (code_point == end_of_file || code_point == '/' || code_point == '?' || code_point == '#' || (url->is_special() && code_point == '\\')) {
                if (url->is_special() && buffer.is_empty()) {
                    report_validation_error();
                    return {};
                }
                auto host = parse_host(buffer.string_view(), !url->is_special());
                if (!host.has_value())
                    return {};
                url->m_host = host.value();
                buffer.clear();
                state = State::Port;
                continue;
            } else if (code_point == '[') {
                inside_brackets = true;
            } else if (code_point == ']') {
                inside_brackets = false;
            } else {
                buffer.append_code_point(code_point);
            }
            break;
        case State::Port:
            if (is_ascii_digit(code_point)) {
                buffer.append_code_point(code_point);
            } else if (code_point == end_of_file || code_point == '/' || code_point == '?' || code_point == '#' || (url->is_special() && code_point == '\\')) {
                if (!buffer.is_empty()) {
                    auto port = buffer.string_view().to_uint();
                    if (!port.has_value() || port.value() > 65535) {
                        report_validation_error();
                        return {};
                    }
                    if (port.value() == URL::default_port_for_scheme(url->scheme()))
                        url->m_port = {};
                    else
                        url->m_port = port.value();
                    buffer.clear();
                }
                state = State::PathStart;
                continue;
            } else {
                report_validation_error();
                return {};
            }
            break;
        case State::File:
            url->m_scheme = "file";
            url->m_host = "";
            if (code_point == '/' || code_point == '\\') {
                if (code_point == '\\')
                    report_validation_error();
                state = State::FileSlash;
            } else if (base_url.has_value() && base_url->m_scheme == "file") {
                url->m_host = base_url->m_host;
                url->m_paths = base_url->m_paths;
                url->m_query = base_url->m_query;
                if (code_point == '?') {
                    url->m_query = "";
                    state = State::Query;
                } else if (code_point == '#') {
                    url->m_fragment = "";
                    state = State::Fragment;
                } else if (code_point != end_of_file) {
                    url->m_query = {};
                    auto substring_from_pointer = input.substring_view(iterator - input.begin()).as_string();
                    if (!starts_with_windows_drive_letter(substring_from_pointer)) {
                        if (!url->m_paths.is_empty() && !(url->scheme() == "file" && url->m_paths.size() == 1 && is_normalized_windows_drive_letter(url->m_paths[0])))
                            url->m_paths.remove(url->m_paths.size() - 1);
                    } else {
                        report_validation_error();
                        url->m_paths.clear();
                    }
                    state = State::Path;
                    continue;
                }
            }
            break;
        case State::FileSlash:
            if (code_point == '/' || code_point == '\\') {
                if (code_point == '\\')
                    report_validation_error();
                state = State::FileHost;
            } else if (base_url.has_value() && base_url->m_scheme == "file") {
                url->m_paths = base_url->m_paths;
                url->m_paths.remove(url->m_paths.size() - 1);
                auto substring_from_pointer = input.substring_view(iterator - input.begin()).as_string();
                if (!starts_with_windows_drive_letter(substring_from_pointer) && is_normalized_windows_drive_letter(base_url->m_paths[0]))
                    url->append_path(base_url->m_paths[0], URL::ApplyPercentEncoding::No);
                state = State::Path;
                continue;
            }
            break;
        case State::FileHost:
            if (code_point == end_of_file || code_point == '/' || code_point == '\\' || code_point == '?' || code_point == '#') {
                if (is_windows_drive_letter(buffer.string_view())) {
                    report_validation_error();
                    state = State::Path;
                } else if (buffer.is_empty()) {
                    url->m_host = "";
                    state = State::PathStart;
                } else {
                    auto host = parse_host(buffer.string_view(), true);
                    if (!host.has_value())
                        return {};
                    if (host.value() == "localhost")
                        host = "";
                    url->m_host = host.release_value();
                    buffer.clear();
                    state = State::PathStart;
                }
                continue;
            } else {
                buffer.append_code_point(code_point);
            }
            break;
        case State::PathStart:
            if (url->is_special()) {
                if (code_point == '\\')
                    report_validation_error();
                state = State::Path;
                if (code_point != '/' && code_point != '\\')
                    continue;
            } else if (code_point == '?') {
                url->m_query = "";
                state = State::Query;
            } else if (code_point == '#') {
                url->m_fragment = "";
                state = State::Fragment;
            } else if (code_point != end_of_file) {
                state = State::Path;
                if (code_point != '/')
                    continue;
            }
            break;
        case State::Path:
            if (code_point == end_of_file || code_point == '/' || (url->is_special() && code_point == '\\') || code_point == '?' || code_point == '#') {
                if (url->is_special() && code_point == '\\')
                    report_validation_error();
                if (is_double_dot_path_segment(buffer.string_view())) {
                    if (!url->m_paths.is_empty() && !(url->m_scheme == "file" && url->m_paths.size() == 1 && is_normalized_windows_drive_letter(url->m_paths[0])))
                        url->m_paths.remove(url->m_paths.size() - 1);
                    if (code_point != '/' && !(url->is_special() && code_point == '\\'))
                        url->append_slash();
                } else if (is_single_dot_path_segment(buffer.string_view()) && code_point != '/' && !(url->is_special() && code_point == '\\')) {
                    url->append_slash();
                } else if (!is_single_dot_path_segment(buffer.string_view())) {
                    if (url->m_scheme == "file" && url->m_paths.is_empty() && is_windows_drive_letter(buffer.string_view())) {
                        auto drive_letter = buffer.string_view()[0];
                        buffer.clear();
                        buffer.append(drive_letter);
                        buffer.append(':');
                    }
                    url->append_path(buffer.string_view(), URL::ApplyPercentEncoding::No);
                }
                buffer.clear();
                if (code_point == '?') {
                    url->m_query = "";
                    state = State::Query;
                } else if (code_point == '#') {
                    url->m_fragment = "";
                    state = State::Fragment;
                }
            } else {
                if (!is_url_code_point(code_point) && code_point != '%')
                    report_validation_error();
                // FIXME: If c is U+0025 (%) and remaining does not start with two ASCII hex digits, validation error.
                URL::append_percent_encoded_if_necessary(buffer, code_point, URL::PercentEncodeSet::Path);
            }
            break;
        case State::CannotBeABaseUrlPath:
            // NOTE: This does not follow the spec exactly but rather uses the buffer and only sets the path on EOF.
            // NOTE: Verify that the assumptions required for this simplification are correct.
            VERIFY(url->m_paths.size() == 1 && url->m_paths[0].is_empty());
            if (code_point == '?') {
                url->m_paths[0] = buffer.string_view();
                url->m_query = "";
                state = State::Query;
            } else if (code_point == '#') {
                // NOTE: This needs to be percent decoded since the member variables contain decoded data.
                url->m_paths[0] = buffer.string_view();
                url->m_fragment = "";
                state = State::Fragment;
            } else {
                if (code_point != end_of_file && !is_url_code_point(code_point) && code_point != '%')
                    report_validation_error();
                // FIXME: If c is U+0025 (%) and remaining does not start with two ASCII hex digits, validation error.
                if (code_point != end_of_file) {
                    URL::append_percent_encoded_if_necessary(buffer, code_point, URL::PercentEncodeSet::C0Control);
                } else {
                    url->m_paths[0] = buffer.string_view();
                }
            }
            break;
        case State::Query:
            // https://url.spec.whatwg.org/#query-state
            if (code_point == end_of_file || code_point == '#') {
                VERIFY(url->m_query == "");
                auto query_percent_encode_set = url->is_special() ? URL::PercentEncodeSet::SpecialQuery : URL::PercentEncodeSet::Query;
                url->m_query = percent_encode_after_encoding(buffer.string_view(), query_percent_encode_set);
                buffer.clear();
                if (code_point == '#') {
                    url->m_fragment = "";
                    state = State::Fragment;
                }
            } else if (code_point != end_of_file) {
                if (!is_url_code_point(code_point) && code_point != '%')
                    report_validation_error();
                // FIXME: If c is U+0025 (%) and remaining does not start with two ASCII hex digits, validation error.
                buffer.append_code_point(code_point);
            }
            break;
        case State::Fragment:
            // NOTE: This does not follow the spec exactly but rather uses the buffer and only sets the fragment on EOF.
            if (code_point != end_of_file) {
                if (!is_url_code_point(code_point) && code_point != '%')
                    report_validation_error();
                // FIXME: If c is U+0025 (%) and remaining does not start with two ASCII hex digits, validation error.
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
    return url.release_value();
}

}
