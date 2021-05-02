/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include "NotificationWindow.h"
#include <AK/HashMap.h>
#include <NotificationServer/NotificationClientEndpoint.h>

namespace NotificationServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ClientConnection<NotificationClientEndpoint, NotificationServerEndpoint>(*this, move(client_socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
}

void ClientConnection::handle(const Messages::NotificationServer::Greet&)
{
}

void ClientConnection::handle(const Messages::NotificationServer::ShowNotification& message)
{
    auto window = NotificationWindow::construct(client_id(), message.text(), message.title(), message.icon());
    window->show();
}

void ClientConnection::handle([[maybe_unused]] const Messages::NotificationServer::CloseNotification& message)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->close();
    }
}

Messages::NotificationServer::UpdateNotificationIconResponse ClientConnection::handle(const Messages::NotificationServer::UpdateNotificationIcon& message)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->set_image(message.icon());
    }
    return !!window;
}

Messages::NotificationServer::UpdateNotificationTextResponse ClientConnection::handle(const Messages::NotificationServer::UpdateNotificationText& message)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->set_text(message.text());
        window->set_title(message.title());
    }
    return !!window;
}

Messages::NotificationServer::IsShowingResponse ClientConnection::handle(const Messages::NotificationServer::IsShowing&)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    return !!window;
}

}
