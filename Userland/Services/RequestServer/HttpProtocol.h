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
#include <LibHTTP/Job.h>
#include <RequestServer/ConnectionFromClient.h>
#include <RequestServer/HttpRequest.h>
#include <RequestServer/Protocol.h>
#include <RequestServer/Request.h>

namespace RequestServer {

class HttpProtocol final : public Protocol {
public:
    using JobType = HTTP::Job;
    using RequestType = HttpRequest;

    HttpProtocol();
    ~HttpProtocol() override = default;

    virtual OwnPtr<Request> start_request(ConnectionFromClient&, String const& method, const URL&, HashMap<String, String> const& headers, ReadonlyBytes body, Core::ProxyData proxy_data = {}) override;
};

}
