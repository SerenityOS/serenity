#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <LibCore/CObject.h>
#include <LibDraw/GraphicsBitmap.h>
#include <LibIPC/IClientConnection.h>
#include <WindowServer/WSEvent.h>
#include <WindowServer/WindowServerEndpoint.h>

class WSWindow;
class WSMenu;
class WSMenuBar;

class WSClientConnection final
    : public IClientConnection<WindowServerEndpoint>
    , public WindowServerEndpoint {
    C_OBJECT(WSClientConnection)
public:
    ~WSClientConnection() override;
    virtual void die() override;

    void boost();
    void deboost();

    static WSClientConnection* from_client_id(int client_id);
    static void for_each_client(Function<void(WSClientConnection&)>);

    WSMenuBar* app_menubar() { return m_app_menubar.ptr(); }

    bool is_showing_modal_window() const;

    void notify_about_new_screen_rect(const Rect&);
    void notify_about_clipboard_contents_changed();
    void post_paint_message(WSWindow&);

    WSMenu* find_menu_by_id(int menu_id)
    {
        auto menu = m_menus.get(menu_id);
        if (!menu.has_value())
            return nullptr;
        return const_cast<WSMenu*>(menu.value().ptr());
    }

private:
    explicit WSClientConnection(CLocalSocket&, int client_id);

    virtual OwnPtr<WindowServer::GreetResponse> handle(const WindowServer::Greet&) override;
    virtual OwnPtr<WindowServer::CreateMenubarResponse> handle(const WindowServer::CreateMenubar&) override;
    virtual OwnPtr<WindowServer::DestroyMenubarResponse> handle(const WindowServer::DestroyMenubar&) override;
    virtual OwnPtr<WindowServer::CreateMenuResponse> handle(const WindowServer::CreateMenu&) override;
    virtual OwnPtr<WindowServer::DestroyMenuResponse> handle(const WindowServer::DestroyMenu&) override;
    virtual OwnPtr<WindowServer::AddMenuToMenubarResponse> handle(const WindowServer::AddMenuToMenubar&) override;
    virtual OwnPtr<WindowServer::SetApplicationMenubarResponse> handle(const WindowServer::SetApplicationMenubar&) override;
    virtual OwnPtr<WindowServer::AddMenuItemResponse> handle(const WindowServer::AddMenuItem&) override;
    virtual OwnPtr<WindowServer::AddMenuSeparatorResponse> handle(const WindowServer::AddMenuSeparator&) override;
    virtual OwnPtr<WindowServer::UpdateMenuItemResponse> handle(const WindowServer::UpdateMenuItem&) override;
    virtual OwnPtr<WindowServer::CreateWindowResponse> handle(const WindowServer::CreateWindow&) override;
    virtual OwnPtr<WindowServer::DestroyWindowResponse> handle(const WindowServer::DestroyWindow&) override;
    virtual OwnPtr<WindowServer::SetWindowTitleResponse> handle(const WindowServer::SetWindowTitle&) override;
    virtual OwnPtr<WindowServer::GetWindowTitleResponse> handle(const WindowServer::GetWindowTitle&) override;
    virtual OwnPtr<WindowServer::SetWindowRectResponse> handle(const WindowServer::SetWindowRect&) override;
    virtual OwnPtr<WindowServer::GetWindowRectResponse> handle(const WindowServer::GetWindowRect&) override;
    virtual void handle(const WindowServer::InvalidateRect&) override;
    virtual void handle(const WindowServer::DidFinishPainting&) override;
    virtual OwnPtr<WindowServer::SetGlobalCursorTrackingResponse> handle(const WindowServer::SetGlobalCursorTracking&) override;
    virtual OwnPtr<WindowServer::SetWindowOpacityResponse> handle(const WindowServer::SetWindowOpacity&) override;
    virtual OwnPtr<WindowServer::SetWindowBackingStoreResponse> handle(const WindowServer::SetWindowBackingStore&) override;
    virtual OwnPtr<WindowServer::GetClipboardContentsResponse> handle(const WindowServer::GetClipboardContents&) override;
    virtual OwnPtr<WindowServer::SetClipboardContentsResponse> handle(const WindowServer::SetClipboardContents&) override;
    virtual void handle(const WindowServer::WM_SetActiveWindow&) override;
    virtual void handle(const WindowServer::WM_SetWindowMinimized&) override;
    virtual void handle(const WindowServer::WM_StartWindowResize&) override;
    virtual void handle(const WindowServer::WM_PopupWindowMenu&) override;
    virtual OwnPtr<WindowServer::SetWindowHasAlphaChannelResponse> handle(const WindowServer::SetWindowHasAlphaChannel&) override;
    virtual OwnPtr<WindowServer::MoveWindowToFrontResponse> handle(const WindowServer::MoveWindowToFront&) override;
    virtual OwnPtr<WindowServer::SetFullscreenResponse> handle(const WindowServer::SetFullscreen&) override;
    virtual void handle(const WindowServer::AsyncSetWallpaper&) override;
    virtual OwnPtr<WindowServer::GetWallpaperResponse> handle(const WindowServer::GetWallpaper&) override;
    virtual OwnPtr<WindowServer::SetResolutionResponse> handle(const WindowServer::SetResolution&) override;
    virtual OwnPtr<WindowServer::SetWindowOverrideCursorResponse> handle(const WindowServer::SetWindowOverrideCursor&) override;
    virtual OwnPtr<WindowServer::PopupMenuResponse> handle(const WindowServer::PopupMenu&) override;
    virtual OwnPtr<WindowServer::DismissMenuResponse> handle(const WindowServer::DismissMenu&) override;
    virtual OwnPtr<WindowServer::SetWindowIconBitmapResponse> handle(const WindowServer::SetWindowIconBitmap&) override;
    virtual void handle(const WindowServer::WM_SetWindowTaskbarRect&) override;
    virtual OwnPtr<WindowServer::StartDragResponse> handle(const WindowServer::StartDrag&) override;

    HashMap<int, NonnullRefPtr<WSWindow>> m_windows;
    HashMap<int, NonnullOwnPtr<WSMenuBar>> m_menubars;
    HashMap<int, NonnullRefPtr<WSMenu>> m_menus;
    WeakPtr<WSMenuBar> m_app_menubar;

    int m_next_menubar_id { 10000 };
    int m_next_menu_id { 20000 };
    int m_next_window_id { 1982 };

    RefPtr<SharedBuffer> m_last_sent_clipboard_content;
};
