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
#include <LibHTTP/HttpJob.h>
#include <ProtocolServer/ClientConnection.h>
#include <ProtocolServer/Download.h>
#include <ProtocolServer/HttpDownload.h>
#include <ProtocolServer/Protocol.h>

namespace ProtocolServer {

class HttpProtocol final : public Protocol {
public:
    using JobType = HTTP::HttpJob;
    using DownloadType = HttpDownload;

    HttpProtocol();
    ~HttpProtocol() override = default;

    virtual OwnPtr<Download> start_download(ClientConnection&, const String& method, const URL&, const HashMap<String, String>& headers, ReadonlyBytes body) override;
};

}
