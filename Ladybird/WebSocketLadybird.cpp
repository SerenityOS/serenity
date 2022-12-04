/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WebSocketLadybird.h"

namespace Ladybird {

NonnullRefPtr<WebSocketLadybird> WebSocketLadybird::create(NonnullRefPtr<WebSocket::WebSocket> underlying_socket)
{
    return adopt_ref(*new WebSocketLadybird(move(underlying_socket)));
}

WebSocketLadybird::WebSocketLadybird(NonnullRefPtr<WebSocket::WebSocket> underlying_socket)
    : m_websocket(move(underlying_socket))
{
    m_websocket->on_open = [weak_this = make_weak_ptr()] {
        if (auto strong_this = weak_this.strong_ref())
            if (strong_this->on_open)
                strong_this->on_open();
    };
    m_websocket->on_message = [weak_this = make_weak_ptr()](auto message) {
        if (auto strong_this = weak_this.strong_ref()) {
            if (strong_this->on_message) {
                strong_this->on_message(Web::WebSockets::WebSocketClientSocket::Message {
                    .data = move(message.data()),
                    .is_text = message.is_text(),
                });
            }
        }
    };
    m_websocket->on_error = [weak_this = make_weak_ptr()](auto error) {
        if (auto strong_this = weak_this.strong_ref()) {
            if (strong_this->on_error) {
                switch (error) {
                case WebSocket::WebSocket::Error::CouldNotEstablishConnection:
                    strong_this->on_error(Web::WebSockets::WebSocketClientSocket::Error::CouldNotEstablishConnection);
                    return;
                case WebSocket::WebSocket::Error::ConnectionUpgradeFailed:
                    strong_this->on_error(Web::WebSockets::WebSocketClientSocket::Error::ConnectionUpgradeFailed);
                    return;
                case WebSocket::WebSocket::Error::ServerClosedSocket:
                    strong_this->on_error(Web::WebSockets::WebSocketClientSocket::Error::ServerClosedSocket);
                    return;
                }
                VERIFY_NOT_REACHED();
            }
        }
    };
    m_websocket->on_close = [weak_this = make_weak_ptr()](u16 code, DeprecatedString reason, bool was_clean) {
        if (auto strong_this = weak_this.strong_ref())
            if (strong_this->on_close)
                strong_this->on_close(code, move(reason), was_clean);
    };
}

WebSocketLadybird::~WebSocketLadybird() = default;

Web::WebSockets::WebSocket::ReadyState WebSocketLadybird::ready_state()
{
    switch (m_websocket->ready_state()) {
    case WebSocket::ReadyState::Connecting:
        return Web::WebSockets::WebSocket::ReadyState::Connecting;
    case WebSocket::ReadyState::Open:
        return Web::WebSockets::WebSocket::ReadyState::Open;
    case WebSocket::ReadyState::Closing:
        return Web::WebSockets::WebSocket::ReadyState::Closing;
    case WebSocket::ReadyState::Closed:
        return Web::WebSockets::WebSocket::ReadyState::Closed;
    }
    VERIFY_NOT_REACHED();
}

void WebSocketLadybird::send(ByteBuffer binary_or_text_message, bool is_text)
{
    m_websocket->send(WebSocket::Message(binary_or_text_message, is_text));
}

void WebSocketLadybird::send(StringView message)
{
    m_websocket->send(WebSocket::Message(message));
}

void WebSocketLadybird::close(u16 code, DeprecatedString reason)
{
    m_websocket->close(code, reason);
}

}
