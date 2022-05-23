/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/StandardCursor.h>
#include <LibGfx/SystemTheme.h>
#include <WindowServer/AppletManager.h>
#include <WindowServer/Compositor.h>
#include <WindowServer/ConnectionFromClient.h>
#include <WindowServer/Menu.h>
#include <WindowServer/MenuItem.h>
#include <WindowServer/Screen.h>
#include <WindowServer/Window.h>
#include <WindowServer/WindowClientEndpoint.h>
#include <WindowServer/WindowManager.h>
#include <WindowServer/WindowSwitcher.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>

namespace WindowServer {

HashMap<int, NonnullRefPtr<ConnectionFromClient>>* s_connections;

void ConnectionFromClient::for_each_client(Function<void(ConnectionFromClient&)> callback)
{
    if (!s_connections)
        return;
    for (auto& it : *s_connections) {
        callback(*it.value);
    }
}

ConnectionFromClient* ConnectionFromClient::from_client_id(int client_id)
{
    if (!s_connections)
        return nullptr;
    auto it = s_connections->find(client_id);
    if (it == s_connections->end())
        return nullptr;
    return (*it).value.ptr();
}

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> client_socket, int client_id)
    : IPC::ConnectionFromClient<WindowClientEndpoint, WindowServerEndpoint>(*this, move(client_socket), client_id)
{
    if (!s_connections)
        s_connections = new HashMap<int, NonnullRefPtr<ConnectionFromClient>>;
    s_connections->set(client_id, *this);

    auto& wm = WindowManager::the();
    async_fast_greet(Screen::rects(), Screen::main().index(), wm.window_stack_rows(), wm.window_stack_columns(), Gfx::current_system_theme_buffer(), Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query(), client_id);
}

ConnectionFromClient::~ConnectionFromClient()
{
    auto& wm = WindowManager::the();
    if (wm.dnd_client() == this)
        wm.end_dnd_drag();

    if (m_has_display_link)
        Compositor::the().decrement_display_link_count({});

    MenuManager::the().close_all_menus_from_client({}, *this);
    auto windows = move(m_windows);
    for (auto& window : windows) {
        window.value->detach_client({});
        if (window.value->type() == WindowType::Applet)
            AppletManager::the().remove_applet(window.value);
    }

    if (m_show_screen_number)
        Compositor::the().decrement_show_screen_number({});
}

void ConnectionFromClient::die()
{
    deferred_invoke([this] {
        s_connections->remove(client_id());
    });
}

void ConnectionFromClient::notify_about_new_screen_rects()
{
    auto& wm = WindowManager::the();
    async_screen_rects_changed(Screen::rects(), Screen::main().index(), wm.window_stack_rows(), wm.window_stack_columns());
}

void ConnectionFromClient::create_menu(i32 menu_id, String const& menu_title)
{
    auto menu = Menu::construct(this, menu_id, menu_title);
    m_menus.set(menu_id, move(menu));
}

void ConnectionFromClient::destroy_menu(i32 menu_id)
{
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("DestroyMenu: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.close();
    m_menus.remove(it);
    remove_child(menu);
}

void ConnectionFromClient::add_menu(i32 window_id, i32 menu_id)
{
    auto it = m_windows.find(window_id);
    auto jt = m_menus.find(menu_id);
    if (it == m_windows.end()) {
        did_misbehave("AddMenu: Bad window ID");
        return;
    }
    if (jt == m_menus.end()) {
        did_misbehave("AddMenu: Bad menu ID");
        return;
    }
    auto& window = *(*it).value;
    auto& menu = *(*jt).value;
    window.add_menu(menu);
}

void ConnectionFromClient::add_menu_item(i32 menu_id, i32 identifier, i32 submenu_id,
    String const& text, bool enabled, bool checkable, bool checked, bool is_default,
    String const& shortcut, Gfx::ShareableBitmap const& icon, bool exclusive)
{
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        dbgln("AddMenuItem: Bad menu ID: {}", menu_id);
        return;
    }
    auto& menu = *(*it).value;
    auto menu_item = make<MenuItem>(menu, identifier, text, shortcut, enabled, checkable, checked);
    if (is_default)
        menu_item->set_default(true);
    menu_item->set_icon(icon.bitmap());
    menu_item->set_submenu_id(submenu_id);
    menu_item->set_exclusive(exclusive);
    menu.add_item(move(menu_item));
}

