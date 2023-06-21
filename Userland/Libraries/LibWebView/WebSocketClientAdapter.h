/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Weakable.h>
#include <LibWeb/WebSockets/WebSocket.h>

namespace Protocol {
class WebSocket;
class WebSocketClient;
};

namespace WebView {

class WebSocketClientSocketAdapter
    : public Web::WebSockets::WebSocketClientSocket
    , public Weakable<WebSocketClientSocketAdapter> {
public:
    static RefPtr<WebSocketClientSocketAdapter> create(NonnullRefPtr<Protocol::WebSocket>);
    virtual ~WebSocketClientSocketAdapter() override;

    virtual Web::WebSockets::WebSocket::ReadyState ready_state() override;
    virtual DeprecatedString subprotocol_in_use() override;

    virtual void send(ByteBuffer binary_or_text_message, bool is_text) override;
    virtual void send(StringView text_message) override;
    virtual void close(u16 code = 1005, DeprecatedString reason = {}) override;

private:
    WebSocketClientSocketAdapter(NonnullRefPtr<Protocol::WebSocket>);

    NonnullRefPtr<Protocol::WebSocket> m_websocket;
};

class WebSocketClientManagerAdapter : public Web::WebSockets::WebSocketClientManager {
public:
    static ErrorOr<NonnullRefPtr<WebSocketClientManagerAdapter>> try_create();

    virtual ~WebSocketClientManagerAdapter() override;

    virtual RefPtr<Web::WebSockets::WebSocketClientSocket> connect(const AK::URL&, DeprecatedString const& origin, Vector<DeprecatedString> const& protocols) override;

private:
    WebSocketClientManagerAdapter(NonnullRefPtr<Protocol::WebSocketClient>);

    NonnullRefPtr<Protocol::WebSocketClient> m_websocket_client;
};

}
