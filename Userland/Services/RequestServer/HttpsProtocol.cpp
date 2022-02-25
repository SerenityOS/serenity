/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
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
#include <RequestServer/HttpsProtocol.h>
#include <RequestServer/Request.h>

namespace RequestServer {

HttpsProtocol::HttpsProtocol()
    : Protocol("https")
{
}

OwnPtr<Request> HttpsProtocol::start_request(ConnectionFromClient& client, const String& method, const URL& url, const HashMap<String, String>& headers, ReadonlyBytes body)
{
    return Detail::start_request(Badge<HttpsProtocol> {}, client, method, url, headers, body, get_pipe_for_request());
}

}
