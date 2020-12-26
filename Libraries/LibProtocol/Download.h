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
#include <AK/FileStream.h>
#include <AK/Function.h>
#include <AK/MemoryStream.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/Notifier.h>
#include <LibIPC/Forward.h>

namespace Protocol {

class Client;

class Download : public RefCounted<Download> {
public:
    struct CertificateAndKey {
        String certificate;
        String key;
    };

    static NonnullRefPtr<Download> create_from_id(Badge<Client>, Client& client, i32 download_id)
    {
        return adopt(*new Download(client, download_id));
    }

    int id() const { return m_download_id; }
    int fd() const { return m_fd; }
    bool stop();

    void stream_into(OutputStream&);

    bool should_buffer_all_input() const { return m_should_buffer_all_input; }
    /// Note: Will override `on_finish', and `on_headers_received', and expects `on_buffered_download_finish' to be set!
    void set_should_buffer_all_input(bool);

    /// Note: Must be set before `set_should_buffer_all_input(true)`.
    Function<void(bool success, u32 total_size, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> response_code, ReadonlyBytes payload)> on_buffered_download_finish;
    Function<void(bool success, u32 total_size)> on_finish;
    Function<void(Optional<u32> total_size, u32 downloaded_size)> on_progress;
    Function<void(const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> response_code)> on_headers_received;
    Function<CertificateAndKey()> on_certificate_requested;

    void did_finish(Badge<Client>, bool success, u32 total_size);
    void did_progress(Badge<Client>, Optional<u32> total_size, u32 downloaded_size);
    void did_receive_headers(Badge<Client>, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> response_code);
    void did_request_certificates(Badge<Client>);

    RefPtr<Core::Notifier>& write_notifier(Badge<Client>) { return m_write_notifier; }
    void set_download_fd(Badge<Client>, int fd) { m_fd = fd; }

private:
    explicit Download(Client&, i32 download_id);
    WeakPtr<Client> m_client;
    int m_download_id { -1 };
    RefPtr<Core::Notifier> m_write_notifier;
    int m_fd { -1 };
    bool m_should_buffer_all_input { false };

    struct InternalBufferedData {
        InternalBufferedData(int fd)
            : read_stream(fd)
        {
        }

        InputFileStream read_stream;
        DuplexMemoryStream payload_stream;
        HashMap<String, String, CaseInsensitiveStringTraits> response_headers;
        Optional<u32> response_code;
    };

    struct InternalStreamData {
        InternalStreamData(int fd)
            : read_stream(fd)
        {
        }

        InputFileStream read_stream;
        RefPtr<Core::Notifier> read_notifier;
        bool success;
        u32 total_size { 0 };
        bool download_done { false };
    };

    OwnPtr<InternalBufferedData> m_internal_buffered_data;
    OwnPtr<InternalStreamData> m_internal_stream_data;
};

}
