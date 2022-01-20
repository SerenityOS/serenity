/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibProtocol/WebSocket.h>
#include <LibProtocol/WebSocketClient.h>

namespace Protocol {

WebSocket::WebSocket(WebSocketClient& client, i32 connection_id)
    : m_client(client)
    , m_connection_id(connection_id)
{
}

WebSocket::ReadyState WebSocket::ready_state()
{
    return (WebSocket::ReadyState)m_client->ready_state({}, *this);
}

void WebSocket::send(ByteBuffer binary_or_text_message, bool is_text)
{
    m_client->send({}, *this, move(binary_or_text_message), is_text);
}

void WebSocket::send(StringView text_message)
{
    send(ByteBuffer::copy(text_message.bytes()).release_value_but_fixme_should_propagate_errors(), true);
}

void WebSocket::close(u16 code, String reason)
{
    m_client->close({}, *this, code, move(reason));
}

void WebSocket::did_open(Badge<WebSocketClient>)
{
    if (on_open)
        on_open();
}

void WebSocket::did_receive(Badge<WebSocketClient>, ByteBuffer data, bool is_text)
{
    if (on_message)
        on_message(WebSocket::Message { move(data), is_text });
}

void WebSocket::did_error(Badge<WebSocketClient>, i32 error_code)
{
    if (on_error)
        on_error((WebSocket::Error)error_code);
}

void WebSocket::did_close(Badge<WebSocketClient>, u16 code, String reason, bool was_clean)
{
    if (on_close)
        on_close(code, move(reason), was_clean);
}

void WebSocket::did_request_certificates(Badge<WebSocketClient>)
{
    if (on_certificate_requested) {
        auto result = on_certificate_requested();
        if (!m_client->set_certificate({}, *this, result.certificate, result.key))
            dbgln("WebSocket: set_certificate failed");
    }
}
}
