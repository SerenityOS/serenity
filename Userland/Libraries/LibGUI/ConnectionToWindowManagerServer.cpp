/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2023, David Ganz <david.g.ganz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/Event.h>
#include <LibGUI/Window.h>

namespace GUI {

ConnectionToWindowManagerServer& ConnectionToWindowManagerServer::the()
{
    static RefPtr<ConnectionToWindowManagerServer> s_connection = nullptr;
    if (!s_connection)
        s_connection = ConnectionToWindowManagerServer::try_create().release_value_but_fixme_should_propagate_errors();
    return *s_connection;
}

void ConnectionToWindowManagerServer::window_state_changed(i32 wm_id, i32 client_id, i32 window_id,
    u32 workspace_row, u32 workspace_column, bool is_active, bool is_blocked, bool is_minimized, bool is_frameless,
    i32 window_type, ByteString const& title, Gfx::IntRect const& rect, Optional<i32> const& progress)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMWindowStateChangedEvent>(client_id, window_id, title, rect, workspace_row, workspace_column, is_active, is_blocked, static_cast<WindowType>(window_type), is_minimized, is_frameless, progress));
}

void ConnectionToWindowManagerServer::applet_area_size_changed(i32 wm_id, Gfx::IntSize size)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMAppletAreaSizeChangedEvent>(size));
}

void ConnectionToWindowManagerServer::window_rect_changed(i32 wm_id, i32 client_id, i32 window_id, Gfx::IntRect const& rect)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMWindowRectChangedEvent>(client_id, window_id, rect));
}

void ConnectionToWindowManagerServer::window_icon_bitmap_changed(i32 wm_id, i32 client_id, i32 window_id, Gfx::ShareableBitmap const& bitmap)
{
    if (auto* window = Window::from_window_id(wm_id)) {
        Core::EventLoop::current().post_event(*window, make<WMWindowIconBitmapChangedEvent>(client_id, window_id, bitmap.bitmap()));
    }
}

void ConnectionToWindowManagerServer::window_removed(i32 wm_id, i32 client_id, i32 window_id)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMWindowRemovedEvent>(client_id, window_id));
}

void ConnectionToWindowManagerServer::super_key_pressed(i32 wm_id)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMSuperKeyPressedEvent>(wm_id));
}

void ConnectionToWindowManagerServer::super_space_key_pressed(i32 wm_id)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMSuperSpaceKeyPressedEvent>(wm_id));
}

void ConnectionToWindowManagerServer::super_d_key_pressed(i32 wm_id)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMSuperDKeyPressedEvent>(wm_id));
}

void ConnectionToWindowManagerServer::super_digit_key_pressed(i32 wm_id, u8 digit)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMSuperDigitKeyPressedEvent>(wm_id, digit));
}

void ConnectionToWindowManagerServer::workspace_changed(i32 wm_id, u32 row, u32 column)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMWorkspaceChangedEvent>(wm_id, row, column));
}

void ConnectionToWindowManagerServer::keymap_changed(i32 wm_id, ByteString const& keymap)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMKeymapChangedEvent>(wm_id, keymap));
}

void ConnectionToWindowManagerServer::add_to_quick_launch(i32 wm_id, pid_t pid)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMAddToQuickLaunchEvent>(wm_id, pid));
}

}
