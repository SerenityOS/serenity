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
#include <LibHTTP/HttpsJob.h>
#include <ProtocolServer/ClientConnection.h>
#include <ProtocolServer/Download.h>
#include <ProtocolServer/HttpsDownload.h>
#include <ProtocolServer/Protocol.h>

namespace ProtocolServer {

class HttpsProtocol final : public Protocol {
public:
    using JobType = HTTP::HttpsJob;
    using DownloadType = HttpsDownload;

    HttpsProtocol();
    ~HttpsProtocol() override = default;

    virtual OwnPtr<Download> start_download(ClientConnection&, const String& method, const URL&, const HashMap<String, String>& headers, ReadonlyBytes body) override;
};

}
