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
#include <WindowServer/ClientConnection.h>
#include <WindowServer/Compositor.h>
#include <WindowServer/Menu.h>
#include <WindowServer/MenuItem.h>
#include <WindowServer/Menubar.h>
#include <WindowServer/Screen.h>
#include <WindowServer/Window.h>
#include <WindowServer/WindowClientEndpoint.h>
#include <WindowServer/WindowManager.h>
#include <WindowServer/WindowSwitcher.h>
#include <errno.h>
#include <stdio.h>
#include <unistd.h>

namespace WindowServer {

HashMap<int, NonnullRefPtr<ClientConnection>>* s_connections;

void ClientConnection::for_each_client(Function<void(ClientConnection&)> callback)
{
    if (!s_connections)
        return;
    for (auto& it : *s_connections) {
        callback(*it.value);
    }
}

ClientConnection* ClientConnection::from_client_id(int client_id)
{
    if (!s_connections)
        return nullptr;
    auto it = s_connections->find(client_id);
    if (it == s_connections->end())
        return nullptr;
    return (*it).value.ptr();
}

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> client_socket, int client_id)
    : IPC::ClientConnection<WindowClientEndpoint, WindowServerEndpoint>(*this, move(client_socket), client_id)
{
    if (!s_connections)
        s_connections = new HashMap<int, NonnullRefPtr<ClientConnection>>;
    s_connections->set(client_id, *this);

    auto& wm = WindowManager::the();
    async_fast_greet(Screen::rects(), Screen::main().index(), wm.window_stack_rows(), wm.window_stack_columns(), Gfx::current_system_theme_buffer(), Gfx::FontDatabase::default_font_query(), Gfx::FontDatabase::fixed_width_font_query(), client_id);
}

ClientConnection::~ClientConnection()
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

void ClientConnection::die()
{
    deferred_invoke([this](auto&) {
        s_connections->remove(client_id());
    });
}

void ClientConnection::notify_about_new_screen_rects()
{
    auto& wm = WindowManager::the();
    async_screen_rects_changed(Screen::rects(), Screen::main().index(), wm.window_stack_rows(), wm.window_stack_columns());
}

void ClientConnection::create_menubar(i32 menubar_id)
{
    auto menubar = Menubar::create(*this, menubar_id);
    m_menubars.set(menubar_id, move(menubar));
}

void ClientConnection::destroy_menubar(i32 menubar_id)
{
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        did_misbehave("DestroyMenubar: Bad menubar ID");
        return;
    }
    m_menubars.remove(it);
}

void ClientConnection::create_menu(i32 menu_id, String const& menu_title)
{
    auto menu = Menu::construct(this, menu_id, menu_title);
    m_menus.set(menu_id, move(menu));
}

void ClientConnection::destroy_menu(i32 menu_id)
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

void ClientConnection::set_window_menubar(i32 window_id, i32 menubar_id)
{
    RefPtr<Window> window;
    {
        auto it = m_windows.find(window_id);
        if (it == m_windows.end()) {
            did_misbehave("SetWindowMenubar: Bad window ID");
            return;
        }
        window = it->value;
    }
    RefPtr<Menubar> menubar;
    if (menubar_id != -1) {
        auto it = m_menubars.find(menubar_id);
        if (it == m_menubars.end()) {
            did_misbehave("SetWindowMenubar: Bad menubar ID");
            return;
        }
        menubar = *(*it).value;
    }
    window->set_menubar(menubar);
}

void ClientConnection::add_menu_to_menubar(i32 menubar_id, i32 menu_id)
{
    auto it = m_menubars.find(menubar_id);
    auto jt = m_menus.find(menu_id);
    if (it == m_menubars.end()) {
        did_misbehave("AddMenuToMenubar: Bad menubar ID");
        return;
    }
    if (jt == m_menus.end()) {
        did_misbehave("AddMenuToMenubar: Bad menu ID");
        return;
    }
    auto& menubar = *(*it).value;
    auto& menu = *(*jt).value;
    menubar.add_menu(menu);
}

