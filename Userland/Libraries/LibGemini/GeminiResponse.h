/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <LibCore/NetworkResponse.h>

namespace Gemini {

class GeminiResponse : public Core::NetworkResponse {
public:
    virtual ~GeminiResponse() override = default;
    static NonnullRefPtr<GeminiResponse> create(int status, ByteString meta)
    {
        return adopt_ref(*new GeminiResponse(status, meta));
    }

    int status() const { return m_status; }
    ByteString meta() const { return m_meta; }

private:
    GeminiResponse(int status, ByteString);

    int m_status { 0 };
    ByteString m_meta;
};

}
