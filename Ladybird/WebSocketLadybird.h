/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/WebSockets/WebSocket.h>
#include <LibWebSocket/WebSocket.h>

namespace Ladybird {

class WebSocketLadybird
    : public Web::WebSockets::WebSocketClientSocket
    , public Weakable<WebSocketLadybird> {
public:
    static NonnullRefPtr<WebSocketLadybird> create(NonnullRefPtr<WebSocket::WebSocket>);

    virtual ~WebSocketLadybird() override;

    virtual Web::WebSockets::WebSocket::ReadyState ready_state() override;
    virtual void send(ByteBuffer binary_or_text_message, bool is_text) override;
    virtual void send(StringView message) override;
    virtual void close(u16 code, DeprecatedString reason) override;

private:
    explicit WebSocketLadybird(NonnullRefPtr<WebSocket::WebSocket>);

    NonnullRefPtr<WebSocket::WebSocket> m_websocket;
};

}
