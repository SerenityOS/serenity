/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <LibCore/Forward.h>

namespace Gemini {

class GeminiRequest {
public:
    GeminiRequest() = default;
    ~GeminiRequest() = default;

    const URL& url() const { return m_url; }
    void set_url(const URL& url) { m_url = url; }

    ByteBuffer to_raw_request() const;

    static Optional<GeminiRequest> from_raw_request(ByteBuffer const&);

private:
    URL m_url;
};

}
