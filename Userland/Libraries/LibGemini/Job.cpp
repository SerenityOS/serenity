/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/Stream.h>
#include <LibGemini/GeminiResponse.h>
#include <LibGemini/Job.h>
#include <unistd.h>

namespace Gemini {

Job::Job(const GeminiRequest& request, Core::Stream::Stream& output_stream)
    : Core::NetworkJob(output_stream)
    , m_request(request)
{
}

Job::~Job()
{
}

void Job::start(Core::Stream::Socket& socket)
{
    VERIFY(!m_socket);
    m_socket = verify_cast<Core::Stream::BufferedSocketBase>(&socket);
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

String Job::read_line(size_t size)
{
    ByteBuffer buffer = ByteBuffer::create_uninitialized(size).release_value_but_fixme_should_propagate_errors();
    auto nread = MUST(m_socket->read_until(buffer, "\r\n"sv));
    return String::copy(buffer.span().slice(0, nread));
}

ByteBuffer Job::receive(size_t size)
{
    ByteBuffer buffer = ByteBuffer::create_uninitialized(size).release_value_but_fixme_should_propagate_errors();
    auto nread = MUST(m_socket->read(buffer));
    return buffer.slice(0, nread);
}

bool Job::can_read() const
{
    return MUST(m_socket->can_read_without_blocking());
}

bool Job::write(ReadonlyBytes bytes)
{
    return m_socket->write_or_error(bytes);
}

void Job::flush_received_buffers()
{
    for (size_t i = 0; i < m_received_buffers.size(); ++i) {
        auto& payload = m_received_buffers[i];
        auto result = do_write(payload);
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
        payload = payload.slice(written, payload.size() - written);
        return;
    }
}

void Job::on_socket_connected()
{
    auto raw_request = m_request.to_raw_request();

    if constexpr (JOB_DEBUG) {
        dbgln("Job: raw_request:");
        dbgln("{}", String::copy(raw_request));
    }
    bool success = write(raw_request);
    if (!success)
        deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });

    register_on_ready_to_read([this] {
        if (is_cancelled())
            return;

        if (m_state == State::InStatus) {
            if (!can_read_line())
                return;

            auto line = read_line(PAGE_SIZE);
            if (line.is_null()) {
                dbgln("Job: Expected status line");
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::TransmissionFailed); });
            }

            auto parts = line.split_limit(' ', 2);
            if (parts.size() != 2) {
                dbgln("Job: Expected 2-part status line, got '{}'", line);
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            auto status = parts[0].to_uint();
            if (!status.has_value()) {
                dbgln("Job: Expected numeric status code");
                return deferred_invoke([this] { did_fail(Core::NetworkJob::Error::ProtocolFailed); });
            }

            m_status = status.value();
            m_meta = parts[1];

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

            auto payload = receive(read_size);
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
}
