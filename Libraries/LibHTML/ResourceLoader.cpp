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
#include <LibCore/CFile.h>
#include <LibHTML/ResourceLoader.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>

ResourceLoader& ResourceLoader::the()
{
    static ResourceLoader* s_the;
    if (!s_the)
        s_the = &ResourceLoader::construct().leak_ref();
    return *s_the;
}

ResourceLoader::ResourceLoader()
    : m_protocol_client(LibProtocol::Client::construct())
{
}

void ResourceLoader::load(const URL& url, Function<void(const ByteBuffer&)> callback)
{
    if (url.protocol() == "file") {
        auto f = CFile::construct();
        f->set_filename(url.path());
        if (!f->open(CIODevice::OpenMode::ReadOnly)) {
            dbg() << "ResourceLoader::load: Error: " << f->error_string();
            callback({});
            return;
        }

        auto data = f->read_all();
        deferred_invoke([data = move(data), callback = move(callback)](auto&) {
            callback(data);
        });
        return;
    }

    if (url.protocol() == "http") {
        auto download = protocol_client().start_download(url.to_string());
        download->on_finish = [this, callback = move(callback)](bool success, const ByteBuffer& payload, auto) {
            --m_pending_loads;
            if (on_load_counter_change)
                on_load_counter_change();
            if (!success) {
                dbg() << "HTTP load failed!";
                callback({});
                return;
            }
            callback(ByteBuffer::copy(payload.data(), payload.size()));
        };
        ++m_pending_loads;
        if (on_load_counter_change)
            on_load_counter_change();
        return;
    }

    dbg() << "Unimplemented protocol: " << url.protocol();
    ASSERT_NOT_REACHED();
}
