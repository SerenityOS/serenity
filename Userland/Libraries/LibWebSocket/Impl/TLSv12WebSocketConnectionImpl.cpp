/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebSocket/Impl/TLSv12WebSocketConnectionImpl.h>

namespace WebSocket {

TLSv12WebSocketConnectionImpl::TLSv12WebSocketConnectionImpl(Core::Object* parent)
    : AbstractWebSocketImpl(parent)
{
}

TLSv12WebSocketConnectionImpl::~TLSv12WebSocketConnectionImpl()
{
    discard_connection();
}

void TLSv12WebSocketConnectionImpl::connect(ConnectionInfo const& connection)
{
    VERIFY(!m_socket);
    VERIFY(on_connected);
    VERIFY(on_connection_error);
    VERIFY(on_ready_to_read);
    m_socket = TLS::TLSv12::connect(connection.url().host(), connection.url().port_or_default()).release_value_but_fixme_should_propagate_errors();

    m_socket->on_tls_error = [this](TLS::AlertDescription) {
        on_connection_error();
    };
    m_socket->on_ready_to_read = [this] {
        on_ready_to_read();
    };
    m_socket->on_tls_finished = [this] {
        on_connection_error();
    };
    m_socket->on_tls_certificate_request = [](auto&) {
        // FIXME : Once we handle TLS certificate requests, handle it here as well.
    };
    on_connected();
}

bool TLSv12WebSocketConnectionImpl::send(ReadonlyBytes data)
{
    return m_socket->write_or_error(data);
}

bool TLSv12WebSocketConnectionImpl::can_read_line()
{
    return m_socket->can_read_line();
}

String TLSv12WebSocketConnectionImpl::read_line(size_t size)
{
    return m_socket->read_line(size);
}

bool TLSv12WebSocketConnectionImpl::can_read()
{
    return m_socket->can_read();
}

ByteBuffer TLSv12WebSocketConnectionImpl::read(int max_size)
{
    auto buffer = ByteBuffer::create_uninitialized(max_size).release_value_but_fixme_should_propagate_errors();
    auto nread = m_socket->read(buffer).release_value_but_fixme_should_propagate_errors();
    return buffer.slice(0, nread);
}

bool TLSv12WebSocketConnectionImpl::eof()
{
    return m_socket->is_eof();
}

void TLSv12WebSocketConnectionImpl::discard_connection()
{
    if (!m_socket)
        return;
    m_socket->on_tls_error = nullptr;
    m_socket->on_tls_finished = nullptr;
    m_socket->on_tls_certificate_request = nullptr;
    m_socket->on_ready_to_read = nullptr;
    m_socket = nullptr;
}

}
