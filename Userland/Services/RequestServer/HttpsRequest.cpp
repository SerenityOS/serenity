/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibHTTP/HttpsJob.h>
#include <RequestServer/HttpCommon.h>
#include <RequestServer/HttpsProtocol.h>
#include <RequestServer/HttpsRequest.h>

namespace RequestServer {

HttpsRequest::HttpsRequest(ClientConnection& client, NonnullRefPtr<HTTP::HttpsJob> job, NonnullOwnPtr<OutputFileStream>&& output_stream)
    : Request(client, move(output_stream))
    , m_job(job)
{
    Detail::init(this, job);
}

void HttpsRequest::set_certificate(String certificate, String key)
{
    m_job->set_certificate(move(certificate), move(key));
}

HttpsRequest::~HttpsRequest()
{
    m_job->on_finish = nullptr;
    m_job->on_progress = nullptr;
    m_job->shutdown();
}

NonnullOwnPtr<HttpsRequest> HttpsRequest::create_with_job(Badge<HttpsProtocol>&&, ClientConnection& client, NonnullRefPtr<HTTP::HttpsJob> job, NonnullOwnPtr<OutputFileStream>&& output_stream)
{
    return adopt_own(*new HttpsRequest(client, move(job), move(output_stream)));
}

}
