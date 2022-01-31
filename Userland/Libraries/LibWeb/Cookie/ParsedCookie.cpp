/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ParsedCookie.h"
#include <AK/Function.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <ctype.h>

namespace Web::Cookie {

static constexpr size_t s_max_cookie_size = 4096;

static void parse_attributes(ParsedCookie& parsed_cookie, StringView unparsed_attributes);
static void process_attribute(ParsedCookie& parsed_cookie, StringView attribute_name, StringView attribute_value);
static void on_expires_attribute(ParsedCookie& parsed_cookie, StringView attribute_value);
static void on_max_age_attribute(ParsedCookie& parsed_cookie, StringView attribute_value);
static void on_domain_attribute(ParsedCookie& parsed_cookie, StringView attribute_value);
static void on_path_attribute(ParsedCookie& parsed_cookie, StringView attribute_value);
static void on_secure_attribute(ParsedCookie& parsed_cookie);
static void on_http_only_attribute(ParsedCookie& parsed_cookie);
static Optional<Core::DateTime> parse_date_time(StringView date_string);

Optional<ParsedCookie> parse_cookie(const String& cookie_string)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2

    if (cookie_string.length() > s_max_cookie_size)
        return {};

    StringView name_value_pair;
    StringView unparsed_attributes;

    // 1. If the set-cookie-string contains a %x3B (";") character:
    if (auto position = cookie_string.find(';'); position.has_value()) {
        // The name-value-pair string consists of the characters up to, but not including, the first %x3B (";"), and the unparsed-
        // attributes consist of the remainder of the set-cookie-string (including the %x3B (";") in question).
        name_value_pair = cookie_string.substring_view(0, position.value());
        unparsed_attributes = cookie_string.substring_view(position.value());
    } else {
        // The name-value-pair string consists of all the characters contained in the set-cookie-string, and the unparsed-
        // attributes is the empty string.
        name_value_pair = cookie_string;
    }

    StringView name;
    StringView value;

    if (auto position = name_value_pair.find('='); position.has_value()) {
        // 3. The (possibly empty) name string consists of the characters up to, but not including, the first %x3D ("=") character, and the
        //    (possibly empty) value string consists of the characters after the first %x3D ("=") character.
        name = name_value_pair.substring_view(0, position.value());

        if (position.value() < name_value_pair.length() - 1)
            value = name_value_pair.substring_view(position.value() + 1);
    } else {
        // 2. If the name-value-pair string lacks a %x3D ("=") character, ignore the set-cookie-string entirely.
        return {};
    }

    // 4. Remove any leading or trailing WSP characters from the name string and the value string.
    name = name.trim_whitespace();
    value = value.trim_whitespace();

    // 5. If the name string is empty, ignore the set-cookie-string entirely.
    if (name.is_empty())
        return {};

    // 6. The cookie-name is the name string, and the cookie-value is the value string.
    ParsedCookie parsed_cookie { name, value };

    parse_attributes(parsed_cookie, unparsed_attributes);
    return parsed_cookie;
}

void parse_attributes(ParsedCookie& parsed_cookie, StringView unparsed_attributes)
{
    // 1. If the unparsed-attributes string is empty, skip the rest of these steps.
    if (unparsed_attributes.is_empty())
        return;

    // 2. Discard the first character of the unparsed-attributes (which will be a %x3B (";") character).
    unparsed_attributes = unparsed_attributes.substring_view(1);

    StringView cookie_av;

    // 3. If the remaining unparsed-attributes contains a %x3B (";") character:
    if (auto position = unparsed_attributes.find(';'); position.has_value()) {
        // Consume the characters of the unparsed-attributes up to, but not including, the first %x3B (";") character.
        cookie_av = unparsed_attributes.substring_view(0, position.value());
        unparsed_attributes = unparsed_attributes.substring_view(position.value());
    } else {
        // Consume the remainder of the unparsed-attributes.
        cookie_av = unparsed_attributes;
        unparsed_attributes = {};
    }

    StringView attribute_name;
    StringView attribute_value;

    // 4. If the cookie-av string contains a %x3D ("=") character:
    if (auto position = cookie_av.find('='); position.has_value()) {
        // The (possibly empty) attribute-name string consists of the characters up to, but not including, the first %x3D ("=")
        // character, and the (possibly empty) attribute-value string consists of the characters after the first %x3D ("=") character.
        attribute_name = cookie_av.substring_view(0, position.value());

        if (position.value() < cookie_av.length() - 1)
            attribute_value = cookie_av.substring_view(position.value() + 1);
    } else {
        // The attribute-name string consists of the entire cookie-av string, and the attribute-value string is empty.
        attribute_name = cookie_av;
    }

    // 5. Remove any leading or trailing WSP characters from the attribute-name string and the attribute-value string.
    attribute_name = attribute_name.trim_whitespace();
    attribute_value = attribute_value.trim_whitespace();

    // 6. Process the attribute-name and attribute-value according to the requirements in the following subsections.
    //    (Notice that attributes with unrecognized attribute-names are ignored.)
    process_attribute(parsed_cookie, attribute_name, attribute_value);

    // 7. Return to Step 1 of this algorithm.
    parse_attributes(parsed_cookie, unparsed_attributes);
}

