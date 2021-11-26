/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/NonnullOwnPtr.h>
#include <LibCore/Forward.h>
#include <LibHTTP/Forward.h>
#include <RequestServer/Request.h>

namespace RequestServer {

class HttpRequest final : public Request {
public:
    virtual ~HttpRequest() override;
    static NonnullOwnPtr<HttpRequest> create_with_job(Badge<HttpProtocol>&&, ClientConnection&, NonnullRefPtr<HTTP::HttpJob>, NonnullOwnPtr<OutputFileStream>&&);

    HTTP::HttpJob& job() { return m_job; }
    HTTP::HttpJob const& job() const { return m_job; }

private:
    explicit HttpRequest(ClientConnection&, NonnullRefPtr<HTTP::HttpJob>, NonnullOwnPtr<OutputFileStream>&&);

    NonnullRefPtr<HTTP::HttpJob> m_job;
};

}
