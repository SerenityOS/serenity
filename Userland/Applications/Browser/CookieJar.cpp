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
#include <AK/IPv4Address.h>
#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibWeb/Cookie/ParsedCookie.h>

namespace Browser {

String CookieJar::get_cookie(const URL& url)
{
    purge_expired_cookies();

    auto domain = canonicalize_domain(url);
    if (!domain.has_value())
        return {};

    StringBuilder builder;

    for (const auto& cookie : m_cookies) {
        if (!domain_matches(domain.value(), cookie.value.domain))
            continue;

        if (!builder.is_empty())
            builder.append("; ");
        builder.appendff("{}={}", cookie.value.name, cookie.value.value);
    }

    return builder.build();
}

void CookieJar::set_cookie(const URL& url, const String& cookie_string)
{
    auto domain = canonicalize_domain(url);
    if (!domain.has_value())
        return;

    auto parsed_cookie = Web::Cookie::parse_cookie(cookie_string);
    if (!parsed_cookie.has_value())
        return;

    store_cookie(parsed_cookie.value(), url, move(domain.value()));
    purge_expired_cookies();
}

void CookieJar::dump_cookies() const
{
    static const char* key_color = "\033[34;1m";
    static const char* attribute_color = "\033[33m";
    static const char* no_color = "\033[0m";

    StringBuilder builder;
    builder.appendff("{} cookies stored\n", m_cookies.size());

    for (const auto& cookie : m_cookies) {
        builder.appendff("{}{}{} - ", key_color, cookie.key.name, no_color);
        builder.appendff("{}{}{} - ", key_color, cookie.key.domain, no_color);
        builder.appendff("{}{}{}\n", key_color, cookie.key.path, no_color);

        builder.appendff("\t{}Value{} = {}\n", attribute_color, no_color, cookie.value.value);
        builder.appendff("\t{}CreationTime{} = {}\n", attribute_color, no_color, cookie.value.creation_time.to_string());
        builder.appendff("\t{}LastAccessTime{} = {}\n", attribute_color, no_color, cookie.value.last_access_time.to_string());
        builder.appendff("\t{}ExpiryTime{} = {}\n", attribute_color, no_color, cookie.value.expiry_time.to_string());
        builder.appendff("\t{}Secure{} = {:s}\n", attribute_color, no_color, cookie.value.secure);
        builder.appendff("\t{}HttpOnly{} = {:s}\n", attribute_color, no_color, cookie.value.http_only);
        builder.appendff("\t{}HostOnly{} = {:s}\n", attribute_color, no_color, cookie.value.host_only);
        builder.appendff("\t{}Persistent{} = {:s}\n", attribute_color, no_color, cookie.value.persistent);
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

bool CookieJar::domain_matches(const String& string, const String& domain_string)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.3

    // A string domain-matches a given domain string if at least one of the following conditions hold:

    // The domain string and the string are identical.
    if (string == domain_string)
        return true;

    // All of the following conditions hold:
    //   - The domain string is a suffix of the string.
    //   - The last character of the string that is not included in the domain string is a %x2E (".") character.
    //   - The string is a host name (i.e., not an IP address).
    if (!string.ends_with(domain_string))
        return false;
    if (string[string.length() - domain_string.length() - 1] != '.')
        return false;
    if (AK::IPv4Address::from_string(string).has_value())
        return false;

    return true;
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

void CookieJar::store_cookie(Web::Cookie::ParsedCookie& parsed_cookie, const URL& url, String canonicalized_domain)
{
    // https://tools.ietf.org/html/rfc6265#section-5.3

    // 2. Create a new cookie with name cookie-name, value cookie-value. Set the creation-time and the last-access-time to the current date and time.
    Cookie cookie { move(parsed_cookie.name), move(parsed_cookie.value) };
    cookie.creation_time = Core::DateTime::now();
    cookie.last_access_time = cookie.creation_time;

    if (parsed_cookie.expiry_time_from_max_age_attribute.has_value()) {
        // 3. If the cookie-attribute-list contains an attribute with an attribute-name of "Max-Age": Set the cookie's persistent-flag to true.
        // Set the cookie's expiry-time to attribute-value of the last attribute in the cookie-attribute-list with an attribute-name of "Max-Age".
        cookie.persistent = true;
        cookie.expiry_time = move(parsed_cookie.expiry_time_from_max_age_attribute.value());
    } else if (parsed_cookie.expiry_time_from_expires_attribute.has_value()) {
        // If the cookie-attribute-list contains an attribute with an attribute-name of "Expires": Set the cookie's persistent-flag to true.
        // Set the cookie's expiry-time to attribute-value of the last attribute in the cookie-attribute-list with an attribute-name of "Expires".
        cookie.persistent = true;
        cookie.expiry_time = move(parsed_cookie.expiry_time_from_expires_attribute.value());
    } else {
        // Set the cookie's persistent-flag to false. Set the cookie's expiry-time to the latest representable gddate.
        cookie.persistent = false;
        cookie.expiry_time = Core::DateTime::create(9999, 12, 31, 23, 59, 59);
    }

    // 4. If the cookie-attribute-list contains an attribute with an attribute-name of "Domain":
    if (parsed_cookie.domain.has_value()) {
        // Let the domain-attribute be the attribute-value of the last attribute in the cookie-attribute-list with an attribute-name of "Domain".
        cookie.domain = move(parsed_cookie.domain.value());
    }

    // 5. If the user agent is configured to reject "public suffixes" and the domain-attribute is a public suffix:
    // FIXME: Support rejection of public suffixes. The full list is here: https://publicsuffix.org/list/public_suffix_list.dat

    // 6. If the domain-attribute is non-empty:
    if (!cookie.domain.is_empty()) {
        // If the canonicalized request-host does not domain-match the domain-attribute: Ignore the cookie entirely and abort these steps.
        if (!domain_matches(canonicalized_domain, cookie.domain))
            return;

        // Set the cookie's host-only-flag to false. Set the cookie's domain to the domain-attribute.
        cookie.host_only = false;
    } else {
        // Set the cookie's host-only-flag to true. Set the cookie's domain to the canonicalized request-host.
        cookie.host_only = true;
        cookie.domain = move(canonicalized_domain);
    }

    // 7. If the cookie-attribute-list contains an attribute with an attribute-name of "Path":
    if (parsed_cookie.path.has_value()) {
        // Set the cookie's path to attribute-value of the last attribute in the cookie-attribute-list with an attribute-name of "Path".
        cookie.path = move(parsed_cookie.path.value());
    } else {
        cookie.path = default_path(url);
    }

    // 8. If the cookie-attribute-list contains an attribute with an attribute-name of "Secure", set the cookie's secure-only-flag to true.
    cookie.secure = parsed_cookie.secure_attribute_present;

    // 9. If the cookie-attribute-list contains an attribute with an attribute-name of "HttpOnly", set the cookie's http-only-flag to false.
    cookie.http_only = parsed_cookie.http_only_attribute_present;

    // 10. If the cookie was received from a "non-HTTP" API and the cookie's http-only-flag is set, abort these steps and ignore the cookie entirely.
    // FIXME: Update CookieJar to track where the cookie originated (an HTTP request vs document.cookie).

    // 11. If the cookie store contains a cookie with the same name, domain, and path as the newly created cookie:
    CookieStorageKey key { cookie.name, cookie.domain, cookie.path };

    if (auto old_cookie = m_cookies.find(key); old_cookie != m_cookies.end()) {
        // If the newly created cookie was received from a "non-HTTP" API and the old-cookie's http-only-flag is set, abort these
        // steps and ignore the newly created cookie entirely.
        // FIXME: Similar to step 10, CookieJar needs to track where the cookie originated.

        // Update the creation-time of the newly created cookie to match the creation-time of the old-cookie.
        cookie.creation_time = old_cookie->value.creation_time;

        // Remove the old-cookie from the cookie store.
        m_cookies.remove(old_cookie);
    }

    // 12. Insert the newly created cookie into the cookie store.
    m_cookies.set(key, move(cookie));
}

void CookieJar::purge_expired_cookies()
{
    time_t now = Core::DateTime::now().timestamp();
    Vector<CookieStorageKey> keys_to_evict;

    for (const auto& cookie : m_cookies) {
        if (cookie.value.expiry_time.timestamp() < now)
            keys_to_evict.append(cookie.key);
    }

    for (const auto& key : keys_to_evict)
        m_cookies.remove(key);
}

}