void process_attribute(ParsedCookie& parsed_cookie, StringView attribute_name, StringView attribute_value)
{
    if (attribute_name.equals_ignoring_case("Expires")) {
        on_expires_attribute(parsed_cookie, attribute_value);
    } else if (attribute_name.equals_ignoring_case("Max-Age")) {
        on_max_age_attribute(parsed_cookie, attribute_value);
    } else if (attribute_name.equals_ignoring_case("Domain")) {
        on_domain_attribute(parsed_cookie, attribute_value);
    } else if (attribute_name.equals_ignoring_case("Path")) {
        on_path_attribute(parsed_cookie, attribute_value);
    } else if (attribute_name.equals_ignoring_case("Secure")) {
        on_secure_attribute(parsed_cookie);
    } else if (attribute_name.equals_ignoring_case("HttpOnly")) {
        on_http_only_attribute(parsed_cookie);
    }
}

void on_expires_attribute(ParsedCookie& parsed_cookie, StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.1
    if (auto expiry_time = parse_date_time(attribute_value); expiry_time.has_value())
        parsed_cookie.expiry_time_from_expires_attribute = move(*expiry_time);
}

void on_max_age_attribute(ParsedCookie& parsed_cookie, StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.2

    // If the first character of the attribute-value is not a DIGIT or a "-" character, ignore the cookie-av.
    if (attribute_value.is_empty() || (!isdigit(attribute_value[0]) && (attribute_value[0] != '-')))
        return;

    // Let delta-seconds be the attribute-value converted to an integer.
    if (auto delta_seconds = attribute_value.to_int(); delta_seconds.has_value()) {
        if (*delta_seconds <= 0) {
            // If delta-seconds is less than or equal to zero (0), let expiry-time be the earliest representable date and time.
            parsed_cookie.expiry_time_from_max_age_attribute = Core::DateTime::from_timestamp(0);
        } else {
            // Otherwise, let the expiry-time be the current date and time plus delta-seconds seconds.
            time_t now = Core::DateTime::now().timestamp();
            parsed_cookie.expiry_time_from_max_age_attribute = Core::DateTime::from_timestamp(now + *delta_seconds);
        }
    }
}

void on_domain_attribute(ParsedCookie& parsed_cookie, StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.3

    // If the attribute-value is empty, the behavior is undefined. However, the user agent SHOULD ignore the cookie-av entirely.
    if (attribute_value.is_empty())
        return;

    StringView cookie_domain;

    // If the first character of the attribute-value string is %x2E ("."):
    if (attribute_value[0] == '.') {
        // Let cookie-domain be the attribute-value without the leading %x2E (".") character.
        cookie_domain = attribute_value.substring_view(1);
    } else {
        // Let cookie-domain be the entire attribute-value.
        cookie_domain = attribute_value;
    }

    // Convert the cookie-domain to lower case.
    parsed_cookie.domain = String(cookie_domain).to_lowercase();
}

void on_path_attribute(ParsedCookie& parsed_cookie, StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.4

    // If the attribute-value is empty or if the first character of the attribute-value is not %x2F ("/"):
    if (attribute_value.is_empty() || attribute_value[0] != '/')
        // Let cookie-path be the default-path.
        return;

    // Let cookie-path be the attribute-value
    parsed_cookie.path = attribute_value;
}

void on_secure_attribute(ParsedCookie& parsed_cookie)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.5
    parsed_cookie.secure_attribute_present = true;
}

void on_http_only_attribute(ParsedCookie& parsed_cookie)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.6
    parsed_cookie.http_only_attribute_present = true;
}

