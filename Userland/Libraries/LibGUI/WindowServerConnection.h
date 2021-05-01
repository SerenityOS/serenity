/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ServerConnection.h>
#include <WindowServer/WindowClientEndpoint.h>
#include <WindowServer/WindowServerEndpoint.h>

namespace GUI {

class WindowServerConnection
    : public IPC::ServerConnection<WindowClientEndpoint, WindowServerEndpoint>
    , public WindowClientEndpoint {
    C_OBJECT(WindowServerConnection)
public:
    WindowServerConnection()
        : IPC::ServerConnection<WindowClientEndpoint, WindowServerEndpoint>(*this, "/tmp/portal/window")
    {
        handshake();
    }

    virtual void handshake() override;
    static WindowServerConnection& the();

private:
    virtual void handle(const Messages::WindowClient::Paint&) override;
    virtual void handle(const Messages::WindowClient::MouseMove&) override;
    virtual void handle(const Messages::WindowClient::MouseDown&) override;
    virtual void handle(const Messages::WindowClient::MouseDoubleClick&) override;
    virtual void handle(const Messages::WindowClient::MouseUp&) override;
    virtual void handle(const Messages::WindowClient::MouseWheel&) override;
    virtual void handle(const Messages::WindowClient::WindowEntered&) override;
    virtual void handle(const Messages::WindowClient::WindowLeft&) override;
    virtual void handle(const Messages::WindowClient::KeyDown&) override;
    virtual void handle(const Messages::WindowClient::KeyUp&) override;
    virtual void handle(const Messages::WindowClient::WindowActivated&) override;
    virtual void handle(const Messages::WindowClient::WindowDeactivated&) override;
    virtual void handle(const Messages::WindowClient::WindowInputEntered&) override;
    virtual void handle(const Messages::WindowClient::WindowInputLeft&) override;
    virtual void handle(const Messages::WindowClient::WindowCloseRequest&) override;
    virtual void handle(const Messages::WindowClient::WindowResized&) override;
    virtual void handle(const Messages::WindowClient::MenuItemActivated&) override;
    virtual void handle(const Messages::WindowClient::MenuItemEntered&) override;
    virtual void handle(const Messages::WindowClient::MenuItemLeft&) override;
    virtual void handle(const Messages::WindowClient::MenuVisibilityDidChange&) override;
    virtual void handle(const Messages::WindowClient::ScreenRectChanged&) override;
    virtual void handle(const Messages::WindowClient::AsyncSetWallpaperFinished&) override;
    virtual void handle(const Messages::WindowClient::DragDropped&) override;
    virtual void handle(const Messages::WindowClient::DragAccepted&) override;
    virtual void handle(const Messages::WindowClient::DragCancelled&) override;
    virtual void handle(const Messages::WindowClient::UpdateSystemTheme&) override;
    virtual void handle(const Messages::WindowClient::WindowStateChanged&) override;
    virtual void handle(const Messages::WindowClient::DisplayLinkNotification&) override;
    virtual void handle(const Messages::WindowClient::Ping&) override;

    bool m_display_link_notification_pending { false };
};

}
