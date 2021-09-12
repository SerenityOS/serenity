/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LoadRequest.h"
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Page/Page.h>

namespace Web {

LoadRequest LoadRequest::create_for_url_on_page(const AK::URL& url, Page* page)
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
