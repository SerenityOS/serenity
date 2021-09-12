/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/URL.h>
#include <LibCore/Forward.h>

namespace Gemini {

class GeminiRequest {
public:
    GeminiRequest();
    ~GeminiRequest();

    const URL& url() const { return m_url; }
    void set_url(const URL& url) { m_url = url; }

    ByteBuffer to_raw_request() const;

    static Optional<GeminiRequest> from_raw_request(const ByteBuffer&);

private:
    URL m_url;
};

}
