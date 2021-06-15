/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ClientConnection.h>
#include <WindowServer/ScreenLayout.h>

// Must be included after WindowServer/ScreenLayout.h
#include <NotificationServer/NotificationClientEndpoint.h>
#include <NotificationServer/NotificationServerEndpoint.h>

namespace NotificationServer {

class ClientConnection final : public IPC::ClientConnection<NotificationClientEndpoint, NotificationServerEndpoint> {
    C_OBJECT(ClientConnection)
public:
    ~ClientConnection() override;

    virtual void die() override;

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);

    virtual void show_notification(String const&, String const&, Gfx::ShareableBitmap const&) override;
    virtual void close_notification() override;
    virtual Messages::NotificationServer::UpdateNotificationIconResponse update_notification_icon(Gfx::ShareableBitmap const&) override;
    virtual Messages::NotificationServer::UpdateNotificationTextResponse update_notification_text(String const&, String const&) override;
    virtual Messages::NotificationServer::IsShowingResponse is_showing() override;
};

}