void ClientConnection::add_menu_item(i32 menu_id, i32 identifier, i32 submenu_id,
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

void ClientConnection::popup_menu(i32 menu_id, Gfx::IntPoint const& screen_position)
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

void ClientConnection::dismiss_menu(i32 menu_id)
{
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("DismissMenu: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.close();
}

void ClientConnection::update_menu_item(i32 menu_id, i32 identifier, [[maybe_unused]] i32 submenu_id,
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

void ClientConnection::add_menu_separator(i32 menu_id)
{
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("AddMenuSeparator: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.add_item(make<MenuItem>(menu, MenuItem::Separator));
}

void ClientConnection::move_window_to_front(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("MoveWindowToFront: Bad window ID");
        return;
    }
    WindowManager::the().move_to_front_and_make_active(*(*it).value);
}

void ClientConnection::set_fullscreen(i32 window_id, bool fullscreen)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetFullscreen: Bad window ID");
        return;
    }
    it->value->set_fullscreen(fullscreen);
}

void ClientConnection::set_frameless(i32 window_id, bool frameless)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetFrameless: Bad window ID");
        return;
    }
    it->value->set_frameless(frameless);
    WindowManager::the().tell_wms_window_state_changed(*it->value);
}

void ClientConnection::set_forced_shadow(i32 window_id, bool shadow)
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

void ClientConnection::set_window_opacity(i32 window_id, float opacity)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowOpacity: Bad window ID");
        return;
    }
    it->value->set_opacity(opacity);
}

void ClientConnection::set_wallpaper(String const& path)
{
    Compositor::the().set_wallpaper(path, [&](bool success) {
        async_set_wallpaper_finished(success);
    });
}

void ClientConnection::set_background_color(String const& background_color)
{
    Compositor::the().set_background_color(background_color);
}

void ClientConnection::set_wallpaper_mode(String const& mode)
{
    Compositor::the().set_wallpaper_mode(mode);
}

Messages::WindowServer::GetWallpaperResponse ClientConnection::get_wallpaper()
{
    return Compositor::the().wallpaper_path();
}

Messages::WindowServer::SetScreenLayoutResponse ClientConnection::set_screen_layout(ScreenLayout const& screen_layout, bool save)
{
    String error_msg;
    bool success = WindowManager::the().set_screen_layout(ScreenLayout(screen_layout), save, error_msg);
    return { success, move(error_msg) };
}

Messages::WindowServer::GetScreenLayoutResponse ClientConnection::get_screen_layout()
{
    return { WindowManager::the().get_screen_layout() };
}

Messages::WindowServer::SaveScreenLayoutResponse ClientConnection::save_screen_layout()
{
    String error_msg;
    bool success = WindowManager::the().save_screen_layout(error_msg);
    return { success, move(error_msg) };
}

Messages::WindowServer::ApplyVirtualDesktopSettingsResponse ClientConnection::apply_virtual_desktop_settings(u32 rows, u32 columns, bool save)
{
    if (rows == 0 || columns == 0 || rows > WindowManager::max_window_stack_rows || columns > WindowManager::max_window_stack_columns)
        return { false };

    return { WindowManager::the().apply_virtual_desktop_settings(rows, columns, save) };
}

Messages::WindowServer::GetVirtualDesktopSettingsResponse ClientConnection::get_virtual_desktop_settings()
{
    auto& wm = WindowManager::the();
    return { (unsigned)wm.window_stack_rows(), (unsigned)wm.window_stack_columns(), WindowManager::max_window_stack_rows, WindowManager::max_window_stack_columns };
}

void ClientConnection::show_screen_numbers(bool show)
{
    if (m_show_screen_number == show)
        return;
    m_show_screen_number = show;
    if (show)
        Compositor::the().increment_show_screen_number({});
    else
        Compositor::the().decrement_show_screen_number({});
}

void ClientConnection::set_window_title(i32 window_id, String const& title)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowTitle: Bad window ID");
        return;
    }
    it->value->set_title(title);
}

Messages::WindowServer::GetWindowTitleResponse ClientConnection::get_window_title(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowTitle: Bad window ID");
        return nullptr;
    }
    return it->value->title();
}

Messages::WindowServer::IsMaximizedResponse ClientConnection::is_maximized(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("IsMaximized: Bad window ID");
        return nullptr;
    }
    return it->value->is_maximized();
}

