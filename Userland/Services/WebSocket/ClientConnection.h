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
    : public IPC::ClientConnection<WebSocketClientEndpoint, WebSocketServerEndpoint>
    , public WebSocketServerEndpoint {
    C_OBJECT(ClientConnection);

public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);
    ~ClientConnection() override;

    virtual void die() override;

private:
    virtual OwnPtr<Messages::WebSocketServer::GreetResponse> handle(const Messages::WebSocketServer::Greet&) override;
    virtual OwnPtr<Messages::WebSocketServer::ConnectResponse> handle(const Messages::WebSocketServer::Connect&) override;
    virtual OwnPtr<Messages::WebSocketServer::ReadyStateResponse> handle(const Messages::WebSocketServer::ReadyState&) override;
    virtual void handle(const Messages::WebSocketServer::Send&) override;
    virtual void handle(const Messages::WebSocketServer::Close&) override;
    virtual OwnPtr<Messages::WebSocketServer::SetCertificateResponse> handle(const Messages::WebSocketServer::SetCertificate&) override;

    void did_connect(i32);
    void did_receive_message(i32, Message);
    void did_error(i32, i32 message);
    void did_close(i32, u16 code, String reason, bool was_clean);
    void did_request_certificates(i32);

    i32 m_connection_ids { 0 };
    HashMap<i32, RefPtr<WebSocket>> m_connections;
};

}
