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
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/DisplayLink.h>
#include <LibGUI/DragOperation.h>
#include <LibGUI/EmojiInputDialog.h>
#include <LibGUI/Event.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MouseTracker.h>
#include <LibGUI/Shortcut.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/SystemTheme.h>

namespace GUI {

ConnectionToWindowServer& ConnectionToWindowServer::the()
{
    static RefPtr<ConnectionToWindowServer> s_connection = nullptr;
    if (!s_connection)
        s_connection = ConnectionToWindowServer::try_create().release_value_but_fixme_should_propagate_errors();
    return *s_connection;
}

static void set_system_theme_from_anonymous_buffer(Core::AnonymousBuffer buffer)
{
    Gfx::set_system_theme(buffer);
    Application::the()->set_system_palette(buffer);
}

ConnectionToWindowServer::ConnectionToWindowServer(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionToServer<WindowClientEndpoint, WindowServerEndpoint>(*this, move(socket))
{
    // NOTE: WindowServer automatically sends a "fast_greet" message to us when we connect.
    //       All we have to do is wait for it to arrive. This avoids a round-trip during application startup.
    auto message = wait_for_specific_message<Messages::WindowClient::FastGreet>();
    set_system_theme_from_anonymous_buffer(message->theme_buffer());
    Desktop::the().did_receive_screen_rects({}, message->screen_rects(), message->main_screen_index(), message->workspace_rows(), message->workspace_columns());
    Desktop::the().set_system_effects(message->effects());
    Gfx::FontDatabase::set_default_font_query(message->default_font_query());
    Gfx::FontDatabase::set_fixed_width_font_query(message->fixed_width_font_query());
    Gfx::FontDatabase::set_window_title_font_query(message->window_title_font_query());
    m_client_id = message->client_id();
}

void ConnectionToWindowServer::fast_greet(Vector<Gfx::IntRect> const&, u32, u32, u32, Core::AnonymousBuffer const&, ByteString const&, ByteString const&, ByteString const&, Vector<bool> const&, i32)
{
    // NOTE: This message is handled in the constructor.
}

void ConnectionToWindowServer::update_system_theme(Core::AnonymousBuffer const& theme_buffer)
{
    set_system_theme_from_anonymous_buffer(theme_buffer);
    Window::update_all_windows({});
    Window::for_each_window({}, [](auto& window) {
        Core::EventLoop::current().post_event(window, make<ThemeChangeEvent>());
    });

    Application::the()->dispatch_event(*make<ThemeChangeEvent>());
}

void ConnectionToWindowServer::update_system_fonts(ByteString const& default_font_query, ByteString const& fixed_width_font_query, ByteString const& window_title_font_query)
{
    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);
    Gfx::FontDatabase::set_window_title_font_query(window_title_font_query);
    Window::update_all_windows({});
    Window::for_each_window({}, [](auto& window) {
        Core::EventLoop::current().post_event(window, make<FontsChangeEvent>());
    });
}

void ConnectionToWindowServer::update_system_effects(Vector<bool> const& effects)
{
    Desktop::the().set_system_effects(effects);
}

void ConnectionToWindowServer::paint(i32 window_id, Gfx::IntSize window_size, Vector<Gfx::IntRect> const& rects)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MultiPaintEvent>(rects, window_size));
}

void ConnectionToWindowServer::window_resized(i32 window_id, Gfx::IntRect const& new_rect)
{
    if (auto* window = Window::from_window_id(window_id)) {
        Core::EventLoop::current().post_event(*window, make<ResizeEvent>(new_rect.size()));
    }
}

void ConnectionToWindowServer::window_moved(i32 window_id, Gfx::IntRect const& new_rect)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MoveEvent>(new_rect.location()));
}

void ConnectionToWindowServer::window_activated(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowBecameActive));
}

void ConnectionToWindowServer::window_deactivated(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowBecameInactive));
}

void ConnectionToWindowServer::window_input_preempted(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowInputPreempted));
}

void ConnectionToWindowServer::window_input_restored(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowInputRestored));
}

void ConnectionToWindowServer::window_close_request(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowCloseRequest));
}

void ConnectionToWindowServer::window_entered(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowEntered));
}

void ConnectionToWindowServer::window_left(i32 window_id)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<Event>(Event::WindowLeft));
}

