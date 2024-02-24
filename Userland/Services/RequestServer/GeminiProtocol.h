/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <RequestServer/Protocol.h>

namespace RequestServer {

class GeminiProtocol final : public Protocol {
public:
    GeminiProtocol();
    virtual ~GeminiProtocol() override = default;

    virtual OwnPtr<Request> start_request(i32, ConnectionFromClient&, ByteString const& method, const URL&, HashMap<ByteString, ByteString> const&, ReadonlyBytes body, Core::ProxyData proxy_data = {}) override;
};

}