void ClientConnection::set_maximized(i32 window_id, bool maximized)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetMaximized: Bad window ID");
        return;
    }
    it->value->set_maximized(maximized);
}

void ClientConnection::set_window_icon_bitmap(i32 window_id, Gfx::ShareableBitmap const& icon)
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

Messages::WindowServer::SetWindowRectResponse ClientConnection::set_window_rect(i32 window_id, Gfx::IntRect const& rect)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowRect: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    if (window.is_fullscreen()) {
        dbgln("ClientConnection: Ignoring SetWindowRect request for fullscreen window");
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

Messages::WindowServer::GetWindowRectResponse ClientConnection::get_window_rect(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowRect: Bad window ID");
        return nullptr;
    }
    return it->value->rect();
}

void ClientConnection::set_window_minimum_size(i32 window_id, Gfx::IntSize const& size)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowMinimumSize: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (window.is_fullscreen()) {
        dbgln("ClientConnection: Ignoring SetWindowMinimumSize request for fullscreen window");
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

Messages::WindowServer::GetWindowMinimumSizeResponse ClientConnection::get_window_minimum_size(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowMinimumSize: Bad window ID");
        return nullptr;
    }
    return it->value->minimum_size();
}

Messages::WindowServer::GetAppletRectOnScreenResponse ClientConnection::get_applet_rect_on_screen(i32 window_id)
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

Window* ClientConnection::window_from_id(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end())
        return nullptr;
    return it->value.ptr();
}

void ClientConnection::create_window(i32 window_id, Gfx::IntRect const& rect,
    bool auto_position, bool has_alpha_channel, bool modal, bool minimizable, bool resizable,
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

    auto window = Window::construct(*this, (WindowType)type, window_id, modal, minimizable, frameless, resizable, fullscreen, accessory, parent_window);

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

void ClientConnection::destroy_window(Window& window, Vector<i32>& destroyed_window_ids)
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

Messages::WindowServer::DestroyWindowResponse ClientConnection::destroy_window(i32 window_id)
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

void ClientConnection::post_paint_message(Window& window, bool ignore_occlusion)
{
    auto rect_set = window.take_pending_paint_rects();
    if (window.is_minimized() || (!ignore_occlusion && window.is_occluded()))
        return;

    async_paint(window.window_id(), window.size(), rect_set.rects());
}

void ClientConnection::invalidate_rect(i32 window_id, Vector<Gfx::IntRect> const& rects, bool ignore_occlusion)
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

void ClientConnection::did_finish_painting(i32 window_id, Vector<Gfx::IntRect> const& rects)
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
        WindowManager::the().reevaluate_hovered_window(&window);

    WindowSwitcher::the().refresh_if_needed();
}

void ClientConnection::set_window_backing_store(i32 window_id, [[maybe_unused]] i32 bpp,
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
        Core::AnonymousBuffer buffer = Core::AnonymousBuffer::create_from_anon_fd(anon_file.take_fd(), pitch * size.height());
        if (!buffer.is_valid()) {
            did_misbehave("SetWindowBackingStore: Failed to create anonymous buffer for window backing store");
            return;
        }
        auto backing_store = Gfx::Bitmap::create_with_anonymous_buffer(
            has_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888,
            move(buffer),
            size,
            1,
            {});
        window.set_backing_store(move(backing_store), serial);
    }

    if (flush_immediately)
        window.invalidate(false);
}

void ClientConnection::set_global_cursor_tracking(i32 window_id, bool enabled)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetGlobalCursorTracking: Bad window ID");
        return;
    }
    it->value->set_global_cursor_tracking_enabled(enabled);
}

void ClientConnection::set_window_cursor(i32 window_id, i32 cursor_type)
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

void ClientConnection::set_window_custom_cursor(i32 window_id, Gfx::ShareableBitmap const& cursor)
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

void ClientConnection::set_window_has_alpha_channel(i32 window_id, bool has_alpha_channel)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowHasAlphaChannel: Bad window ID");
        return;
    }
    it->value->set_has_alpha_channel(has_alpha_channel);
}

