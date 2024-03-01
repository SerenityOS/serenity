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
#include <LibCore/EventReceiver.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibIPC/ConnectionFromClient.h>
#include <WindowServer/Event.h>
#include <WindowServer/Menu.h>
#include <WindowServer/ScreenLayout.h>
#include <WindowServer/WindowClientEndpoint.h>
#include <WindowServer/WindowServerEndpoint.h>

namespace WindowServer {

class Compositor;
class Window;
class Menu;
class Menubar;
class ScreenLayout;
class WMConnectionFromClient;

class ConnectionFromClient final
    : public IPC::ConnectionFromClient<WindowClientEndpoint, WindowServerEndpoint> {
    C_OBJECT(ConnectionFromClient)
public:
    ~ConnectionFromClient() override;

    bool is_unresponsive() const { return m_unresponsive; }
    bool does_global_mouse_tracking() const { return m_does_global_mouse_tracking; }

    static ConnectionFromClient* from_client_id(int client_id);
    static void for_each_client(Function<void(ConnectionFromClient&)>);

    void notify_about_new_screen_rects();
    void post_paint_message(Window&, bool ignore_occlusion = false);

    Menu* find_menu_by_id(int menu_id)
    {
        auto menu = m_menus.get(menu_id);
        if (!menu.has_value())
            return nullptr;
        return menu.value();
    }
    Menu const* find_menu_by_id(int menu_id) const
    {
        auto menu = m_menus.get(menu_id);
        if (!menu.has_value())
            return nullptr;
        return menu.value();
    }

    template<typename Callback>
    void for_each_window(Callback callback)
    {
        for (auto& [_, window] : m_windows) {
            if (callback(*window) == IterationDecision::Break)
                break;
        }
    }
    template<typename Callback>
    void for_each_menu(Callback callback)
    {
        for (auto& [_, menu] : m_menus) {
            if (callback(*menu) == IterationDecision::Break)
                break;
        }
    }

    void notify_display_link(Badge<Compositor>);

    void notify_about_theme_change();

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket>, int client_id);

    // ^ConnectionFromClient
    virtual void die() override;
    virtual void may_have_become_unresponsive() override;
    virtual void did_become_responsive() override;

    void set_unresponsive(bool);
    void destroy_window(Window&, Vector<i32>& destroyed_window_ids);

