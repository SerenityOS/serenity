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

#include <AK/SharedBuffer.h>
#include <AK/StringBuilder.h>
#include <LibCore/EventLoop.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/DisplayLink.h>
#include <LibGUI/DragOperation.h>
#include <LibGUI/EmojiInputDialog.h>
#include <LibGUI/Event.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>

//#define KEYBOARD_SHORTCUTS_DEBUG

namespace GUI {

WindowServerConnection& WindowServerConnection::the()
{
    static WindowServerConnection* s_connection = nullptr;
    if (!s_connection)
        s_connection = new WindowServerConnection;
    return *s_connection;
}

static void set_system_theme_from_shbuf_id(int id)
{
    auto system_theme = SharedBuffer::create_from_shbuf_id(id);
    ASSERT(system_theme);
    Gfx::set_system_theme(*system_theme);
    Application::the()->set_system_palette(*system_theme);
}

void WindowServerConnection::handshake()
{
    auto response = send_sync<Messages::WindowServer::Greet>();
    set_my_client_id(response->client_id());
    set_system_theme_from_shbuf_id(response->system_theme_buffer_id());
    Desktop::the().did_receive_screen_rect({}, response->screen_rect());
}

void WindowServerConnection::handle(const Messages::WindowClient::UpdateSystemTheme& message)
{
    set_system_theme_from_shbuf_id(message.system_theme_buffer_id());
    Window::update_all_windows({});
    Window::for_each_window({}, [](auto& window) {
        Core::EventLoop::current().post_event(window, make<ThemeChangeEvent>());
    });
}

void WindowServerConnection::handle(const Messages::WindowClient::Paint& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MultiPaintEvent>(message.rects(), message.window_size()));
}

void WindowServerConnection::handle(const Messages::WindowClient::WindowResized& message)
{
    if (auto* window = Window::from_window_id(message.window_id())) {
        Core::EventLoop::current().post_event(*window, make<ResizeEvent>(message.new_rect().size()));
    }
}

void WindowServerConnection::handle(const Messages::WindowClient::WindowActivated& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowBecameActive));
}

void WindowServerConnection::handle(const Messages::WindowClient::WindowDeactivated& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowBecameInactive));
}

void WindowServerConnection::handle(const Messages::WindowClient::WindowInputEntered& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowInputEntered));
}

void WindowServerConnection::handle(const Messages::WindowClient::WindowInputLeft& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowInputLeft));
}

void WindowServerConnection::handle(const Messages::WindowClient::WindowCloseRequest& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowCloseRequest));
}

void WindowServerConnection::handle(const Messages::WindowClient::WindowEntered& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowEntered));
}

void WindowServerConnection::handle(const Messages::WindowClient::WindowLeft& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowLeft));
}

void WindowServerConnection::handle(const Messages::WindowClient::KeyDown& message)
{
    auto* window = Window::from_window_id(message.window_id());
    if (!window)
        return;

    auto key_event = make<KeyEvent>(Event::KeyDown, (KeyCode)message.key(), message.modifiers(), message.code_point(), message.scancode());
    Action* action = nullptr;

#ifdef KEYBOARD_SHORTCUTS_DEBUG
    dbg() << "Looking up action for " << key_event->to_string();
#endif

    if (auto* focused_widget = window->focused_widget()) {
        for (auto* widget = focused_widget; widget && !action; widget = widget->parent_widget()) {
            action = widget->action_for_key_event(*key_event);
#ifdef KEYBOARD_SHORTCUTS_DEBUG
            dbg() << "  > Focused widget " << *widget << " gave action: " << action;
#endif
        }
    }

    if (!action) {
        action = window->action_for_key_event(*key_event);
#ifdef KEYBOARD_SHORTCUTS_DEBUG
        dbg() << "  > Asked window " << *window << ", got action: " << action;
#endif
    }

    if (!action) {
        action = Application::the()->action_for_key_event(*key_event);
#ifdef KEYBOARD_SHORTCUTS_DEBUG
        dbg() << "  > Asked application, got action: " << action;
#endif
    }

    if (action && action->is_enabled()) {
        action->activate();
        return;
    }

    bool focused_widget_accepts_emoji_input = window->focused_widget() && window->focused_widget()->accepts_emoji_input();
    if (focused_widget_accepts_emoji_input && (message.modifiers() == (Mod_Ctrl | Mod_Alt)) && message.key() == Key_Space) {
        auto emoji_input_dialog = EmojiInputDialog::construct(window);
        if (emoji_input_dialog->exec() != EmojiInputDialog::ExecOK)
            return;
        key_event->m_key = Key_Invalid;
        key_event->m_modifiers = 0;

        AK::Utf8View m_utf8_view(emoji_input_dialog->selected_emoji_text().characters());
        u32 code_point = *m_utf8_view.begin();

        key_event->m_code_point = code_point;
    }

    Core::EventLoop::current().post_event(*window, move(key_event));
}