Optional<Core::DateTime> parse_date_time(StringView date_string)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.1
    unsigned hour = 0;
    unsigned minute = 0;
    unsigned second = 0;
    unsigned day_of_month = 0;
    unsigned month = 0;
    unsigned year = 0;

    auto to_uint = [](StringView token, unsigned& result) {
        if (!all_of(token, isdigit))
            return false;

        if (auto converted = token.to_uint(); converted.has_value()) {
            result = *converted;
            return true;
        }

        return false;
    };

    auto parse_time = [&](StringView token) {
        Vector<StringView> parts = token.split_view(':');
        if (parts.size() != 3)
            return false;

        for (const auto& part : parts) {
            if (part.is_empty() || part.length() > 2)
                return false;
        }

        return to_uint(parts[0], hour) && to_uint(parts[1], minute) && to_uint(parts[2], second);
    };

    auto parse_day_of_month = [&](StringView token) {
        if (token.is_empty() || token.length() > 2)
            return false;
        return to_uint(token, day_of_month);
    };

    auto parse_month = [&](StringView token) {
        static const char* months[] { "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec" };

        for (unsigned i = 0; i < 12; ++i) {
            if (token.equals_ignoring_case(months[i])) {
                month = i + 1;
                return true;
            }
        }

        return false;
    };

    auto parse_year = [&](StringView token) {
        if (token.length() != 2 && token.length() != 4)
            return false;
        return to_uint(token, year);
    };

    Function<bool(char)> is_delimiter = [](char ch) {
        return ch == 0x09 || (ch >= 0x20 && ch <= 0x2f) || (ch >= 0x3b && ch <= 0x40) || (ch >= 0x5b && ch <= 0x60) || (ch >= 0x7b && ch <= 0x7e);
    };

    // 1. Using the grammar below, divide the cookie-date into date-tokens.
    Vector<StringView> date_tokens = date_string.split_view_if(is_delimiter);

    // 2. Process each date-token sequentially in the order the date-tokens appear in the cookie-date.
    bool found_time = false;
    bool found_day_of_month = false;
    bool found_month = false;
    bool found_year = false;

    for (const auto& date_token : date_tokens) {
        if (!found_time && parse_time(date_token)) {
            found_time = true;
        } else if (!found_day_of_month && parse_day_of_month(date_token)) {
            found_day_of_month = true;
        } else if (!found_month && parse_month(date_token)) {
            found_month = true;
        } else if (!found_year && parse_year(date_token)) {
            found_year = true;
        }
    }

    // 3. If the year-value is greater than or equal to 70 and less than or equal to 99, increment the year-value by 1900.
    if (year >= 70 && year <= 99)
        year += 1900;

    // 4. If the year-value is greater than or equal to 0 and less than or equal to 69, increment the year-value by 2000.
    if (year <= 69)
        year += 2000;

    // 5. Abort these steps and fail to parse the cookie-date if:
    if (!found_time || !found_day_of_month || !found_month || !found_year)
        return {};
    if (day_of_month < 1 || day_of_month > 31)
        return {};
    if (year < 1601)
        return {};
    if (hour > 23)
        return {};
    if (minute > 59)
        return {};
    if (second > 59)
        return {};

    // FIXME: Fail on dates that do not exist.
    return Core::DateTime::create(year, month, day_of_month, hour, minute, second);
}

}

bool IPC::encode(IPC::Encoder& encoder, const Web::Cookie::ParsedCookie& cookie)
{
    encoder << cookie.name;
    encoder << cookie.value;
    encoder << cookie.expiry_time_from_expires_attribute;
    encoder << cookie.expiry_time_from_max_age_attribute;
    encoder << cookie.domain;
    encoder << cookie.path;
    encoder << cookie.secure_attribute_present;
    encoder << cookie.http_only_attribute_present;

    return true;
}

ErrorOr<void> IPC::decode(IPC::Decoder& decoder, Web::Cookie::ParsedCookie& cookie)
{
    TRY(decoder.decode(cookie.name));
    TRY(decoder.decode(cookie.value));
    TRY(decoder.decode(cookie.expiry_time_from_expires_attribute));
    TRY(decoder.decode(cookie.expiry_time_from_max_age_attribute));
    TRY(decoder.decode(cookie.domain));
    TRY(decoder.decode(cookie.path));
    TRY(decoder.decode(cookie.secure_attribute_present));
    TRY(decoder.decode(cookie.http_only_attribute_present));
    return {};
}
