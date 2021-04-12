/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "CookieJar.h"
#include <AK/AllOf.h>
#include <AK/NumericLimits.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <ctype.h>

namespace Browser {

String CookieJar::get_cookie(const URL& url) const
{
    auto domain = canonicalize_domain(url);
    if (!domain.has_value())
        return {};

    StringBuilder builder;

    if (auto it = m_cookies.find(*domain); it != m_cookies.end()) {
        for (const auto& cookie : it->value) {
            if (!builder.is_empty())
                builder.append("; ");
            builder.appendff("{}={}", cookie.name, cookie.value);
        }
    }

    return builder.build();
}

void CookieJar::set_cookie(const URL& url, const String& cookie_string)
{
    auto domain = canonicalize_domain(url);
    if (!domain.has_value())
        return;

    auto new_cookie = parse_cookie(cookie_string, *domain, default_path(url));
    if (!new_cookie.has_value())
        return;

    auto it = m_cookies.find(*domain);
    if (it == m_cookies.end()) {
        m_cookies.set(*domain, { move(*new_cookie) });
        return;
    }

    for (auto& cookie : it->value) {
        if (cookie.name == new_cookie->name) {
            cookie = move(*new_cookie);
            return;
        }
    }

    it->value.append(move(*new_cookie));
}

void CookieJar::dump_cookies() const
{
    static const char* url_color = "\033[34;1m";
    static const char* cookie_color = "\033[31m";
    static const char* attribute_color = "\033[33m";
    static const char* no_color = "\033[0m";

    StringBuilder builder;
    builder.appendff("{} URLs with cookies\n", m_cookies.size());

    for (const auto& url_and_cookies : m_cookies) {
        builder.appendff("{}Cookies for:{} {}\n", url_color, no_color, url_and_cookies.key.is_empty() ? "file://" : url_and_cookies.key);

        for (const auto& cookie : url_and_cookies.value) {
            builder.appendff("\t{}{}{} = {}{}{}\n", cookie_color, cookie.name, no_color, cookie_color, cookie.value, no_color);
            builder.appendff("\t\t{}Expiry{} = {}\n", attribute_color, no_color, cookie.expiry_time.to_string());
            builder.appendff("\t\t{}Domain{} = {}\n", attribute_color, no_color, cookie.domain);
            builder.appendff("\t\t{}Path{} = {}\n", attribute_color, no_color, cookie.path);
            builder.appendff("\t\t{}Secure{} = {:s}\n", attribute_color, no_color, cookie.secure);
            builder.appendff("\t\t{}HttpOnly{} = {:s}\n", attribute_color, no_color, cookie.http_only);
        }
    }

    dbgln("{}", builder.build());
}

Optional<String> CookieJar::canonicalize_domain(const URL& url)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.2
    if (!url.is_valid())
        return {};

    // FIXME: Implement RFC 5890 to "Convert each label that is not a Non-Reserved LDH (NR-LDH) label to an A-label".
    return url.host().to_lowercase();
}

String CookieJar::default_path(const URL& url)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.4

    // 1. Let uri-path be the path portion of the request-uri if such a portion exists (and empty otherwise).
    String uri_path = url.path();

    // 2. If the uri-path is empty or if the first character of the uri-path is not a %x2F ("/") character, output %x2F ("/") and skip the remaining steps.
    if (uri_path.is_empty() || (uri_path[0] != '/'))
        return "/";

    StringView uri_path_view = uri_path;
    std::size_t last_separator = uri_path_view.find_last_of('/').value();

    // 3. If the uri-path contains no more than one %x2F ("/") character, output %x2F ("/") and skip the remaining step.
    if (last_separator == 0)
        return "/";

    // 4. Output the characters of the uri-path from the first character up to, but not including, the right-most %x2F ("/").
    return uri_path.substring(0, last_separator);
}

Optional<Cookie> CookieJar::parse_cookie(const String& cookie_string, String default_domain, String default_path)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2
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
    Cookie cookie { name, value };

    cookie.expiry_time = Core::DateTime::create(AK::NumericLimits<unsigned>::max());
    cookie.domain = move(default_domain);
    cookie.path = move(default_path);

    parse_attributes(cookie, unparsed_attributes);
    return cookie;
}

