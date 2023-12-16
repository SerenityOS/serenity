/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <LibIPC/ConnectionFromClient.h>
#include <WindowServer/WindowManagerClientEndpoint.h>
#include <WindowServer/WindowManagerServerEndpoint.h>

namespace WindowServer {

class WMConnectionFromClient final
    : public IPC::ConnectionFromClient<WindowManagerClientEndpoint, WindowManagerServerEndpoint> {
    C_OBJECT(WMConnectionFromClient)

public:
    ~WMConnectionFromClient() override;

    virtual void set_active_window(i32, i32) override;
    virtual void set_window_minimized(i32, i32, bool) override;
    virtual void toggle_show_desktop() override;
    virtual void start_window_resize(i32, i32, i32) override;
    virtual void popup_window_menu(i32, i32, Gfx::IntPoint) override;
    virtual void set_window_taskbar_rect(i32, i32, Gfx::IntRect const&) override;
    virtual void set_applet_area_position(Gfx::IntPoint) override;
    virtual void set_event_mask(u32) override;
    virtual void set_manager_window(i32) override;
    virtual void set_workspace(u32, u32) override;
    virtual void set_keymap(ByteString const&) override;

    unsigned event_mask() const { return m_event_mask; }
    int window_id() const { return m_window_id; }

private:
    explicit WMConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> client_socket, int client_id);

    // ^ConnectionFromClient
    virtual void die() override;

    // RefPtr<Core::Timer> m_ping_timer;
    static HashMap<int, NonnullRefPtr<WMConnectionFromClient>> s_connections;
    unsigned m_event_mask { 0 };
    int m_window_id { -1 };

    // WindowManager needs to access the window manager clients to notify
    // about events.
    friend class WindowManager;
};

};
