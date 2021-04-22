/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ServerConnection.h>
#include <WindowServer/WindowManagerClientEndpoint.h>
#include <WindowServer/WindowManagerServerEndpoint.h>

namespace GUI {

class WindowManagerServerConnection
    : public IPC::ServerConnection<WindowManagerClientEndpoint, WindowManagerServerEndpoint>
    , public WindowManagerClientEndpoint {
    C_OBJECT(WindowManagerServerConnection)
public:
    WindowManagerServerConnection()
        : IPC::ServerConnection<WindowManagerClientEndpoint, WindowManagerServerEndpoint>(*this, "/tmp/portal/wm")
    {
        handshake();
    }

    virtual void handshake() override;
    static WindowManagerServerConnection& the();

private:
    virtual void handle(const Messages::WindowManagerClient::WindowRemoved&) override;
    virtual void handle(const Messages::WindowManagerClient::WindowStateChanged&) override;
    virtual void handle(const Messages::WindowManagerClient::WindowIconBitmapChanged&) override;
    virtual void handle(const Messages::WindowManagerClient::WindowRectChanged&) override;
    virtual void handle(const Messages::WindowManagerClient::AppletAreaSizeChanged&) override;
    virtual void handle(const Messages::WindowManagerClient::SuperKeyPressed&) override;
};

}
