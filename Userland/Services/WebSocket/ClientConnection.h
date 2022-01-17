/*
 * Copyright (c) 2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibIPC/ClientConnection.h>
#include <LibWebSocket/WebSocket.h>
#include <WebSocket/WebSocketClientEndpoint.h>
#include <WebSocket/WebSocketServerEndpoint.h>

namespace WebSocket {

class ClientConnection final
    : public IPC::ClientConnection<WebSocketClientEndpoint, WebSocketServerEndpoint> {
    C_OBJECT(ClientConnection);

public:
    ~ClientConnection() override;

    virtual void die() override;

private:
    explicit ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket>);

    virtual Messages::WebSocketServer::ConnectResponse connect(URL const&, String const&, Vector<String> const&, Vector<String> const&, IPC::Dictionary const&) override;
    virtual Messages::WebSocketServer::ReadyStateResponse ready_state(i32) override;
    virtual void send(i32, bool, ByteBuffer const&) override;
    virtual void close(i32, u16, String const&) override;
    virtual Messages::WebSocketServer::SetCertificateResponse set_certificate(i32, String const&, String const&) override;

    void did_connect(i32);
    void did_receive_message(i32, Message);
    void did_error(i32, i32 message);
    void did_close(i32, u16 code, String reason, bool was_clean);
    void did_request_certificates(i32);

    i32 m_connection_ids { 0 };
    HashMap<i32, RefPtr<WebSocket>> m_connections;
};

}
