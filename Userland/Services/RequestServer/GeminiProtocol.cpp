/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGemini/GeminiJob.h>
#include <LibGemini/GeminiRequest.h>
#include <RequestServer/GeminiProtocol.h>
#include <RequestServer/GeminiRequest.h>
#include <fcntl.h>

namespace RequestServer {

GeminiProtocol::GeminiProtocol()
    : Protocol("gemini")
{
}

GeminiProtocol::~GeminiProtocol()
{
}

OwnPtr<Request> GeminiProtocol::start_request(ClientConnection& client, const String&, const URL& url, const HashMap<String, String>&, ReadonlyBytes)
{
    Gemini::GeminiRequest request;
    request.set_url(url);

    auto pipe_result = get_pipe_for_request();
    if (pipe_result.is_error())
        return {};

    auto output_stream = make<OutputFileStream>(pipe_result.value().write_fd);
    output_stream->make_unbuffered();
    auto job = Gemini::GeminiJob::construct(request, *output_stream);
    auto protocol_request = GeminiRequest::create_with_job({}, client, (Gemini::GeminiJob&)*job, move(output_stream));
    protocol_request->set_request_fd(pipe_result.value().read_fd);
    job->start();
    return protocol_request;
}

}
