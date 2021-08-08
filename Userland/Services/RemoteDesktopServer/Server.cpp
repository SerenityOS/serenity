/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/StringBuilder.h>
#include <RemoteDesktopServer/Client.h>
#include <RemoteDesktopServer/Server.h>

namespace RemoteDesktopServer {

Server::Server(Configuration const& config, RemoteGfx::RemoteGfxFontDatabase& font_database, Core::Object* parent)
    : Core::Object(parent)
    , m_config(config)
    , m_font_database(font_database)
    , m_server(Core::TCPServer::construct(this))
{
    m_server->on_ready_to_accept = [this] {
        auto client_socket = m_server->accept();
        VERIFY(client_socket);
        m_clients.append(Client::construct(client_socket.release_nonnull(), *this));
    };

    dbgln_if(REMOTE_DESKTOP_SERVER_DEBUG, "Server {:p} created", this);
}

void Server::remove_client(Client& client)
{
    m_clients.remove_first_matching([&](auto& c) {
        return c == &client;
    });
    if (m_forwarding_client == &client)
        m_forwarding_client = nullptr;
}

void Server::die()
{
    NonnullRefPtr<Server> protector(*this);
    deferred_invoke([this, protector = move(protector)]() {
        remove_from_parent();
    });
}

bool Server::listen(IPv4Address const& listen_address, u16 port)
{
    return m_server->listen(listen_address, port);
}

}