void ConnectionFromClient::popup_menu(i32 menu_id, Gfx::IntPoint const& screen_position)
{
    auto position = screen_position;
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("PopupMenu: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.popup(position);
}

void ConnectionFromClient::dismiss_menu(i32 menu_id)
{
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("DismissMenu: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.close();
}

void ConnectionFromClient::update_menu_item(i32 menu_id, i32 identifier, [[maybe_unused]] i32 submenu_id,
    String const& text, bool enabled, bool checkable, bool checked, bool is_default,
    String const& shortcut)
{
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("UpdateMenuItem: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    auto* menu_item = menu.item_with_identifier(identifier);
    if (!menu_item) {
        did_misbehave("UpdateMenuItem: Bad menu item identifier");
        return;
    }
    menu_item->set_text(text);
    menu_item->set_shortcut_text(shortcut);
    menu_item->set_enabled(enabled);
    menu_item->set_checkable(checkable);
    menu_item->set_default(is_default);
    if (checkable)
        menu_item->set_checked(checked);
}

void ConnectionFromClient::remove_menu_item(i32 menu_id, i32 identifier)
{
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("RemoveMenuItem: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    if (!menu.remove_item_with_identifier(identifier))
        did_misbehave("RemoveMenuItem: Bad menu item identifier");
}

void ConnectionFromClient::flash_menubar_menu(i32 window_id, i32 menu_id)
{
    auto itw = m_windows.find(window_id);
    if (itw == m_windows.end()) {
        did_misbehave("FlashMenubarMenu: Bad window ID");
        return;
    }
    auto& window = *(*itw).value;

    auto itm = m_menus.find(menu_id);
    if (itm == m_menus.end()) {
        did_misbehave("FlashMenubarMenu: Bad menu ID");
        return;
    }
    auto& menu = *(*itm).value;

    if (window.menubar().flash_menu(&menu)) {
        window.frame().invalidate_menubar();

        if (m_flashed_menu_timer && m_flashed_menu_timer->is_active()) {
            m_flashed_menu_timer->on_timeout();
            m_flashed_menu_timer->stop();
        }

        m_flashed_menu_timer = Core::Timer::create_single_shot(75, [weak_window = window.make_weak_ptr<Window>()]() mutable {
            if (!weak_window)
                return;
            weak_window->menubar().flash_menu(nullptr);
            weak_window->frame().invalidate_menubar();
        });
        m_flashed_menu_timer->start();
    } else if (m_flashed_menu_timer) {
        m_flashed_menu_timer->restart();
    }
}

void ConnectionFromClient::add_menu_separator(i32 menu_id)
{
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("AddMenuSeparator: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.add_item(make<MenuItem>(menu, MenuItem::Separator));
}

void ConnectionFromClient::move_window_to_front(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("MoveWindowToFront: Bad window ID");
        return;
    }
    WindowManager::the().move_to_front_and_make_active(*(*it).value);
}

void ConnectionFromClient::set_fullscreen(i32 window_id, bool fullscreen)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetFullscreen: Bad window ID");
        return;
    }
    it->value->set_fullscreen(fullscreen);
}

void ConnectionFromClient::set_frameless(i32 window_id, bool frameless)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetFrameless: Bad window ID");
        return;
    }
    it->value->set_frameless(frameless);
    WindowManager::the().tell_wms_window_state_changed(*it->value);
}

void ConnectionFromClient::set_forced_shadow(i32 window_id, bool shadow)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetForcedShadow: Bad window ID");
        return;
    }
    it->value->set_forced_shadow(shadow);
    it->value->invalidate();
    Compositor::the().invalidate_occlusions();
}

void ConnectionFromClient::set_window_opacity(i32 window_id, float opacity)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowOpacity: Bad window ID");
        return;
    }
    it->value->set_opacity(opacity);
}

void ConnectionFromClient::set_wallpaper(Gfx::ShareableBitmap const& bitmap)
{
    Compositor::the().set_wallpaper(bitmap.bitmap());
    async_set_wallpaper_finished(true);
}

void ConnectionFromClient::set_background_color(String const& background_color)
{
    Compositor::the().set_background_color(background_color);
}

void ConnectionFromClient::set_wallpaper_mode(String const& mode)
{
    Compositor::the().set_wallpaper_mode(mode);
}

Messages::WindowServer::GetWallpaperResponse ConnectionFromClient::get_wallpaper()
{
    return Compositor::the().wallpaper_bitmap()->to_shareable_bitmap();
}

Messages::WindowServer::SetScreenLayoutResponse ConnectionFromClient::set_screen_layout(ScreenLayout const& screen_layout, bool save)
{
    String error_msg;
    bool success = WindowManager::the().set_screen_layout(ScreenLayout(screen_layout), save, error_msg);
    return { success, move(error_msg) };
}

Messages::WindowServer::GetScreenLayoutResponse ConnectionFromClient::get_screen_layout()
{
    return { WindowManager::the().get_screen_layout() };
}

Messages::WindowServer::SaveScreenLayoutResponse ConnectionFromClient::save_screen_layout()
{
    String error_msg;
    bool success = WindowManager::the().save_screen_layout(error_msg);
    return { success, move(error_msg) };
}

Messages::WindowServer::ApplyWorkspaceSettingsResponse ConnectionFromClient::apply_workspace_settings(u32 rows, u32 columns, bool save)
{
    if (rows == 0 || columns == 0 || rows > WindowManager::max_window_stack_rows || columns > WindowManager::max_window_stack_columns)
        return { false };

    return { WindowManager::the().apply_workspace_settings(rows, columns, save) };
}

