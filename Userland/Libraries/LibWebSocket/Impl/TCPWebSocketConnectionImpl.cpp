/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
    bool success = m_socket->connect(connection.url().host(), connection.url().port_or_default());
    if (!success) {
        deferred_invoke([this] {
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
