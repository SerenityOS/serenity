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

// https://fetch.spec.whatwg.org/#cors-safelisted-method
bool is_cors_safelisted_method(const String& method)
{
    return method.is_one_of("GET", "HEAD", "POST");
}

// https://datatracker.ietf.org/doc/html/rfc7231#section-4.2.1
// A "safe" method are essentially read-only methods, such as "GET", "HEAD", etc.
bool is_safe_method(const String& method)
{
    // "Of the request methods defined by this specification, the GET, HEAD, OPTIONS, and TRACE methods are defined to be safe."
    return method.is_one_of("GET", "HEAD", "OPTIONS", "TRACE");
}

LoadRequest LoadRequest::create_for_url_on_page(const URL& url, Page* page)
{
    LoadRequest request(url);

    if (page) {
        String cookie = page->client().page_did_request_cookie(url, Cookie::Source::Http);
        if (!cookie.is_empty())
            request.set_header("Cookie", cookie);
    }

    return request;
}

// https://fetch.spec.whatwg.org/#serializing-a-request-origin
String LoadRequest::serialize_origin() const
{
    if (m_tainted_origin)
        return "null";

    return m_origin.get<Origin>().serialize();
}

}