Messages::WindowServer::GetWorkspaceSettingsResponse ConnectionFromClient::get_workspace_settings()
{
    auto& wm = WindowManager::the();
    return { (unsigned)wm.window_stack_rows(), (unsigned)wm.window_stack_columns(), WindowManager::max_window_stack_rows, WindowManager::max_window_stack_columns };
}

void ConnectionFromClient::show_screen_numbers(bool show)
{
    if (m_show_screen_number == show)
        return;
    m_show_screen_number = show;
    if (show)
        Compositor::the().increment_show_screen_number({});
    else
        Compositor::the().decrement_show_screen_number({});
}

void ConnectionFromClient::set_window_title(i32 window_id, String const& title)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowTitle: Bad window ID");
        return;
    }
    it->value->set_title(title);
}

Messages::WindowServer::GetWindowTitleResponse ConnectionFromClient::get_window_title(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowTitle: Bad window ID");
        return nullptr;
    }
    return it->value->title();
}

Messages::WindowServer::IsMaximizedResponse ConnectionFromClient::is_maximized(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("IsMaximized: Bad window ID");
        return nullptr;
    }
    return it->value->is_maximized();
}

void ConnectionFromClient::set_maximized(i32 window_id, bool maximized)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetMaximized: Bad window ID");
        return;
    }
    it->value->set_maximized(maximized);
}

void ConnectionFromClient::set_window_icon_bitmap(i32 window_id, Gfx::ShareableBitmap const& icon)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowIconBitmap: Bad window ID");
        return;
    }
    auto& window = *(*it).value;

    if (icon.is_valid()) {
        window.set_icon(*icon.bitmap());
    } else {
        window.set_default_icon();
    }

    window.frame().invalidate_titlebar();
    WindowManager::the().tell_wms_window_icon_changed(window);
}

Messages::WindowServer::SetWindowRectResponse ConnectionFromClient::set_window_rect(i32 window_id, Gfx::IntRect const& rect)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowRect: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    if (window.is_fullscreen()) {
        dbgln("ConnectionFromClient: Ignoring SetWindowRect request for fullscreen window");
        return nullptr;
    }
    if (rect.width() > INT16_MAX || rect.height() > INT16_MAX) {
        did_misbehave(String::formatted("SetWindowRect: Bad window sizing(width={}, height={}), dimension exceeds INT16_MAX", rect.width(), rect.height()).characters());
        return nullptr;
    }

    if (rect.location() != window.rect().location()) {
        window.set_default_positioned(false);
    }
    auto new_rect = rect;
    window.apply_minimum_size(new_rect);
    window.set_rect(new_rect);
    window.nudge_into_desktop(nullptr);
    window.request_update(window.rect());
    return window.rect();
}

Messages::WindowServer::GetWindowRectResponse ConnectionFromClient::get_window_rect(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowRect: Bad window ID");
        return nullptr;
    }
    return it->value->rect();
}

void ConnectionFromClient::set_window_minimum_size(i32 window_id, Gfx::IntSize const& size)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowMinimumSize: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (window.is_fullscreen()) {
        dbgln("ConnectionFromClient: Ignoring SetWindowMinimumSize request for fullscreen window");
        return;
    }

    window.set_minimum_size(size);

    if (window.width() < window.minimum_size().width() || window.height() < window.minimum_size().height()) {
        // New minimum size is larger than the current window size, resize accordingly.
        auto new_rect = window.rect();
        bool did_size_clamp = window.apply_minimum_size(new_rect);
        window.set_rect(new_rect);
        window.nudge_into_desktop(nullptr);
        window.request_update(window.rect());

        if (did_size_clamp)
            window.refresh_client_size();
    }
}

Messages::WindowServer::GetWindowMinimumSizeResponse ConnectionFromClient::get_window_minimum_size(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowMinimumSize: Bad window ID");
        return nullptr;
    }
    return it->value->minimum_size();
}

Messages::WindowServer::GetAppletRectOnScreenResponse ConnectionFromClient::get_applet_rect_on_screen(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetAppletRectOnScreen: Bad window ID");
        return nullptr;
    }

    Gfx::IntRect applet_area_rect;
    if (auto* applet_area_window = AppletManager::the().window())
        applet_area_rect = applet_area_window->rect();

    return it->value->rect_in_applet_area().translated(applet_area_rect.location());
}

Window* ConnectionFromClient::window_from_id(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return nullptr;
    return it->value.ptr();
}

