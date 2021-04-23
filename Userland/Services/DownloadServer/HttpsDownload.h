/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <DownloadServer/Download.h>
#include <LibCore/Forward.h>
#include <LibHTTP/HttpsJob.h>

namespace DownloadServer {

class HttpsDownload final : public Download {
public:
    virtual ~HttpsDownload() override;
    static NonnullOwnPtr<HttpsDownload> create_with_job(Badge<HttpsProtocol>&&, ClientConnection&, NonnullRefPtr<HTTP::HttpsJob>, NonnullOwnPtr<OutputFileStream>&&);

    HTTP::HttpsJob& job() { return m_job; }

private:
    explicit HttpsDownload(ClientConnection&, NonnullRefPtr<HTTP::HttpsJob>, NonnullOwnPtr<OutputFileStream>&&);

    virtual void set_certificate(String certificate, String key) override;

    NonnullRefPtr<HTTP::HttpsJob> m_job;
};

}
