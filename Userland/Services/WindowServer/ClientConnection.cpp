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
#include <serenity.h>
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
}

ClientConnection::~ClientConnection()
{
    if (m_has_display_link)
        Compositor::the().decrement_display_link_count({});

    MenuManager::the().close_all_menus_from_client({}, *this);
    auto windows = move(m_windows);
    for (auto& window : windows) {
        window.value->detach_client({});
        if (window.value->type() == WindowType::Applet)
            AppletManager::the().remove_applet(window.value);
    }
}

void ClientConnection::die()
{
    deferred_invoke([this](auto&) {
        s_connections->remove(client_id());
    });
}

void ClientConnection::notify_about_new_screen_rect(const Gfx::IntRect& rect)
{
    post_message(Messages::WindowClient::ScreenRectChanged(rect));
}

Messages::WindowServer::CreateMenubarResponse ClientConnection::handle(const Messages::WindowServer::CreateMenubar&)
{
    int menubar_id = m_next_menubar_id++;
    auto menubar = Menubar::create(*this, menubar_id);
    m_menubars.set(menubar_id, move(menubar));
    return menubar_id;
}

Messages::WindowServer::DestroyMenubarResponse ClientConnection::handle(const Messages::WindowServer::DestroyMenubar& message)
{
    int menubar_id = message.menubar_id();
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        did_misbehave("DestroyMenubar: Bad menubar ID");
        return nullptr;
    }
    m_menubars.remove(it);
    return {};
}

Messages::WindowServer::CreateMenuResponse ClientConnection::handle(const Messages::WindowServer::CreateMenu& message)
{
    int menu_id = m_next_menu_id++;
    auto menu = Menu::construct(this, menu_id, message.menu_title());
    m_menus.set(menu_id, move(menu));
    return menu_id;
}

Messages::WindowServer::DestroyMenuResponse ClientConnection::handle(const Messages::WindowServer::DestroyMenu& message)
{
    int menu_id = message.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("DestroyMenu: Bad menu ID");
        return nullptr;
    }
    auto& menu = *(*it).value;
    menu.close();
    m_menus.remove(it);
    remove_child(menu);
    return {};
}

Messages::WindowServer::SetWindowMenubarResponse ClientConnection::handle(const Messages::WindowServer::SetWindowMenubar& message)
{
    RefPtr<Window> window;
    {
        auto it = m_windows.find(message.window_id());
        if (it == m_windows.end()) {
            did_misbehave("SetWindowMenubar: Bad window ID");
            return nullptr;
        }
        window = it->value;
    }
    RefPtr<Menubar> menubar;
    if (message.menubar_id() != -1) {
        auto it = m_menubars.find(message.menubar_id());
        if (it == m_menubars.end()) {
            did_misbehave("SetWindowMenubar: Bad menubar ID");
            return nullptr;
        }
        menubar = *(*it).value;
    }
    window->set_menubar(menubar);
    return {};
}

Messages::WindowServer::AddMenuToMenubarResponse ClientConnection::handle(const Messages::WindowServer::AddMenuToMenubar& message)
{
    int menubar_id = message.menubar_id();
    int menu_id = message.menu_id();
    auto it = m_menubars.find(menubar_id);
    auto jt = m_menus.find(menu_id);
    if (it == m_menubars.end()) {
        did_misbehave("AddMenuToMenubar: Bad menubar ID");
        return nullptr;
    }
    if (jt == m_menus.end()) {
        did_misbehave("AddMenuToMenubar: Bad menu ID");
        return nullptr;
    }
    auto& menubar = *(*it).value;
    auto& menu = *(*jt).value;
    menubar.add_menu(menu);
    return {};
}

