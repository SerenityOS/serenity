/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <LibCore/NetworkResponse.h>

namespace Gemini {

class GeminiResponse : public Core::NetworkResponse {
public:
    virtual ~GeminiResponse() override = default;
    static NonnullRefPtr<GeminiResponse> create(int status, DeprecatedString meta)
    {
        return adopt_ref(*new GeminiResponse(status, meta));
    }

    int status() const { return m_status; }
    DeprecatedString meta() const { return m_meta; }

private:
    GeminiResponse(int status, DeprecatedString);

    int m_status { 0 };
    DeprecatedString m_meta;
};

}