void ConnectionFromClient::create_window(i32 window_id, Gfx::IntRect const& rect,
    bool auto_position, bool has_alpha_channel, bool modal, bool minimizable, bool closeable, bool resizable,
    bool fullscreen, bool frameless, bool forced_shadow, bool accessory, float opacity,
    float alpha_hit_threshold, Gfx::IntSize const& base_size, Gfx::IntSize const& size_increment,
    Gfx::IntSize const& minimum_size, Optional<Gfx::IntSize> const& resize_aspect_ratio, i32 type,
    String const& title, i32 parent_window_id, Gfx::IntRect const& launch_origin_rect)
{
    Window* parent_window = nullptr;
    if (parent_window_id) {
        parent_window = window_from_id(parent_window_id);
        if (!parent_window) {
            did_misbehave("CreateWindow with bad parent_window_id");
            return;
        }
    }

    if (type < 0 || type >= (i32)WindowType::_Count) {
        did_misbehave("CreateWindow with a bad type");
        return;
    }

    if (m_windows.contains(window_id)) {
        did_misbehave("CreateWindow with already-used window ID");
        return;
    }

    auto window = Window::construct(*this, (WindowType)type, window_id, modal, minimizable, closeable, frameless, resizable, fullscreen, accessory, parent_window);

    window->set_forced_shadow(forced_shadow);

    if (!launch_origin_rect.is_empty())
        window->start_launch_animation(launch_origin_rect);

    window->set_has_alpha_channel(has_alpha_channel);
    window->set_title(title);
    if (!fullscreen) {
        Gfx::IntRect new_rect = rect;
        if (auto_position && window->is_movable()) {
            new_rect = { WindowManager::the().get_recommended_window_position({ 100, 100 }), rect.size() };
            window->set_default_positioned(true);
        }
        window->set_minimum_size(minimum_size);
        bool did_size_clamp = window->apply_minimum_size(new_rect);
        window->set_rect(new_rect);
        window->nudge_into_desktop(nullptr);

        if (did_size_clamp)
            window->refresh_client_size();
    }
    if (window->type() == WindowType::Desktop) {
        window->set_rect(Screen::bounding_rect());
        window->recalculate_rect();
    }
    window->set_opacity(opacity);
    window->set_alpha_hit_threshold(alpha_hit_threshold);
    window->set_size_increment(size_increment);
    window->set_base_size(base_size);
    if (resize_aspect_ratio.has_value() && !resize_aspect_ratio.value().is_null())
        window->set_resize_aspect_ratio(resize_aspect_ratio);
    window->invalidate(true, true);
    if (window->type() == WindowType::Applet)
        AppletManager::the().add_applet(*window);
    m_windows.set(window_id, move(window));
}

void ConnectionFromClient::destroy_window(Window& window, Vector<i32>& destroyed_window_ids)
{
    for (auto& child_window : window.child_windows()) {
        if (!child_window)
            continue;
        VERIFY(child_window->window_id() != window.window_id());
        destroy_window(*child_window, destroyed_window_ids);
    }

    for (auto& accessory_window : window.accessory_windows()) {
        if (!accessory_window)
            continue;
        VERIFY(accessory_window->window_id() != window.window_id());
        destroy_window(*accessory_window, destroyed_window_ids);
    }

    destroyed_window_ids.append(window.window_id());

    if (window.type() == WindowType::Applet)
        AppletManager::the().remove_applet(window);

    window.destroy();
    remove_child(window);
    m_windows.remove(window.window_id());
}

Messages::WindowServer::DestroyWindowResponse ConnectionFromClient::destroy_window(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("DestroyWindow: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    Vector<i32> destroyed_window_ids;
    destroy_window(window, destroyed_window_ids);
    return destroyed_window_ids;
}

void ConnectionFromClient::post_paint_message(Window& window, bool ignore_occlusion)
{
    auto rect_set = window.take_pending_paint_rects();
    if (window.is_minimized() || (!ignore_occlusion && window.is_occluded()))
        return;

    async_paint(window.window_id(), window.size(), rect_set.rects());
}

void ConnectionFromClient::invalidate_rect(i32 window_id, Vector<Gfx::IntRect> const& rects, bool ignore_occlusion)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("InvalidateRect: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    for (size_t i = 0; i < rects.size(); ++i)
        window.request_update(rects[i].intersected({ {}, window.size() }), ignore_occlusion);
}

void ConnectionFromClient::did_finish_painting(i32 window_id, Vector<Gfx::IntRect> const& rects)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("DidFinishPainting: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    for (auto& rect : rects)
        window.invalidate(rect);
    if (window.has_alpha_channel() && window.alpha_hit_threshold() > 0.0f)
        WindowManager::the().reevaluate_hover_state_for_window(&window);

    WindowSwitcher::the().refresh_if_needed();
}

void ConnectionFromClient::set_window_backing_store(i32 window_id, [[maybe_unused]] i32 bpp,
    [[maybe_unused]] i32 pitch, IPC::File const& anon_file, i32 serial, bool has_alpha_channel,
    Gfx::IntSize const& size, bool flush_immediately)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowBackingStore: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (window.last_backing_store() && window.last_backing_store_serial() == serial) {
        window.swap_backing_stores();
    } else {
        // FIXME: Plumb scale factor here eventually.
        auto buffer_or_error = Core::AnonymousBuffer::create_from_anon_fd(anon_file.take_fd(), pitch * size.height());
        if (buffer_or_error.is_error()) {
            did_misbehave("SetWindowBackingStore: Failed to create anonymous buffer for window backing store");
            return;
        }
        auto backing_store_or_error = Gfx::Bitmap::try_create_with_anonymous_buffer(
            has_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888,
            buffer_or_error.release_value(),
            size,
            1,
            {});
        if (backing_store_or_error.is_error()) {
            did_misbehave("");
        }
        window.set_backing_store(backing_store_or_error.release_value(), serial);
    }

    if (flush_immediately)
        window.invalidate(false);
}

