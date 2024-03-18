/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGemini/GeminiRequest.h>
#include <LibURL/URL.h>

namespace Gemini {

ErrorOr<ByteBuffer> GeminiRequest::to_raw_request() const
{
    StringBuilder builder;
    TRY(builder.try_append(m_url.to_byte_string()));
    TRY(builder.try_append("\r\n"sv));
    return builder.to_byte_buffer();
}

Optional<GeminiRequest> GeminiRequest::from_raw_request(ByteBuffer const& raw_request)
{
    URL::URL url = StringView(raw_request);
    if (!url.is_valid())
        return {};
    GeminiRequest request;
    request.m_url = url;
    return request;
}

}
