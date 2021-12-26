/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibHTTP/HttpRequest.h>
#include <ProtocolServer/ClientConnection.h>
#include <ProtocolServer/Download.h>

namespace ProtocolServer::Detail {

template<typename TSelf, typename TJob>
void init(TSelf* self, TJob job)
{
    job->on_headers_received = [self](auto& headers, auto response_code) {
        if (response_code.has_value())
            self->set_status_code(response_code.value());
        self->set_response_headers(headers);
    };

    job->on_finish = [self](bool success) {
        if (auto* response = self->job().response()) {
            self->set_status_code(response->code());
            self->set_response_headers(response->headers());
            self->set_downloaded_size(self->output_stream().size());
        }

        // if we didn't know the total size, pretend that the download finished successfully
        // and set the total size to the downloaded size
        if (!self->total_size().has_value())
            self->did_progress(self->downloaded_size(), self->downloaded_size());

        self->did_finish(success);
    };
    job->on_progress = [self](Optional<u32> total, u32 current) {
        self->did_progress(total, current);
    };
    if constexpr (requires { job->on_certificate_requested; }) {
        job->on_certificate_requested = [self](auto&) {
            self->did_request_certificates();
        };
    }
}

template<typename TBadgedProtocol, typename TPipeResult>
OwnPtr<Download> start_download(TBadgedProtocol&& protocol, ClientConnection& client, const String& method, const URL& url, const HashMap<String, String>& headers, ReadonlyBytes body, TPipeResult&& pipe_result)
{
    using TJob = TBadgedProtocol::Type::JobType;
    using TDownload = TBadgedProtocol::Type::DownloadType;

    if (pipe_result.is_error()) {
        return {};
    }

    HTTP::HttpRequest request;
    if (method.equals_ignoring_case("post"))
        request.set_method(HTTP::HttpRequest::Method::POST);
    else
        request.set_method(HTTP::HttpRequest::Method::GET);
    request.set_url(url);
    request.set_headers(headers);
    request.set_body(body);

    auto output_stream = make<OutputFileStream>(pipe_result.value().write_fd);
    output_stream->make_unbuffered();
    auto job = TJob::construct(request, *output_stream);
    auto download = TDownload::create_with_job(forward<TBadgedProtocol>(protocol), client, (TJob&)*job, move(output_stream));
    download->set_download_fd(pipe_result.value().read_fd);
    job->start();
    return download;
}

}
