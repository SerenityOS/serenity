/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <RequestServer/ConnectionFromClient.h>
#include <RequestServer/HttpCommon.h>
#include <RequestServer/HttpProtocol.h>
#include <RequestServer/Request.h>

namespace RequestServer {

HttpProtocol::HttpProtocol()
    : Protocol("http")
{
}

OwnPtr<Request> HttpProtocol::start_request(ConnectionFromClient& client, String const& method, const URL& url, HashMap<String, String> const& headers, ReadonlyBytes body, Core::ProxyData proxy_data)
{
    return Detail::start_request(Badge<HttpProtocol> {}, client, method, url, headers, body, get_pipe_for_request(), proxy_data);
}

}
