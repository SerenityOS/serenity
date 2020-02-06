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

#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GClipboard.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GDragOperation.h>
#include <LibGUI/GEvent.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWindowServerConnection.h>

//#define GEVENTLOOP_DEBUG

namespace GUI {

WindowServerConnection& WindowServerConnection::the()
{
    static WindowServerConnection* s_connection = nullptr;
    if (!s_connection)
        s_connection = new WindowServerConnection;
    return *s_connection;
}

static void set_system_theme_from_shared_buffer_id(int id)
{
    auto system_theme = SharedBuffer::create_from_shared_buffer_id(id);
    ASSERT(system_theme);
    Gfx::set_system_theme(*system_theme);
    Application::the().set_system_palette(*system_theme);
}

void WindowServerConnection::handshake()
{
    auto response = send_sync<WindowServer::Greet>();
    set_my_client_id(response->client_id());
    set_system_theme_from_shared_buffer_id(response->system_theme_buffer_id());
    Desktop::the().did_receive_screen_rect({}, response->screen_rect());
}

void WindowServerConnection::handle(const WindowClient::UpdateSystemTheme& message)
{
    set_system_theme_from_shared_buffer_id(message.system_theme_buffer_id());
    Window::update_all_windows({});
}

void WindowServerConnection::handle(const WindowClient::Paint& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d Paint\n", message.window_id());
#endif
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MultiPaintEvent>(message.rects(), message.window_size()));
}

void WindowServerConnection::handle(const WindowClient::WindowResized& message)
{
    if (auto* window = Window::from_window_id(message.window_id())) {
        Core::EventLoop::current().post_event(*window, make<ResizeEvent>(message.old_rect().size(), message.new_rect().size()));
    }
}

void WindowServerConnection::handle(const WindowClient::WindowActivated& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("(%d) WID=%d WindowActivated\n", getpid(), message.window_id());
#endif
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowBecameActive));
}

void WindowServerConnection::handle(const WindowClient::WindowDeactivated& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("(%d) WID=%d WindowDeactivated\n", getpid(), message.window_id());
#endif
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowBecameInactive));
}

void WindowServerConnection::handle(const WindowClient::WindowCloseRequest& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowCloseRequest));
}

void WindowServerConnection::handle(const WindowClient::WindowEntered& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowEntered));
}

void WindowServerConnection::handle(const WindowClient::WindowLeft& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowLeft));
}

void WindowServerConnection::handle(const WindowClient::KeyDown& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d KeyDown character=0x%02x\n", message.window_id(), message.character());
#endif
    auto* window = Window::from_window_id(message.window_id());
    if (!window)
        return;

    auto key_event = make<KeyEvent>(Event::KeyDown, message.key(), message.modifiers());
    if (message.character() != '\0') {
        char ch = message.character();
        key_event->m_text = String(&ch, 1);
    }

    Action* action = nullptr;

    if (auto* focused_widget = window->focused_widget())
        action = focused_widget->action_for_key_event(*key_event);

    if (!action)
        action = window->action_for_key_event(*key_event);

    if (!action)
        action = Application::the().action_for_key_event(*key_event);

    if (action && action->is_enabled()) {
        action->activate();
        return;
    }
    Core::EventLoop::current().post_event(*window, move(key_event));
}

void WindowServerConnection::handle(const WindowClient::KeyUp& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d KeyUp character=0x%02x\n", message.window_id(), message.character());
#endif
    auto* window = Window::from_window_id(message.window_id());
    if (!window)
        return;

    auto key_event = make<KeyEvent>(Event::KeyUp, message.key(), message.modifiers());
    if (message.character() != '\0') {
        char ch = message.character();
        key_event->m_text = String(&ch, 1);
    }

    Core::EventLoop::current().post_event(*window, move(key_event));
}

MouseButton to_gmousebutton(u32 button)
{
    switch (button) {
    case 0:
        return MouseButton::None;
    case 1:
        return MouseButton::Left;
    case 2:
        return MouseButton::Right;
    case 4:
        return MouseButton::Middle;
    default:
        ASSERT_NOT_REACHED();
        break;
    }
}

void WindowServerConnection::handle(const WindowClient::MouseDown& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseDown %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseDown, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void WindowServerConnection::handle(const WindowClient::MouseUp& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseUp %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseUp, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void WindowServerConnection::handle(const WindowClient::MouseMove& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseMove %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseMove, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void WindowServerConnection::handle(const WindowClient::MouseDoubleClick& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseDoubleClick %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseDoubleClick, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void WindowServerConnection::handle(const WindowClient::MouseWheel& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseWheel %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseWheel, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void WindowServerConnection::handle(const WindowClient::MenuItemActivated& message)
{
    auto* menu = Menu::from_menu_id(message.menu_id());
    if (!menu) {
        dbgprintf("EventLoop received event for invalid menu ID %d\n", message.menu_id());
        return;
    }
    if (auto* action = menu->action_at(message.identifier()))
        action->activate(menu);
}

void WindowServerConnection::handle(const WindowClient::WM_WindowStateChanged& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("EventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowStateChangedEvent>(message.client_id(), message.window_id(), message.title(), message.rect(), message.is_active(), static_cast<WindowType>(message.window_type()), message.is_minimized()));
}

void WindowServerConnection::handle(const WindowClient::WM_WindowRectChanged& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("EventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowRectChangedEvent>(message.client_id(), message.window_id(), message.rect()));
}

void WindowServerConnection::handle(const WindowClient::WM_WindowIconBitmapChanged& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("EventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowIconBitmapChangedEvent>(message.client_id(), message.window_id(), message.icon_buffer_id(), message.icon_size()));
}

void WindowServerConnection::handle(const WindowClient::WM_WindowRemoved& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("EventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowRemovedEvent>(message.client_id(), message.window_id()));
}

void WindowServerConnection::handle(const WindowClient::ScreenRectChanged& message)
{
    Desktop::the().did_receive_screen_rect({}, message.rect());
}

void WindowServerConnection::handle(const WindowClient::ClipboardContentsChanged& message)
{
    Clipboard::the().did_receive_clipboard_contents_changed({}, message.content_type());
}

void WindowServerConnection::handle(const WindowClient::AsyncSetWallpaperFinished&)
{
    // This is handled manually by Desktop::set_wallpaper().
}

void WindowServerConnection::handle(const WindowClient::DragDropped& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<DropEvent>(message.mouse_position(), message.text(), message.data_type(), message.data()));
}

void WindowServerConnection::handle(const WindowClient::DragAccepted&)
{
    DragOperation::notify_accepted({});
}

void WindowServerConnection::handle(const WindowClient::DragCancelled&)
{
    DragOperation::notify_cancelled({});
}

void WindowServerConnection::handle(const WindowClient::WindowStateChanged& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        window->notify_state_changed({}, message.minimized(), message.occluded());
}

}
