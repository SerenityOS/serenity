/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibWebSocket/ConnectionInfo.h>
#include <LibWebSocket/Message.h>
#include <WebSocket/ClientConnection.h>
#include <WebSocket/WebSocketClientEndpoint.h>

namespace WebSocket {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<WebSocketClientEndpoint, WebSocketServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
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

OwnPtr<Messages::WebSocketServer::GreetResponse> ClientConnection::handle(const Messages::WebSocketServer::Greet&)
{
    return make<Messages::WebSocketServer::GreetResponse>();
}

OwnPtr<Messages::WebSocketServer::ConnectResponse> ClientConnection::handle(const Messages::WebSocketServer::Connect& message)
{
    const auto& url = message.url();
    if (!url.is_valid()) {
        dbgln("WebSocket::Connect: Invalid URL requested: '{}'", url);
        return make<Messages::WebSocketServer::ConnectResponse>(-1);
    }

    ConnectionInfo connection_info(url);
    connection_info.set_origin(message.origin());
    connection_info.set_protocols(message.protocols());
    connection_info.set_extensions(message.extensions());

    Vector<ConnectionInfo::Header> headers;
    for (const auto& header : message.additional_request_headers().entries()) {
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
    return make<Messages::WebSocketServer::ConnectResponse>(id);
}

OwnPtr<Messages::WebSocketServer::ReadyStateResponse> ClientConnection::handle(const Messages::WebSocketServer::ReadyState& message)
{
    RefPtr<WebSocket> connection = m_connections.get(message.connection_id()).value_or({});
    if (connection) {
        return make<Messages::WebSocketServer::ReadyStateResponse>((u32)connection->ready_state());
    }
    return make<Messages::WebSocketServer::ReadyStateResponse>((u32)ReadyState::Closed);
}

void ClientConnection::handle(const Messages::WebSocketServer::Send& message)
{
    RefPtr<WebSocket> connection = m_connections.get(message.connection_id()).value_or({});
    if (connection && connection->ready_state() == ReadyState::Open) {
        Message websocket_message(message.data(), message.is_text());
        connection->send(websocket_message);
    }
}

void ClientConnection::handle(const Messages::WebSocketServer::Close& message)
{
    RefPtr<WebSocket> connection = m_connections.get(message.connection_id()).value_or({});
    if (connection && connection->ready_state() == ReadyState::Open)
        connection->close(message.code(), message.reason());
}

OwnPtr<Messages::WebSocketServer::SetCertificateResponse> ClientConnection::handle(const Messages::WebSocketServer::SetCertificate& message)
{
    RefPtr<WebSocket> connection = m_connections.get(message.connection_id()).value_or({});
    bool success = false;
    if (connection) {
        // NO OP here
        // connection->set_certificate(message.certificate(), message.key());
        success = true;
    }
    return make<Messages::WebSocketServer::SetCertificateResponse>(success);
}

void ClientConnection::did_connect(i32 connection_id)
{
    post_message(Messages::WebSocketClient::Connected(connection_id));
}

void ClientConnection::did_receive_message(i32 connection_id, Message message)
{
    post_message(Messages::WebSocketClient::Received(connection_id, message.is_text(), message.data()));
}

void ClientConnection::did_error(i32 connection_id, i32 message)
{
    post_message(Messages::WebSocketClient::Errored(connection_id, message));
}

void ClientConnection::did_close(i32 connection_id, u16 code, String reason, bool was_clean)
{
    post_message(Messages::WebSocketClient::Closed(connection_id, code, reason, was_clean));
    deferred_invoke([this, connection_id] {
        m_connections.remove(connection_id);
    });
}

void ClientConnection::did_request_certificates(i32 connection_id)
{
    post_message(Messages::WebSocketClient::CertificateRequested(connection_id));
}

}
