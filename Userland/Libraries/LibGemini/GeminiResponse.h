/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibCore/NetworkResponse.h>

namespace Gemini {

class GeminiResponse : public Core::NetworkResponse {
public:
    virtual ~GeminiResponse() override = default;
    static NonnullRefPtr<GeminiResponse> create(int status, String meta)
    {
        return adopt_ref(*new GeminiResponse(status, meta));
    }

    int status() const { return m_status; }
    String meta() const { return m_meta; }

private:
    GeminiResponse(int status, String);

    int m_status { 0 };
    String m_meta;
};

}
