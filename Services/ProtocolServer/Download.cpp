/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Badge.h>
#include <ProtocolServer/ClientConnection.h>
#include <ProtocolServer/Download.h>

namespace ProtocolServer {

// FIXME: What about rollover?
static i32 s_next_id = 1;

Download::Download(ClientConnection& client)
    : m_client(client)
    , m_id(s_next_id++)
{
}

Download::~Download()
{
}

void Download::stop()
{
    m_client.did_finish_download({}, *this, false);
}

void Download::set_payload(const ByteBuffer& payload)
{
    m_payload = payload;
    m_total_size = payload.size();
}

void Download::set_response_headers(const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)
{
    m_response_headers = response_headers;
}

void Download::set_certificate(String, String)
{
}

void Download::did_finish(bool success)
{
    m_client.did_finish_download({}, *this, success);
}

void Download::did_progress(Optional<u32> total_size, u32 downloaded_size)
{
    m_total_size = total_size;
    m_downloaded_size = downloaded_size;
    m_client.did_progress_download({}, *this);
}

void Download::did_request_certificates()
{
    m_client.did_request_certificates({}, *this);
}

}
