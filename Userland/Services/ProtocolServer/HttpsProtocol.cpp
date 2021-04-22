/*
 * Copyright (c) 2018-2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <LibHTTP/HttpsJob.h>
#include <ProtocolServer/ClientConnection.h>
#include <ProtocolServer/Download.h>
#include <ProtocolServer/HttpCommon.h>
#include <ProtocolServer/HttpsDownload.h>
#include <ProtocolServer/HttpsProtocol.h>

namespace ProtocolServer {

HttpsProtocol::HttpsProtocol()
    : Protocol("https")
{
}

OwnPtr<Download> HttpsProtocol::start_download(ClientConnection& client, const String& method, const URL& url, const HashMap<String, String>& headers, ReadonlyBytes body)
{
    return Detail::start_download(Badge<HttpsProtocol> {}, client, method, url, headers, body, get_pipe_for_download());
}

}
