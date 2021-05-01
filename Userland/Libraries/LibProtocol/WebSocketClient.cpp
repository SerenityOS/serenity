/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibProtocol/WebSocket.h>
#include <LibProtocol/WebSocketClient.h>

namespace Protocol {

WebSocketClient::WebSocketClient()
    : IPC::ServerConnection<WebSocketClientEndpoint, WebSocketServerEndpoint>(*this, "/tmp/portal/websocket")
{
    handshake();
}

void WebSocketClient::handshake()
{
    send_sync<Messages::WebSocketServer::Greet>();
}

RefPtr<WebSocket> WebSocketClient::connect(const URL& url, const String& origin, const Vector<String>& protocols, const Vector<String>& extensions, const HashMap<String, String>& request_headers)
{
    IPC::Dictionary header_dictionary;
    for (auto& it : request_headers)
        header_dictionary.add(it.key, it.value);
    auto response = send_sync<Messages::WebSocketServer::Connect>(url, origin, protocols, extensions, header_dictionary);
    auto connection_id = response->connection_id();
    if (connection_id < 0)
        return nullptr;
    auto connection = WebSocket::create_from_id({}, *this, connection_id);
    m_connections.set(connection_id, connection);
    return connection;
}

u32 WebSocketClient::ready_state(Badge<WebSocket>, WebSocket& connection)
{
    if (!m_connections.contains(connection.id()))
        return (u32)WebSocket::ReadyState::Closed;
    return send_sync<Messages::WebSocketServer::ReadyState>(connection.id())->ready_state();
}

void WebSocketClient::send(Badge<WebSocket>, WebSocket& connection, ByteBuffer data, bool is_text)
{
    if (!m_connections.contains(connection.id()))
        return;
    post_message(Messages::WebSocketServer::Send(connection.id(), is_text, move(data)));
}

void WebSocketClient::close(Badge<WebSocket>, WebSocket& connection, u16 code, String message)
{
    if (!m_connections.contains(connection.id()))
        return;
    post_message(Messages::WebSocketServer::Close(connection.id(), code, move(message)));
}

bool WebSocketClient::set_certificate(Badge<WebSocket>, WebSocket& connection, String certificate, String key)
{
    if (!m_connections.contains(connection.id()))
        return false;
    return send_sync<Messages::WebSocketServer::SetCertificate>(connection.id(), move(certificate), move(key))->success();
}

void WebSocketClient::handle(const Messages::WebSocketClient::Connected& message)
{
    auto maybe_connection = m_connections.get(message.connection_id());
    if (maybe_connection.has_value())
        maybe_connection.value()->did_open({});
}

void WebSocketClient::handle(const Messages::WebSocketClient::Received& message)
{
    auto maybe_connection = m_connections.get(message.connection_id());
    if (maybe_connection.has_value())
        maybe_connection.value()->did_receive({}, message.data(), message.is_text());
}

void WebSocketClient::handle(const Messages::WebSocketClient::Errored& message)
{
    auto maybe_connection = m_connections.get(message.connection_id());
    if (maybe_connection.has_value())
        maybe_connection.value()->did_error({}, message.message());
}

void WebSocketClient::handle(const Messages::WebSocketClient::Closed& message)
{
    auto maybe_connection = m_connections.get(message.connection_id());
    if (maybe_connection.has_value())
        maybe_connection.value()->did_close({}, message.code(), message.reason(), message.clean());
}

void WebSocketClient::handle(const Messages::WebSocketClient::CertificateRequested& message)
{
    auto maybe_connection = m_connections.get(message.connection_id());
    if (maybe_connection.has_value())
        maybe_connection.value()->did_request_certificates({});
}

}
