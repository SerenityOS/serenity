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

#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>

namespace AK {
class SharedBuffer;
}

namespace Protocol {

class Client;

class Download : public RefCounted<Download> {
public:
    static NonnullRefPtr<Download> create_from_id(Badge<Client>, Client& client, i32 download_id)
    {
        return adopt(*new Download(client, download_id));
    }

    int id() const { return m_download_id; }
    bool stop();

    Function<void(bool success, const ByteBuffer& payload, RefPtr<SharedBuffer> payload_storage)> on_finish;
    Function<void(u32 total_size, u32 downloaded_size)> on_progress;

    void did_finish(Badge<Client>, bool success, u32 total_size, i32 shared_buffer_id);
    void did_progress(Badge<Client>, u32 total_size, u32 downloaded_size);

private:
    explicit Download(Client&, i32 download_id);
    WeakPtr<Client> m_client;
    int m_download_id { -1 };
};

}
