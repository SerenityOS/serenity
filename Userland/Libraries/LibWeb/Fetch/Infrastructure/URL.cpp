/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Fetch/Infrastructure/URL.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#is-local
bool is_local_url(AK::URL const& url)
{
    // A URL is local if its scheme is a local scheme.
    return any_of(LOCAL_SCHEMES, [&](auto scheme) { return url.scheme() == scheme; });
}

}
