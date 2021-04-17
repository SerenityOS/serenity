/*
 * Copyright (c) 2021, The SerenityOS developers.
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

#include <LibWebSocket/ConnectionInfo.h>

namespace WebSocket {

ConnectionInfo::ConnectionInfo(URL url)
    : m_url(move(url))
{
}

bool ConnectionInfo::is_secure() const
{
    // RFC 6455 Section 3 :
    // The URI is called "secure" if the scheme component matches "wss" case-insensitively.
    return m_url.protocol().equals_ignoring_case("wss"sv);
}

String ConnectionInfo::resource_name() const
{
    // RFC 6455 Section 3 :
    // The "resource-name" can be constructed by concatenating the following:
    StringBuilder builder;
    // "/" if the path component is empty
    if (m_url.path().is_empty())
        builder.append("/");
    // The path component
    builder.append(m_url.path());
    // "?" if the query component is non-empty
    if (!m_url.query().is_empty())
        builder.append("?");
    // the query component
    builder.append(m_url.query());
    return builder.to_string();
}

}
