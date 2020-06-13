/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <LibHTTP/HttpJob.h>
#include <LibHTTP/HttpResponse.h>
#include <ProtocolServer/HttpDownload.h>

namespace ProtocolServer {

HttpDownload::HttpDownload(ClientConnection& client, NonnullRefPtr<HTTP::HttpJob> job)
    : Download(client)
    , m_job(job)
{
    m_job->on_finish = [this](bool success) {
        if (auto* response = m_job->response()) {
            set_status_code(response->code());
            set_payload(response->payload());
            set_response_headers(response->headers());
        }

        // if we didn't know the total size, pretend that the download finished successfully
        // and set the total size to the downloaded size
        if (!total_size().has_value())
            did_progress(downloaded_size(), downloaded_size());

        did_finish(success);
    };
    m_job->on_progress = [this](Optional<u32> total, u32 current) {
        did_progress(total, current);
    };
}

HttpDownload::~HttpDownload()
{
    m_job->on_finish = nullptr;
    m_job->on_progress = nullptr;
    m_job->shutdown();
}

NonnullOwnPtr<HttpDownload> HttpDownload::create_with_job(Badge<HttpProtocol>, ClientConnection& client, NonnullRefPtr<HTTP::HttpJob> job)
{
    return adopt_own(*new HttpDownload(client, move(job)));
}

}
