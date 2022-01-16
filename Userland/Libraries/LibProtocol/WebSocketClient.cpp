/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibProtocol/WebSocket.h>
#include <LibProtocol/WebSocketClient.h>

namespace Protocol {

WebSocketClient::WebSocketClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
    : IPC::ServerConnection<WebSocketClientEndpoint, WebSocketServerEndpoint>(*this, move(socket))
{
}

RefPtr<WebSocket> WebSocketClient::connect(const URL& url, const String& origin, const Vector<String>& protocols, const Vector<String>& extensions, const HashMap<String, String>& request_headers)
{
    IPC::Dictionary header_dictionary;
    for (auto& it : request_headers)
        header_dictionary.add(it.key, it.value);
    auto connection_id = IPCProxy::connect(url, origin, protocols, extensions, header_dictionary);
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
    return IPCProxy::ready_state(connection.id());
}

void WebSocketClient::send(Badge<WebSocket>, WebSocket& connection, ByteBuffer data, bool is_text)
{
    if (!m_connections.contains(connection.id()))
        return;
    async_send(connection.id(), is_text, move(data));
}

void WebSocketClient::close(Badge<WebSocket>, WebSocket& connection, u16 code, String message)
{
    if (!m_connections.contains(connection.id()))
        return;
    async_close(connection.id(), code, move(message));
}

bool WebSocketClient::set_certificate(Badge<WebSocket>, WebSocket& connection, String certificate, String key)
{
    if (!m_connections.contains(connection.id()))
        return false;
    return IPCProxy::set_certificate(connection.id(), move(certificate), move(key));
}

void WebSocketClient::connected(i32 connection_id)
{
    auto maybe_connection = m_connections.get(connection_id);
    if (maybe_connection.has_value())
        maybe_connection.value()->did_open({});
}

void WebSocketClient::received(i32 connection_id, bool is_text, ByteBuffer const& data)
{
    auto maybe_connection = m_connections.get(connection_id);
    if (maybe_connection.has_value())
        maybe_connection.value()->did_receive({}, data, is_text);
}

void WebSocketClient::errored(i32 connection_id, i32 message)
{
    auto maybe_connection = m_connections.get(connection_id);
    if (maybe_connection.has_value())
        maybe_connection.value()->did_error({}, message);
}

void WebSocketClient::closed(i32 connection_id, u16 code, String const& reason, bool clean)
{
    auto maybe_connection = m_connections.get(connection_id);
    if (maybe_connection.has_value())
        maybe_connection.value()->did_close({}, code, reason, clean);
}

void WebSocketClient::certificate_requested(i32 connection_id)
{
    auto maybe_connection = m_connections.get(connection_id);
    if (maybe_connection.has_value())
        maybe_connection.value()->did_request_certificates({});
}

}
