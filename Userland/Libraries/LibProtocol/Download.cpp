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

#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>

namespace Protocol {

Download::Download(Client& client, i32 download_id)
    : m_client(client)
    , m_download_id(download_id)
{
}

bool Download::stop()
{
    return m_client->stop_download({}, *this);
}

void Download::stream_into(OutputStream& stream)
{
    ASSERT(!m_internal_stream_data);

    auto notifier = Core::Notifier::construct(fd(), Core::Notifier::Read);

    m_internal_stream_data = make<InternalStreamData>(fd());
    m_internal_stream_data->read_notifier = notifier;

    auto user_on_finish = move(on_finish);
    on_finish = [this](auto success, auto total_size) {
        m_internal_stream_data->success = success;
        m_internal_stream_data->total_size = total_size;
        m_internal_stream_data->download_done = true;
    };

    notifier->on_ready_to_read = [this, &stream, user_on_finish = move(user_on_finish)] {
        constexpr size_t buffer_size = PAGE_SIZE;
        static char buf[buffer_size];
        auto nread = m_internal_stream_data->read_stream.read({ buf, buffer_size });
        if (!stream.write_or_error({ buf, nread })) {
            // FIXME: What do we do here?
            TODO();
        }

        if (m_internal_stream_data->read_stream.eof() && m_internal_stream_data->download_done) {
            m_internal_stream_data->read_notifier->close();
            user_on_finish(m_internal_stream_data->success, m_internal_stream_data->total_size);
        } else {
            m_internal_stream_data->read_stream.handle_any_error();
        }
    };
}

void Download::set_should_buffer_all_input(bool value)
{
    if (m_should_buffer_all_input == value)
        return;

    if (m_internal_buffered_data && !value) {
        m_internal_buffered_data = nullptr;
        m_should_buffer_all_input = false;
        return;
    }

    ASSERT(!m_internal_stream_data);
    ASSERT(!m_internal_buffered_data);
    ASSERT(on_buffered_download_finish); // Not having this set makes no sense.
    m_internal_buffered_data = make<InternalBufferedData>(fd());
    m_should_buffer_all_input = true;

    on_headers_received = [this](auto& headers, auto response_code) {
        m_internal_buffered_data->response_headers = headers;
        m_internal_buffered_data->response_code = move(response_code);
    };

    on_finish = [this](auto success, u32 total_size) {
        auto output_buffer = m_internal_buffered_data->payload_stream.copy_into_contiguous_buffer();
        on_buffered_download_finish(
            success,
            total_size,
            m_internal_buffered_data->response_headers,
            m_internal_buffered_data->response_code,
            output_buffer);
    };

    stream_into(m_internal_buffered_data->payload_stream);
}

void Download::did_finish(Badge<Client>, bool success, u32 total_size)
{
    if (!on_finish)
        return;

    on_finish(success, total_size);
}

void Download::did_progress(Badge<Client>, Optional<u32> total_size, u32 downloaded_size)
{
    if (on_progress)
        on_progress(total_size, downloaded_size);
}

void Download::did_receive_headers(Badge<Client>, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> response_code)
{
    if (on_headers_received)
        on_headers_received(response_headers, response_code);
}

void Download::did_request_certificates(Badge<Client>)
{
    if (on_certificate_requested) {
        auto result = on_certificate_requested();
        if (!m_client->set_certificate({}, *this, result.certificate, result.key)) {
            dbgln("Download: set_certificate failed");
        }
    }
}
}
