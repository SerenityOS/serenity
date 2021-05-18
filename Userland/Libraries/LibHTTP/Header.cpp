/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibHTTP/Header.h>

namespace HTTP {

// https://fetch.spec.whatwg.org/#forbidden-header-name
bool is_forbidden_header_name(String const& header_name)
{
    if (header_name.starts_with("Proxy-", CaseSensitivity::CaseInsensitive) || header_name.starts_with("Sec-", CaseSensitivity::CaseInsensitive))
        return true;

    auto lowercase_header_name = header_name.to_lowercase();
    return lowercase_header_name.is_one_of("accept-charset", "accept-encoding", "access-control-request-headers", "access-control-request-method", "connection", "content-length", "cookie", "cookie2", "date", "dnt", "expect", "host", "keep-alive", "origin", "referer", "te", "trailer", "transfer-encoding", "upgrade", "via");
}

// https://fetch.spec.whatwg.org/#concept-header-value-normalize
String normalize_header_value(String const& header_value)
{
    // FIXME: This is not the right trim, it should only be HTML whitespace bytes.
    return header_value.trim_whitespace();
}

}
