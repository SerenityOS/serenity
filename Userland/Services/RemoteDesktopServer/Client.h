/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>
#include <LibCore/TCPSocket.h>
#include <LibRemoteDesktop/RemoteCompositorServerConnection.h>
#include <LibRemoteDesktop/RemoteDesktopClientConnection.h>

namespace RemoteDesktopServer {

class Server;

class Client final : public RemoteDesktop::RemoteDesktopClientConnection {
    C_OBJECT(Client);

public:
    ~Client() override;

    virtual Messages::RemoteDesktopServer::StartSessionResponse start_session(Vector<ByteBuffer> const&) override;
    virtual void send_compositor_message(Vector<u8> const&) override;

private:
    Client(NonnullRefPtr<Core::TCPSocket>, Server& server);

    NonnullRefPtr<Server> m_server;
    RefPtr<RemoteDesktop::RemoteCompositorServerConnection> m_compositor_connection;
};

}
