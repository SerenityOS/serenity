/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ConnectionFromClient.h>
#include <LibWebSocket/WebSocket.h>
#include <WebSocket/WebSocketClientEndpoint.h>
#include <WebSocket/WebSocketServerEndpoint.h>

namespace WebSocket {

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<WebSocketClientEndpoint, WebSocketServerEndpoint> {
    C_OBJECT(ConnectionFromClient);

public:
    ~ConnectionFromClient() override = default;

    virtual void die() override;

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>);

    virtual Messages::WebSocketServer::ConnectResponse connect(URL const&, DeprecatedString const&, Vector<DeprecatedString> const&, Vector<DeprecatedString> const&, IPC::Dictionary const&) override;
    virtual Messages::WebSocketServer::ReadyStateResponse ready_state(i32) override;
    virtual void send(i32, bool, ByteBuffer const&) override;
    virtual void close(i32, u16, DeprecatedString const&) override;
    virtual Messages::WebSocketServer::SetCertificateResponse set_certificate(i32, DeprecatedString const&, DeprecatedString const&) override;

    void did_connect(i32);
    void did_receive_message(i32, Message);
    void did_error(i32, i32 message);
    void did_close(i32, u16 code, DeprecatedString reason, bool was_clean);
    void did_request_certificates(i32);

    i32 m_connection_ids { 0 };
    HashMap<i32, RefPtr<WebSocket>> m_connections;
};

}
