/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionCache.h"
#include <LibGemini/GeminiRequest.h>
#include <LibGemini/Job.h>
#include <RequestServer/GeminiProtocol.h>
#include <RequestServer/GeminiRequest.h>

namespace RequestServer {

GeminiProtocol::GeminiProtocol()
    : Protocol("gemini")
{
}

OwnPtr<Request> GeminiProtocol::start_request(i32 request_id, ConnectionFromClient& client, ByteString const&, const URL::URL& url, HTTP::HeaderMap const&, ReadonlyBytes, Core::ProxyData proxy_data)
{
    Gemini::GeminiRequest request;
    request.set_url(url);

    auto pipe_result = get_pipe_for_request();
    if (pipe_result.is_error())
        return {};

    auto output_stream = MUST(Core::File::adopt_fd(pipe_result.value().write_fd, Core::File::OpenMode::Write));
    auto job = Gemini::Job::construct(request, *output_stream);
    auto protocol_request = GeminiRequest::create_with_job({}, client, *job, move(output_stream), request_id);
    protocol_request->set_request_fd(pipe_result.value().read_fd);

    Core::EventLoop::current().deferred_invoke([=] {
        ConnectionCache::ensure_connection(ConnectionCache::g_tls_connection_cache, url, job, proxy_data);
    });

    return protocol_request;
}

void GeminiProtocol::install()
{
    Protocol::install(adopt_own(*new GeminiProtocol()));
}

}
