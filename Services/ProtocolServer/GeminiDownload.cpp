/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGemini/GeminiJob.h>
#include <LibGemini/GeminiResponse.h>
#include <ProtocolServer/GeminiDownload.h>

namespace ProtocolServer {

GeminiDownload::GeminiDownload(ClientConnection& client, NonnullRefPtr<Gemini::GeminiJob> job)
    : Download(client)
    , m_job(job)
{
    m_job->on_finish = [this](bool success) {
        if (auto* response = m_job->response()) {
            set_payload(response->payload());
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

NonnullOwnPtr<GeminiDownload> GeminiDownload::create_with_job(Badge<GeminiProtocol>, ClientConnection& client, NonnullRefPtr<Gemini::GeminiJob> job)
{
    return adopt_own(*new GeminiDownload(client, move(job)));
}

}
