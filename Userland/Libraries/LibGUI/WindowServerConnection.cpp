/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/StringBuilder.h>
#include <LibCore/EventLoop.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/CommandPalette.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/DisplayLink.h>
#include <LibGUI/DragOperation.h>
#include <LibGUI/EmojiInputDialog.h>
#include <LibGUI/Event.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MouseTracker.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>

namespace GUI {

WindowServerConnection& WindowServerConnection::the()
{
    static RefPtr<WindowServerConnection> s_connection = nullptr;
    if (!s_connection)
        s_connection = WindowServerConnection::try_create().release_value_but_fixme_should_propagate_errors();
    return *s_connection;
}

static void set_system_theme_from_anonymous_buffer(Core::AnonymousBuffer buffer)
{
    Gfx::set_system_theme(buffer);
    Application::the()->set_system_palette(buffer);
}

WindowServerConnection::WindowServerConnection(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
    : IPC::ServerConnection<WindowClientEndpoint, WindowServerEndpoint>(*this, move(socket))
{
    // NOTE: WindowServer automatically sends a "fast_greet" message to us when we connect.
    //       All we have to do is wait for it to arrive. This avoids a round-trip during application startup.
    auto message = wait_for_specific_message<Messages::WindowClient::FastGreet>();
    set_system_theme_from_anonymous_buffer(message->theme_buffer());
    Desktop::the().did_receive_screen_rects({}, message->screen_rects(), message->main_screen_index(), message->workspace_rows(), message->workspace_columns());
    Gfx::FontDatabase::set_default_font_query(message->default_font_query());
    Gfx::FontDatabase::set_fixed_width_font_query(message->fixed_width_font_query());
    m_client_id = message->client_id();
}

void WindowServerConnection::fast_greet(Vector<Gfx::IntRect> const&, u32, u32, u32, Core::AnonymousBuffer const&, String const&, String const&, i32)
{
    // NOTE: This message is handled in the constructor.
}

void WindowServerConnection::update_system_theme(Core::AnonymousBuffer const& theme_buffer)
{
    set_system_theme_from_anonymous_buffer(theme_buffer);
    Window::update_all_windows({});
    Window::for_each_window({}, [](auto& window) {
        Core::EventLoop::current().post_event(window, make<ThemeChangeEvent>());
    });
}

void WindowServerConnection::update_system_fonts(const String& default_font_query, const String& fixed_width_font_query)
{
    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);
    Window::update_all_windows({});
    Window::for_each_window({}, [](auto& window) {
        Core::EventLoop::current().post_event(window, make<FontsChangeEvent>());
    });
}

void WindowServerConnection::paint(i32 window_id, Gfx::IntSize const& window_size, Vector<Gfx::IntRect> const& rects)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MultiPaintEvent>(rects, window_size));
}

void WindowServerConnection::window_resized(i32 window_id, Gfx::IntRect const& new_rect)
{
    if (auto* window = Window::from_window_id(window_id)) {
        Core::EventLoop::current().post_event(*window, make<ResizeEvent>(new_rect.size()));
    }
}

void WindowServerConnection::window_activated(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowBecameActive));
}

void WindowServerConnection::window_deactivated(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowBecameInactive));
}

void WindowServerConnection::window_input_entered(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowInputEntered));
}

void WindowServerConnection::window_input_left(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowInputLeft));
}

void WindowServerConnection::window_close_request(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowCloseRequest));
}

void WindowServerConnection::window_entered(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowEntered));
}

void WindowServerConnection::window_left(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowLeft));
}

static Action* action_for_key_event(Window& window, KeyEvent const& event)
{
    if (event.key() == KeyCode::Key_Invalid)
        return nullptr;

    dbgln_if(KEYBOARD_SHORTCUTS_DEBUG, "Looking up action for {}", event.to_string());

    for (auto* widget = window.focused_widget(); widget; widget = widget->parent_widget()) {
        if (auto* action = widget->action_for_key_event(event)) {
            dbgln_if(KEYBOARD_SHORTCUTS_DEBUG, "  > Focused widget {} gave action: {}", *widget, action);
            return action;
        }
    }

    if (auto* action = window.action_for_key_event(event)) {
        dbgln_if(KEYBOARD_SHORTCUTS_DEBUG, "  > Asked window {}, got action: {}", window, action);
        return action;
    }

    // NOTE: Application-global shortcuts are ignored while a modal window is up.
    if (!window.is_modal()) {
        if (auto* action = Application::the()->action_for_key_event(event)) {
            dbgln_if(KEYBOARD_SHORTCUTS_DEBUG, "  > Asked application, got action: {}", action);
            return action;
        }
    }

    return nullptr;
}

