/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/ByteBuffer.h>
#include <YAK/HashMap.h>
#include <YAK/OwnPtr.h>
#include <YAK/String.h>
#include <YAK/URL.h>
#include <LibHTTP/HttpJob.h>
#include <RequestServer/ClientConnection.h>
#include <RequestServer/HttpRequest.h>
#include <RequestServer/Protocol.h>
#include <RequestServer/Request.h>

namespace RequestServer {

class HttpProtocol final : public Protocol {
public:
    using JobType = HTTP::HttpJob;
    using RequestType = HttpRequest;

    HttpProtocol();
    ~HttpProtocol() override = default;

    virtual OwnPtr<Request> start_request(ClientConnection&, const String& method, const URL&, const HashMap<String, String>& headers, ReadonlyBytes body) override;
};

}
