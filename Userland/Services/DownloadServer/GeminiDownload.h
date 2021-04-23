/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <DownloadServer/Download.h>
#include <LibCore/Forward.h>
#include <LibGemini/Forward.h>

namespace DownloadServer {

class GeminiDownload final : public Download {
public:
    virtual ~GeminiDownload() override;
    static NonnullOwnPtr<GeminiDownload> create_with_job(Badge<GeminiProtocol>, ClientConnection&, NonnullRefPtr<Gemini::GeminiJob>, NonnullOwnPtr<OutputFileStream>&&);

private:
    explicit GeminiDownload(ClientConnection&, NonnullRefPtr<Gemini::GeminiJob>, NonnullOwnPtr<OutputFileStream>&&);

    virtual void set_certificate(String certificate, String key) override;

    NonnullRefPtr<Gemini::GeminiJob> m_job;
};

}
