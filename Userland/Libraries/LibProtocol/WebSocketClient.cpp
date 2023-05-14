/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibProtocol/WebSocket.h>
#include <LibProtocol/WebSocketClient.h>

namespace Protocol {

WebSocketClient::WebSocketClient(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionToServer<WebSocketClientEndpoint, WebSocketServerEndpoint>(*this, move(socket))
{
}

RefPtr<WebSocket> WebSocketClient::connect(const URL& url, DeprecatedString const& origin, Vector<DeprecatedString> const& protocols, Vector<DeprecatedString> const& extensions, HashMap<DeprecatedString, DeprecatedString> const& request_headers)
{
    auto headers_or_error = request_headers.clone();
    if (headers_or_error.is_error())
        return nullptr;
    auto connection_id = IPCProxy::connect(url, origin, protocols, extensions, headers_or_error.release_value());
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

DeprecatedString WebSocketClient::subprotocol_in_use(Badge<WebSocket>, WebSocket& connection)
{
    if (!m_connections.contains(connection.id()))
        return DeprecatedString::empty();
    return IPCProxy::subprotocol_in_use(connection.id());
}

void WebSocketClient::send(Badge<WebSocket>, WebSocket& connection, ByteBuffer data, bool is_text)
{
    if (!m_connections.contains(connection.id()))
        return;
    async_send(connection.id(), is_text, move(data));
}

void WebSocketClient::close(Badge<WebSocket>, WebSocket& connection, u16 code, DeprecatedString message)
{
    if (!m_connections.contains(connection.id()))
        return;
    async_close(connection.id(), code, move(message));
}

bool WebSocketClient::set_certificate(Badge<WebSocket>, WebSocket& connection, DeprecatedString certificate, DeprecatedString key)
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

void WebSocketClient::closed(i32 connection_id, u16 code, DeprecatedString const& reason, bool clean)
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
