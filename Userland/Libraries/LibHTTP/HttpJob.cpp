/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/TCPSocket.h>
#include <LibHTTP/HttpJob.h>
#include <LibHTTP/HttpResponse.h>
#include <stdio.h>
#include <unistd.h>

namespace HTTP {
void HttpJob::start(NonnullRefPtr<Core::Socket> socket)
{
    VERIFY(!m_socket);
    m_socket = move(socket);
    m_socket->on_error = [this] {
        dbgln_if(CHTTPJOB_DEBUG, "HttpJob: on_error callback");
        deferred_invoke([this] {
            did_fail(Core::NetworkJob::Error::ConnectionFailed);
        });
    };
    m_socket->set_idle(false);
    if (m_socket->is_connected()) {
        dbgln("Reusing previous connection for {}", url());
        deferred_invoke([this] {
            dbgln_if(CHTTPJOB_DEBUG, "HttpJob: on_connected callback");
            on_socket_connected();
        });
    } else {
        dbgln("Creating new connection for {}", url());
        m_socket->on_connected = [this] {
            dbgln_if(CHTTPJOB_DEBUG, "HttpJob: on_connected callback");
            on_socket_connected();
        };
        bool success = m_socket->connect(m_request.url().host(), m_request.url().port_or_default());
        if (!success) {
            deferred_invoke([this] {
                return did_fail(Core::NetworkJob::Error::ConnectionFailed);
            });
        }
    };
}

void HttpJob::shutdown(ShutdownMode mode)
{
    if (!m_socket)
        return;
    if (mode == ShutdownMode::CloseSocket) {
        m_socket->close();
    } else {
        m_socket->on_ready_to_read = nullptr;
        m_socket->on_connected = nullptr;
        m_socket->set_idle(true);
        m_socket = nullptr;
    }
}

void HttpJob::register_on_ready_to_read(Function<void()> callback)
{
    m_socket->on_ready_to_read = [callback = move(callback), this] {
        callback();
        // As IODevice so graciously buffers everything, there's a possible
        // scenario where it buffers the entire response, and we get stuck waiting
        // for select() in the notifier (which will never return).
        // So handle this case by exhausting the buffer here.
        if (m_socket->can_read_only_from_buffer() && m_state != State::Finished && !has_error()) {
            deferred_invoke([this] {
                if (m_socket && m_socket->on_ready_to_read)
                    m_socket->on_ready_to_read();
            });
        }
    };
}

void HttpJob::register_on_ready_to_write(Function<void()> callback)
{
    // There is no need to wait, the connection is already established
    callback();
}

bool HttpJob::can_read_line() const
{
    return m_socket->can_read_line();
}

String HttpJob::read_line(size_t size)
{
    return m_socket->read_line(size);
}

ByteBuffer HttpJob::receive(size_t size)
{
    return m_socket->receive(size);
}

bool HttpJob::can_read() const
{
    return m_socket->can_read();
}

bool HttpJob::eof() const
{
    return m_socket->eof();
}

bool HttpJob::write(ReadonlyBytes bytes)
{
    return m_socket->write(bytes);
}

}