void ConnectionFromClient::set_global_mouse_tracking(bool enabled)
{
    m_does_global_mouse_tracking = enabled;
}

void ConnectionFromClient::set_window_cursor(i32 window_id, i32 cursor_type)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowCursor: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (cursor_type < 0 || cursor_type >= (i32)Gfx::StandardCursor::__Count) {
        did_misbehave("SetWindowCursor: Bad cursor type");
        return;
    }
    window.set_cursor(Cursor::create((Gfx::StandardCursor)cursor_type));
    if (&window == WindowManager::the().hovered_window())
        Compositor::the().invalidate_cursor();
}

void ConnectionFromClient::set_window_custom_cursor(i32 window_id, Gfx::ShareableBitmap const& cursor)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowCustomCursor: Bad window ID");
        return;
    }

    auto& window = *(*it).value;
    if (!cursor.is_valid()) {
        did_misbehave("SetWindowCustomCursor: Bad cursor");
        return;
    }

    window.set_cursor(Cursor::create(*cursor.bitmap(), 1));
    Compositor::the().invalidate_cursor();
}

void ConnectionFromClient::set_window_has_alpha_channel(i32 window_id, bool has_alpha_channel)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowHasAlphaChannel: Bad window ID");
        return;
    }
    it->value->set_has_alpha_channel(has_alpha_channel);
}

void ConnectionFromClient::set_window_alpha_hit_threshold(i32 window_id, float threshold)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowAlphaHitThreshold: Bad window ID");
        return;
    }
    it->value->set_alpha_hit_threshold(threshold);
}

void ConnectionFromClient::start_window_resize(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("WM_StartWindowResize: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (!window.is_resizable()) {
        dbgln("Client wants to start resizing a non-resizable window");
        return;
    }
    // FIXME: We are cheating a bit here by using the current cursor location and hard-coding the left button.
    //        Maybe the client should be allowed to specify what initiated this request?
    WindowManager::the().start_window_resize(window, ScreenInput::the().cursor_location(), MouseButton::Primary);
}

Messages::WindowServer::StartDragResponse ConnectionFromClient::start_drag(String const& text, HashMap<String, ByteBuffer> const& mime_data, Gfx::ShareableBitmap const& drag_bitmap)
{
    auto& wm = WindowManager::the();
    if (wm.dnd_client())
        return false;

    wm.start_dnd_drag(*this, text, drag_bitmap.bitmap(), Core::MimeData::construct(mime_data));
    return true;
}

Messages::WindowServer::SetSystemThemeResponse ConnectionFromClient::set_system_theme(String const& theme_path, String const& theme_name, bool keep_desktop_background)
{
    bool success = WindowManager::the().update_theme(theme_path, theme_name, keep_desktop_background);
    return success;
}

Messages::WindowServer::GetSystemThemeResponse ConnectionFromClient::get_system_theme()
{
    auto wm_config = Core::ConfigFile::open("/etc/WindowServer.ini").release_value_but_fixme_should_propagate_errors();
    auto name = wm_config->read_entry("Theme", "Name");
    return name;
}

void ConnectionFromClient::apply_cursor_theme(String const& name)
{
    WindowManager::the().apply_cursor_theme(name);
}

Messages::WindowServer::GetCursorThemeResponse ConnectionFromClient::get_cursor_theme()
{
    auto config = Core::ConfigFile::open("/etc/WindowServer.ini").release_value_but_fixme_should_propagate_errors();
    auto name = config->read_entry("Mouse", "CursorTheme");
    return name;
}

Messages::WindowServer::SetSystemFontsResponse ConnectionFromClient::set_system_fonts(String const& default_font_query, String const& fixed_width_font_query)
{
    if (!Gfx::FontDatabase::the().get_by_name(default_font_query)
        || !Gfx::FontDatabase::the().get_by_name(fixed_width_font_query)) {
        dbgln("Received unusable font queries: '{}' and '{}'", default_font_query, fixed_width_font_query);
        return false;
    }

    dbgln("Updating fonts: '{}' and '{}'", default_font_query, fixed_width_font_query);

    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);

    ConnectionFromClient::for_each_client([&](auto& client) {
        client.async_update_system_fonts(default_font_query, fixed_width_font_query);
    });

    WindowManager::the().invalidate_after_theme_or_font_change();

    auto wm_config_or_error = Core::ConfigFile::open("/etc/WindowServer.ini", Core::ConfigFile::AllowWriting::Yes);
    if (wm_config_or_error.is_error()) {
        dbgln("Unable to open WindowServer.ini to set system fonts: {}", wm_config_or_error.error());
        return false;
    }
    auto wm_config = wm_config_or_error.release_value();
    wm_config->write_entry("Fonts", "Default", default_font_query);
    wm_config->write_entry("Fonts", "FixedWidth", fixed_width_font_query);
    return true;
}