Messages::WindowServer::AddMenuItemResponse ClientConnection::handle(const Messages::WindowServer::AddMenuItem& message)
{
    int menu_id = message.menu_id();
    unsigned identifier = message.identifier();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        dbgln("AddMenuItem: Bad menu ID: {}", menu_id);
        return nullptr;
    }
    auto& menu = *(*it).value;
    auto menu_item = make<MenuItem>(menu, identifier, message.text(), message.shortcut(), message.enabled(), message.checkable(), message.checked());
    if (message.is_default())
        menu_item->set_default(true);
    menu_item->set_icon(message.icon().bitmap());
    menu_item->set_submenu_id(message.submenu_id());
    menu_item->set_exclusive(message.exclusive());
    menu.add_item(move(menu_item));
    return {};
}

Messages::WindowServer::PopupMenuResponse ClientConnection::handle(const Messages::WindowServer::PopupMenu& message)
{
    int menu_id = message.menu_id();
    auto position = message.screen_position();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("PopupMenu: Bad menu ID");
        return nullptr;
    }
    auto& menu = *(*it).value;
    menu.popup(position);
    return {};
}

Messages::WindowServer::DismissMenuResponse ClientConnection::handle(const Messages::WindowServer::DismissMenu& message)
{
    int menu_id = message.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("DismissMenu: Bad menu ID");
        return nullptr;
    }
    auto& menu = *(*it).value;
    menu.close();
    return {};
}

Messages::WindowServer::UpdateMenuItemResponse ClientConnection::handle(const Messages::WindowServer::UpdateMenuItem& message)
{
    int menu_id = message.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("UpdateMenuItem: Bad menu ID");
        return nullptr;
    }
    auto& menu = *(*it).value;
    auto* menu_item = menu.item_with_identifier(message.identifier());
    if (!menu_item) {
        did_misbehave("UpdateMenuItem: Bad menu item identifier");
        return nullptr;
    }
    menu_item->set_text(message.text());
    menu_item->set_shortcut_text(message.shortcut());
    menu_item->set_enabled(message.enabled());
    menu_item->set_checkable(message.checkable());
    menu_item->set_default(message.is_default());
    if (message.checkable())
        menu_item->set_checked(message.checked());
    return {};
}

Messages::WindowServer::AddMenuSeparatorResponse ClientConnection::handle(const Messages::WindowServer::AddMenuSeparator& message)
{
    int menu_id = message.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("AddMenuSeparator: Bad menu ID");
        return nullptr;
    }
    auto& menu = *(*it).value;
    menu.add_item(make<MenuItem>(menu, MenuItem::Separator));
    return {};
}

Messages::WindowServer::MoveWindowToFrontResponse ClientConnection::handle(const Messages::WindowServer::MoveWindowToFront& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("MoveWindowToFront: Bad window ID");
        return nullptr;
    }
    WindowManager::the().move_to_front_and_make_active(*(*it).value);
    return {};
}

Messages::WindowServer::SetFullscreenResponse ClientConnection::handle(const Messages::WindowServer::SetFullscreen& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetFullscreen: Bad window ID");
        return nullptr;
    }
    it->value->set_fullscreen(message.fullscreen());
    return {};
}

Messages::WindowServer::SetFramelessResponse ClientConnection::handle(const Messages::WindowServer::SetFrameless& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetFrameless: Bad window ID");
        return nullptr;
    }
    it->value->set_frameless(message.frameless());
    WindowManager::the().tell_wms_window_state_changed(*it->value);
    return {};
}

Messages::WindowServer::SetWindowOpacityResponse ClientConnection::handle(const Messages::WindowServer::SetWindowOpacity& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowOpacity: Bad window ID");
        return nullptr;
    }
    it->value->set_opacity(message.opacity());
    return {};
}

void ClientConnection::handle(const Messages::WindowServer::AsyncSetWallpaper& message)
{
    Compositor::the().set_wallpaper(message.path(), [&](bool success) {
        post_message(Messages::WindowClient::AsyncSetWallpaperFinished(success));
    });
}

Messages::WindowServer::SetBackgroundColorResponse ClientConnection::handle(const Messages::WindowServer::SetBackgroundColor& message)
{
    Compositor::the().set_background_color(message.background_color());
    return {};
}

