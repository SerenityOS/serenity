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

void ClientConnection::show_notification(String const& text, String const& title, Gfx::ShareableBitmap const& icon)
{
    auto window = NotificationWindow::construct(client_id(), text, title, icon);
    window->show();
}

void ClientConnection::close_notification()
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->close();
    }
}

Messages::NotificationServer::UpdateNotificationIconResponse ClientConnection::update_notification_icon(Gfx::ShareableBitmap const& icon)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->set_image(icon);
    }
    return !!window;
}

Messages::NotificationServer::UpdateNotificationTextResponse ClientConnection::update_notification_text(String const& text, String const& title)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->set_text(text);
        window->set_title(title);
    }
    return !!window;
}

Messages::NotificationServer::IsShowingResponse ClientConnection::is_showing()
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    return !!window;
}

}
