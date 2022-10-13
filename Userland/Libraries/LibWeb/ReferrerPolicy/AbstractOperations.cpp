/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Fetch/Infrastructure/URL.h>
#include <LibWeb/ReferrerPolicy/AbstractOperations.h>
#include <LibWeb/URL/URL.h>

namespace Web::ReferrerPolicy {

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
