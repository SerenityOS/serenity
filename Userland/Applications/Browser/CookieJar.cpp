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
#include <AK/URL.h>

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

    auto new_cookie = parse_cookie(cookie_string);
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

Optional<String> CookieJar::canonicalize_domain(const URL& url)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.2
    if (!url.is_valid())
        return {};

    // FIXME: Implement RFC 5890 to "Convert each label that is not a Non-Reserved LDH (NR-LDH) label to an A-label".
    return url.host().to_lowercase();
}

Optional<Cookie> CookieJar::parse_cookie(const String& cookie_string)
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
        on_secure_attribute(cookie, attribute_value);
    } else if (attribute_name.equals_ignoring_case("HttpOnly")) {
        on_http_only_attribute(cookie, attribute_value);
    }
}

void CookieJar::on_expires_attribute([[maybe_unused]] Cookie& cookie, [[maybe_unused]] StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.1
}

void CookieJar::on_max_age_attribute([[maybe_unused]] Cookie& cookie, [[maybe_unused]] StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.2
}

void CookieJar::on_domain_attribute([[maybe_unused]] Cookie& cookie, [[maybe_unused]] StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.3
}

void CookieJar::on_path_attribute([[maybe_unused]] Cookie& cookie, [[maybe_unused]] StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.4
}

void CookieJar::on_secure_attribute([[maybe_unused]] Cookie& cookie, [[maybe_unused]] StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.5
}

void CookieJar::on_http_only_attribute([[maybe_unused]] Cookie& cookie, [[maybe_unused]] StringView attribute_value)
{
    // https://tools.ietf.org/html/rfc6265#section-5.2.6
}

}