void ConnectionFromClient::set_window_base_size_and_size_increment(i32 window_id, Gfx::IntSize const& base_size, Gfx::IntSize const& size_increment)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowBaseSizeAndSizeIncrementResponse: Bad window ID");
        return;
    }

    auto& window = *it->value;
    window.set_base_size(base_size);
    window.set_size_increment(size_increment);
}

void ConnectionFromClient::set_window_resize_aspect_ratio(i32 window_id, Optional<Gfx::IntSize> const& resize_aspect_ratio)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowResizeAspectRatioResponse: Bad window ID");
        return;
    }

    auto& window = *it->value;
    window.set_resize_aspect_ratio(resize_aspect_ratio);
}

void ConnectionFromClient::enable_display_link()
{
    if (m_has_display_link)
        return;
    m_has_display_link = true;
    Compositor::the().increment_display_link_count({});
}

void ConnectionFromClient::disable_display_link()
{
    if (!m_has_display_link)
        return;
    m_has_display_link = false;
    Compositor::the().decrement_display_link_count({});
}

void ConnectionFromClient::notify_display_link(Badge<Compositor>)
{
    if (!m_has_display_link)
        return;

    async_display_link_notification();
}

void ConnectionFromClient::set_window_progress(i32 window_id, Optional<i32> const& progress)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowProgress with bad window ID");
        return;
    }
    it->value->set_progress(progress);
}

void ConnectionFromClient::refresh_system_theme()
{
    // Post the client an UpdateSystemTheme message to refresh its theme.
    async_update_system_theme(Gfx::current_system_theme_buffer());
}

void ConnectionFromClient::pong()
{
    m_ping_timer = nullptr;
    set_unresponsive(false);
}

void ConnectionFromClient::set_global_cursor_position(Gfx::IntPoint const& position)
{
    if (!Screen::main().rect().contains(position)) {
        did_misbehave("SetGlobalCursorPosition with bad position");
        return;
    }
    if (position != ScreenInput::the().cursor_location()) {
        ScreenInput::the().set_cursor_location(position);
        Compositor::the().invalidate_cursor();
    }
}

Messages::WindowServer::GetGlobalCursorPositionResponse ConnectionFromClient::get_global_cursor_position()
{
    return ScreenInput::the().cursor_location();
}

void ConnectionFromClient::set_mouse_acceleration(float factor)
{
    double dbl_factor = (double)factor;
    if (dbl_factor < mouse_accel_min || dbl_factor > mouse_accel_max) {
        did_misbehave("SetMouseAcceleration with bad acceleration factor");
        return;
    }
    WindowManager::the().set_acceleration_factor(dbl_factor);
}

Messages::WindowServer::GetMouseAccelerationResponse ConnectionFromClient::get_mouse_acceleration()
{
    return ScreenInput::the().acceleration_factor();
}

void ConnectionFromClient::set_scroll_step_size(u32 step_size)
{
    if (step_size < scroll_step_size_min) {
        did_misbehave("SetScrollStepSize with bad scroll step size");
        return;
    }
    WindowManager::the().set_scroll_step_size(step_size);
}

Messages::WindowServer::GetScrollStepSizeResponse ConnectionFromClient::get_scroll_step_size()
{
    return ScreenInput::the().scroll_step_size();
}

void ConnectionFromClient::set_double_click_speed(i32 speed)
{
    if (speed < double_click_speed_min || speed > double_click_speed_max) {
        did_misbehave("SetDoubleClickSpeed with bad speed");
        return;
    }
    WindowManager::the().set_double_click_speed(speed);
}

Messages::WindowServer::GetDoubleClickSpeedResponse ConnectionFromClient::get_double_click_speed()
{
    return WindowManager::the().double_click_speed();
}

void ConnectionFromClient::set_buttons_switched(bool switched)
{
    WindowManager::the().set_buttons_switched(switched);
}

Messages::WindowServer::GetButtonsSwitchedResponse ConnectionFromClient::get_buttons_switched()
{
    return WindowManager::the().get_buttons_switched();
}

void ConnectionFromClient::set_unresponsive(bool unresponsive)
{
    if (m_unresponsive == unresponsive)
        return;
    m_unresponsive = unresponsive;
    for (auto& it : m_windows) {
        auto& window = *it.value;
        window.invalidate(true, true);
        if (unresponsive) {
            window.set_cursor_override(WindowManager::the().wait_cursor());
        } else {
            window.remove_cursor_override();
        }
    }
    Compositor::the().invalidate_cursor();
}

void ConnectionFromClient::may_have_become_unresponsive()
{
    async_ping();
    m_ping_timer = Core::Timer::create_single_shot(1000, [this] {
        set_unresponsive(true);
    });
    m_ping_timer->start();
}

