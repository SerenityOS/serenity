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

#include <AK/SharedBuffer.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>

namespace Protocol {

Download::Download(Client& client, i32 download_id)
    : m_client(client.make_weak_ptr())
    , m_download_id(download_id)
{
}

bool Download::stop()
{
    return m_client->stop_download({}, *this);
}

void Download::did_finish(Badge<Client>, bool success, Optional<u32> status_code, u32 total_size, i32 shbuf_id, const IPC::Dictionary& response_headers)
{
    if (!on_finish)
        return;

    ByteBuffer payload;
    RefPtr<SharedBuffer> shared_buffer;
    if (success && shbuf_id != -1) {
        shared_buffer = SharedBuffer::create_from_shbuf_id(shbuf_id);
        payload = ByteBuffer::wrap(shared_buffer->data<void>(), total_size);
    }

    // FIXME: It's a bit silly that we copy the response headers here just so we can move them into a HashMap with different traits.
    HashMap<String, String, CaseInsensitiveStringTraits> caseless_response_headers;
    response_headers.for_each_entry([&](auto& name, auto& value) {
        caseless_response_headers.set(name, value);
    });

    on_finish(success, payload, move(shared_buffer), caseless_response_headers, status_code);
}

void Download::did_progress(Badge<Client>, Optional<u32> total_size, u32 downloaded_size)
{
    if (on_progress)
        on_progress(total_size, downloaded_size);
}

void Download::did_request_certificates(Badge<Client>)
{
    if (on_certificate_requested) {
        auto result = on_certificate_requested();
        if (!m_client->set_certificate({}, *this, result.certificate, result.key)) {
            dbg() << "Download: set_certificate failed";
        }
    }
}
}
