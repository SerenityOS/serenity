/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibIPC/ServerConnection.h>
#include <WindowServer/ScreenLayout.h>
#include <WindowServer/WindowManagerClientEndpoint.h>
#include <WindowServer/WindowManagerServerEndpoint.h>

namespace GUI {

class WindowManagerServerConnection final
    : public IPC::ServerConnection<WindowManagerClientEndpoint, WindowManagerServerEndpoint>
    , public WindowManagerClientEndpoint {
    IPC_CLIENT_CONNECTION(WindowManagerServerConnection, "/tmp/portal/wm")

public:
    static WindowManagerServerConnection& the();

private:
    WindowManagerServerConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
        : IPC::ServerConnection<WindowManagerClientEndpoint, WindowManagerServerEndpoint>(*this, move(socket))
    {
    }

    virtual void window_removed(i32, i32, i32) override;
    virtual void window_state_changed(i32, i32, i32, i32, i32, u32, u32, bool, bool, bool, bool, i32, String const&, Gfx::IntRect const&, Optional<i32> const&) override;
    virtual void window_icon_bitmap_changed(i32, i32, i32, Gfx::ShareableBitmap const&) override;
    virtual void window_rect_changed(i32, i32, i32, Gfx::IntRect const&) override;
    virtual void applet_area_size_changed(i32, Gfx::IntSize const&) override;
    virtual void super_key_pressed(i32) override;
    virtual void super_space_key_pressed(i32) override;
    virtual void workspace_changed(i32, u32, u32) override;
    virtual void keymap_changed(i32, String const&) override;
};

}
