#include <LibC/SharedBuffer.h>
#include <LibDraw/GraphicsBitmap.h>
#include <SharedBuffer.h>
#include <WindowServer/WSAPITypes.h>
#include <WindowServer/WSClientConnection.h>
#include <WindowServer/WSClipboard.h>
#include <WindowServer/WSCompositor.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSMenu.h>
#include <WindowServer/WSMenuBar.h>
#include <WindowServer/WSMenuItem.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSWindowSwitcher.h>
#include <errno.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/uio.h>
#include <unistd.h>

HashMap<int, NonnullRefPtr<WSClientConnection>>* s_connections;

void WSClientConnection::for_each_client(Function<void(WSClientConnection&)> callback)
{
    if (!s_connections)
        return;
    for (auto& it : *s_connections) {
        callback(*it.value);
    }
}

WSClientConnection* WSClientConnection::from_client_id(int client_id)
{
    if (!s_connections)
        return nullptr;
    auto it = s_connections->find(client_id);
    if (it == s_connections->end())
        return nullptr;
    return (*it).value.ptr();
}

WSClientConnection::WSClientConnection(CLocalSocket& client_socket, int client_id)
    : Connection(client_socket, client_id)
{
    if (!s_connections)
        s_connections = new HashMap<int, NonnullRefPtr<WSClientConnection>>;
    s_connections->set(client_id, *this);
}

WSClientConnection::~WSClientConnection()
{
    auto windows = move(m_windows);
}

void WSClientConnection::send_greeting()
{
    WSAPI_ServerMessage message;
    message.type = WSAPI_ServerMessage::Type::Greeting;
    message.greeting.server_pid = getpid();
    message.greeting.your_client_id = client_id();
    message.greeting.screen_rect = WSScreen::the().rect();
    post_message(message);
}

void WSClientConnection::die()
{
    s_connections->remove(client_id());
}

void WSClientConnection::post_error(const String& error_message)
{
    dbgprintf("WSClientConnection::post_error: client_id=%d: %s\n", client_id(), error_message.characters());
    WSAPI_ServerMessage message;
    message.type = WSAPI_ServerMessage::Type::Error;
    ASSERT(error_message.length() < (ssize_t)sizeof(message.text));
    strcpy(message.text, error_message.characters());
    message.text_length = error_message.length();
    post_message(message);
}

void WSClientConnection::notify_about_new_screen_rect(const Rect& rect)
{
    WSAPI_ServerMessage message;
    message.type = WSAPI_ServerMessage::Type::ScreenRectChanged;
    message.screen.rect = rect;
    post_message(message);
}

void WSClientConnection::notify_about_clipboard_contents_changed()
{
    auto& clipboard = WSClipboard::the();
    WSAPI_ServerMessage message;
    message.type = WSAPI_ServerMessage::Type::ClipboardContentsChanged;
    message.clipboard.shared_buffer_id = -1;
    message.clipboard.contents_size = -1;
    ASSERT(clipboard.data_type().length() < (ssize_t)sizeof(message.text));
    strcpy(message.text, clipboard.data_type().characters());
    message.text_length = clipboard.data_type().length();
    post_message(message);
}

void WSClientConnection::event(CEvent& event)
{
    if (static_cast<WSEvent&>(event).is_client_request()) {
        on_request(static_cast<const WSAPIClientRequest&>(event));
        return;
    }

    Connection::event(event);
}

static Vector<Rect, 32> get_rects(const WSAPI_ClientMessage& message, const ByteBuffer& extra_data)
{
    Vector<Rect, 32> rects;
    if (message.rect_count > (int)(WSAPI_ClientMessage::max_inline_rect_count + extra_data.size() / sizeof(WSAPI_Rect))) {
        return {};
    }
    for (int i = 0; i < min(WSAPI_ClientMessage::max_inline_rect_count, message.rect_count); ++i)
        rects.append(message.rects[i]);
    if (!extra_data.is_empty()) {
        auto* extra_rects = reinterpret_cast<const WSAPI_Rect*>(extra_data.data());
        for (int i = 0; i < (int)(extra_data.size() / sizeof(WSAPI_Rect)); ++i)
            rects.append(extra_rects[i]);
    }
    return rects;
}

