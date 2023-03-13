/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/Fetch/Infrastructure/URL.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/ReferrerPolicy/AbstractOperations.h>
#include <LibWeb/ReferrerPolicy/ReferrerPolicy.h>
#include <LibWeb/SecureContexts/AbstractOperations.h>
#include <LibWeb/URL/URL.h>

namespace Web::ReferrerPolicy {

// https://w3c.github.io/webappsec-referrer-policy/#determine-requests-referrer
Optional<AK::URL> determine_requests_referrer(Fetch::Infrastructure::Request const& request)
{
    // 1. Let policy be request’s referrer policy.
    auto const& policy = request.referrer_policy();

    // 2. Let environment be request’s client.
    auto environment = request.client();

    // 3. Switch on request’s referrer:
    auto referrer_source = request.referrer().visit(
        // "client"
        [&](Fetch::Infrastructure::Request::Referrer referrer) -> Optional<AK::URL> {
            // Note: If request’s referrer is "no-referrer", Fetch will not call into this algorithm.
            VERIFY(referrer == Fetch::Infrastructure::Request::Referrer::Client);

            // FIXME: Add a const global_object() getter to ESO
            auto& global_object = const_cast<HTML::EnvironmentSettingsObject&>(*environment).global_object();

            // 1. If environment’s global object is a Window object, then
            if (is<HTML::Window>(global_object)) {
                // 1. Let document be the associated Document of environment’s global object.
                auto const& document = static_cast<HTML::Window const&>(global_object).associated_document();

                // 2. If document’s origin is an opaque origin, return no referrer.
                if (document.origin().is_opaque())
                    return {};

                // FIXME: 3. While document is an iframe srcdoc document, let document be document’s browsing context’s
                //           browsing context container’s node document.

                // 4. Let referrerSource be document’s URL.
                return document.url();
            }
            // 2. Otherwise, let referrerSource be environment’s creation URL.
            else {
                return environment->creation_url;
            }
        },
        // a URL
        [&](AK::URL const& url) -> Optional<AK::URL> {
            // Let referrerSource be request’s referrer.
            return url;
        });
    // NOTE: This only happens in step 1.2. of the "client" case above.
    if (!referrer_source.has_value())
        return {};

    // 4. Let request’s referrerURL be the result of stripping referrerSource for use as a referrer.
    auto referrer_url = strip_url_for_use_as_referrer(referrer_source);

    // 5. Let referrerOrigin be the result of stripping referrerSource for use as a referrer, with the origin-only flag
    //    set to true.
    auto referrer_origin = strip_url_for_use_as_referrer(referrer_source, OriginOnly::Yes);

    // 6. If the result of serializing referrerURL is a string whose length is greater than 4096, set referrerURL to
    //    referrerOrigin.
    if (referrer_url.has_value() && referrer_url.value().serialize().length() > 4096)
        referrer_url = referrer_origin;

    // 7. The user agent MAY alter referrerURL or referrerOrigin at this point to enforce arbitrary policy
    //    considerations in the interests of minimizing data leakage. For example, the user agent could strip the URL
    //    down to an origin, modify its host, replace it with an empty string, etc.

    // 8. Execute the statements corresponding to the value of policy:
    // Note: If request’s referrer policy is the empty string, Fetch will not call into this algorithm.
    VERIFY(policy.has_value());
    switch (*policy) {
    // "no-referrer"
    case ReferrerPolicy::NoReferrer:
        // Return no referrer
        return {};
    // "origin"
    case ReferrerPolicy::Origin:
        // Return referrerOrigin
        return referrer_origin;
    // "unsafe-url"
    case ReferrerPolicy::UnsafeURL:
        // Return referrerURL.
        return referrer_url;
    // "strict-origin"
    case ReferrerPolicy::StrictOrigin:
        // 1. If referrerURL is a potentially trustworthy URL and request’s current URL is not a potentially
        //    trustworthy URL, then return no referrer.
        if (referrer_url.has_value()
            && SecureContexts::is_url_potentially_trustworthy(*referrer_url) == SecureContexts::Trustworthiness::PotentiallyTrustworthy
            && SecureContexts::is_url_potentially_trustworthy(request.current_url()) == SecureContexts::Trustworthiness::NotTrustworthy) {
            return {};
        }

        // 2. Return referrerOrigin.
        return referrer_origin;
    // "strict-origin-when-cross-origin"
    case ReferrerPolicy::StrictOriginWhenCrossOrigin:
        // 1. If the origin of referrerURL and the origin of request’s current URL are the same, then return
        //    referrerURL.
        if (referrer_url.has_value() && URL::url_origin(*referrer_url).is_same_origin(URL::url_origin(request.current_url())))
            return referrer_url;

        // 2. If referrerURL is a potentially trustworthy URL and request’s current URL is not a potentially
        //    trustworthy URL, then return no referrer.
        if (referrer_url.has_value()
            && SecureContexts::is_url_potentially_trustworthy(*referrer_url) == SecureContexts::Trustworthiness::PotentiallyTrustworthy
            && SecureContexts::is_url_potentially_trustworthy(request.current_url()) == SecureContexts::Trustworthiness::NotTrustworthy) {
            return {};
        }

        // 3. Return referrerOrigin.
        return referrer_origin;
    // "same-origin"
    case ReferrerPolicy::SameOrigin:
        // 1. If the origin of referrerURL and the origin of request’s current URL are the same, then return
        //    referrerURL.
        if (referrer_url.has_value()
            && URL::url_origin(*referrer_url).is_same_origin(URL::url_origin(request.current_url()))) {
            return referrer_url;
        }

        // 2. Return no referrer.
        return {};
    // "origin-when-cross-origin"
    case ReferrerPolicy::OriginWhenCrossOrigin:
        // 1. If the origin of referrerURL and the origin of request’s current URL are the same, then return
        //    referrerURL.
        if (referrer_url.has_value()
            && URL::url_origin(*referrer_url).is_same_origin(URL::url_origin(request.current_url()))) {
            return referrer_url;
        }

        // 2. Return referrerOrigin.
        return referrer_origin;
    // "no-referrer-when-downgrade"
    case ReferrerPolicy::NoReferrerWhenDowngrade:
        // 1. If referrerURL is a potentially trustworthy URL and request’s current URL is not a potentially
        //    trustworthy URL, then return no referrer.
        if (referrer_url.has_value()
            && SecureContexts::is_url_potentially_trustworthy(*referrer_url) == SecureContexts::Trustworthiness::PotentiallyTrustworthy
            && SecureContexts::is_url_potentially_trustworthy(request.current_url()) == SecureContexts::Trustworthiness::NotTrustworthy) {
            return {};
        }

        // 2. Return referrerURL.
        return referrer_url;
    default:
        VERIFY_NOT_REACHED();
    }
}

Optional<AK::URL> strip_url_for_use_as_referrer(Optional<AK::URL> url, OriginOnly origin_only)
{
    // 1. If url is null, return no referrer.
    if (!url.has_value())
        return {};

    // 2. If url’s scheme is a local scheme, then return no referrer.
    if (Fetch::Infrastructure::LOCAL_SCHEMES.span().contains_slow(url->scheme()))
        return {};

    // 3. Set url’s username to the empty string.
    url->set_username(""sv);

    // 4. Set url’s password to the empty string.
    url->set_password(""sv);

    // 5. Set url’s fragment to null.
    url->set_fragment({});

    // 6. If the origin-only flag is true, then:
    if (origin_only == OriginOnly::Yes) {
        // 1. Set url’s path to « the empty string ».
        url->set_paths({ ""sv });

        // 2. Set url’s query to null.
        url->set_query({});
    }

    // 7. Return url.
    return url;
}

}
