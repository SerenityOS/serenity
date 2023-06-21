/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IPv4Address.h>
#include <AK/IPv6Address.h>
#include <AK/URL.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/SecureContexts/AbstractOperations.h>
#include <LibWeb/URL/URL.h>

namespace Web::SecureContexts {

// https://w3c.github.io/webappsec-secure-contexts/#is-origin-trustworthy
Trustworthiness is_origin_potentially_trustworthy(HTML::Origin const& origin)
{
    // 1. If origin is an opaque origin, return "Not Trustworthy".
    if (origin.is_opaque())
        return Trustworthiness::NotTrustworthy;

    // 2. Assert: origin is a tuple origin.

    // 3. If origin’s scheme is either "https" or "wss", return "Potentially Trustworthy".
    // Note: This is meant to be analog to the a priori authenticated URL concept in [MIX].
    if (origin.scheme().is_one_of("https"sv, "wss"sv))
        return Trustworthiness::PotentiallyTrustworthy;

    // 4. If origin’s host matches one of the CIDR notations 127.0.0.0/8 or ::1/128 [RFC4632], return "Potentially Trustworthy".
    if (auto ipv4_address = IPv4Address::from_string(origin.host()); ipv4_address.has_value() && (ipv4_address->to_u32() & 0xff000000) != 0)
        return Trustworthiness::PotentiallyTrustworthy;
    if (auto ipv6_address = IPv6Address::from_string(origin.host()); ipv6_address.has_value() && ipv6_address == IPv6Address::loopback())
        return Trustworthiness::PotentiallyTrustworthy;

    // 5. If the user agent conforms to the name resolution rules in [let-localhost-be-localhost] and one of the following is true:
    // - origin’s host is "localhost" or "localhost."
    // - origin’s host ends with ".localhost" or ".localhost."
    // then return "Potentially Trustworthy".
    // Note: See § 5.2 localhost for details on the requirements here.
    if (origin.host().is_one_of("localhost"sv, "localhost.")
        || origin.host().ends_with(".localhost"sv)
        || origin.host().ends_with(".localhost."sv)) {
        return Trustworthiness::PotentiallyTrustworthy;
    }

    // 6. If origin’s scheme is "file", return "Potentially Trustworthy".
    if (origin.scheme() == "file"sv)
        return Trustworthiness::PotentiallyTrustworthy;

    // 7. If origin’s scheme component is one which the user agent considers to be authenticated, return "Potentially Trustworthy".
    // Note: See § 7.1 Packaged Applications for detail here.

    // 8. If origin has been configured as a trustworthy origin, return "Potentially Trustworthy".
    // Note: See § 7.2 Development Environments for detail here.

    // 9. Return "Not Trustworthy".
    return Trustworthiness::NotTrustworthy;
}

// https://w3c.github.io/webappsec-secure-contexts/#is-url-trustworthy
Trustworthiness is_url_potentially_trustworthy(AK::URL const& url)
{
    // 1. If url is "about:blank" or "about:srcdoc", return "Potentially Trustworthy".
    if (url == "about:blank"sv || url == "about:srcdoc"sv)
        return Trustworthiness::PotentiallyTrustworthy;

    // 2. If url’s scheme is "data", return "Potentially Trustworthy".
    if (url.scheme() == "data"sv)
        return Trustworthiness::PotentiallyTrustworthy;

    // 3. Return the result of executing § 3.1 Is origin potentially trustworthy? on url’s origin.
    return is_origin_potentially_trustworthy(URL::url_origin(url));
}

}
