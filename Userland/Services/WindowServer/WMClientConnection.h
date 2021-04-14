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

#include "AK/NonnullRefPtr.h"
#include <AK/HashMap.h>
#include <LibIPC/ClientConnection.h>
#include <WindowServer/WindowManagerClientEndpoint.h>
#include <WindowServer/WindowManagerServerEndpoint.h>

namespace WindowServer {

class WMClientConnection final
    : public IPC::ClientConnection<WindowManagerClientEndpoint, WindowManagerServerEndpoint>
    , public WindowManagerServerEndpoint {
    C_OBJECT(WMClientConnection)

public:
    ~WMClientConnection() override;

    virtual void handle(const Messages::WindowManagerServer::SetActiveWindow&) override;
    virtual void handle(const Messages::WindowManagerServer::SetWindowMinimized&) override;
    virtual void handle(const Messages::WindowManagerServer::StartWindowResize&) override;
    virtual void handle(const Messages::WindowManagerServer::PopupWindowMenu&) override;
    virtual void handle(const Messages::WindowManagerServer::SetWindowTaskbarRect&) override;
    virtual OwnPtr<Messages::WindowManagerServer::SetAppletAreaPositionResponse> handle(const Messages::WindowManagerServer::SetAppletAreaPosition&) override;
    virtual OwnPtr<Messages::WindowManagerServer::SetEventMaskResponse> handle(const Messages::WindowManagerServer::SetEventMask&) override;
    virtual OwnPtr<Messages::WindowManagerServer::SetManagerWindowResponse> handle(const Messages::WindowManagerServer::SetManagerWindow&) override;

    unsigned event_mask() const { return m_event_mask; }
    int window_id() const { return m_window_id; }

private:
    explicit WMClientConnection(NonnullRefPtr<Core::LocalSocket> client_socket, int client_id);

    // ^ClientConnection
    virtual void die() override;

    // RefPtr<Core::Timer> m_ping_timer;
    static HashMap<int, NonnullRefPtr<WMClientConnection>> s_connections;
    unsigned m_event_mask { 0 };
    int m_window_id { -1 };

    // WindowManager needs to access the window manager clients to notify
    // about events.
    friend class WindowManager;
};

};