    virtual void create_menu(i32, String const&, i32) override;
    virtual void set_menu_name(i32, String const&) override;
    virtual void set_menu_minimum_width(i32, i32) override;
    virtual void destroy_menu(i32) override;
    virtual void add_menu(i32, i32) override;
    virtual void add_menu_item(i32, i32, i32, ByteString const&, bool, bool, bool, bool, bool, ByteString const&, Gfx::ShareableBitmap const&, bool) override;
    virtual void add_menu_separator(i32) override;
    virtual void update_menu_item(i32, i32, i32, ByteString const&, bool, bool, bool, bool, bool, ByteString const&, Gfx::ShareableBitmap const&) override;
    virtual void remove_menu_item(i32 menu_id, i32 identifier) override;
    virtual void flash_menubar_menu(i32, i32) override;
    virtual void create_window(i32, i32, Gfx::IntRect const&, bool, bool, bool,
        bool, bool, bool, bool, bool, float, Gfx::IntSize, Gfx::IntSize, Gfx::IntSize,
        Optional<Gfx::IntSize> const&, i32, i32, ByteString const&, i32, Gfx::IntRect const&) override;
    virtual Messages::WindowServer::DestroyWindowResponse destroy_window(i32) override;
    virtual void set_window_title(i32, ByteString const&) override;
    virtual Messages::WindowServer::GetWindowTitleResponse get_window_title(i32) override;
    virtual Messages::WindowServer::IsMaximizedResponse is_maximized(i32) override;
    virtual void set_maximized(i32, bool) override;
    virtual Messages::WindowServer::IsMinimizedResponse is_minimized(i32) override;
    virtual void set_minimized(i32, bool) override;
    virtual void start_window_resize(i32, i32) override;
    virtual Messages::WindowServer::SetWindowRectResponse set_window_rect(i32, Gfx::IntRect const&) override;
    virtual Messages::WindowServer::GetWindowRectResponse get_window_rect(i32) override;
    virtual Messages::WindowServer::GetWindowFloatingRectResponse get_window_floating_rect(i32) override;
    virtual void set_window_minimum_size(i32, Gfx::IntSize) override;
    virtual Messages::WindowServer::GetWindowMinimumSizeResponse get_window_minimum_size(i32) override;
    virtual Messages::WindowServer::GetAppletRectOnScreenResponse get_applet_rect_on_screen(i32) override;
    virtual void invalidate_rect(i32, Vector<Gfx::IntRect> const&, bool) override;
    virtual void did_finish_painting(i32, Vector<Gfx::IntRect> const&) override;
    virtual void set_global_mouse_tracking(bool) override;
    virtual void set_window_backing_store(i32, i32, i32, IPC::File const&, i32, bool, Gfx::IntSize, Gfx::IntSize, bool) override;
    virtual void set_window_has_alpha_channel(i32, bool) override;
    virtual void set_window_alpha_hit_threshold(i32, float) override;
    virtual void move_window_to_front(i32) override;
    virtual void set_fullscreen(i32, bool) override;
    virtual void set_frameless(i32, bool) override;
    virtual void set_forced_shadow(i32, bool) override;
    virtual Messages::WindowServer::SetWallpaperResponse set_wallpaper(Gfx::ShareableBitmap const&) override;
    virtual void set_background_color(ByteString const&) override;
    virtual void set_wallpaper_mode(ByteString const&) override;
    virtual Messages::WindowServer::GetWallpaperResponse get_wallpaper() override;
    virtual Messages::WindowServer::SetScreenLayoutResponse set_screen_layout(ScreenLayout const&, bool) override;
    virtual Messages::WindowServer::GetScreenLayoutResponse get_screen_layout() override;
    virtual Messages::WindowServer::SaveScreenLayoutResponse save_screen_layout() override;
    virtual Messages::WindowServer::ApplyWorkspaceSettingsResponse apply_workspace_settings(u32, u32, bool) override;
    virtual Messages::WindowServer::GetWorkspaceSettingsResponse get_workspace_settings() override;
    virtual void show_screen_numbers(bool) override;
    virtual void set_window_cursor(i32, i32) override;
    virtual void set_window_custom_cursor(i32, Gfx::ShareableBitmap const&) override;
    virtual void popup_menu(i32, Gfx::IntPoint, Gfx::IntRect const&) override;
    virtual void dismiss_menu(i32) override;
    virtual void set_window_icon_bitmap(i32, Gfx::ShareableBitmap const&) override;
    virtual Messages::WindowServer::StartDragResponse start_drag(ByteString const&, HashMap<String, ByteBuffer> const&, Gfx::ShareableBitmap const&) override;
    virtual void set_accepts_drag(bool) override;
    virtual Messages::WindowServer::SetSystemThemeResponse set_system_theme(ByteString const&, ByteString const&, bool keep_desktop_background, Optional<ByteString> const& color_scheme_path) override;
    virtual Messages::WindowServer::GetSystemThemeResponse get_system_theme() override;
    virtual Messages::WindowServer::SetSystemThemeOverrideResponse set_system_theme_override(Core::AnonymousBuffer const&) override;
    virtual Messages::WindowServer::GetSystemThemeOverrideResponse get_system_theme_override() override;
    virtual void clear_system_theme_override() override;
    virtual Messages::WindowServer::IsSystemThemeOverriddenResponse is_system_theme_overridden() override;
    virtual Messages::WindowServer::GetPreferredColorSchemeResponse get_preferred_color_scheme() override;
    virtual void apply_cursor_theme(ByteString const&) override;
    virtual void set_cursor_highlight_radius(int radius) override;
    virtual Messages::WindowServer::GetCursorHighlightRadiusResponse get_cursor_highlight_radius() override;
    virtual void set_cursor_highlight_color(Gfx::Color color) override;
    virtual Messages::WindowServer::GetCursorHighlightColorResponse get_cursor_highlight_color() override;
    virtual Messages::WindowServer::GetCursorThemeResponse get_cursor_theme() override;
    virtual Messages::WindowServer::SetSystemFontsResponse set_system_fonts(ByteString const&, ByteString const&, ByteString const&) override;
    virtual void set_system_effects(Vector<bool> const&, u8, u8) override;
    virtual void set_window_base_size_and_size_increment(i32, Gfx::IntSize, Gfx::IntSize) override;
    virtual void set_window_resize_aspect_ratio(i32, Optional<Gfx::IntSize> const&) override;
    virtual void enable_display_link() override;
    virtual void disable_display_link() override;
    virtual void set_window_progress(i32, Optional<i32> const&) override;
    virtual void refresh_system_theme() override;
    virtual void pong() override;
    virtual void set_global_cursor_position(Gfx::IntPoint) override;
    virtual Messages::WindowServer::GetGlobalCursorPositionResponse get_global_cursor_position() override;
    virtual void set_mouse_acceleration(float) override;
    virtual Messages::WindowServer::GetMouseAccelerationResponse get_mouse_acceleration() override;
    virtual void set_scroll_step_size(u32) override;
    virtual Messages::WindowServer::GetScrollStepSizeResponse get_scroll_step_size() override;
    virtual Messages::WindowServer::GetScreenBitmapResponse get_screen_bitmap(Optional<Gfx::IntRect> const&, Optional<u32> const&) override;
    virtual Messages::WindowServer::GetScreenBitmapAroundCursorResponse get_screen_bitmap_around_cursor(Gfx::IntSize) override;
    virtual Messages::WindowServer::GetScreenBitmapAroundLocationResponse get_screen_bitmap_around_location(Gfx::IntSize, Gfx::IntPoint) override;
    virtual void set_double_click_speed(i32) override;
    virtual Messages::WindowServer::GetDoubleClickSpeedResponse get_double_click_speed() override;
    virtual void set_mouse_buttons_switched(bool) override;
    virtual Messages::WindowServer::AreMouseButtonsSwitchedResponse are_mouse_buttons_switched() override;
    virtual void set_natural_scroll(bool) override;
    virtual Messages::WindowServer::IsNaturalScrollResponse is_natural_scroll() override;
    virtual void set_window_modified(i32, bool) override;
    virtual Messages::WindowServer::IsWindowModifiedResponse is_window_modified(i32) override;
    virtual Messages::WindowServer::GetDesktopDisplayScaleResponse get_desktop_display_scale(u32) override;
    virtual void set_flash_flush(bool) override;
    virtual void set_window_parent_from_client(i32, i32, i32) override;
    virtual Messages::WindowServer::GetWindowRectFromClientResponse get_window_rect_from_client(i32, i32) override;
    virtual void add_window_stealing_for_client(i32, i32) override;
    virtual void remove_window_stealing_for_client(i32, i32) override;
    virtual void remove_window_stealing(i32) override;
    virtual void set_always_on_top(i32, bool) override;
    virtual Messages::WindowServer::GetColorUnderCursorResponse get_color_under_cursor() override;

    Window* window_from_id(i32 window_id);

    HashMap<int, NonnullRefPtr<Window>> m_windows;
    HashMap<int, NonnullRefPtr<Menu>> m_menus;

    RefPtr<Core::Timer> m_flashed_menu_timer;
    RefPtr<Core::Timer> m_ping_timer;

    bool m_has_display_link { false };
    bool m_show_screen_number { false };
    bool m_unresponsive { false };
    bool m_does_global_mouse_tracking { false };

    // Need this to get private client connection stuff
    friend WMConnectionFromClient;
};

}
