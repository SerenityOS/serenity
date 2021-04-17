/*
 * Copyright (c) 2021, the SerenityOS developers.
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
