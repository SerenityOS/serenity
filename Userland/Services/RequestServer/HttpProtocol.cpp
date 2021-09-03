/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Badge.h>
#include <YAK/ByteBuffer.h>
#include <YAK/HashMap.h>
#include <YAK/OwnPtr.h>
#include <YAK/String.h>
#include <YAK/URL.h>
#include <RequestServer/ClientConnection.h>
#include <RequestServer/HttpCommon.h>
#include <RequestServer/HttpProtocol.h>
#include <RequestServer/Request.h>

namespace RequestServer {

HttpProtocol::HttpProtocol()
    : Protocol("http")
{
}

OwnPtr<Request> HttpProtocol::start_request(ClientConnection& client, const String& method, const URL& url, const HashMap<String, String>& headers, ReadonlyBytes body)
{
    return Detail::start_request(Badge<HttpProtocol> {}, client, method, url, headers, body, get_pipe_for_request());
}

}