void ClientConnection::set_window_alpha_hit_threshold(i32 window_id, float threshold)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowAlphaHitThreshold: Bad window ID");
        return;
    }
    it->value->set_alpha_hit_threshold(threshold);
}

void ClientConnection::start_window_resize(i32 window_id)
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
    WindowManager::the().start_window_resize(window, ScreenInput::the().cursor_location(), MouseButton::Left);
}

Messages::WindowServer::StartDragResponse ClientConnection::start_drag(String const& text, HashMap<String, ByteBuffer> const& mime_data, Gfx::ShareableBitmap const& drag_bitmap)
{
    auto& wm = WindowManager::the();
    if (wm.dnd_client())
        return false;

    wm.start_dnd_drag(*this, text, drag_bitmap.bitmap(), Core::MimeData::construct(mime_data));
    return true;
}

Messages::WindowServer::SetSystemThemeResponse ClientConnection::set_system_theme(String const& theme_path, String const& theme_name)
{
    bool success = WindowManager::the().update_theme(theme_path, theme_name);
    return success;
}

Messages::WindowServer::GetSystemThemeResponse ClientConnection::get_system_theme()
{
    auto wm_config = Core::ConfigFile::open("/etc/WindowServer.ini");
    auto name = wm_config->read_entry("Theme", "Name");
    return name;
}

Messages::WindowServer::SetSystemFontsResponse ClientConnection::set_system_fonts(String const& default_font_query, String const& fixed_width_font_query)
{
    if (!Gfx::FontDatabase::the().get_by_name(default_font_query)
        || !Gfx::FontDatabase::the().get_by_name(fixed_width_font_query)) {
        dbgln("Received unusable font queries: '{}' and '{}'", default_font_query, fixed_width_font_query);
        return false;
    }

    dbgln("Updating fonts: '{}' and '{}'", default_font_query, fixed_width_font_query);

    Gfx::FontDatabase::set_default_font_query(default_font_query);
    Gfx::FontDatabase::set_fixed_width_font_query(fixed_width_font_query);

    ClientConnection::for_each_client([&](auto& client) {
        client.async_update_system_fonts(default_font_query, fixed_width_font_query);
    });

    WindowManager::the().invalidate_after_theme_or_font_change();

    auto wm_config = Core::ConfigFile::open("/etc/WindowServer.ini");
    wm_config->write_entry("Fonts", "Default", default_font_query);
    wm_config->write_entry("Fonts", "FixedWidth", fixed_width_font_query);
    return true;
}

void ClientConnection::set_window_base_size_and_size_increment(i32 window_id, Gfx::IntSize const& base_size, Gfx::IntSize const& size_increment)
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

void ClientConnection::set_window_resize_aspect_ratio(i32 window_id, Optional<Gfx::IntSize> const& resize_aspect_ratio)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowResizeAspectRatioResponse: Bad window ID");
        return;
    }

    auto& window = *it->value;
    window.set_resize_aspect_ratio(resize_aspect_ratio);
}

void ClientConnection::enable_display_link()
{
    if (m_has_display_link)
        return;
    m_has_display_link = true;
    Compositor::the().increment_display_link_count({});
}

void ClientConnection::disable_display_link()
{
    if (!m_has_display_link)
        return;
    m_has_display_link = false;
    Compositor::the().decrement_display_link_count({});
}

void ClientConnection::notify_display_link(Badge<Compositor>)
{
    if (!m_has_display_link)
        return;

    async_display_link_notification();
}

void ClientConnection::set_window_progress(i32 window_id, Optional<i32> const& progress)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowProgress with bad window ID");
        return;
    }
    it->value->set_progress(progress);
}

void ClientConnection::refresh_system_theme()
{
    // Post the client an UpdateSystemTheme message to refresh its theme.
    async_update_system_theme(Gfx::current_system_theme_buffer());
}

void ClientConnection::pong()
{
    m_ping_timer = nullptr;
    set_unresponsive(false);
}

void ClientConnection::set_global_cursor_position(Gfx::IntPoint const& position)
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

Messages::WindowServer::GetGlobalCursorPositionResponse ClientConnection::get_global_cursor_position()
{
    return ScreenInput::the().cursor_location();
}

