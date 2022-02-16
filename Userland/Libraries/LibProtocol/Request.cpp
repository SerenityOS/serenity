/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>

namespace Protocol {

Request::Request(RequestClient& client, i32 request_id)
    : m_client(client)
    , m_request_id(request_id)
{
}

bool Request::stop()
{
    return m_client->stop_request({}, *this);
}

template<typename T>
void Request::stream_into_impl(T& stream)
{
    VERIFY(!m_internal_stream_data);

    m_internal_stream_data = make<InternalStreamData>(MUST(Core::Stream::File::adopt_fd(fd(), Core::Stream::OpenMode::Read)));
    m_internal_stream_data->read_notifier = Core::Notifier::construct(fd(), Core::Notifier::Read);

    auto user_on_finish = move(on_finish);
    on_finish = [this](auto success, auto total_size) {
        m_internal_stream_data->success = success;
        m_internal_stream_data->total_size = total_size;
        m_internal_stream_data->request_done = true;
        m_internal_stream_data->on_finish();
    };

    m_internal_stream_data->on_finish = [this, user_on_finish = move(user_on_finish)] {
        if (!m_internal_stream_data->user_finish_called && m_internal_stream_data->read_stream->is_eof()) {
            m_internal_stream_data->user_finish_called = true;
            user_on_finish(m_internal_stream_data->success, m_internal_stream_data->total_size);
        }
    };
    m_internal_stream_data->read_notifier->on_ready_to_read = [this, &stream] {
        constexpr size_t buffer_size = 16 * KiB;
        static char buf[buffer_size];
        do {
            auto result = m_internal_stream_data->read_stream->read({ buf, buffer_size });
            if (result.is_error() && (!result.error().is_errno() || (result.error().is_errno() && result.error().code() != EINTR)))
                break;
            if (result.is_error())
                continue;
            auto nread = result.value();
            if (nread == 0)
                break;
            if (!stream.write_or_error({ buf, nread })) {
                // FIXME: What do we do here?
                TODO();
            }
        } while (true);

        if (m_internal_stream_data->read_stream->is_eof() && m_internal_stream_data->request_done) {
            m_internal_stream_data->read_notifier->close();
            m_internal_stream_data->on_finish();
        }
    };
}

void Request::stream_into(Core::Stream::Stream& stream)
{
    stream_into_impl(stream);
}

void Request::stream_into(OutputStream& stream)
{
    stream_into_impl(stream);
}

void Request::set_should_buffer_all_input(bool value)
{
    if (m_should_buffer_all_input == value)
        return;

    if (m_internal_buffered_data && !value) {
        m_internal_buffered_data = nullptr;
        m_should_buffer_all_input = false;
        return;
    }

    VERIFY(!m_internal_stream_data);
    VERIFY(!m_internal_buffered_data);
    VERIFY(on_buffered_request_finish); // Not having this set makes no sense.
    m_internal_buffered_data = make<InternalBufferedData>();
    m_should_buffer_all_input = true;

    on_headers_received = [this](auto& headers, auto response_code) {
        m_internal_buffered_data->response_headers = headers;
        m_internal_buffered_data->response_code = move(response_code);
    };

    on_finish = [this](auto success, u32 total_size) {
        auto output_buffer = m_internal_buffered_data->payload_stream.copy_into_contiguous_buffer();
        on_buffered_request_finish(
            success,
            total_size,
            m_internal_buffered_data->response_headers,
            m_internal_buffered_data->response_code,
            output_buffer);
    };

    stream_into(m_internal_buffered_data->payload_stream);
}

void Request::did_finish(Badge<RequestClient>, bool success, u32 total_size)
{
    if (!on_finish)
        return;

    on_finish(success, total_size);
}

void Request::did_progress(Badge<RequestClient>, Optional<u32> total_size, u32 downloaded_size)
{
    if (on_progress)
        on_progress(total_size, downloaded_size);
}

void Request::did_receive_headers(Badge<RequestClient>, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> response_code)
{
    if (on_headers_received)
        on_headers_received(response_headers, response_code);
}

void Request::did_request_certificates(Badge<RequestClient>)
{
    if (on_certificate_requested) {
        auto result = on_certificate_requested();
        if (!m_client->set_certificate({}, *this, result.certificate, result.key)) {
            dbgln("Request: set_certificate failed");
        }
    }
}
}
