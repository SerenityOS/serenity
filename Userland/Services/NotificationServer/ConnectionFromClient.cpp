/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include "NotificationWindow.h"
#include <AK/HashMap.h>
#include <NotificationServer/NotificationClientEndpoint.h>

namespace NotificationServer {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ConnectionFromClient<NotificationClientEndpoint, NotificationServerEndpoint>(*this, move(client_socket), client_id)
{
    s_connections.set(client_id, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
}

void ConnectionFromClient::show_notification(String const& text, String const& title, Gfx::ShareableBitmap const& icon, URL::URL const& launch_url)
{
    auto window = NotificationWindow::construct(client_id(), text, title, icon, launch_url);
    window->show();
}

void ConnectionFromClient::close_notification()
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->close();
    }
}

Messages::NotificationServer::UpdateNotificationIconResponse ConnectionFromClient::update_notification_icon(Gfx::ShareableBitmap const& icon)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->set_image(icon);
    }
    return !!window;
}

Messages::NotificationServer::UpdateNotificationTextResponse ConnectionFromClient::update_notification_text(String const& text, String const& title)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->set_text(text);
        window->set_title(title);
    }
    return !!window;
}

Messages::NotificationServer::UpdateNotificationLaunchUrlResponse ConnectionFromClient::update_notification_launch_url(URL::URL const& launch_url)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->set_launch_url(launch_url);
    }
    return !!window;
}

Messages::NotificationServer::IsShowingResponse ConnectionFromClient::is_showing()
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    return !!window;
}

}
