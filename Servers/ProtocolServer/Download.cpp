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
#include <ProtocolServer/Download.h>
#include <ProtocolServer/PSClientConnection.h>

// FIXME: What about rollover?
static i32 s_next_id = 1;

static HashMap<i32, RefPtr<Download>>& all_downloads()
{
    static HashMap<i32, RefPtr<Download>> map;
    return map;
}

Download* Download::find_by_id(i32 id)
{
    return const_cast<Download*>(all_downloads().get(id).value_or(nullptr));
}

Download::Download(PSClientConnection& client)
    : m_id(s_next_id++)
    , m_client(client.make_weak_ptr())
{
    all_downloads().set(m_id, this);
}

Download::~Download()
{
}

void Download::stop()
{
    all_downloads().remove(m_id);
}

void Download::set_payload(const ByteBuffer& payload)
{
    m_payload = payload;
    m_total_size = payload.size();
}

void Download::did_finish(bool success)
{
    if (!m_client) {
        dbg() << "Download::did_finish() after the client already disconnected.";
        return;
    }
    m_client->did_finish_download({}, *this, success);
    all_downloads().remove(m_id);
}

void Download::did_progress(size_t total_size, size_t downloaded_size)
{
    if (!m_client) {
        // FIXME: We should also abort the download in this situation, I guess!
        dbg() << "Download::did_progress() after the client already disconnected.";
        return;
    }
    m_total_size = total_size;
    m_downloaded_size = downloaded_size;
    m_client->did_progress_download({}, *this);
}