static Action* action_for_shortcut(Window& window, Shortcut const& shortcut)
{
    if (!shortcut.is_valid())
        return nullptr;

    dbgln_if(KEYBOARD_SHORTCUTS_DEBUG, "Looking up action for {}", shortcut.to_byte_string());

    for (auto* widget = window.focused_widget(); widget; widget = widget->parent_widget()) {
        if (auto* action = widget->action_for_shortcut(shortcut)) {
            dbgln_if(KEYBOARD_SHORTCUTS_DEBUG, "  > Focused widget {} gave action: {} {} (enabled: {}, shortcut: {}, alt-shortcut: {})", *widget, action, action->text(), action->is_enabled(), action->shortcut().to_byte_string(), action->alternate_shortcut().to_byte_string());
            return action;
        }
    }

    if (auto* action = window.action_for_shortcut(shortcut)) {
        dbgln_if(KEYBOARD_SHORTCUTS_DEBUG, "  > Asked window {}, got action: {} {} (enabled: {}, shortcut: {}, alt-shortcut: {})", window, action, action->text(), action->is_enabled(), action->shortcut().to_byte_string(), action->alternate_shortcut().to_byte_string());
        return action;
    }

    // NOTE: Application-global shortcuts are ignored while a blocking modal window is up.
    if (!window.is_blocking() && !window.is_popup()) {
        if (auto* action = Application::the()->action_for_shortcut(shortcut)) {
            dbgln_if(KEYBOARD_SHORTCUTS_DEBUG, "  > Asked application, got action: {} {} (enabled: {}, shortcut: {}, alt-shortcut: {})", action, action->text(), action->is_enabled(), action->shortcut().to_byte_string(), action->alternate_shortcut().to_byte_string());
            return action;
        }
    }

    return nullptr;
}

void ConnectionToWindowServer::key_down(i32 window_id, u32 code_point, u32 key, u8 map_entry_index, u32 modifiers, u32 scancode)
{
    auto* window = Window::from_window_id(window_id);
    if (!window)
        return;

    auto key_event = make<KeyEvent>(Event::KeyDown, (KeyCode)key, map_entry_index, modifiers, code_point, scancode);

    bool focused_widget_accepts_emoji_input = window->focused_widget() && window->focused_widget()->on_emoji_input;
    if (!window->blocks_emoji_input() && focused_widget_accepts_emoji_input && (modifiers == (Mod_Ctrl | Mod_Alt)) && key == Key_Space) {
        auto emoji_input_dialog = EmojiInputDialog::construct(window);
        if (emoji_input_dialog->exec() != EmojiInputDialog::ExecResult::OK)
            return;

        window->focused_widget()->on_emoji_input(emoji_input_dialog->selected_emoji_text());
        return;
    }

    Core::EventLoop::current().post_event(*window, move(key_event));
}

void ConnectionToWindowServer::key_up(i32 window_id, u32 code_point, u32 key, u8 map_entry_index, u32 modifiers, u32 scancode)
{
    auto* window = Window::from_window_id(window_id);
    if (!window)
        return;

    auto key_event = make<KeyEvent>(Event::KeyUp, (KeyCode)key, map_entry_index, modifiers, code_point, scancode);
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

void ConnectionToWindowServer::mouse_down(i32 window_id, Gfx::IntPoint mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y, i32 wheel_raw_delta_x, i32 wheel_raw_delta_y)
{
    auto* window = Window::from_window_id(window_id);
    if (!window)
        return;

    auto mouse_event = make<MouseEvent>(Event::MouseDown, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y, wheel_raw_delta_x, wheel_raw_delta_y);

    if (auto* action = action_for_shortcut(*window, Shortcut(mouse_event->modifiers(), mouse_event->button()))) {
        if (action->is_enabled()) {
            action->flash_menubar_menu(*window);
            action->activate();
            return;
        }
        if (action->swallow_key_event_when_disabled())
            return;
    }

    Core::EventLoop::current().post_event(*window, move(mouse_event));
}

void ConnectionToWindowServer::mouse_up(i32 window_id, Gfx::IntPoint mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y, i32 wheel_raw_delta_x, i32 wheel_raw_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseUp, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y, wheel_raw_delta_x, wheel_raw_delta_y));
}

void ConnectionToWindowServer::mouse_move(i32 window_id, Gfx::IntPoint mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y, i32 wheel_raw_delta_x, i32 wheel_raw_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseMove, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y, wheel_raw_delta_x, wheel_raw_delta_y));
}

void ConnectionToWindowServer::mouse_double_click(i32 window_id, Gfx::IntPoint mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y, i32 wheel_raw_delta_x, i32 wheel_raw_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseDoubleClick, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y, wheel_raw_delta_x, wheel_raw_delta_y));
}

