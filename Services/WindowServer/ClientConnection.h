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
#include <WindowServer/WindowClientEndpoint.h>
#include <WindowServer/WindowServerEndpoint.h>

namespace WindowServer {

class Compositor;
class Window;
class Menu;
class MenuBar;

class ClientConnection final
    : public IPC::ClientConnection<WindowClientEndpoint, WindowServerEndpoint>
    , public WindowServerEndpoint {
    C_OBJECT(ClientConnection)
public:
    ~ClientConnection() override;

    bool is_unresponsive() const { return m_unresponsive; }

    void boost();
    void deboost();

    static ClientConnection* from_client_id(int client_id);
    static void for_each_client(Function<void(ClientConnection&)>);

    MenuBar* app_menubar() { return m_app_menubar.ptr(); }

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

    void notify_display_link(Badge<Compositor>);

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id);

    // ^ClientConnection
    virtual void die() override;
    virtual void may_have_become_unresponsive() override;
    virtual void did_become_responsive() override;

    void set_unresponsive(bool);
    void destroy_window(Window&, Vector<i32>& destroyed_window_ids);

    virtual OwnPtr<Messages::WindowServer::GreetResponse> handle(const Messages::WindowServer::Greet&) override;
    virtual OwnPtr<Messages::WindowServer::CreateMenubarResponse> handle(const Messages::WindowServer::CreateMenubar&) override;
    virtual OwnPtr<Messages::WindowServer::DestroyMenubarResponse> handle(const Messages::WindowServer::DestroyMenubar&) override;
    virtual OwnPtr<Messages::WindowServer::CreateMenuResponse> handle(const Messages::WindowServer::CreateMenu&) override;
    virtual OwnPtr<Messages::WindowServer::DestroyMenuResponse> handle(const Messages::WindowServer::DestroyMenu&) override;
    virtual OwnPtr<Messages::WindowServer::AddMenuToMenubarResponse> handle(const Messages::WindowServer::AddMenuToMenubar&) override;
    virtual OwnPtr<Messages::WindowServer::SetApplicationMenubarResponse> handle(const Messages::WindowServer::SetApplicationMenubar&) override;
    virtual OwnPtr<Messages::WindowServer::AddMenuItemResponse> handle(const Messages::WindowServer::AddMenuItem&) override;
    virtual OwnPtr<Messages::WindowServer::AddMenuSeparatorResponse> handle(const Messages::WindowServer::AddMenuSeparator&) override;
    virtual OwnPtr<Messages::WindowServer::UpdateMenuItemResponse> handle(const Messages::WindowServer::UpdateMenuItem&) override;
    virtual OwnPtr<Messages::WindowServer::CreateWindowResponse> handle(const Messages::WindowServer::CreateWindow&) override;
    virtual OwnPtr<Messages::WindowServer::DestroyWindowResponse> handle(const Messages::WindowServer::DestroyWindow&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowTitleResponse> handle(const Messages::WindowServer::SetWindowTitle&) override;
    virtual OwnPtr<Messages::WindowServer::GetWindowTitleResponse> handle(const Messages::WindowServer::GetWindowTitle&) override;
    virtual OwnPtr<Messages::WindowServer::IsMaximizedResponse> handle(const Messages::WindowServer::IsMaximized&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowRectResponse> handle(const Messages::WindowServer::SetWindowRect&) override;
    virtual OwnPtr<Messages::WindowServer::GetWindowRectResponse> handle(const Messages::WindowServer::GetWindowRect&) override;
    virtual OwnPtr<Messages::WindowServer::GetWindowRectInMenubarResponse> handle(const Messages::WindowServer::GetWindowRectInMenubar&) override;
    virtual void handle(const Messages::WindowServer::InvalidateRect&) override;
    virtual void handle(const Messages::WindowServer::DidFinishPainting&) override;
    virtual OwnPtr<Messages::WindowServer::SetGlobalCursorTrackingResponse> handle(const Messages::WindowServer::SetGlobalCursorTracking&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowOpacityResponse> handle(const Messages::WindowServer::SetWindowOpacity&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowBackingStoreResponse> handle(const Messages::WindowServer::SetWindowBackingStore&) override;
    virtual void handle(const Messages::WindowServer::WM_SetActiveWindow&) override;
    virtual void handle(const Messages::WindowServer::WM_SetWindowMinimized&) override;
    virtual void handle(const Messages::WindowServer::WM_StartWindowResize&) override;
    virtual void handle(const Messages::WindowServer::WM_PopupWindowMenu&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowHasAlphaChannelResponse> handle(const Messages::WindowServer::SetWindowHasAlphaChannel&) override;
    virtual OwnPtr<Messages::WindowServer::MoveWindowToFrontResponse> handle(const Messages::WindowServer::MoveWindowToFront&) override;
    virtual OwnPtr<Messages::WindowServer::SetFullscreenResponse> handle(const Messages::WindowServer::SetFullscreen&) override;
    virtual void handle(const Messages::WindowServer::AsyncSetWallpaper&) override;
    virtual OwnPtr<Messages::WindowServer::SetBackgroundColorResponse> handle(const Messages::WindowServer::SetBackgroundColor&) override;
    virtual OwnPtr<Messages::WindowServer::SetWallpaperModeResponse> handle(const Messages::WindowServer::SetWallpaperMode&) override;
    virtual OwnPtr<Messages::WindowServer::GetWallpaperResponse> handle(const Messages::WindowServer::GetWallpaper&) override;
    virtual OwnPtr<Messages::WindowServer::SetResolutionResponse> handle(const Messages::WindowServer::SetResolution&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowCursorResponse> handle(const Messages::WindowServer::SetWindowCursor&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowCustomCursorResponse> handle(const Messages::WindowServer::SetWindowCustomCursor&) override;
    virtual OwnPtr<Messages::WindowServer::PopupMenuResponse> handle(const Messages::WindowServer::PopupMenu&) override;
    virtual OwnPtr<Messages::WindowServer::DismissMenuResponse> handle(const Messages::WindowServer::DismissMenu&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowIconBitmapResponse> handle(const Messages::WindowServer::SetWindowIconBitmap&) override;
    virtual void handle(const Messages::WindowServer::WM_SetWindowTaskbarRect&) override;
    virtual OwnPtr<Messages::WindowServer::StartDragResponse> handle(const Messages::WindowServer::StartDrag&) override;
    virtual OwnPtr<Messages::WindowServer::SetSystemMenuResponse> handle(const Messages::WindowServer::SetSystemMenu&) override;
    virtual OwnPtr<Messages::WindowServer::SetSystemThemeResponse> handle(const Messages::WindowServer::SetSystemTheme&) override;
    virtual OwnPtr<Messages::WindowServer::GetSystemThemeResponse> handle(const Messages::WindowServer::GetSystemTheme&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowBaseSizeAndSizeIncrementResponse> handle(const Messages::WindowServer::SetWindowBaseSizeAndSizeIncrement&) override;
    virtual OwnPtr<Messages::WindowServer::SetWindowResizeAspectRatioResponse> handle(const Messages::WindowServer::SetWindowResizeAspectRatio&) override;
    virtual void handle(const Messages::WindowServer::EnableDisplayLink&) override;
    virtual void handle(const Messages::WindowServer::DisableDisplayLink&) override;
    virtual void handle(const Messages::WindowServer::SetWindowProgress&) override;
    virtual void handle(const Messages::WindowServer::Pong&) override;

    Window* window_from_id(i32 window_id);

    HashMap<int, NonnullRefPtr<Window>> m_windows;
    HashMap<int, NonnullOwnPtr<MenuBar>> m_menubars;
    HashMap<int, NonnullRefPtr<Menu>> m_menus;
    WeakPtr<MenuBar> m_app_menubar;

    RefPtr<Core::Timer> m_ping_timer;

    int m_next_menubar_id { 10000 };
    int m_next_menu_id { 20000 };
    int m_next_window_id { 1982 };

    bool m_has_display_link { false };
    bool m_unresponsive { false };
};

}
