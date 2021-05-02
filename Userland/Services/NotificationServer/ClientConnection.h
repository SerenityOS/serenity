/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ClientConnection.h>
#include <NotificationServer/NotificationClientEndpoint.h>
#include <NotificationServer/NotificationServerEndpoint.h>

namespace NotificationServer {

class ClientConnection final : public IPC::ClientConnection<NotificationClientEndpoint, NotificationServerEndpoint>
    , public NotificationServerEndpoint {
    C_OBJECT(ClientConnection)
public:
    ~ClientConnection() override;

    virtual void die() override;

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);

    virtual void handle(const Messages::NotificationServer::Greet&) override;
    virtual void handle(const Messages::NotificationServer::ShowNotification&) override;
    virtual void handle(const Messages::NotificationServer::CloseNotification& message) override;
    virtual Messages::NotificationServer::UpdateNotificationIconResponse handle(const Messages::NotificationServer::UpdateNotificationIcon& message) override;
    virtual Messages::NotificationServer::UpdateNotificationTextResponse handle(const Messages::NotificationServer::UpdateNotificationText& message) override;
    virtual Messages::NotificationServer::IsShowingResponse handle(const Messages::NotificationServer::IsShowing& message) override;
};

}