Messages::WindowServer::SetWallpaperModeResponse ClientConnection::handle(const Messages::WindowServer::SetWallpaperMode& message)
{
    Compositor::the().set_wallpaper_mode(message.mode());
    return {};
}

Messages::WindowServer::GetWallpaperResponse ClientConnection::handle(const Messages::WindowServer::GetWallpaper&)
{
    return Compositor::the().wallpaper_path();
}

Messages::WindowServer::SetResolutionResponse ClientConnection::handle(const Messages::WindowServer::SetResolution& message)
{
    return { WindowManager::the().set_resolution(message.resolution().width(), message.resolution().height(), message.scale_factor()), WindowManager::the().resolution(), WindowManager::the().scale_factor() };
}

Messages::WindowServer::SetWindowTitleResponse ClientConnection::handle(const Messages::WindowServer::SetWindowTitle& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowTitle: Bad window ID");
        return nullptr;
    }
    it->value->set_title(message.title());
    return {};
}

Messages::WindowServer::GetWindowTitleResponse ClientConnection::handle(const Messages::WindowServer::GetWindowTitle& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("GetWindowTitle: Bad window ID");
        return nullptr;
    }
    return it->value->title();
}

Messages::WindowServer::IsMaximizedResponse ClientConnection::handle(const Messages::WindowServer::IsMaximized& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("IsMaximized: Bad window ID");
        return nullptr;
    }
    return it->value->is_maximized();
}

Messages::WindowServer::SetWindowIconBitmapResponse ClientConnection::handle(const Messages::WindowServer::SetWindowIconBitmap& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowIconBitmap: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;

    if (message.icon().is_valid()) {
        window.set_icon(*message.icon().bitmap());
    } else {
        window.set_default_icon();
    }

    window.frame().invalidate_titlebar();
    WindowManager::the().tell_wms_window_icon_changed(window);
    return nullptr;
}

Messages::WindowServer::SetWindowRectResponse ClientConnection::handle(const Messages::WindowServer::SetWindowRect& message)
{
    int window_id = message.window_id();
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

    if (message.rect().location() != window.rect().location()) {
        window.set_default_positioned(false);
    }
    auto rect = message.rect();
    window.apply_minimum_size(rect);
    window.set_rect(rect);
    window.nudge_into_desktop();
    window.request_update(window.rect());
    return window.rect();
}

Messages::WindowServer::GetWindowRectResponse ClientConnection::handle(const Messages::WindowServer::GetWindowRect& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowRect: Bad window ID");
        return nullptr;
    }
    return it->value->rect();
}

Messages::WindowServer::SetWindowMinimumSizeResponse ClientConnection::handle(const Messages::WindowServer::SetWindowMinimumSize& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowMinimumSize: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    if (window.is_fullscreen()) {
        dbgln("ClientConnection: Ignoring SetWindowMinimumSize request for fullscreen window");
        return nullptr;
    }

    window.set_minimum_size(message.size());

    if (window.width() < window.minimum_size().width() || window.height() < window.minimum_size().height()) {
        // New minimum size is larger than the current window size, resize accordingly.
        auto new_rect = window.rect();
        bool did_size_clamp = window.apply_minimum_size(new_rect);
        window.set_rect(new_rect);
        window.nudge_into_desktop();
        window.request_update(window.rect());

        if (did_size_clamp)
            window.refresh_client_size();
    }

    return {};
}

Messages::WindowServer::GetWindowMinimumSizeResponse ClientConnection::handle(const Messages::WindowServer::GetWindowMinimumSize& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowMinimumSize: Bad window ID");
        return nullptr;
    }
    return it->value->minimum_size();
}

