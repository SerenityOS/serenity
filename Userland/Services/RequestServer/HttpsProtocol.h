/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
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
#include <RequestServer/ConnectionFromClient.h>
#include <RequestServer/HttpsRequest.h>
#include <RequestServer/Protocol.h>
#include <RequestServer/Request.h>

namespace RequestServer {

class HttpsProtocol final : public Protocol {
public:
    using JobType = HTTP::HttpsJob;
    using RequestType = HttpsRequest;

    HttpsProtocol();
    ~HttpsProtocol() override = default;

    virtual OwnPtr<Request> start_request(ConnectionFromClient&, const String& method, const URL&, const HashMap<String, String>& headers, ReadonlyBytes body) override;
};

}
