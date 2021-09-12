/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/TCPSocket.h>
#include <LibHTTP/HttpJob.h>
#include <stdio.h>
#include <unistd.h>

namespace HTTP {
void HttpJob::start()
{
    VERIFY(!m_socket);
    m_socket = Core::TCPSocket::construct(this);
    m_socket->on_connected = [this] {
        dbgln_if(CHTTPJOB_DEBUG, "HttpJob: on_connected callback");
        on_socket_connected();
    };
    m_socket->on_error = [this] {
        dbgln_if(CHTTPJOB_DEBUG, "HttpJob: on_error callback");
        deferred_invoke([this] {
            did_fail(Core::NetworkJob::Error::ConnectionFailed);
        });
    };
    bool success = m_socket->connect(m_request.url().host(), m_request.url().port());
    if (!success) {
        deferred_invoke([this] {
            return did_fail(Core::NetworkJob::Error::ConnectionFailed);
        });
    }
}

void HttpJob::shutdown()
{
    if (!m_socket)
        return;
    m_socket->on_ready_to_read = nullptr;
    m_socket->on_connected = nullptr;
    remove_child(*m_socket);
    m_socket = nullptr;
}

void HttpJob::register_on_ready_to_read(Function<void()> callback)
{
    m_socket->on_ready_to_read = move(callback);
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
