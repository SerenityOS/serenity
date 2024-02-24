/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibHTTP/HttpsJob.h>
#include <RequestServer/HttpCommon.h>
#include <RequestServer/HttpsProtocol.h>
#include <RequestServer/HttpsRequest.h>

namespace RequestServer {

HttpsRequest::HttpsRequest(ConnectionFromClient& client, NonnullRefPtr<HTTP::HttpsJob> job, NonnullOwnPtr<Core::File>&& output_stream, i32 request_id)
    : Request(client, move(output_stream), request_id)
    , m_job(job)
{
    Detail::init(this, job);
}

void HttpsRequest::set_certificate(ByteString certificate, ByteString key)
{
    m_job->set_certificate(move(certificate), move(key));
}

HttpsRequest::~HttpsRequest()
{
    m_job->on_finish = nullptr;
    m_job->on_progress = nullptr;
    m_job->cancel();
}

NonnullOwnPtr<HttpsRequest> HttpsRequest::create_with_job(Badge<HttpsProtocol>&&, ConnectionFromClient& client, NonnullRefPtr<HTTP::HttpsJob> job, NonnullOwnPtr<Core::File>&& output_stream, i32 request_id)
{
    return adopt_own(*new HttpsRequest(client, move(job), move(output_stream), request_id));
}

}
