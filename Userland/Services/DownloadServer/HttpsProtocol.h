/*
 * Copyright (c) 2018-2020, The SerenityOS developers.
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
#include <DownloadServer/HttpsDownload.h>
#include <DownloadServer/Protocol.h>
#include <LibHTTP/HttpsJob.h>

namespace DownloadServer {

class HttpsProtocol final : public Protocol {
public:
    using JobType = HTTP::HttpsJob;
    using DownloadType = HttpsDownload;

    HttpsProtocol();
    ~HttpsProtocol() override = default;

    virtual OwnPtr<Download> start_download(ClientConnection&, const String& method, const URL&, const HashMap<String, String>& headers, ReadonlyBytes body) override;
};

}
