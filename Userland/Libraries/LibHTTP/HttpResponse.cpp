/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibHTTP/HttpResponse.h>

namespace HTTP {

HttpResponse::HttpResponse(int code, HashMap<String, String, CaseInsensitiveStringTraits>&& headers)
    : m_code(code)
    , m_headers(move(headers))
{
}

HttpResponse::~HttpResponse()
{
}

}
