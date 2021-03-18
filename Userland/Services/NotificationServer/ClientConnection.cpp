/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
