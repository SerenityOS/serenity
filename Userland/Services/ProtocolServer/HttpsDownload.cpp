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

#include <LibHTTP/HttpsJob.h>
#include <ProtocolServer/HttpCommon.h>
#include <ProtocolServer/HttpsDownload.h>
#include <ProtocolServer/HttpsProtocol.h>

namespace ProtocolServer {

HttpsDownload::HttpsDownload(ClientConnection& client, NonnullRefPtr<HTTP::HttpsJob> job, NonnullOwnPtr<OutputFileStream>&& output_stream)
    : Download(client, move(output_stream))
    , m_job(job)
{
    Detail::init(this, job);
}

void HttpsDownload::set_certificate(String certificate, String key)
{
    m_job->set_certificate(move(certificate), move(key));
}

HttpsDownload::~HttpsDownload()
{
    m_job->on_finish = nullptr;
    m_job->on_progress = nullptr;
    m_job->shutdown();
}

NonnullOwnPtr<HttpsDownload> HttpsDownload::create_with_job(Badge<HttpsProtocol>&&, ClientConnection& client, NonnullRefPtr<HTTP::HttpsJob> job, NonnullOwnPtr<OutputFileStream>&& output_stream)
{
    return adopt_own(*new HttpsDownload(client, move(job), move(output_stream)));
}

}
