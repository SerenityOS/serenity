/*
 * Copyright (c) 2021, The SerenityOS developers.
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

#include <LibWebSocket/Impl/TCPWebSocketConnectionImpl.h>

namespace WebSocket {

TCPWebSocketConnectionImpl::TCPWebSocketConnectionImpl(Core::Object* parent)
    : AbstractWebSocketImpl(parent)
{
}

TCPWebSocketConnectionImpl::~TCPWebSocketConnectionImpl()
{
    discard_connection();
}

void TCPWebSocketConnectionImpl::connect(ConnectionInfo const& connection)
{
    VERIFY(!m_socket);
    VERIFY(on_connected);
    VERIFY(on_connection_error);
    VERIFY(on_ready_to_read);
    m_socket = Core::TCPSocket::construct(this);

    m_notifier = Core::Notifier::construct(m_socket->fd(), Core::Notifier::Read);
    m_notifier->on_ready_to_read = [this] {
        on_ready_to_read();
    };

    m_socket->on_connected = [this] {
        on_connected();
    };
    bool success = m_socket->connect(connection.url().host(), connection.url().port());
    if (!success) {
        deferred_invoke([this](auto&) {
            on_connection_error();
        });
    }
}

bool TCPWebSocketConnectionImpl::send(ReadonlyBytes data)
{
    return m_socket->write(data);
}

bool TCPWebSocketConnectionImpl::can_read_line()
{
    return m_socket->can_read_line();
}

String TCPWebSocketConnectionImpl::read_line(size_t size)
{
    return m_socket->read_line(size);
}

bool TCPWebSocketConnectionImpl::can_read()
{
    return m_socket->can_read();
}

ByteBuffer TCPWebSocketConnectionImpl::read(int max_size)
{
    return m_socket->read(max_size);
}

bool TCPWebSocketConnectionImpl::eof()
{
    return m_socket->eof();
}

void TCPWebSocketConnectionImpl::discard_connection()
{
    if (!m_socket)
        return;
    m_socket->on_ready_to_read = nullptr;
    remove_child(*m_socket);
    m_socket = nullptr;
}

}
