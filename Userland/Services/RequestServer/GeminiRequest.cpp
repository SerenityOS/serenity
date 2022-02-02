/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionCache.h"
#include <LibCore/EventLoop.h>
#include <LibGemini/GeminiResponse.h>
#include <LibGemini/Job.h>
#include <RequestServer/GeminiRequest.h>

namespace RequestServer {

GeminiRequest::GeminiRequest(ClientConnection& client, NonnullRefPtr<Gemini::Job> job, NonnullOwnPtr<Core::Stream::File>&& output_stream)
    : Request(client, move(output_stream))
    , m_job(move(job))
{
    m_job->on_finish = [this](bool success) {
        Core::deferred_invoke([url = m_job->url(), socket = m_job->socket()] {
            ConnectionCache::request_did_finish(url, socket);
        });
        if (auto* response = m_job->response()) {
            set_downloaded_size(MUST(const_cast<Core::Stream::File&>(this->output_stream()).size()));
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

        // signal 100% request progress so any listeners can react
        // appropriately
        did_progress(downloaded_size(), downloaded_size());

        did_finish(success);
    };
    m_job->on_progress = [this](Optional<u32> total, u32 current) {
        did_progress(move(total), current);
    };
}

void GeminiRequest::set_certificate(String, String)
{
}

GeminiRequest::~GeminiRequest()
{
    m_job->on_finish = nullptr;
    m_job->on_progress = nullptr;
    m_job->cancel();
}

NonnullOwnPtr<GeminiRequest> GeminiRequest::create_with_job(Badge<GeminiProtocol>, ClientConnection& client, NonnullRefPtr<Gemini::Job> job, NonnullOwnPtr<Core::Stream::File>&& output_stream)
{
    return adopt_own(*new GeminiRequest(client, move(job), move(output_stream)));
}

}
