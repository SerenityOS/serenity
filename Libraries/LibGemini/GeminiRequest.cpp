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

#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <LibGemini/GeminiJob.h>
#include <LibGemini/GeminiRequest.h>

namespace Gemini {

GeminiRequest::GeminiRequest()
{
}

GeminiRequest::~GeminiRequest()
{
}

RefPtr<Core::NetworkJob> GeminiRequest::schedule()
{
    auto job = GeminiJob::construct(*this);
    job->start();
    return job;
}

ByteBuffer GeminiRequest::to_raw_request() const
{
    StringBuilder builder;
    builder.append(m_url.to_string());
    builder.append("\r\n");
    return builder.to_byte_buffer();
}

Optional<GeminiRequest> GeminiRequest::from_raw_request(const ByteBuffer& raw_request)
{
    URL url = StringView(raw_request);
    if (!url.is_valid())
        return {};
    GeminiRequest request;
    request.m_url = url;
    return request;
}

}
