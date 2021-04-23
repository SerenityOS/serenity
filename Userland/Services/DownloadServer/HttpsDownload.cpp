/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <DownloadServer/HttpCommon.h>
#include <DownloadServer/HttpsDownload.h>
#include <DownloadServer/HttpsProtocol.h>
#include <LibHTTP/HttpsJob.h>

namespace DownloadServer {

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
