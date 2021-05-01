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

void WindowManagerServerConnection::handshake()
{
    // :^)
}

void WindowManagerServerConnection::handle(const Messages::WindowManagerClient::WindowStateChanged& message)
{
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowStateChangedEvent>(message.client_id(), message.window_id(), message.parent_client_id(), message.parent_window_id(), message.title(), message.rect(), message.is_active(), message.is_modal(), static_cast<WindowType>(message.window_type()), message.is_minimized(), message.is_frameless(), message.progress()));
}

void WindowManagerServerConnection::handle(const Messages::WindowManagerClient::AppletAreaSizeChanged& message)
{
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMAppletAreaSizeChangedEvent>(message.size()));
}

void WindowManagerServerConnection::handle(const Messages::WindowManagerClient::WindowRectChanged& message)
{
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowRectChangedEvent>(message.client_id(), message.window_id(), message.rect()));
}

void WindowManagerServerConnection::handle(const Messages::WindowManagerClient::WindowIconBitmapChanged& message)
{
    if (auto* window = Window::from_window_id(message.wm_id())) {
        Core::EventLoop::current().post_event(*window, make<WMWindowIconBitmapChangedEvent>(message.client_id(), message.window_id(), message.bitmap().bitmap()));
    }
}

void WindowManagerServerConnection::handle(const Messages::WindowManagerClient::WindowRemoved& message)
{
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowRemovedEvent>(message.client_id(), message.window_id()));
}

void WindowManagerServerConnection::handle(const Messages::WindowManagerClient::SuperKeyPressed& message)
{
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMSuperKeyPressedEvent>(message.wm_id()));
}
}