void ClientConnection::set_mouse_acceleration(float factor)
{
    double dbl_factor = (double)factor;
    if (dbl_factor < mouse_accel_min || dbl_factor > mouse_accel_max) {
        did_misbehave("SetMouseAcceleration with bad acceleration factor");
        return;
    }
    WindowManager::the().set_acceleration_factor(dbl_factor);
}

Messages::WindowServer::GetMouseAccelerationResponse ClientConnection::get_mouse_acceleration()
{
    return ScreenInput::the().acceleration_factor();
}

void ClientConnection::set_scroll_step_size(u32 step_size)
{
    if (step_size < scroll_step_size_min) {
        did_misbehave("SetScrollStepSize with bad scroll step size");
        return;
    }
    WindowManager::the().set_scroll_step_size(step_size);
}

Messages::WindowServer::GetScrollStepSizeResponse ClientConnection::get_scroll_step_size()
{
    return ScreenInput::the().scroll_step_size();
}

void ClientConnection::set_double_click_speed(i32 speed)
{
    if (speed < double_click_speed_min || speed > double_click_speed_max) {
        did_misbehave("SetDoubleClickSpeed with bad speed");
        return;
    }
    WindowManager::the().set_double_click_speed(speed);
}

Messages::WindowServer::GetDoubleClickSpeedResponse ClientConnection::get_double_click_speed()
{
    return WindowManager::the().double_click_speed();
}

void ClientConnection::set_unresponsive(bool unresponsive)
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

void ClientConnection::may_have_become_unresponsive()
{
    async_ping();
    m_ping_timer = Core::Timer::create_single_shot(1000, [this] {
        set_unresponsive(true);
    });
    m_ping_timer->start();
}

void ClientConnection::did_become_responsive()
{
    set_unresponsive(false);
}

Messages::WindowServer::GetScreenBitmapResponse ClientConnection::get_screen_bitmap(Optional<Gfx::IntRect> const& rect, Optional<u32> const& screen_index)
{
    if (screen_index.has_value()) {
        auto* screen = Screen::find_by_index(screen_index.value());
        if (!screen) {
            dbgln("get_screen_bitmap: Screen {} does not exist!", screen_index.value());
            return { {} };
        }
        if (rect.has_value()) {
            auto bitmap = Compositor::the().front_bitmap_for_screenshot({}, *screen).cropped(rect.value());
            return bitmap->to_shareable_bitmap();
        }
        auto& bitmap = Compositor::the().front_bitmap_for_screenshot({}, *screen);
        return bitmap.to_shareable_bitmap();
    }
    // TODO: Mixed scale setups at what scale? Lowest? Highest? Configurable?
    if (auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, Screen::bounding_rect().size(), 1)) {
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
    return { {} };
}

Messages::WindowServer::GetScreenBitmapAroundCursorResponse ClientConnection::get_screen_bitmap_around_cursor(Gfx::IntSize const& size)
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

    if (intersecting_with_screens == 1) {
        auto& screen = Screen::closest_to_rect(rect);
        auto bitmap = Compositor::the().front_bitmap_for_screenshot({}, screen).cropped(rect.translated(-screen.rect().location()));
        return bitmap->to_shareable_bitmap();
    }

    if (auto bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, rect.size(), 1)) {
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

Messages::WindowServer::IsWindowModifiedResponse ClientConnection::is_window_modified(i32 window_id)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("IsWindowModified: Bad window ID");
        return nullptr;
    }
    auto& window = *it->value;
    return window.is_modified();
}

Messages::WindowServer::GetDesktopDisplayScaleResponse ClientConnection::get_desktop_display_scale(u32 screen_index)
{
    if (auto* screen = Screen::find_by_index(screen_index))
        return screen->scale_factor();
    dbgln("GetDesktopDisplayScale: Screen {} does not exist", screen_index);
    return 0;
}

void ClientConnection::set_window_modified(i32 window_id, bool modified)
{
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowModified: Bad window ID");
        return;
    }
    auto& window = *it->value;
    window.set_modified(modified);
}

void ClientConnection::set_flash_flush(bool enabled)
{
    Compositor::the().set_flash_flush(enabled);
}

}