void ConnectionToWindowServer::mouse_wheel(i32 window_id, Gfx::IntPoint mouse_position, u32 button, u32 buttons, u32 modifiers, i32 wheel_delta_x, i32 wheel_delta_y, i32 wheel_raw_delta_x, i32 wheel_raw_delta_y)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<MouseEvent>(Event::MouseWheel, mouse_position, buttons, to_mouse_button(button), modifiers, wheel_delta_x, wheel_delta_y, wheel_raw_delta_x, wheel_raw_delta_y));
}

void ConnectionToWindowServer::menu_visibility_did_change(i32 menu_id, bool visible)
{
    auto* menu = Menu::from_menu_id(menu_id);
    if (!menu) {
        dbgln("EventLoop received visibility change event for invalid menu ID {}", menu_id);
        return;
    }
    menu->visibility_did_change({}, visible);
}

void ConnectionToWindowServer::menu_item_activated(i32 menu_id, u32 identifier)
{
    auto* menu = Menu::from_menu_id(menu_id);
    if (!menu) {
        dbgln("EventLoop received event for invalid menu ID {}", menu_id);
        return;
    }
    if (auto* action = menu->action_at(identifier))
        action->activate(menu);
}

void ConnectionToWindowServer::menu_item_entered(i32 menu_id, u32 identifier)
{
    auto* menu = Menu::from_menu_id(menu_id);
    if (!menu) {
        dbgln("ConnectionToWindowServer received MenuItemEntered for invalid menu ID {}", menu_id);
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

void ConnectionToWindowServer::menu_item_left(i32 menu_id, u32 identifier)
{
    auto* menu = Menu::from_menu_id(menu_id);
    if (!menu) {
        dbgln("ConnectionToWindowServer received MenuItemLeft for invalid menu ID {}", menu_id);
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

void ConnectionToWindowServer::screen_rects_changed(Vector<Gfx::IntRect> const& rects, u32 main_screen_index, u32 workspace_rows, u32 workspace_columns)
{
    Desktop::the().did_receive_screen_rects({}, rects, main_screen_index, workspace_rows, workspace_columns);
    Window::for_each_window({}, [&](auto& window) {
        Core::EventLoop::current().post_event(window, make<ScreenRectsChangeEvent>(rects, main_screen_index));
    });
}

void ConnectionToWindowServer::applet_area_rect_changed(Gfx::IntRect const& rect)
{
    Window::for_each_window({}, [&](auto& window) {
        Core::EventLoop::current().post_event(window, make<AppletAreaRectChangeEvent>(rect));
    });
}

void ConnectionToWindowServer::drag_moved(i32 window_id, Gfx::IntPoint mouse_position, u32 button, u32 buttons, u32 modifiers, ByteString const& text, HashMap<String, ByteBuffer> const& mime_data)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<DragEvent>(Event::Type::DragMove, mouse_position, to_mouse_button(button), buttons, modifiers, text, Core::MimeData::construct(mime_data)));
}

void ConnectionToWindowServer::drag_dropped(i32 window_id, Gfx::IntPoint mouse_position, u32 button, u32 buttons, u32 modifiers, ByteString const& text, HashMap<String, ByteBuffer> const& mime_data)
{
    if (auto* window = Window::from_window_id(window_id))
        Core::EventLoop::current().post_event(*window, make<DropEvent>(Event::Type::Drop, mouse_position, to_mouse_button(button), buttons, modifiers, text, Core::MimeData::construct(mime_data)));
}

void ConnectionToWindowServer::drag_accepted()
{
    DragOperation::notify_accepted({});
}

void ConnectionToWindowServer::drag_cancelled()
{
    DragOperation::notify_cancelled({});
    Application::the()->notify_drag_cancelled({});
}

void ConnectionToWindowServer::window_state_changed(i32 window_id, bool minimized, bool maximized, bool occluded)
{
    if (auto* window = Window::from_window_id(window_id))
        window->notify_state_changed({}, minimized, maximized, occluded);
}

void ConnectionToWindowServer::display_link_notification()
{
    if (m_display_link_notification_pending)
        return;

    m_display_link_notification_pending = true;
    deferred_invoke([this] {
        DisplayLink::notify({});
        m_display_link_notification_pending = false;
    });
}

void ConnectionToWindowServer::track_mouse_move(Gfx::IntPoint mouse_position)
{
    MouseTracker::track_mouse_move({}, mouse_position);
}

void ConnectionToWindowServer::ping()
{
    async_pong();
}

}
