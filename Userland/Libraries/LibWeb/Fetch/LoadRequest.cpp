/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LoadRequest.h"
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Page/Page.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#forbidden-method
bool is_forbidden_method(const String& method)
{
    auto lowercase_method = method.to_lowercase();
    return lowercase_method.is_one_of("connect", "trace", "track");
}

// https://fetch.spec.whatwg.org/#concept-method-normalize
String normalize_method(const String& method)
{
    auto lowercase_method = method.to_lowercase();
    if (lowercase_method.is_one_of("delete", "get", "head", "options", "post", "put"))
        return method.to_uppercase();
    return method;
}

LoadRequest LoadRequest::create_for_url_on_page(const URL& url, Page* page)
{
    LoadRequest request;
    request.set_url(url);

    if (page) {
        String cookie = page->client().page_did_request_cookie(url, Cookie::Source::Http);
        if (!cookie.is_empty())
            request.set_header("Cookie", cookie);
    }

    return request;
}

}
