/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGemini/GeminiJob.h>
#include <LibGemini/GeminiResponse.h>
#include <ProtocolServer/GeminiDownload.h>

namespace ProtocolServer {

GeminiDownload::GeminiDownload(ClientConnection& client, NonnullRefPtr<Gemini::GeminiJob> job, NonnullOwnPtr<OutputFileStream>&& output_stream)
    : Download(client, move(output_stream))
    , m_job(job)
{
    m_job->on_finish = [this](bool success) {
        if (auto* response = m_job->response()) {
            set_downloaded_size(this->output_stream().size());
            if (!response->meta().is_empty()) {
                HashMap<String, String, CaseInsensitiveStringTraits> headers;
                headers.set("meta", response->meta());
                // Note: We're setting content-type to meta only on status==SUCCESS
                //       we should perhaps have a better mechanism for this, since we
                //       are already shoehorning the concept of "headers" here
                if (response->status() >= 20 && response->status() < 30) {
                    headers.set("content-type", response->meta());
                }
                set_response_headers(headers);
            }
        }

        // signal 100% download progress so any listeners can react
        // appropriately
        did_progress(downloaded_size(), downloaded_size());

        did_finish(success);
    };
    m_job->on_progress = [this](Optional<u32> total, u32 current) {
        did_progress(total, current);
    };
    m_job->on_certificate_requested = [this](auto&) {
        did_request_certificates();
    };
}

void GeminiDownload::set_certificate(String certificate, String key)
{
    m_job->set_certificate(move(certificate), move(key));
}

GeminiDownload::~GeminiDownload()
{
    m_job->on_finish = nullptr;
    m_job->on_progress = nullptr;
    m_job->shutdown();
}

NonnullOwnPtr<GeminiDownload> GeminiDownload::create_with_job(Badge<GeminiProtocol>, ClientConnection& client, NonnullRefPtr<Gemini::GeminiJob> job, NonnullOwnPtr<OutputFileStream>&& output_stream)
{
    return adopt_own(*new GeminiDownload(client, move(job), move(output_stream)));
}

}
