/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <RequestServer/ClientConnection.h>
#include <RequestServer/ConnectionCache.h>
#include <RequestServer/Request.h>

namespace RequestServer::Detail {

template<typename TSelf, typename TJob>
void init(TSelf* self, TJob job)
{
    job->on_headers_received = [self](auto& headers, auto response_code) {
        if (response_code.has_value())
            self->set_status_code(response_code.value());
        self->set_response_headers(headers);
    };

    job->on_finish = [self](bool success) {
        Core::deferred_invoke([url = self->job().url(), socket = self->job().socket()] {
            ConnectionCache::request_did_finish(url, socket);
        });
        if (auto* response = self->job().response()) {
            self->set_status_code(response->code());
            self->set_response_headers(response->headers());
            self->set_downloaded_size(response->downloaded_size());
        }

        // if we didn't know the total size, pretend that the request finished successfully
        // and set the total size to the downloaded size
        if (!self->total_size().has_value())
            self->did_progress(self->downloaded_size(), self->downloaded_size());

        self->did_finish(success);
    };
    job->on_progress = [self](Optional<u32> total, u32 current) {
        self->did_progress(total, current);
    };
    if constexpr (requires { job->on_certificate_requested; }) {
        job->on_certificate_requested = [job, self] {
            self->did_request_certificates();
            Core::EventLoop::current().spin_until([&] {
                return job->received_client_certificates();
            });
            return job->take_client_certificates();
        };
    }
}

template<typename TBadgedProtocol, typename TPipeResult>
OwnPtr<Request> start_request(TBadgedProtocol&& protocol, ClientConnection& client, const String& method, const URL& url, const HashMap<String, String>& headers, ReadonlyBytes body, TPipeResult&& pipe_result)
{
    using TJob = typename TBadgedProtocol::Type::JobType;
    using TRequest = typename TBadgedProtocol::Type::RequestType;

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

    auto allocated_body_result = ByteBuffer::copy(body);
    if (allocated_body_result.is_error())
        return {};
    request.set_body(allocated_body_result.release_value());

    auto output_stream = MUST(Core::Stream::File::adopt_fd(pipe_result.value().write_fd, Core::Stream::OpenMode::Write));
    auto job = TJob::construct(move(request), *output_stream);
    auto protocol_request = TRequest::create_with_job(forward<TBadgedProtocol>(protocol), client, (TJob&)*job, move(output_stream));
    protocol_request->set_request_fd(pipe_result.value().read_fd);

    if constexpr (IsSame<typename TBadgedProtocol::Type, HttpsProtocol>)
        ConnectionCache::get_or_create_connection(ConnectionCache::g_tls_connection_cache, url, *job);
    else
        ConnectionCache::get_or_create_connection(ConnectionCache::g_tcp_connection_cache, url, *job);

    return protocol_request;
}

}
