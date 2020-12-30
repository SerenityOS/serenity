/*
 * Copyright (c) 2020, The SerenityOS developers.
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
        return nullptr;

    auto output_stream = make<OutputFileStream>(pipe_result.value().write_fd);
    output_stream->make_unbuffered();
    auto job = Gemini::GeminiJob::construct(request, *output_stream);
    auto download = GeminiDownload::create_with_job({}, client, (Gemini::GeminiJob&)*job, move(output_stream));
    download->set_download_fd(pipe_result.value().read_fd);
    job->start();
    return download;
}

}