void WindowServerConnection::key_down(i32 window_id, u32 code_point, u32 key, u32 modifiers, u32 scancode)
{
    auto* window = Window::from_window_id(window_id);
    if (!window)
        return;

    auto key_event = make<KeyEvent>(Event::KeyDown, (KeyCode)key, modifiers, code_point, scancode);

    if (auto* action = action_for_key_event(*window, *key_event)) {
        if (action->is_enabled()) {
            action->flash_menubar_menu(*window);
            action->activate();
            return;
        }
        if (action->swallow_key_event_when_disabled())
            return;
    }

    bool focused_widget_accepts_emoji_input = window->focused_widget() && window->focused_widget()->accepts_emoji_input();
    if (focused_widget_accepts_emoji_input && (modifiers == (Mod_Ctrl | Mod_Alt)) && key == Key_Space) {
        auto emoji_input_dialog = EmojiInputDialog::construct(window);
        if (emoji_input_dialog->exec() != EmojiInputDialog::ExecOK)
            return;
        key_event->m_key = Key_Invalid;
        key_event->m_modifiers = 0;

        Utf8View m_utf8_view(emoji_input_dialog->selected_emoji_text());
        u32 emoji_code_point = *m_utf8_view.begin();

        key_event->m_code_point = emoji_code_point;
    }

    // FIXME: This shortcut should be configurable.
    bool focused_widget_accepts_command_palette = window->focused_widget() && window->focused_widget()->accepts_command_palette();
    if (focused_widget_accepts_command_palette && !m_in_command_palette && modifiers == (Mod_Ctrl | Mod_Shift) && key == Key_A) {
        auto command_palette = CommandPalette::construct(*window);
        TemporaryChange change { m_in_command_palette, true };
        if (command_palette->exec() != GUI::Dialog::ExecOK)
            return;
        auto* action = command_palette->selected_action();
        VERIFY(action);
        action->flash_menubar_menu(*window);
        action->activate();
        return;
    }

    Core::EventLoop::current().post_event(*window, move(key_event));
}

void WindowServerConnection::key_up(i32 window_id, u32 code_point, u32 key, u32 modifiers, u32 scancode)
{
    auto* window = Window::from_window_id(window_id);
    if (!window)
        return;

    auto key_event = make<KeyEvent>(Event::KeyUp, (KeyCode)key, modifiers, code_point, scancode);
    Core::EventLoop::current().post_event(*window, move(key_event));
}

static MouseButton to_mouse_button(u32 button)
{
    switch (button) {
    case 0:
        return MouseButton::None;
    case 1:
        return MouseButton::Primary;
    case 2:
        return MouseButton::Secondary;
    case 4:
        return MouseButton::Middle;
    case 8:
        return MouseButton::Backward;
    case 16:
        return MouseButton::Forward;
    default:
        VERIFY_NOT_REACHED();
        break;
    }
}

void WindowServerConnection::mouse_down(i32 window_id, Gfx::IntPoint const& mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseDown, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y));
}

void WindowServerConnection::mouse_up(i32 window_id, Gfx::IntPoint const& mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseUp, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y));
}

void WindowServerConnection::mouse_move(i32 window_id, Gfx::IntPoint const& mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y, bool is_drag, Vector<String> const& mime_types)
{
    if (auto* window = Window::from_window_id(window_id)) {
        if (is_drag)
            Core::EventLoop::current().post_event(*window, make<DragEvent>(Event::DragMove, mouse_position, mime_types));
        else
            Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseMove, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y));
    }
}

void WindowServerConnection::mouse_double_click(i32 window_id, Gfx::IntPoint const& mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseDoubleClick, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y));
}

