/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <LibCore/Forward.h>
#include <LibGemini/Forward.h>
#include <RequestServer/Request.h>

namespace RequestServer {

class GeminiRequest final : public Request {
public:
    virtual ~GeminiRequest() override;
    static NonnullOwnPtr<GeminiRequest> create_with_job(Badge<GeminiProtocol>, ClientConnection&, NonnullRefPtr<Gemini::GeminiJob>, NonnullOwnPtr<OutputFileStream>&&);

    Gemini::GeminiJob const& job() const { return *m_job; }

private:
    explicit GeminiRequest(ClientConnection&, NonnullRefPtr<Gemini::GeminiJob>, NonnullOwnPtr<OutputFileStream>&&);

    virtual void set_certificate(String certificate, String key) override;

    NonnullRefPtr<Gemini::GeminiJob> m_job;
};

}