Messages::WindowServer::GetAppletRectOnScreenResponse ClientConnection::handle(const Messages::WindowServer::GetAppletRectOnScreen& message)
{
    int window_id = message.window_id();
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

Messages::WindowServer::CreateWindowResponse ClientConnection::handle(const Messages::WindowServer::CreateWindow& message)
{
    Window* parent_window = nullptr;
    if (message.parent_window_id()) {
        parent_window = window_from_id(message.parent_window_id());
        if (!parent_window) {
            did_misbehave("CreateWindow with bad parent_window_id");
            return nullptr;
        }
    }

    int window_id = m_next_window_id++;
    auto window = Window::construct(*this, (WindowType)message.type(), window_id, message.modal(), message.minimizable(), message.frameless(), message.resizable(), message.fullscreen(), message.accessory(), parent_window);

    window->set_has_alpha_channel(message.has_alpha_channel());
    window->set_title(message.title());
    if (!message.fullscreen()) {
        auto rect = message.rect();
        if (message.auto_position() && window->is_movable()) {
            rect = { WindowManager::the().get_recommended_window_position({ 100, 100 }), message.rect().size() };
            window->set_default_positioned(true);
        }
        window->set_minimum_size(message.minimum_size());
        bool did_size_clamp = window->apply_minimum_size(rect);
        window->set_rect(rect);
        window->nudge_into_desktop();

        if (did_size_clamp)
            window->refresh_client_size();
    }
    if (window->type() == WindowType::Desktop) {
        window->set_rect(WindowManager::the().desktop_rect());
        window->recalculate_rect();
    }
    window->set_opacity(message.opacity());
    window->set_alpha_hit_threshold(message.alpha_hit_threshold());
    window->set_size_increment(message.size_increment());
    window->set_base_size(message.base_size());
    window->set_resize_aspect_ratio(message.resize_aspect_ratio());
    window->invalidate(true, true);
    if (window->type() == WindowType::Applet)
        AppletManager::the().add_applet(*window);
    m_windows.set(window_id, move(window));
    return window_id;
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

Messages::WindowServer::DestroyWindowResponse ClientConnection::handle(const Messages::WindowServer::DestroyWindow& message)
{
    auto it = m_windows.find(message.window_id());
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

    post_message(Messages::WindowClient::Paint(window.window_id(), window.size(), rect_set.rects()));
}

void ClientConnection::handle(const Messages::WindowServer::InvalidateRect& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("InvalidateRect: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    for (size_t i = 0; i < message.rects().size(); ++i)
        window.request_update(message.rects()[i].intersected({ {}, window.size() }), message.ignore_occlusion());
}

void ClientConnection::handle(const Messages::WindowServer::DidFinishPainting& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("DidFinishPainting: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    for (auto& rect : message.rects())
        window.invalidate(rect);
    if (window.has_alpha_channel() && window.alpha_hit_threshold() > 0.0f)
        WindowManager::the().reevaluate_hovered_window(&window);

    WindowSwitcher::the().refresh_if_needed();
}

Messages::WindowServer::SetWindowBackingStoreResponse ClientConnection::handle(const Messages::WindowServer::SetWindowBackingStore& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowBackingStore: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    if (window.last_backing_store() && window.last_backing_store_serial() == message.serial()) {
        window.swap_backing_stores();
    } else {
        // FIXME: Plumb scale factor here eventually.
        auto backing_store = Gfx::Bitmap::create_with_anon_fd(
            message.has_alpha_channel() ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888,
            message.anon_file().take_fd(),
            message.size(),
            1,
            {},
            Gfx::Bitmap::ShouldCloseAnonymousFile::Yes);
        window.set_backing_store(move(backing_store), message.serial());
    }

    if (message.flush_immediately())
        window.invalidate(false);

    return {};
}

Messages::WindowServer::SetGlobalCursorTrackingResponse ClientConnection::handle(const Messages::WindowServer::SetGlobalCursorTracking& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetGlobalCursorTracking: Bad window ID");
        return nullptr;
    }
    it->value->set_global_cursor_tracking_enabled(message.enabled());
    return {};
}

Messages::WindowServer::SetWindowCursorResponse ClientConnection::handle(const Messages::WindowServer::SetWindowCursor& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowCursor: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    if (message.cursor_type() < 0 || message.cursor_type() >= (i32)Gfx::StandardCursor::__Count) {
        did_misbehave("SetWindowCursor: Bad cursor type");
        return nullptr;
    }
    window.set_cursor(Cursor::create((Gfx::StandardCursor)message.cursor_type()));
    if (&window == WindowManager::the().hovered_window())
        Compositor::the().invalidate_cursor();
    return {};
}

Messages::WindowServer::SetWindowCustomCursorResponse ClientConnection::handle(const Messages::WindowServer::SetWindowCustomCursor& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowCustomCursor: Bad window ID");
        return nullptr;
    }

    auto& window = *(*it).value;
    if (!message.cursor().is_valid()) {
        did_misbehave("SetWindowCustomCursor: Bad cursor");
        return nullptr;
    }

    window.set_cursor(Cursor::create(*message.cursor().bitmap()));
    Compositor::the().invalidate_cursor();
    return {};
}

Messages::WindowServer::SetWindowHasAlphaChannelResponse ClientConnection::handle(const Messages::WindowServer::SetWindowHasAlphaChannel& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowHasAlphaChannel: Bad window ID");
        return nullptr;
    }
    it->value->set_has_alpha_channel(message.has_alpha_channel());
    return {};
}

Messages::WindowServer::SetWindowAlphaHitThresholdResponse ClientConnection::handle(const Messages::WindowServer::SetWindowAlphaHitThreshold& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowAlphaHitThreshold: Bad window ID");
        return nullptr;
    }
    it->value->set_alpha_hit_threshold(message.threshold());
    return {};
}

