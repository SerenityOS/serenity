/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <DownloadServer/ClientConnection.h>
#include <DownloadServer/Download.h>
#include <DownloadServer/HttpDownload.h>
#include <DownloadServer/Protocol.h>
#include <LibHTTP/HttpJob.h>

namespace DownloadServer {

class HttpProtocol final : public Protocol {
public:
    using JobType = HTTP::HttpJob;
    using DownloadType = HttpDownload;

    HttpProtocol();
    ~HttpProtocol() override = default;

    virtual OwnPtr<Download> start_download(ClientConnection&, const String& method, const URL&, const HashMap<String, String>& headers, ReadonlyBytes body) override;
};

}