void ConnectionFromClient::did_become_responsive()
{
    set_unresponsive(false);
}

Messages::WindowServer::GetScreenBitmapResponse ConnectionFromClient::get_screen_bitmap(Optional<Gfx::IntRect> const& rect, Optional<u32> const& screen_index)
{
    if (screen_index.has_value()) {
        auto* screen = Screen::find_by_index(screen_index.value());
        if (!screen) {
            dbgln("get_screen_bitmap: Screen {} does not exist!", screen_index.value());
            return { Gfx::ShareableBitmap() };
        }
        if (rect.has_value()) {
            auto bitmap_or_error = Compositor::the().front_bitmap_for_screenshot({}, *screen).cropped(rect.value());
            if (bitmap_or_error.is_error()) {
                dbgln("get_screen_bitmap: Failed to crop screenshot: {}", bitmap_or_error.error());
                return { Gfx::ShareableBitmap() };
            }
            return bitmap_or_error.release_value()->to_shareable_bitmap();
        }
        auto& bitmap = Compositor::the().front_bitmap_for_screenshot({}, *screen);
        return bitmap.to_shareable_bitmap();
    }
    // TODO: Mixed scale setups at what scale? Lowest? Highest? Configurable?
    auto bitmap_size = rect.value_or(Screen::bounding_rect()).size();
    if (auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, bitmap_size, 1); !bitmap_or_error.is_error()) {
        auto bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
        Gfx::Painter painter(*bitmap);
        Screen::for_each([&](auto& screen) {
            auto screen_rect = screen.rect();
            if (rect.has_value() && !rect.value().intersects(screen_rect))
                return IterationDecision::Continue;
            auto src_rect = rect.has_value() ? rect.value().intersected(screen_rect) : screen_rect;
            VERIFY(Screen::bounding_rect().contains(src_rect));
            auto& screen_bitmap = Compositor::the().front_bitmap_for_screenshot({}, screen);
            // TODO: painter does *not* support down-sampling!!!
            painter.blit(screen_rect.location(), screen_bitmap, src_rect.translated(-screen_rect.location()), 1.0f, false);
            return IterationDecision::Continue;
        });
        return bitmap->to_shareable_bitmap();
    }
    return { Gfx::ShareableBitmap() };
}

Messages::WindowServer::GetScreenBitmapAroundCursorResponse ConnectionFromClient::get_screen_bitmap_around_cursor(Gfx::IntSize const& size)
{
    // TODO: Mixed scale setups at what scale? Lowest? Highest? Configurable?
    auto cursor_location = ScreenInput::the().cursor_location();
    Gfx::Rect rect { cursor_location.x() - (size.width() / 2), cursor_location.y() - (size.height() / 2), size.width(), size.height() };

    // Recompose the screen to make sure the cursor is painted in the location we think it is.
    // FIXME: This is rather wasteful. We can probably think of a way to avoid this.
    Compositor::the().compose();

    // Check if we need to compose from multiple screens. If not we can take a fast path
    size_t intersecting_with_screens = 0;
    Screen::for_each([&](auto& screen) {
        if (rect.intersects(screen.rect()))
            intersecting_with_screens++;
        return IterationDecision::Continue;
    });

    auto screen_scale_factor = ScreenInput::the().cursor_location_screen().scale_factor();
    if (intersecting_with_screens == 1) {
        auto& screen = Screen::closest_to_rect(rect);
        auto crop_rect = rect.translated(-screen.rect().location()) * screen_scale_factor;
        auto bitmap_or_error = Compositor::the().front_bitmap_for_screenshot({}, screen).cropped(crop_rect);
        if (bitmap_or_error.is_error()) {
            dbgln("get_screen_bitmap_around_cursor: Failed to crop screenshot: {}", bitmap_or_error.error());
            return { {} };
        }
        return bitmap_or_error.release_value()->to_shareable_bitmap();
    }

    if (auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, rect.size(), 1); !bitmap_or_error.is_error()) {
        auto bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
        auto bounding_screen_src_rect = Screen::bounding_rect().intersected(rect);
        Gfx::Painter painter(*bitmap);
        auto& screen_with_cursor = ScreenInput::the().cursor_location_screen();
        auto cursor_rect = Compositor::the().current_cursor_rect();
        Screen::for_each([&](auto& screen) {
            auto screen_rect = screen.rect();
            auto src_rect = screen_rect.intersected(bounding_screen_src_rect);
            if (src_rect.is_empty())
                return IterationDecision ::Continue;
            auto& screen_bitmap = Compositor::the().front_bitmap_for_screenshot({}, screen);
            // TODO: Add scaling support for multiple screens
            auto from_rect = src_rect.translated(-screen_rect.location());
            auto target_location = rect.intersected(screen_rect).location().translated(-rect.location());
            // TODO: painter does *not* support down-sampling!!!
            painter.blit(target_location, screen_bitmap, from_rect, 1.0f, false);
            // Check if we are a screen that doesn't have the cursor but the cursor would
            // have normally been cut off (we don't draw portions of the cursor on a screen
            // that doesn't actually have the cursor). In that case we need to render the remaining
            // portion of the cursor on that screen's capture manually
            if (&screen != &screen_with_cursor) {
                auto screen_cursor_rect = cursor_rect.intersected(screen_rect);
                if (!screen_cursor_rect.is_empty()) {
                    if (auto const* cursor_bitmap = Compositor::the().cursor_bitmap_for_screenshot({}, screen)) {
                        auto src_rect = screen_cursor_rect.translated(-cursor_rect.location());
                        auto cursor_target = cursor_rect.intersected(screen_rect).location().translated(-rect.location());
                        // TODO: painter does *not* support down-sampling!!!
                        painter.blit(cursor_target, *cursor_bitmap, src_rect);
                    }
                }
            }
            return IterationDecision::Continue;
        });
        return bitmap->to_shareable_bitmap();
    }
    return { {} };
}

