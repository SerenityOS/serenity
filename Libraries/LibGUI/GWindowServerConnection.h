/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    virtual void handle(const WindowClient::Paint&) override;
    virtual void handle(const WindowClient::MouseMove&) override;
    virtual void handle(const WindowClient::MouseDown&) override;
    virtual void handle(const WindowClient::MouseDoubleClick&) override;
    virtual void handle(const WindowClient::MouseUp&) override;
    virtual void handle(const WindowClient::MouseWheel&) override;
    virtual void handle(const WindowClient::WindowEntered&) override;
    virtual void handle(const WindowClient::WindowLeft&) override;
    virtual void handle(const WindowClient::KeyDown&) override;
    virtual void handle(const WindowClient::KeyUp&) override;
    virtual void handle(const WindowClient::WindowActivated&) override;
    virtual void handle(const WindowClient::WindowDeactivated&) override;
    virtual void handle(const WindowClient::WindowCloseRequest&) override;
    virtual void handle(const WindowClient::WindowResized&) override;
    virtual void handle(const WindowClient::MenuItemActivated&) override;
    virtual void handle(const WindowClient::ScreenRectChanged&) override;
    virtual void handle(const WindowClient::ClipboardContentsChanged&) override;
    virtual void handle(const WindowClient::WM_WindowRemoved&) override;
    virtual void handle(const WindowClient::WM_WindowStateChanged&) override;
    virtual void handle(const WindowClient::WM_WindowIconBitmapChanged&) override;
    virtual void handle(const WindowClient::WM_WindowRectChanged&) override;
    virtual void handle(const WindowClient::AsyncSetWallpaperFinished&) override;
    virtual void handle(const WindowClient::DragDropped&) override;
    virtual void handle(const WindowClient::DragAccepted&) override;
    virtual void handle(const WindowClient::DragCancelled&) override;
    virtual void handle(const WindowClient::UpdateSystemTheme&) override;
    virtual void handle(const WindowClient::WindowStateChanged&) override;
};

}
