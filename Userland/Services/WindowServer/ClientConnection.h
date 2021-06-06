/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibIPC/ClientConnection.h>
#include <WindowServer/Event.h>
#include <WindowServer/Menu.h>
#include <WindowServer/WindowClientEndpoint.h>
#include <WindowServer/WindowServerEndpoint.h>

namespace WindowServer {

class Compositor;
class Window;
class Menu;
class Menubar;
class WMClientConnection;

class ClientConnection final
    : public IPC::ClientConnection<WindowClientEndpoint, WindowServerEndpoint> {
    C_OBJECT(ClientConnection)
public:
    ~ClientConnection() override;

    bool is_unresponsive() const { return m_unresponsive; }

    static ClientConnection* from_client_id(int client_id);
    static void for_each_client(Function<void(ClientConnection&)>);

    void notify_about_new_screen_rect(const Gfx::IntRect&);
    void post_paint_message(Window&, bool ignore_occlusion = false);

    Menu* find_menu_by_id(int menu_id)
    {
        auto menu = m_menus.get(menu_id);
        if (!menu.has_value())
            return nullptr;
        return menu.value();
    }
    const Menu* find_menu_by_id(int menu_id) const
    {
        auto menu = m_menus.get(menu_id);
        if (!menu.has_value())
            return nullptr;
        return menu.value();
    }

    template<typename Callback>
    void for_each_window(Callback callback)
    {
        for (auto& it : m_windows) {
            if (callback(*it.value) == IterationDecision::Break)
                break;
        }
    }
    template<typename Callback>
    void for_each_menu(Callback callback)
    {
        for (auto& it : m_menus) {
            if (callback(*it.value) == IterationDecision::Break)
                break;
        }
    }

    void notify_display_link(Badge<Compositor>);

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);

    // ^ClientConnection
    virtual void die() override;
    virtual void may_have_become_unresponsive() override;
    virtual void did_become_responsive() override;

    void set_unresponsive(bool);
    void destroy_window(Window&, Vector<i32>& destroyed_window_ids);

