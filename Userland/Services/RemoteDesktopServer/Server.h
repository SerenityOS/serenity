/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Object.h>
#include <LibCore/TCPServer.h>
#include <LibGfx/Remote/RemoteFontDatabase.h>

namespace RemoteDesktopServer {

class Client;
class Configuration;

class Server final : public Core::Object {
    C_OBJECT(Server);
    friend class Client;

public:
    void remove_client(Client&);
    bool listen(IPv4Address const&, u16);

    void client_disconnected(Client& client)
    {
        if (m_forwarding_client == &client)
            m_forwarding_client = nullptr;
    }
    bool set_forwarding_client(Client* client)
    {
        if (m_forwarding_client && client != nullptr && client != m_forwarding_client)
            return false;
        m_forwarding_client = client;
        return true;
    }
    Client* forwarding_client() { return m_forwarding_client; }

    auto& font_database() { return m_font_database; }

private:
    Server(Configuration const&, RemoteGfx::RemoteGfxFontDatabase&, Core::Object* parent);

    void die();

    RemoteDesktopServer::Configuration const& m_config;
    RemoteGfx::RemoteGfxFontDatabase& m_font_database;
    NonnullRefPtr<Core::TCPServer> m_server;
    NonnullRefPtrVector<Client, 4> m_clients;
    Client* m_forwarding_client { nullptr };
};

}
