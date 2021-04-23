/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <DownloadServer/HttpCommon.h>
#include <DownloadServer/HttpDownload.h>
#include <DownloadServer/HttpProtocol.h>
#include <LibHTTP/HttpJob.h>

namespace DownloadServer {

HttpDownload::HttpDownload(ClientConnection& client, NonnullRefPtr<HTTP::HttpJob> job, NonnullOwnPtr<OutputFileStream>&& output_stream)
    : Download(client, move(output_stream))
    , m_job(job)
{
    Detail::init(this, job);
}

HttpDownload::~HttpDownload()
{
    m_job->on_finish = nullptr;
    m_job->on_progress = nullptr;
    m_job->shutdown();
}

NonnullOwnPtr<HttpDownload> HttpDownload::create_with_job(Badge<HttpProtocol>&&, ClientConnection& client, NonnullRefPtr<HTTP::HttpJob> job, NonnullOwnPtr<OutputFileStream>&& output_stream)
{
    return adopt_own(*new HttpDownload(client, move(job), move(output_stream)));
}

}
