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
#include <ProtocolServer/Download.h>

namespace ProtocolServer {

class HttpDownload final : public Download {
public:
    virtual ~HttpDownload() override;
    static NonnullOwnPtr<HttpDownload> create_with_job(Badge<HttpProtocol>&&, ClientConnection&, NonnullRefPtr<HTTP::HttpJob>, NonnullOwnPtr<OutputFileStream>&&);

    HTTP::HttpJob& job() { return m_job; }

private:
    explicit HttpDownload(ClientConnection&, NonnullRefPtr<HTTP::HttpJob>, NonnullOwnPtr<OutputFileStream>&&);

    NonnullRefPtr<HTTP::HttpJob> m_job;
};

}
