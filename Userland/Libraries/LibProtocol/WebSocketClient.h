/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ConnectionToServer.h>
#include <WebSocket/WebSocketClientEndpoint.h>
#include <WebSocket/WebSocketServerEndpoint.h>

namespace Protocol {

class WebSocket;

class WebSocketClient final
    : public IPC::ConnectionToServer<WebSocketClientEndpoint, WebSocketServerEndpoint>
    , public WebSocketClientEndpoint {
    IPC_CLIENT_CONNECTION(WebSocketClient, "/tmp/portal/websocket")

public:
    RefPtr<WebSocket> connect(const URL&, String const& origin = {}, Vector<String> const& protocols = {}, Vector<String> const& extensions = {}, HashMap<String, String> const& request_headers = {});

    u32 ready_state(Badge<WebSocket>, WebSocket&);
    void send(Badge<WebSocket>, WebSocket&, ByteBuffer, bool is_text);
    void close(Badge<WebSocket>, WebSocket&, u16 code, String reason);
    bool set_certificate(Badge<WebSocket>, WebSocket&, String, String);

private:
    WebSocketClient(NonnullOwnPtr<Core::Stream::LocalSocket>);

    virtual void connected(i32) override;
    virtual void received(i32, bool, ByteBuffer const&) override;
    virtual void errored(i32, i32) override;
    virtual void closed(i32, u16, String const&, bool) override;
    virtual void certificate_requested(i32) override;

    HashMap<i32, NonnullRefPtr<WebSocket>> m_connections;
};

}
