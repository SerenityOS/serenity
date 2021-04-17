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
