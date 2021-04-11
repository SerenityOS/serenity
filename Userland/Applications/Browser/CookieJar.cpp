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

    // 1. If the set-cookie-string contains a %x3B (";") character:
    if (auto position = cookie_string.find(';'); position.has_value()) {
        // The name-value-pair string consists of the characters up to, but not including, the first %x3B (";"), and the unparsed-
        // attributes consist of the remainder of the set-cookie-string (including the %x3B (";") in question).

        // FIXME: Support optional cookie attributes. For now, ignore those attributes.
        name_value_pair = cookie_string.substring_view(0, position.value());
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
    return Cookie { name, value };
}

}
