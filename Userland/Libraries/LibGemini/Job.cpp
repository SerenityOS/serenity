/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Error.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibCore/EventLoop.h>
#include <LibGemini/GeminiResponse.h>
#include <LibGemini/Job.h>
#include <unistd.h>

namespace Gemini {

Job::Job(GeminiRequest const& request, Core::File& output_stream)
    : Core::NetworkJob(output_stream)
    , m_request(request)
{
}

void Job::start(Core::BufferedSocketBase& socket)
{
    VERIFY(!m_socket);
    m_socket = &socket;
    on_socket_connected();
}

void Job::shutdown(ShutdownMode mode)
{
    if (!m_socket)
        return;
    if (mode == ShutdownMode::CloseSocket) {
        m_socket->close();
    } else {
        m_socket->on_ready_to_read = nullptr;
        m_socket = nullptr;
    }
}

void Job::register_on_ready_to_read(Function<void()> callback)
{
    m_socket->on_ready_to_read = [this, callback = move(callback)] {
        callback();

        while (can_read()) {
            callback();
        }
    };
}

bool Job::can_read_line() const
{
    return MUST(m_socket->can_read_line());
}

ErrorOr<String> Job::read_line(size_t size)
{
    ByteBuffer buffer = TRY(ByteBuffer::create_uninitialized(size));
    auto bytes_read = TRY(m_socket->read_until(buffer, "\r\n"sv));
    return String::from_utf8(StringView { bytes_read.data(), bytes_read.size() });
}

ErrorOr<ByteBuffer> Job::receive(size_t size)
{
    ByteBuffer buffer = TRY(ByteBuffer::create_uninitialized(size));
    auto nread = TRY(m_socket->read_some(buffer)).size();
    return buffer.slice(0, nread);
}

bool Job::can_read() const
{
    return MUST(m_socket->can_read_without_blocking());
}

bool Job::write(ReadonlyBytes bytes)
{
    return !m_socket->write_until_depleted(bytes).is_error();
}

void Job::flush_received_buffers()
{
    for (size_t i = 0; i < m_received_buffers.size(); ++i) {
        auto& payload = m_received_buffers[i];
        auto result = Core::run_async_in_new_event_loop([&] { return do_write(payload); });
        if (result.is_error()) {
            if (!result.error().is_errno()) {
                dbgln("Job: Failed to flush received buffers: {}", result.error());
                continue;
            }
            if (result.error().code() == EINTR) {
                i--;
                continue;
            }
            break;
        }
        auto written = result.release_value();
        m_buffered_size -= written;
        if (written == payload.size()) {
            // FIXME: Make this a take-first-friendly object?
            m_received_buffers.take_first();
            continue;
        }
        VERIFY(written < payload.size());
        // FIXME: Propagate errors.
        payload = MUST(payload.slice(written, payload.size() - written));
        return;
    }
}

void Job::on_socket_connected()
{
    auto raw_request = m_request.to_raw_request().release_value_but_fixme_should_propagate_errors();

    if constexpr (JOB_DEBUG) {
        dbgln("Job: raw_request:");
        dbgln("{}", ByteString::copy(raw_request));
    }
    bool success = write(raw_request);
    if (!success)
        deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });

    register_on_ready_to_read([this] {
        if (is_cancelled())
            return;
        if (m_state == State::Failed)
            return;

        // https://gemini.circumlunar.space/docs/specification.gmi

        if (m_state == State::InStatus) {
            if (!can_read_line())
                return;

            auto line_or_error = read_line(PAGE_SIZE);
            if (line_or_error.is_error()) {
                dbgln("Job: Error getting status line {}", line_or_error.error());
                m_state = State::Failed;
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }

            auto line = line_or_error.release_value();
            auto view = line.bytes_as_string_view();

            auto first_code_point = line.code_points().begin().peek();
            if (!first_code_point.has_value()) {
                dbgln("Job: empty status line");
                m_state = State::Failed;
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            if (first_code_point.release_value() == 0xFEFF) {
                dbgln("Job: Byte order mark as first character of status line");
                m_state = State::Failed;
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            auto maybe_space_index = view.find(' ');
            if (!maybe_space_index.has_value()) {
                dbgln("Job: Expected 2-part status line, got '{}'", line);
                m_state = State::Failed;
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            auto space_index = maybe_space_index.release_value();
            auto first_part = view.substring_view(0, space_index);
            auto second_part = view.substring_view(space_index + 1);

            auto status = first_part.to_number<unsigned>();
            if (!status.has_value()) {
                dbgln("Job: Expected numeric status code");
                m_state = State::Failed;
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            auto meta_first_code_point = Utf8View(second_part).begin().peek();
            if (meta_first_code_point.release_value() == 0xFEFF) {
                dbgln("Job: Byte order mark as first character of meta");
                m_state = State::Failed;
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            if (second_part.length() > 1024) {
                dbgln("Job: Meta too long");
                m_state = State::Failed;
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            m_status = status.release_value();
            m_meta = second_part;

            if (m_status >= 10 && m_status < 20) {
                m_state = State::Finished;
            } else if (m_status >= 20 && m_status < 30) {
                m_state = State::InBody;
            } else if (m_status >= 30 && m_status < 40) {
                m_state = State::Finished;
            } else if (m_status >= 40 && m_status < 50) {
                m_state = State::Finished;
            } else if (m_status >= 50 && m_status < 60) {
                m_state = State::Finished;
            } else if (m_status >= 60 && m_status < 70) {
                m_state = State::InBody;
            } else {
                dbgln("Job: Expected status between 10 and 69; instead got {}", m_status);
                m_state = State::Failed;
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            if (!can_read()) {
                dbgln("Can't read further :(");
                return;
            }
        }

        VERIFY(m_state == State::InBody || m_state == State::Finished);

        while (MUST(m_socket->can_read_without_blocking())) {
            auto read_size = 64 * KiB;

            auto payload_or_error = receive(read_size);
            if (payload_or_error.is_error()) {
                dbgln("Job: Error in receive {}", payload_or_error.error());
                m_state = State::Failed;
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }
            auto payload = payload_or_error.release_value();

            if (payload.is_empty()) {
                if (m_socket->is_eof()) {
                    finish_up();
                    break;
                }
            }

            m_received_size += payload.size();
            m_buffered_size += payload.size();
            m_received_buffers.append(move(payload));
            flush_received_buffers();

            deferred_invoke([this] { did_progress({}, m_received_size); });

            if (m_socket->is_eof())
                break;
        }

        if (!m_socket->is_open() || m_socket->is_eof()) {
            dbgln_if(JOB_DEBUG, "Connection appears to have closed, finishing up");
            finish_up();
        }
    });
}

void Job::finish_up()
{
    m_state = State::Finished;
    flush_received_buffers();
    if (m_buffered_size != 0) {
        // We have to wait for the client to consume all the downloaded data
        // before we can actually call `did_finish`. in a normal flow, this should
        // never be hit since the client is reading as we are writing, unless there
        // are too many concurrent downloads going on.
        deferred_invoke([this] {
            finish_up();
        });
        return;
    }

    auto response = GeminiResponse::create(m_status, m_meta);
    deferred_invoke([this, response] {
        did_finish(move(response));
    });
}

ErrorOr<size_t> Job::response_length() const
{
    if (m_state != State::Finished)
        return AK::Error::from_string_literal("Gemini response has not finished");

    return m_received_size;
}
}
