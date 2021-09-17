/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <LibCore/Forward.h>
#include <LibHTTP/HttpsJob.h>
#include <RequestServer/Request.h>

namespace RequestServer {

class HttpsRequest final : public Request {
public:
    virtual ~HttpsRequest() override;
    static NonnullOwnPtr<HttpsRequest> create_with_job(Badge<HttpsProtocol>&&, ClientConnection&, NonnullRefPtr<HTTP::HttpsJob>, NonnullOwnPtr<OutputFileStream>&&);

    HTTP::HttpsJob& job() { return m_job; }
    HTTP::HttpsJob const& job() const { return m_job; }

private:
    explicit HttpsRequest(ClientConnection&, NonnullRefPtr<HTTP::HttpsJob>, NonnullOwnPtr<OutputFileStream>&&);

    virtual void set_certificate(String certificate, String key) override;

    NonnullRefPtr<HTTP::HttpsJob> m_job;
};

}