void WindowServerConnection::mouse_wheel(i32 window_id, Gfx::IntPoint const& mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseWheel, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y));
}

void WindowServerConnection::menu_visibility_did_change(i32 menu_id, bool visible)
{
    auto* menu = Menu::from_menu_id(menu_id);
    if (!menu) {
        dbgln("EventLoop received visibility change event for invalid menu ID {}", menu_id);
        return;
    }
    menu->visibility_did_change({}, visible);
}

void WindowServerConnection::menu_item_activated(i32 menu_id, u32 identifier)
{
    auto* menu = Menu::from_menu_id(menu_id);
    if (!menu) {
        dbgln("EventLoop received event for invalid menu ID {}", menu_id);
        return;
    }
    if (auto* action = menu->action_at(identifier))
        action->activate(menu);
}

void WindowServerConnection::menu_item_entered(i32 menu_id, u32 identifier)
{
    auto* menu = Menu::from_menu_id(menu_id);
    if (!menu) {
        dbgln("WindowServerConnection received MenuItemEntered for invalid menu ID {}", menu_id);
        return;
    }
    auto* action = menu->action_at(identifier);
    if (!action)
        return;
    auto* app = Application::the();
    if (!app)
        return;
    Core::EventLoop::current().post_event(*app, make<ActionEvent>(GUI::Event::ActionEnter, *action));
}

void WindowServerConnection::menu_item_left(i32 menu_id, u32 identifier)
{
    auto* menu = Menu::from_menu_id(menu_id);
    if (!menu) {
        dbgln("WindowServerConnection received MenuItemLeft for invalid menu ID {}", menu_id);
        return;
    }
    auto* action = menu->action_at(identifier);
    if (!action)
        return;
    auto* app = Application::the();
    if (!app)
        return;
    Core::EventLoop::current().post_event(*app, make<ActionEvent>(GUI::Event::ActionLeave, *action));
}

void WindowServerConnection::screen_rects_changed(Vector<Gfx::IntRect> const& rects, u32 main_screen_index, u32 workspace_rows, u32 workspace_columns)
{
    Desktop::the().did_receive_screen_rects({}, rects, main_screen_index, workspace_rows, workspace_columns);
    Window::for_each_window({}, [&](auto& window) {
        Core::EventLoop::current().post_event(window, make<ScreenRectsChangeEvent>(rects, main_screen_index));
    });
}

void WindowServerConnection::applet_area_rect_changed(Gfx::IntRect const& rect)
{
    Window::for_each_window({}, [&](auto& window) {
        Core::EventLoop::current().post_event(window, make<AppletAreaRectChangeEvent>(rect));
    });
}

void WindowServerConnection::set_wallpaper_finished(bool)
{
    // This is handled manually by Desktop::set_wallpaper().
}

void WindowServerConnection::drag_dropped(i32 window_id, Gfx::IntPoint const& mouse_position, String const& text, HashMap<String, ByteBuffer> const& mime_data)
{
    if (auto* window = Window::from_window_id(window_id)) {
        auto mime_data_obj = Core::MimeData::construct(mime_data);
        Core::EventLoop::current().post_event(*window, make<DropEvent>(mouse_position, text, mime_data_obj));
    }
}

void WindowServerConnection::drag_accepted()
{
    DragOperation::notify_accepted({});
}

void WindowServerConnection::drag_cancelled()
{
    DragOperation::notify_cancelled({});
    Application::the()->notify_drag_cancelled({});
}

void WindowServerConnection::window_state_changed(i32 window_id, bool minimized, bool occluded)
{
    if (auto* window = Window::from_window_id(window_id))
        window->notify_state_changed({}, minimized, occluded);
}

void WindowServerConnection::display_link_notification()
{
    if (m_display_link_notification_pending)
        return;

    m_display_link_notification_pending = true;
    deferred_invoke([this] {
        DisplayLink::notify({});
        m_display_link_notification_pending = false;
    });
}

void WindowServerConnection::track_mouse_move(Gfx::IntPoint const& mouse_position)
{
    MouseTracker::track_mouse_move({}, mouse_position);
}

void WindowServerConnection::ping()
{
    async_pong();
}

}
