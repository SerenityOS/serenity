/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGfx/Bitmap.h>
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
    : public IPC::ClientConnection<WindowClientEndpoint, WindowServerEndpoint>
    , public WindowServerEndpoint {
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
        return const_cast<Menu*>(menu.value().ptr());
    }
    const Menu* find_menu_by_id(int menu_id) const
    {
        auto menu = m_menus.get(menu_id);
        if (!menu.has_value())
            return nullptr;
        return menu.value().ptr();
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

    virtual Messages::WindowServer::GreetResponse handle(const Messages::WindowServer::Greet&) override;
    virtual Messages::WindowServer::CreateMenubarResponse handle(const Messages::WindowServer::CreateMenubar&) override;
    virtual void handle(const Messages::WindowServer::DestroyMenubar&) override;
    virtual Messages::WindowServer::CreateMenuResponse handle(const Messages::WindowServer::CreateMenu&) override;
    virtual void handle(const Messages::WindowServer::DestroyMenu&) override;
    virtual void handle(const Messages::WindowServer::AddMenuToMenubar&) override;
    virtual void handle(const Messages::WindowServer::SetWindowMenubar&) override;
    virtual void handle(const Messages::WindowServer::AddMenuItem&) override;
    virtual void handle(const Messages::WindowServer::AddMenuSeparator&) override;
    virtual void handle(const Messages::WindowServer::UpdateMenuItem&) override;
    virtual Messages::WindowServer::CreateWindowResponse handle(const Messages::WindowServer::CreateWindow&) override;
    virtual Messages::WindowServer::DestroyWindowResponse handle(const Messages::WindowServer::DestroyWindow&) override;
    virtual void handle(const Messages::WindowServer::SetWindowTitle&) override;
    virtual Messages::WindowServer::GetWindowTitleResponse handle(const Messages::WindowServer::GetWindowTitle&) override;
    virtual Messages::WindowServer::IsMaximizedResponse handle(const Messages::WindowServer::IsMaximized&) override;
    virtual void handle(const Messages::WindowServer::StartWindowResize&) override;
    virtual Messages::WindowServer::SetWindowRectResponse handle(const Messages::WindowServer::SetWindowRect&) override;
    virtual Messages::WindowServer::GetWindowRectResponse handle(const Messages::WindowServer::GetWindowRect&) override;
    virtual void handle(const Messages::WindowServer::SetWindowMinimumSize&) override;
    virtual Messages::WindowServer::GetWindowMinimumSizeResponse handle(const Messages::WindowServer::GetWindowMinimumSize&) override;
    virtual Messages::WindowServer::GetAppletRectOnScreenResponse handle(const Messages::WindowServer::GetAppletRectOnScreen&) override;
    virtual void handle(const Messages::WindowServer::InvalidateRect&) override;
    virtual void handle(const Messages::WindowServer::DidFinishPainting&) override;
    virtual void handle(const Messages::WindowServer::SetGlobalCursorTracking&) override;
    virtual void handle(const Messages::WindowServer::SetWindowOpacity&) override;
    virtual void handle(const Messages::WindowServer::SetWindowBackingStore&) override;
    virtual void handle(const Messages::WindowServer::SetWindowHasAlphaChannel&) override;
    virtual void handle(const Messages::WindowServer::SetWindowAlphaHitThreshold&) override;
    virtual void handle(const Messages::WindowServer::MoveWindowToFront&) override;
    virtual void handle(const Messages::WindowServer::SetFullscreen&) override;
    virtual void handle(const Messages::WindowServer::SetFrameless&) override;
    virtual void handle(const Messages::WindowServer::AsyncSetWallpaper&) override;
    virtual void handle(const Messages::WindowServer::SetBackgroundColor&) override;
    virtual void handle(const Messages::WindowServer::SetWallpaperMode&) override;
    virtual Messages::WindowServer::GetWallpaperResponse handle(const Messages::WindowServer::GetWallpaper&) override;
    virtual Messages::WindowServer::SetResolutionResponse handle(const Messages::WindowServer::SetResolution&) override;
    virtual void handle(const Messages::WindowServer::SetWindowCursor&) override;
    virtual void handle(const Messages::WindowServer::SetWindowCustomCursor&) override;
    virtual void handle(const Messages::WindowServer::PopupMenu&) override;
    virtual void handle(const Messages::WindowServer::DismissMenu&) override;
    virtual void handle(const Messages::WindowServer::SetWindowIconBitmap&) override;
    virtual Messages::WindowServer::StartDragResponse handle(const Messages::WindowServer::StartDrag&) override;
    virtual Messages::WindowServer::SetSystemThemeResponse handle(const Messages::WindowServer::SetSystemTheme&) override;
    virtual Messages::WindowServer::GetSystemThemeResponse handle(const Messages::WindowServer::GetSystemTheme&) override;
    virtual void handle(const Messages::WindowServer::SetWindowBaseSizeAndSizeIncrement&) override;
    virtual void handle(const Messages::WindowServer::SetWindowResizeAspectRatio&) override;
    virtual void handle(const Messages::WindowServer::EnableDisplayLink&) override;
    virtual void handle(const Messages::WindowServer::DisableDisplayLink&) override;
    virtual void handle(const Messages::WindowServer::SetWindowProgress&) override;
    virtual void handle(const Messages::WindowServer::RefreshSystemTheme&) override;
    virtual void handle(const Messages::WindowServer::Pong&) override;
    virtual Messages::WindowServer::GetGlobalCursorPositionResponse handle(const Messages::WindowServer::GetGlobalCursorPosition&) override;
    virtual void handle(const Messages::WindowServer::SetMouseAcceleration&) override;
    virtual Messages::WindowServer::GetMouseAccelerationResponse handle(const Messages::WindowServer::GetMouseAcceleration&) override;
    virtual void handle(const Messages::WindowServer::SetScrollStepSize&) override;
    virtual Messages::WindowServer::GetScrollStepSizeResponse handle(const Messages::WindowServer::GetScrollStepSize&) override;
    virtual Messages::WindowServer::GetScreenBitmapResponse handle(const Messages::WindowServer::GetScreenBitmap&) override;
    virtual void handle(const Messages::WindowServer::SetDoubleClickSpeed&) override;
    virtual Messages::WindowServer::GetDoubleClickSpeedResponse handle(const Messages::WindowServer::GetDoubleClickSpeed&) override;
    virtual void handle(Messages::WindowServer::SetWindowModified const&) override;
    virtual Messages::WindowServer::IsWindowModifiedResponse handle(Messages::WindowServer::IsWindowModified const&) override;

    Window* window_from_id(i32 window_id);

    HashMap<int, NonnullRefPtr<Window>> m_windows;
    HashMap<int, NonnullRefPtr<Menubar>> m_menubars;
    HashMap<int, NonnullRefPtr<Menu>> m_menus;

    RefPtr<Core::Timer> m_ping_timer;

    int m_next_menubar_id { 10000 };
    int m_next_menu_id { 20000 };
    int m_next_window_id { 1982 };

    bool m_has_display_link { false };
    bool m_unresponsive { false };

    // Need this to get private client connection stuff
    friend WMClientConnection;
};

}
