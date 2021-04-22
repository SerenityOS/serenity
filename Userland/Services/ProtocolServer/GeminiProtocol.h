/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <ProtocolServer/Protocol.h>

namespace ProtocolServer {

class GeminiProtocol final : public Protocol {
public:
    GeminiProtocol();
    virtual ~GeminiProtocol() override;

    virtual OwnPtr<Download> start_download(ClientConnection&, const String& method, const URL&, const HashMap<String, String>&, ReadonlyBytes body) override;
};

}
