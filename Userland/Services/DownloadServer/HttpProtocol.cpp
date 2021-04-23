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
#include <DownloadServer/ClientConnection.h>
#include <DownloadServer/Download.h>
#include <DownloadServer/HttpCommon.h>
#include <DownloadServer/HttpDownload.h>
#include <DownloadServer/HttpProtocol.h>
#include <LibHTTP/HttpJob.h>

namespace DownloadServer {

HttpProtocol::HttpProtocol()
    : Protocol("http")
{
}

OwnPtr<Download> HttpProtocol::start_download(ClientConnection& client, const String& method, const URL& url, const HashMap<String, String>& headers, ReadonlyBytes body)
{
    return Detail::start_download(Badge<HttpProtocol> {}, client, method, url, headers, body, get_pipe_for_download());
}

}
