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
    static NonnullOwnPtr<GeminiRequest> create_with_job(Badge<GeminiProtocol>, ConnectionFromClient&, NonnullRefPtr<Gemini::Job>, NonnullOwnPtr<Core::File>&&, i32 request_id);

    Gemini::Job const& job() const { return *m_job; }

    virtual URL::URL url() const override { return m_job->url(); }

private:
    explicit GeminiRequest(ConnectionFromClient&, NonnullRefPtr<Gemini::Job>, NonnullOwnPtr<Core::File>&&, i32 request_id);

    virtual void set_certificate(ByteString certificate, ByteString key) override;

    NonnullRefPtr<Gemini::Job> m_job;
};

}
