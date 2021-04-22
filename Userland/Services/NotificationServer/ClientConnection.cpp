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

OwnPtr<Messages::NotificationServer::GreetResponse> ClientConnection::handle(const Messages::NotificationServer::Greet&)
{
    return make<Messages::NotificationServer::GreetResponse>();
}

OwnPtr<Messages::NotificationServer::ShowNotificationResponse> ClientConnection::handle(const Messages::NotificationServer::ShowNotification& message)
{
    auto window = NotificationWindow::construct(client_id(), message.text(), message.title(), message.icon());
    window->show();
    return make<Messages::NotificationServer::ShowNotificationResponse>();
}

OwnPtr<Messages::NotificationServer::CloseNotificationResponse> ClientConnection::handle([[maybe_unused]] const Messages::NotificationServer::CloseNotification& message)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->close();
    }
    return make<Messages::NotificationServer::CloseNotificationResponse>();
}

OwnPtr<Messages::NotificationServer::UpdateNotificationIconResponse> ClientConnection::handle(const Messages::NotificationServer::UpdateNotificationIcon& message)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->set_image(message.icon());
    }
    return make<Messages::NotificationServer::UpdateNotificationIconResponse>(window);
}

OwnPtr<Messages::NotificationServer::UpdateNotificationTextResponse> ClientConnection::handle(const Messages::NotificationServer::UpdateNotificationText& message)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    if (window) {
        window->set_text(message.text());
        window->set_title(message.title());
    }
    return make<Messages::NotificationServer::UpdateNotificationTextResponse>(window);
}

OwnPtr<Messages::NotificationServer::IsShowingResponse> ClientConnection::handle(const Messages::NotificationServer::IsShowing&)
{
    auto window = NotificationWindow::get_window_by_id(client_id());
    return make<Messages::NotificationServer::IsShowingResponse>(window);
}

}
