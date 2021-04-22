/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