void WindowServerConnection::handle(const Messages::WindowClient::KeyUp& message)
{
    auto* window = Window::from_window_id(message.window_id());
    if (!window)
        return;

    auto key_event = make<KeyEvent>(Event::KeyUp, (KeyCode)message.key(), message.modifiers(), message.code_point(), message.scancode());
    Core::EventLoop::current().post_event(*window, move(key_event));
}

static MouseButton to_gmousebutton(u32 button)
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
    case 8:
        return MouseButton::Back;
    case 16:
        return MouseButton::Forward;
    default:
        ASSERT_NOT_REACHED();
        break;
    }
}

void WindowServerConnection::handle(const Messages::WindowClient::MouseDown& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseDown, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void WindowServerConnection::handle(const Messages::WindowClient::MouseUp& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseUp, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void WindowServerConnection::handle(const Messages::WindowClient::MouseMove& message)
{
    if (auto* window = Window::from_window_id(message.window_id())) {
        if (message.is_drag())
            Core::EventLoop::current().post_event(*window, make<DragEvent>(Event::DragMove, message.mouse_position(), message.drag_data_type()));
        else
            Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseMove, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
    }
}

void WindowServerConnection::handle(const Messages::WindowClient::MouseDoubleClick& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseDoubleClick, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void WindowServerConnection::handle(const Messages::WindowClient::MouseWheel& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseWheel, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void WindowServerConnection::handle(const Messages::WindowClient::MenuItemActivated& message)
{
    auto* menu = Menu::from_menu_id(message.menu_id());
    if (!menu) {
        dbgprintf("EventLoop received event for invalid menu ID %d\n", message.menu_id());
        return;
    }
    if (auto* action = menu->action_at(message.identifier()))
        action->activate(menu);
}

void WindowServerConnection::handle(const Messages::WindowClient::WM_WindowStateChanged& message)
{
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowStateChangedEvent>(message.client_id(), message.window_id(), message.parent_client_id(), message.parent_window_id(), message.title(), message.rect(), message.is_active(), message.is_modal(), static_cast<WindowType>(message.window_type()), message.is_minimized(), message.is_frameless(), message.progress()));
}

void WindowServerConnection::handle(const Messages::WindowClient::WM_WindowRectChanged& message)
{
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowRectChangedEvent>(message.client_id(), message.window_id(), message.rect()));
}

void WindowServerConnection::handle(const Messages::WindowClient::WM_WindowIconBitmapChanged& message)
{
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowIconBitmapChangedEvent>(message.client_id(), message.window_id(), message.icon_buffer_id(), message.icon_size()));
}

void WindowServerConnection::handle(const Messages::WindowClient::WM_WindowRemoved& message)
{
    if (auto* window = Window::from_window_id(message.wm_id()))
        Core::EventLoop::current().post_event(*window, make<WMWindowRemovedEvent>(message.client_id(), message.window_id()));
}

void WindowServerConnection::handle(const Messages::WindowClient::ScreenRectChanged& message)
{
    Desktop::the().did_receive_screen_rect({}, message.rect());
}

void WindowServerConnection::handle(const Messages::WindowClient::AsyncSetWallpaperFinished&)
{
    // This is handled manually by Desktop::set_wallpaper().
}

void WindowServerConnection::handle(const Messages::WindowClient::DragDropped& message)
{
    if (auto* window = Window::from_window_id(message.window_id())) {
        auto mime_data = Core::MimeData::construct();
        mime_data->set_data(message.data_type(), message.data().to_byte_buffer());
        Core::EventLoop::current().post_event(*window, make<DropEvent>(message.mouse_position(), message.text(), mime_data));
    }
}

void WindowServerConnection::handle(const Messages::WindowClient::DragAccepted&)
{
    DragOperation::notify_accepted({});
}

void WindowServerConnection::handle(const Messages::WindowClient::DragCancelled&)
{
    DragOperation::notify_cancelled({});
}

void WindowServerConnection::handle(const Messages::WindowClient::WindowStateChanged& message)
{
    if (auto* window = Window::from_window_id(message.window_id()))
        window->notify_state_changed({}, message.minimized(), message.occluded());
}

void WindowServerConnection::handle(const Messages::WindowClient::DisplayLinkNotification&)
{
    if (m_display_link_notification_pending)
        return;

    m_display_link_notification_pending = true;
    deferred_invoke([this](auto&) {
        DisplayLink::notify({});
        m_display_link_notification_pending = false;
    });
}

void WindowServerConnection::handle(const Messages::WindowClient::Ping&)
{
    post_message(Messages::WindowServer::Pong());
}

}
