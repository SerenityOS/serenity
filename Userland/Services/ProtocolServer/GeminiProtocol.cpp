/*
 * Copyright (c) 2020, The SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGemini/GeminiJob.h>
#include <LibGemini/GeminiRequest.h>
#include <ProtocolServer/GeminiDownload.h>
#include <ProtocolServer/GeminiProtocol.h>
#include <fcntl.h>

namespace ProtocolServer {

GeminiProtocol::GeminiProtocol()
    : Protocol("gemini")
{
}

GeminiProtocol::~GeminiProtocol()
{
}

OwnPtr<Download> GeminiProtocol::start_download(ClientConnection& client, const String&, const URL& url, const HashMap<String, String>&, ReadonlyBytes)
{
    Gemini::GeminiRequest request;
    request.set_url(url);

    auto pipe_result = get_pipe_for_download();
    if (pipe_result.is_error())
        return {};

    auto output_stream = make<OutputFileStream>(pipe_result.value().write_fd);
    output_stream->make_unbuffered();
    auto job = Gemini::GeminiJob::construct(request, *output_stream);
    auto download = GeminiDownload::create_with_job({}, client, (Gemini::GeminiJob&)*job, move(output_stream));
    download->set_download_fd(pipe_result.value().read_fd);
    job->start();
    return download;
}

}
