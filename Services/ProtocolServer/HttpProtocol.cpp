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
#include <LibHTTP/HttpRequest.h>
#include <ProtocolServer/HttpDownload.h>
#include <ProtocolServer/HttpProtocol.h>
#include <fcntl.h>

namespace ProtocolServer {

HttpProtocol::HttpProtocol()
    : Protocol("http")
{
}

HttpProtocol::~HttpProtocol()
{
}

OwnPtr<Download> HttpProtocol::start_download(ClientConnection& client, const String& method, const URL& url, const HashMap<String, String>& headers, ReadonlyBytes body)
{
    HTTP::HttpRequest request;
    if (method.equals_ignoring_case("post"))
        request.set_method(HTTP::HttpRequest::Method::POST);
    else
        request.set_method(HTTP::HttpRequest::Method::GET);
    request.set_url(url);
    request.set_headers(headers);
    request.set_body(body);

    int fd_pair[2] { 0 };
    if (pipe(fd_pair) != 0) {
        auto saved_errno = errno;
        dbgln("Protocol: pipe() failed: {}", strerror(saved_errno));
        return nullptr;
    }

    auto output_stream = make<OutputFileStream>(fd_pair[1]);
    output_stream->make_unbuffered();
    auto job = HTTP::HttpJob::construct(request, *output_stream);
    auto download = HttpDownload::create_with_job({}, client, (HTTP::HttpJob&)*job, move(output_stream));
    download->set_download_fd(fd_pair[0]);
    job->start();
    return download;
}

}