void ClientConnection::handle(const Messages::WindowServer::StartWindowResize& request)
{
    auto it = m_windows.find(request.window_id());
    if (it == m_windows.end()) {
        did_misbehave("WM_StartWindowResize: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    // FIXME: We are cheating a bit here by using the current cursor location and hard-coding the left button.
    //        Maybe the client should be allowed to specify what initiated this request?
    WindowManager::the().start_window_resize(window, Screen::the().cursor_location(), MouseButton::Left);
}

Messages::WindowServer::GreetResponse ClientConnection::handle(const Messages::WindowServer::Greet&)
{
    return { Screen::the().rect(), Gfx::current_system_theme_buffer() };
}

Messages::WindowServer::StartDragResponse ClientConnection::handle(const Messages::WindowServer::StartDrag& message)
{
    auto& wm = WindowManager::the();
    if (wm.dnd_client())
        return false;

    wm.start_dnd_drag(*this, message.text(), message.drag_bitmap().bitmap(), Core::MimeData::construct(message.mime_data()));
    return true;
}

Messages::WindowServer::SetSystemThemeResponse ClientConnection::handle(const Messages::WindowServer::SetSystemTheme& message)
{
    bool success = WindowManager::the().update_theme(message.theme_path(), message.theme_name());
    return success;
}

Messages::WindowServer::GetSystemThemeResponse ClientConnection::handle(const Messages::WindowServer::GetSystemTheme&)
{
    auto wm_config = Core::ConfigFile::open("/etc/WindowServer.ini");
    auto name = wm_config->read_entry("Theme", "Name");
    return name;
}

Messages::WindowServer::SetWindowBaseSizeAndSizeIncrementResponse ClientConnection::handle(const Messages::WindowServer::SetWindowBaseSizeAndSizeIncrement& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowBaseSizeAndSizeIncrementResponse: Bad window ID");
        return nullptr;
    }

    auto& window = *it->value;
    window.set_base_size(message.base_size());
    window.set_size_increment(message.size_increment());

    return {};
}

Messages::WindowServer::SetWindowResizeAspectRatioResponse ClientConnection::handle(const Messages::WindowServer::SetWindowResizeAspectRatio& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowResizeAspectRatioResponse: Bad window ID");
        return nullptr;
    }

    auto& window = *it->value;
    window.set_resize_aspect_ratio(message.resize_aspect_ratio());

    return {};
}

