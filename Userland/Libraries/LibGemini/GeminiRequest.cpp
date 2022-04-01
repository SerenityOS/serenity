/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/URL.h>
#include <LibGemini/GeminiRequest.h>

namespace Gemini {

ByteBuffer GeminiRequest::to_raw_request() const
{
    StringBuilder builder;
    builder.append(m_url.to_string());
    builder.append("\r\n");
    return builder.to_byte_buffer();
}

Optional<GeminiRequest> GeminiRequest::from_raw_request(ByteBuffer const& raw_request)
{
    URL url = StringView(raw_request);
    if (!url.is_valid())
        return {};
    GeminiRequest request;
    request.m_url = url;
    return request;
}

}
