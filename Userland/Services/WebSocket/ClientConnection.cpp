/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWebSocket/ConnectionInfo.h>
#include <LibWebSocket/Message.h>
#include <WebSocket/ClientConnection.h>
#include <WebSocket/WebSocketClientEndpoint.h>

namespace WebSocket {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket)
    : IPC::ClientConnection<WebSocketClientEndpoint, WebSocketServerEndpoint>(*this, move(socket), 1)
{
    s_connections.set(1, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
    if (s_connections.is_empty())
        Core::EventLoop::current().quit(0);
}

Messages::WebSocketServer::ConnectResponse ClientConnection::connect(URL const& url, String const& origin,
    Vector<String> const& protocols, Vector<String> const& extensions, IPC::Dictionary const& additional_request_headers)
{
    if (!url.is_valid()) {
        dbgln("WebSocket::Connect: Invalid URL requested: '{}'", url);
        return -1;
    }

    ConnectionInfo connection_info(url);
    connection_info.set_origin(origin);
    connection_info.set_protocols(protocols);
    connection_info.set_extensions(extensions);

    Vector<ConnectionInfo::Header> headers;
    for (auto const& header : additional_request_headers.entries()) {
        headers.append({ header.key, header.value });
    }
    connection_info.set_headers(headers);

    VERIFY(m_connection_ids < NumericLimits<i32>::max());
    auto id = ++m_connection_ids;
    auto connection = WebSocket::create(move(connection_info));
    connection->on_open = [this, id]() {
        did_connect(id);
    };
    connection->on_message = [this, id](auto message) {
        did_receive_message(id, move(message));
    };
    connection->on_error = [this, id](auto message) {
        did_error(id, (i32)message);
    };
    connection->on_close = [this, id](u16 code, String reason, bool was_clean) {
        did_close(id, code, move(reason), was_clean);
    };

    connection->start();
    m_connections.set(id, move(connection));
    return id;
}

Messages::WebSocketServer::ReadyStateResponse ClientConnection::ready_state(i32 connection_id)
{
    RefPtr<WebSocket> connection = m_connections.get(connection_id).value_or({});
    if (connection) {
        return (u32)connection->ready_state();
    }
    return (u32)ReadyState::Closed;
}

void ClientConnection::send(i32 connection_id, bool is_text, ByteBuffer const& data)
{
    RefPtr<WebSocket> connection = m_connections.get(connection_id).value_or({});
    if (connection && connection->ready_state() == ReadyState::Open) {
        Message websocket_message(data, is_text);
        connection->send(websocket_message);
    }
}

void ClientConnection::close(i32 connection_id, u16 code, String const& reason)
{
    RefPtr<WebSocket> connection = m_connections.get(connection_id).value_or({});
    if (connection && connection->ready_state() == ReadyState::Open)
        connection->close(code, reason);
}

Messages::WebSocketServer::SetCertificateResponse ClientConnection::set_certificate(i32 connection_id,
    [[maybe_unused]] String const& certificate, [[maybe_unused]] String const& key)
{
    RefPtr<WebSocket> connection = m_connections.get(connection_id).value_or({});
    bool success = false;
    if (connection) {
        // NO OP here
        // connection->set_certificate(certificate, key);
        success = true;
    }
    return success;
}

void ClientConnection::did_connect(i32 connection_id)
{
    async_connected(connection_id);
}

void ClientConnection::did_receive_message(i32 connection_id, Message message)
{
    async_received(connection_id, message.is_text(), message.data());
}

void ClientConnection::did_error(i32 connection_id, i32 message)
{
    async_errored(connection_id, message);
}

void ClientConnection::did_close(i32 connection_id, u16 code, String reason, bool was_clean)
{
    async_closed(connection_id, code, reason, was_clean);
    deferred_invoke([this, connection_id] {
        m_connections.remove(connection_id);
    });
}

void ClientConnection::did_request_certificates(i32 connection_id)
{
    async_certificate_requested(connection_id);
}

}