Messages::WindowServer::GetColorUnderCursorResponse ConnectionFromClient::get_color_under_cursor()
{
    auto screen_scale_factor = ScreenInput::the().cursor_location_screen().scale_factor();
    // FIXME: Add a mechanism to get screen bitmap without cursor, so we don't have to do this
    //        manual translation to avoid sampling the color on the actual cursor itself.
    auto cursor_location = (ScreenInput::the().cursor_location() * screen_scale_factor).translated(-1, -1);
    auto& screen_with_cursor = ScreenInput::the().cursor_location_screen();
    auto scaled_screen_rect = screen_with_cursor.rect() * screen_scale_factor;

    if (!scaled_screen_rect.contains(cursor_location))
        return Optional<Color> {};

    return { Compositor::the().color_at_position({}, screen_with_cursor, cursor_location) };
}

Messages::WindowServer::IsWindowModifiedResponse ConnectionFromClient::is_window_modified(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("IsWindowModified: Bad window ID");
        return nullptr;
    }
    auto& window = *it->value;
    return window.is_modified();
}

Messages::WindowServer::GetDesktopDisplayScaleResponse ConnectionFromClient::get_desktop_display_scale(u32 screen_index)
{
    if (auto* screen = Screen::find_by_index(screen_index))
        return screen->scale_factor();
    dbgln("GetDesktopDisplayScale: Screen {} does not exist", screen_index);
    return 0;
}

void ConnectionFromClient::set_window_modified(i32 window_id, bool modified)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowModified: Bad window ID");
        return;
    }
    auto& window = *it->value;
    window.set_modified(modified);
}

void ConnectionFromClient::set_flash_flush(bool enabled)
{
    Compositor::the().set_flash_flush(enabled);
}

void ConnectionFromClient::set_window_parent_from_client(i32 client_id, i32 parent_id, i32 child_id)
{
    auto child_window = window_from_id(child_id);
    if (!child_window)
        did_misbehave("SetWindowParentFromClient: Bad child window ID");

    auto client_connection = from_client_id(client_id);
    if (!client_connection)
        did_misbehave("SetWindowParentFromClient: Bad client ID");

    auto parent_window = client_connection->window_from_id(parent_id);
    if (!parent_window)
        did_misbehave("SetWindowParentFromClient: Bad parent window ID");

    if (parent_window->is_stealable_by_client(this->client_id())) {
        child_window->set_parent_window(*parent_window);
    } else {
        did_misbehave("SetWindowParentFromClient: Window is not stealable");
    }
}

Messages::WindowServer::GetWindowRectFromClientResponse ConnectionFromClient::get_window_rect_from_client(i32 client_id, i32 window_id)
{
    auto client_connection = from_client_id(client_id);
    if (!client_connection)
        did_misbehave("GetWindowRectFromClient: Bad client ID");

    auto window = client_connection->window_from_id(window_id);
    if (!window)
        did_misbehave("GetWindowRectFromClient: Bad window ID");

    return window->rect();
}

void ConnectionFromClient::add_window_stealing_for_client(i32 client_id, i32 window_id)
{
    auto window = window_from_id(window_id);
    if (!window)
        did_misbehave("AddWindowStealingForClient: Bad window ID");

    if (!from_client_id(client_id))
        did_misbehave("AddWindowStealingForClient: Bad client ID");

    window->add_stealing_for_client(client_id);
}

void ConnectionFromClient::remove_window_stealing_for_client(i32 client_id, i32 window_id)
{
    auto window = window_from_id(window_id);
    if (!window)
        did_misbehave("RemoveWindowStealingForClient: Bad window ID");

    // Don't check if the client exists, it may have died

    window->remove_stealing_for_client(client_id);
}

void ConnectionFromClient::remove_window_stealing(i32 window_id)
{
    auto window = window_from_id(window_id);
    if (!window)
        did_misbehave("RemoveWindowStealing: Bad window ID");

    window->remove_all_stealing();
}

}
