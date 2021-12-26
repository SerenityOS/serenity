/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowManagerServerConnection.h>
#include <WindowServer/Window.h>

namespace GUI {

WindowManagerServerConnection& WindowManagerServerConnection::the()
{
    static WindowManagerServerConnection* s_connection = nullptr;
    if (!s_connection)
        s_connection = new WindowManagerServerConnection;
    return *s_connection;
}

void WindowManagerServerConnection::window_state_changed(i32 wm_id, i32 client_id, i32 window_id,
    i32 parent_client_id, i32 parent_window_id, bool is_active, bool is_minimized, bool is_modal,
    bool is_frameless, i32 window_type, String const& title, Gfx::IntRect const& rect, Optional<i32> const& progress)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMWindowStateChangedEvent>(client_id, window_id, parent_client_id, parent_window_id, title, rect, is_active, is_modal, static_cast<WindowType>(window_type), is_minimized, is_frameless, progress));
}

void WindowManagerServerConnection::applet_area_size_changed(i32 wm_id, const Gfx::IntSize& size)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMAppletAreaSizeChangedEvent>(size));
}

void WindowManagerServerConnection::window_rect_changed(i32 wm_id, i32 client_id, i32 window_id, Gfx::IntRect const& rect)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMWindowRectChangedEvent>(client_id, window_id, rect));
}

void WindowManagerServerConnection::window_icon_bitmap_changed(i32 wm_id, i32 client_id, i32 window_id, Gfx::ShareableBitmap const& bitmap)
{
    if (auto* window = Window::from_window_id(wm_id)) {
        Core::EventLoop::current().post_event(*window, make<WMWindowIconBitmapChangedEvent>(client_id, window_id, bitmap.bitmap()));
    }
}

void WindowManagerServerConnection::window_removed(i32 wm_id, i32 client_id, i32 window_id)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMWindowRemovedEvent>(client_id, window_id));
}

void WindowManagerServerConnection::super_key_pressed(i32 wm_id)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMSuperKeyPressedEvent>(wm_id));
}

void WindowManagerServerConnection::super_space_key_pressed(i32 wm_id)
{
    if (auto* window = Window::from_window_id(wm_id))
        Core::EventLoop::current().post_event(*window, make<WMSuperSpaceKeyPressedEvent>(wm_id));
}
}