bool WSClientConnection::handle_message(const WSAPI_ClientMessage& message, const ByteBuffer&& extra_data)
{
    switch (message.type) {
    case WSAPI_ClientMessage::Type::Greeting:
        set_client_pid(message.greeting.client_pid);
        break;
    case WSAPI_ClientMessage::Type::CreateMenubar:
        CEventLoop::current().post_event(*this, make<WSAPICreateMenubarRequest>(client_id()));
        break;
    case WSAPI_ClientMessage::Type::DestroyMenubar:
        CEventLoop::current().post_event(*this, make<WSAPIDestroyMenubarRequest>(client_id(), message.menu.menubar_id));
        break;
    case WSAPI_ClientMessage::Type::SetApplicationMenubar:
        CEventLoop::current().post_event(*this, make<WSAPISetApplicationMenubarRequest>(client_id(), message.menu.menubar_id));
        break;
    case WSAPI_ClientMessage::Type::AddMenuToMenubar:
        CEventLoop::current().post_event(*this, make<WSAPIAddMenuToMenubarRequest>(client_id(), message.menu.menubar_id, message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::CreateMenu:
        if (message.text_length > (int)sizeof(message.text)) {
            did_misbehave();
            return false;
        }
        CEventLoop::current().post_event(*this, make<WSAPICreateMenuRequest>(client_id(), String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::PopupMenu:
        CEventLoop::current().post_event(*this, make<WSAPIPopupMenuRequest>(client_id(), message.menu.menu_id, message.menu.position));
        break;
    case WSAPI_ClientMessage::Type::DismissMenu:
        CEventLoop::current().post_event(*this, make<WSAPIDismissMenuRequest>(client_id(), message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowIconBitmap:
        CEventLoop::current().post_event(*this, make<WSAPISetWindowIconBitmapRequest>(client_id(), message.window_id, message.window.icon_buffer_id, message.window.icon_size));
        break;
    case WSAPI_ClientMessage::Type::DestroyMenu:
        CEventLoop::current().post_event(*this, make<WSAPIDestroyMenuRequest>(client_id(), message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::AddMenuItem:
        if (message.text_length > (int)sizeof(message.text)) {
            did_misbehave();
            return false;
        }
        if (message.menu.shortcut_text_length > (int)sizeof(message.menu.shortcut_text)) {
            did_misbehave();
            return false;
        }
        CEventLoop::current().post_event(*this, make<WSAPIAddMenuItemRequest>(client_id(), message.menu.menu_id, message.menu.identifier, String(message.text, message.text_length), String(message.menu.shortcut_text, message.menu.shortcut_text_length), message.menu.enabled, message.menu.checkable, message.menu.checked, message.menu.icon_buffer_id, message.menu.submenu_id));
        break;
    case WSAPI_ClientMessage::Type::UpdateMenuItem:
        if (message.text_length > (int)sizeof(message.text)) {
            did_misbehave();
            return false;
        }
        if (message.menu.shortcut_text_length > (int)sizeof(message.menu.shortcut_text)) {
            did_misbehave();
            return false;
        }
        CEventLoop::current().post_event(*this, make<WSAPIUpdateMenuItemRequest>(client_id(), message.menu.menu_id, message.menu.identifier, String(message.text, message.text_length), String(message.menu.shortcut_text, message.menu.shortcut_text_length), message.menu.enabled, message.menu.checkable, message.menu.checked));
        break;
    case WSAPI_ClientMessage::Type::AddMenuSeparator:
        CEventLoop::current().post_event(*this, make<WSAPIAddMenuSeparatorRequest>(client_id(), message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::CreateWindow: {
        if (message.text_length > (int)sizeof(message.text)) {
            did_misbehave();
            return false;
        }

        auto ws_window_type = WSWindowType::Invalid;
        switch (message.window.type) {
        case WSAPI_WindowType::Normal:
            ws_window_type = WSWindowType::Normal;
            break;
        case WSAPI_WindowType::Menu:
            ws_window_type = WSWindowType::Menu;
            break;
        case WSAPI_WindowType::WindowSwitcher:
            ws_window_type = WSWindowType::WindowSwitcher;
            break;
        case WSAPI_WindowType::Taskbar:
            ws_window_type = WSWindowType::Taskbar;
            break;
        case WSAPI_WindowType::Tooltip:
            ws_window_type = WSWindowType::Tooltip;
            break;
        case WSAPI_WindowType::Menubar:
            ws_window_type = WSWindowType::Menubar;
            break;
        case WSAPI_WindowType::Launcher:
            ws_window_type = WSWindowType::Launcher;
            break;
        case WSAPI_WindowType::Invalid:
        default:
            dbgprintf("Unknown WSAPI_WindowType: %d\n", message.window.type);
            did_misbehave();
            return false;
        }

        CEventLoop::current().post_event(*this,
            make<WSAPICreateWindowRequest>(client_id(),
                message.window.rect,
                String(message.text, message.text_length),
                message.window.has_alpha_channel,
                message.window.modal,
                message.window.resizable,
                message.window.fullscreen,
                message.window.show_titlebar,
                message.window.opacity,
                message.window.base_size,
                message.window.size_increment,
                ws_window_type,
                Color::from_rgba(message.window.background_color)));
        break;
    }
    case WSAPI_ClientMessage::Type::DestroyWindow:
        CEventLoop::current().post_event(*this, make<WSAPIDestroyWindowRequest>(client_id(), message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowTitle:
        if (message.text_length > (int)sizeof(message.text)) {
            did_misbehave();
            return false;
        }
        CEventLoop::current().post_event(*this, make<WSAPISetWindowTitleRequest>(client_id(), message.window_id, String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::GetWindowTitle:
        CEventLoop::current().post_event(*this, make<WSAPIGetWindowTitleRequest>(client_id(), message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowRect:
        CEventLoop::current().post_event(*this, make<WSAPISetWindowRectRequest>(client_id(), message.window_id, message.window.rect));
        break;
    case WSAPI_ClientMessage::Type::GetWindowRect:
        CEventLoop::current().post_event(*this, make<WSAPIGetWindowRectRequest>(client_id(), message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetClipboardContents:
        if (message.text_length > (int)sizeof(message.text)) {
            did_misbehave();
            return false;
        }
        CEventLoop::current().post_event(*this, make<WSAPISetClipboardContentsRequest>(client_id(), message.clipboard.shared_buffer_id, message.clipboard.contents_size, String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::GetClipboardContents:
        CEventLoop::current().post_event(*this, make<WSAPIGetClipboardContentsRequest>(client_id()));
        break;
    case WSAPI_ClientMessage::Type::InvalidateRect: {
        auto rects = get_rects(message, extra_data);
        if (rects.is_empty()) {
            did_misbehave();
            return false;
        }
        CEventLoop::current().post_event(*this, make<WSAPIInvalidateRectRequest>(client_id(), message.window_id, rects));
        break;
    }
    case WSAPI_ClientMessage::Type::DidFinishPainting: {
        auto rects = get_rects(message, extra_data);
        if (rects.is_empty()) {
            did_misbehave();
            return false;
        }
        CEventLoop::current().post_event(*this, make<WSAPIDidFinishPaintingNotification>(client_id(), message.window_id, rects));
        break;
    }
    case WSAPI_ClientMessage::Type::GetWindowBackingStore:
        CEventLoop::current().post_event(*this, make<WSAPIGetWindowBackingStoreRequest>(client_id(), message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowBackingStore:
        CEventLoop::current().post_event(*this, make<WSAPISetWindowBackingStoreRequest>(client_id(), message.window_id, message.backing.shared_buffer_id, message.backing.size, message.backing.bpp, message.backing.pitch, message.backing.has_alpha_channel, message.backing.flush_immediately));
        break;
    case WSAPI_ClientMessage::Type::SetGlobalCursorTracking:
        CEventLoop::current().post_event(*this, make<WSAPISetGlobalCursorTrackingRequest>(client_id(), message.window_id, message.value));
        break;
    case WSAPI_ClientMessage::Type::SetWallpaper:
        if (message.text_length > (int)sizeof(message.text)) {
            did_misbehave();
            return false;
        }
        CEventLoop::current().post_event(*this, make<WSAPISetWallpaperRequest>(client_id(), String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::GetWallpaper:
        CEventLoop::current().post_event(*this, make<WSAPIGetWallpaperRequest>(client_id()));
        break;
    case WSAPI_ClientMessage::Type::SetResolution:
        CEventLoop::current().post_event(*this, make<WSAPISetResolutionRequest>(client_id(), message.wm_conf.resolution.width, message.wm_conf.resolution.height));
        break;
    case WSAPI_ClientMessage::Type::SetWindowOverrideCursor:
        CEventLoop::current().post_event(*this, make<WSAPISetWindowOverrideCursorRequest>(client_id(), message.window_id, (WSStandardCursor)message.cursor.cursor));
        break;
    case WSAPI_ClientMessage::SetWindowHasAlphaChannel:
        CEventLoop::current().post_event(*this, make<WSAPISetWindowHasAlphaChannelRequest>(client_id(), message.window_id, message.value));
        break;
    case WSAPI_ClientMessage::Type::WM_SetActiveWindow:
        CEventLoop::current().post_event(*this, make<WSWMAPISetActiveWindowRequest>(client_id(), message.wm.client_id, message.wm.window_id));
        break;
    case WSAPI_ClientMessage::Type::WM_SetWindowMinimized:
        CEventLoop::current().post_event(*this, make<WSWMAPISetWindowMinimizedRequest>(client_id(), message.wm.client_id, message.wm.window_id, message.wm.minimized));
        break;
    case WSAPI_ClientMessage::Type::WM_StartWindowResize:
        CEventLoop::current().post_event(*this, make<WSWMAPIStartWindowResizeRequest>(client_id(), message.wm.client_id, message.wm.window_id));
        break;
    case WSAPI_ClientMessage::Type::WM_PopupWindowMenu:
        CEventLoop::current().post_event(*this, make<WSWMAPIPopupWindowMenuRequest>(client_id(), message.wm.client_id, message.wm.window_id, message.wm.position));
        break;
    case WSAPI_ClientMessage::Type::MoveWindowToFront:
        CEventLoop::current().post_event(*this, make<WSAPIMoveWindowToFrontRequest>(client_id(), message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetFullscreen:
        CEventLoop::current().post_event(*this, make<WSAPISetFullscreenRequest>(client_id(), message.window_id, message.value));
        break;
    default:
        break;
    }
    return true;
}

void WSClientConnection::handle_request(const WSAPICreateMenubarRequest&)
{
    int menubar_id = m_next_menubar_id++;
    auto menubar = make<WSMenuBar>(*this, menubar_id);
    m_menubars.set(menubar_id, move(menubar));
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidCreateMenubar;
    response.menu.menubar_id = menubar_id;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIDestroyMenubarRequest& request)
{
    int menubar_id = request.menubar_id();
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        post_error("WSAPIDestroyMenubarRequest: Bad menubar ID");
        return;
    }
    auto& menubar = *(*it).value;
    WSWindowManager::the().close_menubar(menubar);
    m_menubars.remove(it);
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidDestroyMenubar;
    response.menu.menubar_id = menubar_id;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPICreateMenuRequest& request)
{
    int menu_id = m_next_menu_id++;
    auto menu = WSMenu::construct(this, menu_id, request.text());
    m_menus.set(menu_id, move(menu));
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidCreateMenu;
    response.menu.menu_id = menu_id;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIDestroyMenuRequest& request)
{
    int menu_id = static_cast<const WSAPIDestroyMenuRequest&>(request).menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        post_error("WSAPIDestroyMenuRequest: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.close();
    m_menus.remove(it);
    remove_child(menu);
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidDestroyMenu;
    response.menu.menu_id = menu_id;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPISetApplicationMenubarRequest& request)
{
    int menubar_id = request.menubar_id();
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        post_error("WSAPISetApplicationMenubarRequest: Bad menubar ID");
        return;
    }
    auto& menubar = *(*it).value;
    m_app_menubar = menubar.make_weak_ptr();
    WSWindowManager::the().notify_client_changed_app_menubar(*this);
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidSetApplicationMenubar;
    response.menu.menubar_id = menubar_id;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIAddMenuToMenubarRequest& request)
{
    int menubar_id = request.menubar_id();
    int menu_id = request.menu_id();
    auto it = m_menubars.find(menubar_id);
    auto jt = m_menus.find(menu_id);
    if (it == m_menubars.end()) {
        post_error("WSAPIAddMenuToMenubarRequest: Bad menubar ID");
        return;
    }
    if (jt == m_menus.end()) {
        post_error("WSAPIAddMenuToMenubarRequest: Bad menu ID");
        return;
    }
    auto& menubar = *(*it).value;
    auto& menu = *(*jt).value;
    menubar.add_menu(menu);
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidAddMenuToMenubar;
    response.menu.menubar_id = menubar_id;
    response.menu.menu_id = menu_id;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIAddMenuItemRequest& request)
{
    int menu_id = request.menu_id();
    unsigned identifier = request.identifier();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        post_error("WSAPIAddMenuItemRequest: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    auto menu_item = make<WSMenuItem>(menu, identifier, request.text(), request.shortcut_text(), request.is_enabled(), request.is_checkable(), request.is_checked());
    if (request.icon_buffer_id() != -1) {
        auto icon_buffer = SharedBuffer::create_from_shared_buffer_id(request.icon_buffer_id());
        if (!icon_buffer) {
            did_misbehave();
            return;
        }
        // FIXME: Verify that the icon buffer can accomodate a 16x16 bitmap view.
        auto shared_icon = GraphicsBitmap::create_with_shared_buffer(GraphicsBitmap::Format::RGBA32, icon_buffer.release_nonnull(), { 16, 16 });
        menu_item->set_icon(shared_icon);
    }
    menu_item->set_submenu_id(request.submenu_id());
    menu.add_item(move(menu_item));
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidAddMenuItem;
    response.menu.menu_id = menu_id;
    response.menu.identifier = identifier;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIPopupMenuRequest& request)
{
    int menu_id = request.menu_id();
    auto position = request.position();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        post_error("WSAPIPopupMenuRequest: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.popup(position);
}

void WSClientConnection::handle_request(const WSAPIDismissMenuRequest& request)
{
    int menu_id = request.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        post_error("WSAPIDismissMenuRequest: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.close();
}

void WSClientConnection::handle_request(const WSAPIUpdateMenuItemRequest& request)
{
    int menu_id = request.menu_id();
    unsigned identifier = request.identifier();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        post_error("WSAPIUpdateMenuItemRequest: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    auto* menu_item = menu.item_with_identifier(request.identifier());
    if (!menu_item) {
        post_error("WSAPIUpdateMenuItemRequest: Bad menu item identifier");
        return;
    }
    menu_item->set_text(request.text());
    menu_item->set_shortcut_text(request.shortcut_text());
    menu_item->set_enabled(request.is_enabled());
    menu_item->set_checkable(request.is_checkable());
    if (request.is_checkable())
        menu_item->set_checked(request.is_checked());
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidUpdateMenuItem;
    response.menu.menu_id = menu_id;
    response.menu.identifier = identifier;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIAddMenuSeparatorRequest& request)
{
    int menu_id = request.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        post_error("WSAPIAddMenuSeparatorRequest: Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.add_item(make<WSMenuItem>(menu, WSMenuItem::Separator));
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidAddMenuSeparator;
    response.menu.menu_id = menu_id;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIMoveWindowToFrontRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPIMoveWindowToFrontRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WSWindowManager::the().move_to_front_and_make_active(window);
}

void WSClientConnection::handle_request(const WSAPISetFullscreenRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetFullscreenRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_fullscreen(request.fullscreen());

    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidSetFullscreen;
    response.window_id = window_id;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPISetWindowOpacityRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetWindowOpacityRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_opacity(request.opacity());
}

void WSClientConnection::handle_request(const WSAPISetWallpaperRequest& request)
{
    WSCompositor::the().set_wallpaper(request.wallpaper(), [&](bool success) {
        WSAPI_ServerMessage response;
        response.type = WSAPI_ServerMessage::Type::DidSetWallpaper;
        response.value = success;
        post_message(response);
    });
}

void WSClientConnection::handle_request(const WSAPIGetWallpaperRequest&)
{
    auto path = WSCompositor::the().wallpaper_path();
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidGetWallpaper;
    ASSERT(path.length() < (int)sizeof(response.text));
    memcpy(response.text, path.characters(), path.length() + 1);
    response.text_length = path.length();
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPISetResolutionRequest& request)
{
    WSWindowManager::the().set_resolution(request.resolution().width(), request.resolution().height());
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidSetResolution;
    response.value = true;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPISetWindowTitleRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetWindowTitleRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_title(request.title());
}

void WSClientConnection::handle_request(const WSAPIGetWindowTitleRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPIGetWindowTitleRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidGetWindowTitle;
    response.window_id = window.window_id();
    ASSERT(window.title().length() < (ssize_t)sizeof(response.text));
    strcpy(response.text, window.title().characters());
    response.text_length = window.title().length();
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPISetWindowIconBitmapRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetWindowIconBitmapRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;

    auto icon_buffer = SharedBuffer::create_from_shared_buffer_id(request.icon_buffer_id());

    if (!icon_buffer) {
        window.set_default_icon();
    } else {
        window.set_icon(GraphicsBitmap::create_with_shared_buffer(GraphicsBitmap::Format::RGBA32, *icon_buffer, request.icon_size()));
    }

    window.frame().invalidate_title_bar();
    WSWindowManager::the().tell_wm_listeners_window_icon_changed(window);
}

void WSClientConnection::handle_request(const WSAPISetWindowRectRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetWindowRectRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (window.is_fullscreen()) {
        dbgprintf("WSClientConnection: Ignoring SetWindowRect request for fullscreen window\n");
        return;
    }
    window.set_rect(request.rect());
    window.request_update(request.rect());
}

void WSClientConnection::handle_request(const WSAPIGetWindowRectRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPIGetWindowRectRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidGetWindowRect;
    response.window_id = window.window_id();
    response.window.rect = window.rect();
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPISetClipboardContentsRequest& request)
{
    auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(request.shared_buffer_id());
    if (!shared_buffer) {
        post_error("WSAPISetClipboardContentsRequest: Bad shared buffer ID");
        return;
    }
    WSClipboard::the().set_data(*shared_buffer, request.size(), request.data_type());
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidSetClipboardContents;
    response.clipboard.shared_buffer_id = shared_buffer->shared_buffer_id();
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIGetClipboardContentsRequest&)
{
    auto& clipboard = WSClipboard::the();
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidGetClipboardContents;
    response.clipboard.shared_buffer_id = -1;
    response.clipboard.contents_size = 0;
    if (clipboard.size()) {
        // FIXME: Optimize case where an app is copy/pasting within itself.
        //        We can just reuse the SharedBuffer then, since it will have the same peer PID.
        //        It would be even nicer if a SharedBuffer could have an arbitrary number of clients..
        RefPtr<SharedBuffer> shared_buffer = SharedBuffer::create_with_size(clipboard.size());
        ASSERT(shared_buffer);
        memcpy(shared_buffer->data(), clipboard.data(), clipboard.size());
        shared_buffer->seal();
        shared_buffer->share_with(client_pid());
        response.clipboard.shared_buffer_id = shared_buffer->shared_buffer_id();
        response.clipboard.contents_size = clipboard.size();

        // FIXME: This is a workaround for the fact that SharedBuffers will go away if neither side is retaining them.
        //        After we respond to GetClipboardContents, we have to wait for the client to ref the buffer on his side.
        m_last_sent_clipboard_content = move(shared_buffer);
    }
    ASSERT(clipboard.data_type().length() < (ssize_t)sizeof(response.text));
    if (!clipboard.data_type().is_null())
        strcpy(response.text, clipboard.data_type().characters());
    response.text_length = clipboard.data_type().length();
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPICreateWindowRequest& request)
{
    int window_id = m_next_window_id++;
    auto window = WSWindow::construct(*this, request.window_type(), window_id, request.is_modal(), request.is_resizable(), request.is_fullscreen());
    window->set_background_color(request.background_color());
    window->set_has_alpha_channel(request.has_alpha_channel());
    window->set_title(request.title());
    if (!request.is_fullscreen())
        window->set_rect(request.rect());
    window->set_show_titlebar(request.show_titlebar());
    window->set_opacity(request.opacity());
    window->set_size_increment(request.size_increment());
    window->set_base_size(request.base_size());
    window->invalidate();
    m_windows.set(window_id, move(window));
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidCreateWindow;
    response.window_id = window_id;
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIDestroyWindowRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPIDestroyWindowRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WSWindowManager::the().invalidate(window);
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidDestroyWindow;
    response.window_id = window.window_id();
    post_message(response);

    remove_child(window);
    ASSERT(it->value.ptr() == &window);
    m_windows.remove(window_id);
}

void WSClientConnection::post_paint_message(WSWindow& window)
{
    auto rect_set = window.take_pending_paint_rects();
    if (window.is_minimized())
        return;
    WSAPI_ServerMessage message;
    message.type = WSAPI_ServerMessage::Type::Paint;
    message.window_id = window.window_id();
    auto& rects = rect_set.rects();
    message.rect_count = rects.size();
    for (int i = 0; i < min(WSAPI_ServerMessage::max_inline_rect_count, rects.size()); ++i)
        message.rects[i] = rects[i];
    message.paint.window_size = window.size();
    ByteBuffer extra_data;
    if (rects.size() > WSAPI_ServerMessage::max_inline_rect_count)
        extra_data = ByteBuffer::wrap(&rects[WSAPI_ServerMessage::max_inline_rect_count], (rects.size() - WSAPI_ServerMessage::max_inline_rect_count) * sizeof(WSAPI_Rect));
    post_message(message, extra_data);
}

void WSClientConnection::handle_request(const WSAPIInvalidateRectRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPIInvalidateRectRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    for (int i = 0; i < request.rects().size(); ++i)
        window.request_update(request.rects()[i].intersected({ {}, window.size() }));
}

void WSClientConnection::handle_request(const WSAPIDidFinishPaintingNotification& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPIDidFinishPaintingNotification: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    for (auto& rect : request.rects())
        WSWindowManager::the().invalidate(window, rect);

    WSWindowSwitcher::the().refresh_if_needed();
}

void WSClientConnection::handle_request(const WSAPIGetWindowBackingStoreRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPIGetWindowBackingStoreRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    auto* backing_store = window.backing_store();

    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidGetWindowBackingStore;
    response.window_id = window_id;
    response.backing.bpp = sizeof(RGBA32);
    response.backing.pitch = backing_store->pitch();
    response.backing.size = backing_store->size();
    response.backing.has_alpha_channel = backing_store->has_alpha_channel();
    response.backing.shared_buffer_id = backing_store->shared_buffer_id();
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPISetWindowBackingStoreRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetWindowBackingStoreRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (window.last_backing_store() && window.last_backing_store()->shared_buffer_id() == request.shared_buffer_id()) {
        window.swap_backing_stores();
    } else {
        auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(request.shared_buffer_id());
        if (!shared_buffer)
            return;
        auto backing_store = GraphicsBitmap::create_with_shared_buffer(
            request.has_alpha_channel() ? GraphicsBitmap::Format::RGBA32 : GraphicsBitmap::Format::RGB32,
            *shared_buffer,
            request.size());
        window.set_backing_store(move(backing_store));
    }

    if (request.flush_immediately())
        window.invalidate();

    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidSetWindowBackingStore;
    response.window_id = window_id;
    response.backing.shared_buffer_id = request.shared_buffer_id();
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPISetGlobalCursorTrackingRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetGlobalCursorTrackingRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_global_cursor_tracking_enabled(request.value());
}

void WSClientConnection::handle_request(const WSAPISetWindowOverrideCursorRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetWindowOverrideCursorRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_override_cursor(WSCursor::create(request.cursor()));
}

void WSClientConnection::handle_request(const WSAPISetWindowHasAlphaChannelRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetWindowHasAlphaChannelRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_has_alpha_channel(request.value());

    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidSetWindowHasAlphaChannel;
    response.window_id = window_id;
    response.value = request.value();
    post_message(response);
}

void WSClientConnection::handle_request(const WSWMAPISetActiveWindowRequest& request)
{
    auto* client = WSClientConnection::from_client_id(request.target_client_id());
    if (!client) {
        post_error("WSWMAPISetActiveWindowRequest: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(request.target_window_id());
    if (it == client->m_windows.end()) {
        post_error("WSWMAPISetActiveWindowRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_minimized(false);
    WSWindowManager::the().move_to_front_and_make_active(window);
}

void WSClientConnection::handle_request(const WSWMAPIPopupWindowMenuRequest& request)
{
    auto* client = WSClientConnection::from_client_id(request.target_client_id());
    if (!client) {
        post_error("WSWMAPIPopupWindowMenuRequest: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(request.target_window_id());
    if (it == client->m_windows.end()) {
        post_error("WSWMAPIPopupWindowMenuRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.popup_window_menu(request.position());
}

void WSClientConnection::handle_request(const WSWMAPIStartWindowResizeRequest& request)
{
    auto* client = WSClientConnection::from_client_id(request.target_client_id());
    if (!client) {
        post_error("WSWMAPIStartWindowResizeRequest: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(request.target_window_id());
    if (it == client->m_windows.end()) {
        post_error("WSWMAPIStartWindowResizeRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    // FIXME: We are cheating a bit here by using the current cursor location and hard-coding the left button.
    //        Maybe the client should be allowed to specify what initiated this request?
    WSWindowManager::the().start_window_resize(window, WSScreen::the().cursor_location(), MouseButton::Left);
}

void WSClientConnection::handle_request(const WSWMAPISetWindowMinimizedRequest& request)
{
    auto* client = WSClientConnection::from_client_id(request.target_client_id());
    if (!client) {
        post_error("WSWMAPISetWindowMinimizedRequest: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(request.target_window_id());
    if (it == client->m_windows.end()) {
        post_error("WSWMAPISetWindowMinimizedRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_minimized(request.is_minimized());
}

void WSClientConnection::on_request(const WSAPIClientRequest& request)
{
    switch (request.type()) {
    case WSEvent::APICreateMenubarRequest:
        return handle_request(static_cast<const WSAPICreateMenubarRequest&>(request));
    case WSEvent::APIDestroyMenubarRequest:
        return handle_request(static_cast<const WSAPIDestroyMenubarRequest&>(request));
    case WSEvent::APICreateMenuRequest:
        return handle_request(static_cast<const WSAPICreateMenuRequest&>(request));
    case WSEvent::APIDestroyMenuRequest:
        return handle_request(static_cast<const WSAPIDestroyMenuRequest&>(request));
    case WSEvent::APISetApplicationMenubarRequest:
        return handle_request(static_cast<const WSAPISetApplicationMenubarRequest&>(request));
    case WSEvent::APIAddMenuToMenubarRequest:
        return handle_request(static_cast<const WSAPIAddMenuToMenubarRequest&>(request));
    case WSEvent::APIAddMenuItemRequest:
        return handle_request(static_cast<const WSAPIAddMenuItemRequest&>(request));
    case WSEvent::APIAddMenuSeparatorRequest:
        return handle_request(static_cast<const WSAPIAddMenuSeparatorRequest&>(request));
    case WSEvent::APIUpdateMenuItemRequest:
        return handle_request(static_cast<const WSAPIUpdateMenuItemRequest&>(request));
    case WSEvent::APISetWindowTitleRequest:
        return handle_request(static_cast<const WSAPISetWindowTitleRequest&>(request));
    case WSEvent::APIGetWindowTitleRequest:
        return handle_request(static_cast<const WSAPIGetWindowTitleRequest&>(request));
    case WSEvent::APISetWindowRectRequest:
        return handle_request(static_cast<const WSAPISetWindowRectRequest&>(request));
    case WSEvent::APIGetWindowRectRequest:
        return handle_request(static_cast<const WSAPIGetWindowRectRequest&>(request));
    case WSEvent::APISetWindowIconBitmapRequest:
        return handle_request(static_cast<const WSAPISetWindowIconBitmapRequest&>(request));
    case WSEvent::APISetClipboardContentsRequest:
        return handle_request(static_cast<const WSAPISetClipboardContentsRequest&>(request));
    case WSEvent::APIGetClipboardContentsRequest:
        return handle_request(static_cast<const WSAPIGetClipboardContentsRequest&>(request));
    case WSEvent::APICreateWindowRequest:
        return handle_request(static_cast<const WSAPICreateWindowRequest&>(request));
    case WSEvent::APIDestroyWindowRequest:
        return handle_request(static_cast<const WSAPIDestroyWindowRequest&>(request));
    case WSEvent::APIInvalidateRectRequest:
        return handle_request(static_cast<const WSAPIInvalidateRectRequest&>(request));
    case WSEvent::APIDidFinishPaintingNotification:
        return handle_request(static_cast<const WSAPIDidFinishPaintingNotification&>(request));
    case WSEvent::APIGetWindowBackingStoreRequest:
        return handle_request(static_cast<const WSAPIGetWindowBackingStoreRequest&>(request));
    case WSEvent::APISetGlobalCursorTrackingRequest:
        return handle_request(static_cast<const WSAPISetGlobalCursorTrackingRequest&>(request));
    case WSEvent::APISetWindowOpacityRequest:
        return handle_request(static_cast<const WSAPISetWindowOpacityRequest&>(request));
    case WSEvent::APISetWindowBackingStoreRequest:
        return handle_request(static_cast<const WSAPISetWindowBackingStoreRequest&>(request));
    case WSEvent::APISetWallpaperRequest:
        return handle_request(static_cast<const WSAPISetWallpaperRequest&>(request));
    case WSEvent::APIGetWallpaperRequest:
        return handle_request(static_cast<const WSAPIGetWallpaperRequest&>(request));
    case WSEvent::APISetResolutionRequest:
        return handle_request(static_cast<const WSAPISetResolutionRequest&>(request));
    case WSEvent::APISetWindowOverrideCursorRequest:
        return handle_request(static_cast<const WSAPISetWindowOverrideCursorRequest&>(request));
    case WSEvent::WMAPISetActiveWindowRequest:
        return handle_request(static_cast<const WSWMAPISetActiveWindowRequest&>(request));
    case WSEvent::WMAPISetWindowMinimizedRequest:
        return handle_request(static_cast<const WSWMAPISetWindowMinimizedRequest&>(request));
    case WSEvent::WMAPIStartWindowResizeRequest:
        return handle_request(static_cast<const WSWMAPIStartWindowResizeRequest&>(request));
    case WSEvent::WMAPIPopupWindowMenuRequest:
        return handle_request(static_cast<const WSWMAPIPopupWindowMenuRequest&>(request));
    case WSEvent::APIPopupMenuRequest:
        return handle_request(static_cast<const WSAPIPopupMenuRequest&>(request));
    case WSEvent::APIDismissMenuRequest:
        return handle_request(static_cast<const WSAPIDismissMenuRequest&>(request));
    case WSEvent::APISetWindowHasAlphaChannelRequest:
        return handle_request(static_cast<const WSAPISetWindowHasAlphaChannelRequest&>(request));
    case WSEvent::APIMoveWindowToFrontRequest:
        return handle_request(static_cast<const WSAPIMoveWindowToFrontRequest&>(request));
    case WSEvent::APISetFullscreenRequest:
        return handle_request(static_cast<const WSAPISetFullscreenRequest&>(request));
    default:
        break;
    }
}

bool WSClientConnection::is_showing_modal_window() const
{
    for (auto& it : m_windows) {
        auto& window = *it.value;
        if (window.is_visible() && window.is_modal())
            return true;
    }
    return false;
}
