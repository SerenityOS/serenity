/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebSocket/Impl/WebSocketImpl.h>

namespace WebSocket {

WebSocketImpl::WebSocketImpl(Core::Object* parent)
    : Object(parent)
{
}

WebSocketImpl::~WebSocketImpl()
{
}

void WebSocketImpl::connect(ConnectionInfo const& connection_info)
{
    VERIFY(!m_socket);
    VERIFY(on_connected);
    VERIFY(on_connection_error);
    VERIFY(on_ready_to_read);
    auto socket_result = [&]() -> ErrorOr<NonnullOwnPtr<Core::Stream::BufferedSocketBase>> {
        if (connection_info.is_secure()) {
            TLS::Options options;
            options.set_alert_handler([this](auto) {
                on_connection_error();
            });
            return TRY(Core::Stream::BufferedSocket<TLS::TLSv12>::create(
                TRY(TLS::TLSv12::connect(connection_info.url().host(), connection_info.url().port_or_default(), move(options)))));
        }

        return TRY(Core::Stream::BufferedTCPSocket::create(
            TRY(Core::Stream::TCPSocket::connect(connection_info.url().host(), connection_info.url().port_or_default()))));
    }();

    if (socket_result.is_error()) {
        deferred_invoke([this] {
            on_connection_error();
        });
        return;
    }

    m_socket = socket_result.release_value();

    m_socket->on_ready_to_read = [this] {
        on_ready_to_read();
    };

    deferred_invoke([this] {
        on_connected();
    });
}

ErrorOr<ByteBuffer> WebSocketImpl::read(int max_size)
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(max_size));
    auto nread = TRY(m_socket->read(buffer));
    return buffer.slice(0, nread);
}

ErrorOr<String> WebSocketImpl::read_line(size_t size)
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(size));
    auto nread = TRY(m_socket->read_line(buffer));
    return String::copy(buffer.span().slice(0, nread));
}

}