    virtual void create_menubar(i32) override;
    virtual void destroy_menubar(i32) override;
    virtual void create_menu(i32, String const&) override;
    virtual void destroy_menu(i32) override;
    virtual void add_menu_to_menubar(i32, i32) override;
    virtual void set_window_menubar(i32, i32) override;
    virtual void add_menu_item(i32, i32, i32, String const&, bool, bool, bool, bool, String const&, Gfx::ShareableBitmap const&, bool) override;
    virtual void add_menu_separator(i32) override;
    virtual void update_menu_item(i32, i32, i32, String const&, bool, bool, bool, bool, String const&) override;
    virtual Messages::WindowServer::CreateWindowResponse create_window(Gfx::IntRect const&, bool, bool, bool,
        bool, bool, bool, bool, bool, float, float, Gfx::IntSize const&, Gfx::IntSize const&, Gfx::IntSize const&,
        Optional<Gfx::IntSize> const&, i32, String const&, i32) override;
    virtual Messages::WindowServer::DestroyWindowResponse destroy_window(i32) override;
    virtual void set_window_title(i32, String const&) override;
    virtual Messages::WindowServer::GetWindowTitleResponse get_window_title(i32) override;
    virtual Messages::WindowServer::IsMaximizedResponse is_maximized(i32) override;
    virtual void start_window_resize(i32) override;
    virtual Messages::WindowServer::SetWindowRectResponse set_window_rect(i32, Gfx::IntRect const&) override;
    virtual Messages::WindowServer::GetWindowRectResponse get_window_rect(i32) override;
    virtual void set_window_minimum_size(i32, Gfx::IntSize const&) override;
    virtual Messages::WindowServer::GetWindowMinimumSizeResponse get_window_minimum_size(i32) override;
    virtual Messages::WindowServer::GetAppletRectOnScreenResponse get_applet_rect_on_screen(i32) override;
    virtual void invalidate_rect(i32, Vector<Gfx::IntRect> const&, bool) override;
    virtual void did_finish_painting(i32, Vector<Gfx::IntRect> const&) override;
    virtual void set_global_cursor_tracking(i32, bool) override;
    virtual void set_window_opacity(i32, float) override;
    virtual void set_window_backing_store(i32, i32, i32, IPC::File const&, i32, bool, Gfx::IntSize const&, bool) override;
    virtual void set_window_has_alpha_channel(i32, bool) override;
    virtual void set_window_alpha_hit_threshold(i32, float) override;
    virtual void move_window_to_front(i32) override;
    virtual void set_fullscreen(i32, bool) override;
    virtual void set_frameless(i32, bool) override;
    virtual void set_wallpaper(String const&) override;
    virtual void set_background_color(String const&) override;
    virtual void set_wallpaper_mode(String const&) override;
    virtual Messages::WindowServer::GetWallpaperResponse get_wallpaper() override;
    virtual Messages::WindowServer::SetResolutionResponse set_resolution(Gfx::IntSize const&, int) override;
    virtual void set_window_cursor(i32, i32) override;
    virtual void set_window_custom_cursor(i32, Gfx::ShareableBitmap const&) override;
    virtual void popup_menu(i32, Gfx::IntPoint const&) override;
    virtual void dismiss_menu(i32) override;
    virtual void set_window_icon_bitmap(i32, Gfx::ShareableBitmap const&) override;
    virtual Messages::WindowServer::StartDragResponse start_drag(String const&, HashMap<String, ByteBuffer> const&, Gfx::ShareableBitmap const&) override;
    virtual Messages::WindowServer::SetSystemThemeResponse set_system_theme(String const&, String const&) override;
    virtual Messages::WindowServer::GetSystemThemeResponse get_system_theme() override;
    virtual Messages::WindowServer::SetSystemFontsResponse set_system_fonts(String const&, String const&) override;
    virtual void set_window_base_size_and_size_increment(i32, Gfx::IntSize const&, Gfx::IntSize const&) override;
    virtual void set_window_resize_aspect_ratio(i32, Optional<Gfx::IntSize> const&) override;
    virtual void enable_display_link() override;
    virtual void disable_display_link() override;
    virtual void set_window_progress(i32, Optional<i32> const&) override;
    virtual void refresh_system_theme() override;
    virtual void pong() override;
    virtual Messages::WindowServer::GetGlobalCursorPositionResponse get_global_cursor_position() override;
    virtual void set_mouse_acceleration(float) override;
    virtual Messages::WindowServer::GetMouseAccelerationResponse get_mouse_acceleration() override;
    virtual void set_scroll_step_size(u32) override;
    virtual Messages::WindowServer::GetScrollStepSizeResponse get_scroll_step_size() override;
    virtual Messages::WindowServer::GetScreenBitmapResponse get_screen_bitmap(Optional<Gfx::IntRect> const&) override;
    virtual void set_double_click_speed(i32) override;
    virtual Messages::WindowServer::GetDoubleClickSpeedResponse get_double_click_speed() override;
    virtual void set_window_modified(i32, bool) override;
    virtual Messages::WindowServer::IsWindowModifiedResponse is_window_modified(i32) override;
    virtual Messages::WindowServer::GetDesktopDisplayScaleResponse get_desktop_display_scale() override;

    Window* window_from_id(i32 window_id);

    HashMap<int, NonnullRefPtr<Window>> m_windows;
    HashMap<int, NonnullRefPtr<Menubar>> m_menubars;
    HashMap<int, NonnullRefPtr<Menu>> m_menus;

    RefPtr<Core::Timer> m_ping_timer;

    int m_next_window_id { 1982 };

    bool m_has_display_link { false };
    bool m_unresponsive { false };

    // Need this to get private client connection stuff
    friend WMClientConnection;
};

}
