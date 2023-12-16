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
    IPC_CLIENT_CONNECTION(WebSocketClient, "/tmp/session/%sid/portal/websocket"sv)

public:
    explicit WebSocketClient(NonnullOwnPtr<Core::LocalSocket>);

    RefPtr<WebSocket> connect(const URL&, ByteString const& origin = {}, Vector<ByteString> const& protocols = {}, Vector<ByteString> const& extensions = {}, HashMap<ByteString, ByteString> const& request_headers = {});

    u32 ready_state(Badge<WebSocket>, WebSocket&);
    ByteString subprotocol_in_use(Badge<WebSocket>, WebSocket&);
    void send(Badge<WebSocket>, WebSocket&, ByteBuffer, bool is_text);
    void close(Badge<WebSocket>, WebSocket&, u16 code, ByteString reason);
    bool set_certificate(Badge<WebSocket>, WebSocket&, ByteString, ByteString);

private:
    virtual void connected(i32) override;
    virtual void received(i32, bool, ByteBuffer const&) override;
    virtual void errored(i32, i32) override;
    virtual void closed(i32, u16, ByteString const&, bool) override;
    virtual void certificate_requested(i32) override;

    HashMap<i32, NonnullRefPtr<WebSocket>> m_connections;
};

}
