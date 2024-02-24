/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibHTTP/Job.h>
#include <RequestServer/HttpCommon.h>
#include <RequestServer/HttpProtocol.h>
#include <RequestServer/HttpRequest.h>

namespace RequestServer {

HttpRequest::HttpRequest(ConnectionFromClient& client, NonnullRefPtr<HTTP::Job> job, NonnullOwnPtr<Core::File>&& output_stream, i32 request_id)
    : Request(client, move(output_stream), request_id)
    , m_job(job)
{
    Detail::init(this, job);
}

HttpRequest::~HttpRequest()
{
    m_job->on_finish = nullptr;
    m_job->on_progress = nullptr;
    m_job->cancel();
}

NonnullOwnPtr<HttpRequest> HttpRequest::create_with_job(Badge<HttpProtocol>&&, ConnectionFromClient& client, NonnullRefPtr<HTTP::Job> job, NonnullOwnPtr<Core::File>&& output_stream, i32 request_id)
{
    return adopt_own(*new HttpRequest(client, move(job), move(output_stream), request_id));
}

}