void ClientConnection::handle(const Messages::WindowServer::EnableDisplayLink&)
{
    if (m_has_display_link)
        return;
    m_has_display_link = true;
    Compositor::the().increment_display_link_count({});
}

void ClientConnection::handle(const Messages::WindowServer::DisableDisplayLink&)
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

    post_message(Messages::WindowClient::DisplayLinkNotification());
}

void ClientConnection::handle(const Messages::WindowServer::SetWindowProgress& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowProgress with bad window ID");
        return;
    }
    it->value->set_progress(message.progress());
}

void ClientConnection::handle(const Messages::WindowServer::RefreshSystemTheme&)
{
    // Post the client an UpdateSystemTheme message to refresh its theme.
    post_message(Messages::WindowClient::UpdateSystemTheme(Gfx::current_system_theme_buffer()));
}

void ClientConnection::handle(const Messages::WindowServer::Pong&)
{
    m_ping_timer = nullptr;
    set_unresponsive(false);
}

Messages::WindowServer::GetGlobalCursorPositionResponse ClientConnection::handle(const Messages::WindowServer::GetGlobalCursorPosition&)
{
    return Screen::the().cursor_location();
}

Messages::WindowServer::SetMouseAccelerationResponse ClientConnection::handle(const Messages::WindowServer::SetMouseAcceleration& message)
{
    double factor = message.factor();
    if (factor < mouse_accel_min || factor > mouse_accel_max) {
        did_misbehave("SetMouseAcceleration with bad acceleration factor");
        return nullptr;
    }
    WindowManager::the().set_acceleration_factor(factor);
    return {};
}

Messages::WindowServer::GetMouseAccelerationResponse ClientConnection::handle(const Messages::WindowServer::GetMouseAcceleration&)
{
    return Screen::the().acceleration_factor();
}

Messages::WindowServer::SetScrollStepSizeResponse ClientConnection::handle(const Messages::WindowServer::SetScrollStepSize& message)
{
    if (message.step_size() < scroll_step_size_min) {
        did_misbehave("SetScrollStepSize with bad scroll step size");
        return nullptr;
    }
    WindowManager::the().set_scroll_step_size(message.step_size());
    return {};
}
Messages::WindowServer::GetScrollStepSizeResponse ClientConnection::handle(const Messages::WindowServer::GetScrollStepSize&)
{
    return Screen::the().scroll_step_size();
}
Messages::WindowServer::SetDoubleClickSpeedResponse ClientConnection::handle(const Messages::WindowServer::SetDoubleClickSpeed& message)
{
    if (message.speed() < double_click_speed_min || message.speed() > double_click_speed_max) {
        did_misbehave("SetDoubleClickSpeed with bad speed");
        return nullptr;
    }
    WindowManager::the().set_double_click_speed(message.speed());
    return {};
}
Messages::WindowServer::GetDoubleClickSpeedResponse ClientConnection::handle(const Messages::WindowServer::GetDoubleClickSpeed&)
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
        window.invalidate();
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
    post_message(Messages::WindowClient::Ping());
    m_ping_timer = Core::Timer::create_single_shot(1000, [this] {
        set_unresponsive(true);
    });
    m_ping_timer->start();
}

void ClientConnection::did_become_responsive()
{
    set_unresponsive(false);
}

Messages::WindowServer::GetScreenBitmapResponse ClientConnection::handle(const Messages::WindowServer::GetScreenBitmap&)
{
    auto& bitmap = Compositor::the().front_bitmap_for_screenshot({});
    return bitmap.to_shareable_bitmap();
}

Messages::WindowServer::IsWindowModifiedResponse ClientConnection::handle(Messages::WindowServer::IsWindowModified const& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("IsWindowModified: Bad window ID");
        return nullptr;
    }
    auto& window = *it->value;
    return window.is_modified();
}

void ClientConnection::handle(Messages::WindowServer::SetWindowModified const& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowModified: Bad window ID");
        return;
    }
    auto& window = *it->value;
    window.set_modified(message.modified());
}

}