void CookieJar::parse_attributes(Cookie& cookie, StringView unparsed_attributes)
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
    process_attribute(cookie, attribute_name, attribute_value);

    // 7. Return to Step 1 of this algorithm.
    parse_attributes(cookie, unparsed_attributes);
}

void CookieJar::process_attribute(Cookie& cookie, StringView attribute_name, StringView attribute_value)
{
    if (attribute_name.equals_ignoring_case("Expires")) {
        on_expires_attribute(cookie, attribute_value);
    } else if (attribute_name.equals_ignoring_case("Max-Age")) {
        on_max_age_attribute(cookie, attribute_value);
    } else if (attribute_name.equals_ignoring_case("Domain")) {
        on_domain_attribute(cookie, attribute_value);
    } else if (attribute_name.equals_ignoring_case("Path")) {
        on_path_attribute(cookie, attribute_value);
    } else if (attribute_name.equals_ignoring_case("Secure")) {
        on_secure_attribute(cookie);
    } else if (attribute_name.equals_ignoring_case("HttpOnly")) {
        on_http_only_attribute(cookie);
    }
}

void CookieJar::on_expires_attribute(Cookie& cookie, StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.1
    if (auto expiry_time = parse_date_time(attribute_value); expiry_time.has_value())
        cookie.expiry_time = *expiry_time;
}

void CookieJar::on_max_age_attribute(Cookie& cookie, StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.2

    // If the first character of the attribute-value is not a DIGIT or a "-" character, ignore the cookie-av.
    if (attribute_value.is_empty() || (!isdigit(attribute_value[0]) && (attribute_value[0] != '-')))
        return;

    // Let delta-seconds be the attribute-value converted to an integer.
    if (auto delta_seconds = attribute_value.to_int(); delta_seconds.has_value()) {
        Core::DateTime expiry_time;

        if (*delta_seconds <= 0) {
            // If delta-seconds is less than or equal to zero (0), let expiry-time be the earliest representable date and time.
            cookie.expiry_time = Core::DateTime::from_timestamp(0);
        } else {
            // Otherwise, let the expiry-time be the current date and time plus delta-seconds seconds.
            time_t now = Core::DateTime::now().timestamp();
            cookie.expiry_time = Core::DateTime::from_timestamp(now + *delta_seconds);
        }
    }
}

void CookieJar::on_domain_attribute(Cookie& cookie, StringView attribute_value)
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
    cookie.domain = String(cookie_domain).to_lowercase();
}

void CookieJar::on_path_attribute(Cookie& cookie, StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.4

    // If the attribute-value is empty or if the first character of the attribute-value is not %x2F ("/"):
    if (attribute_value.is_empty() || attribute_value[0] != '/')
        // Let cookie-path be the default-path.
        return;

    // Let cookie-path be the attribute-value
    cookie.path = attribute_value;
}

void CookieJar::on_secure_attribute(Cookie& cookie)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.5
    cookie.secure = true;
}

void CookieJar::on_http_only_attribute(Cookie& cookie)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.6
    cookie.http_only = true;
}

Optional<Core::DateTime> CookieJar::parse_date_time(StringView date_string)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.1
    unsigned hour = 0;
    unsigned minute = 0;
    unsigned second = 0;
    unsigned day_of_month = 0;
    unsigned month = 0;
    unsigned year = 0;

    auto to_uint = [](StringView token, unsigned& result) {
        if (!all_of(token.begin(), token.end(), isdigit))
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

    auto is_delimeter = [](char ch) {
        return ch == 0x09 || (ch >= 0x20 && ch <= 0x2f) || (ch >= 0x3b && ch <= 0x40) || (ch >= 0x5b && ch <= 0x60) || (ch >= 0x7b && ch <= 0x7e);
    };

    // 1. Using the grammar below, divide the cookie-date into date-tokens.
    Vector<StringView> date_tokens = date_string.split_view_if(is_delimeter);

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
