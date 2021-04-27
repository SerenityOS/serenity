/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ServerConnection.h>
#include <WebSocket/WebSocketClientEndpoint.h>
#include <WebSocket/WebSocketServerEndpoint.h>

namespace Protocol {

class WebSocket;

class WebSocketClient
    : public IPC::ServerConnection<WebSocketClientEndpoint, WebSocketServerEndpoint>
    , public WebSocketClientEndpoint {
    C_OBJECT(WebSocketClient);

public:
    virtual void handshake() override;

    RefPtr<WebSocket> connect(const URL&, const String& origin = {}, const Vector<String>& protocols = {}, const Vector<String>& extensions = {}, const HashMap<String, String>& request_headers = {});

    u32 ready_state(Badge<WebSocket>, WebSocket&);
    void send(Badge<WebSocket>, WebSocket&, ByteBuffer, bool is_text);
    void close(Badge<WebSocket>, WebSocket&, u16 code, String reason);
    bool set_certificate(Badge<WebSocket>, WebSocket&, String, String);

private:
    WebSocketClient();

    virtual void handle(const Messages::WebSocketClient::Connected&) override;
    virtual void handle(const Messages::WebSocketClient::Received&) override;
    virtual void handle(const Messages::WebSocketClient::Errored&) override;
    virtual void handle(const Messages::WebSocketClient::Closed&) override;
    virtual void handle(const Messages::WebSocketClient::CertificateRequested&) override;

    HashMap<i32, NonnullRefPtr<WebSocket>> m_connections;
};

}
