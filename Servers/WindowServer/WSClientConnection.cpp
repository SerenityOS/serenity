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
#include <sys/uio.h>
#include <unistd.h>

HashMap<int, WSClientConnection*>* s_connections;

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
    return (*it).value;
}

WSClientConnection::WSClientConnection(int fd)
    : m_fd(fd)
{
    static int s_next_client_id = 0;
    m_client_id = ++s_next_client_id;

    if (!s_connections)
        s_connections = new HashMap<int, WSClientConnection*>;
    s_connections->set(m_client_id, this);

    WSAPI_ServerMessage message;
    message.type = WSAPI_ServerMessage::Type::Greeting;
    message.greeting.server_pid = getpid();
    message.greeting.your_client_id = m_client_id;
    message.greeting.screen_rect = WSScreen::the().rect();
    post_message(message);
}

WSClientConnection::~WSClientConnection()
{
    s_connections->remove(m_client_id);
    int rc = close(m_fd);
    ASSERT(rc == 0);
}

void WSClientConnection::post_error(const String& error_message)
{
    dbgprintf("WSClientConnection::post_error: client_id=%d: %s\n", m_client_id, error_message.characters());
    WSAPI_ServerMessage message;
    message.type = WSAPI_ServerMessage::Type::Error;
    ASSERT(error_message.length() < (ssize_t)sizeof(message.text));
    strcpy(message.text, error_message.characters());
    message.text_length = error_message.length();
    post_message(message);
}

void WSClientConnection::post_message(const WSAPI_ServerMessage& message, const ByteBuffer& extra_data)
{
    if (!extra_data.is_empty())
        const_cast<WSAPI_ServerMessage&>(message).extra_size = extra_data.size();

    struct iovec iov[2];
    int iov_count = 1;

    iov[0].iov_base = const_cast<WSAPI_ServerMessage*>(&message);
    iov[0].iov_len = sizeof(message);

    if (!extra_data.is_empty()) {
        iov[1].iov_base = const_cast<u8*>(extra_data.data());
        iov[1].iov_len = extra_data.size();
        ++iov_count;
    }

    int nwritten = writev(m_fd, iov, iov_count);
    if (nwritten < 0) {
        switch (errno) {
        case EPIPE:
            dbgprintf("WSClientConnection::post_message: Disconnected from peer.\n");
            delete_later();
            return;
            break;
        case EAGAIN:
            dbgprintf("WSClientConnection::post_message: Client buffer overflowed.\n");
            did_misbehave();
            return;
            break;
        default:
            perror("WSClientConnection::post_message writev");
            ASSERT_NOT_REACHED();
        }
    }

    ASSERT(nwritten == (int)(sizeof(message) + extra_data.size()));
}

void WSClientConnection::notify_about_new_screen_rect(const Rect& rect)
{
    WSAPI_ServerMessage message;
    message.type = WSAPI_ServerMessage::Type::ScreenRectChanged;
    message.screen.rect = rect;
    post_message(message);
}

void WSClientConnection::event(CEvent& event)
{
    if (static_cast<WSEvent&>(event).is_client_request()) {
        on_request(static_cast<const WSAPIClientRequest&>(event));
        return;
    }

    if (event.type() == WSEvent::WM_ClientDisconnected) {
        int client_id = static_cast<const WSClientDisconnectedNotification&>(event).client_id();
        dbgprintf("WSClientConnection: Client disconnected: %d\n", client_id);
        delete this;
        return;
    }

    CObject::event(event);
}

void WSClientConnection::did_misbehave()
{
    dbgprintf("WSClientConnection{%p} (id=%d, pid=%d) misbehaved, disconnecting.\n", this, m_client_id, m_pid);
    // FIXME: We should make sure we avoid processing any further messages from this client.
    delete_later();
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
    auto menu = make<WSMenu>(this, menu_id, request.text());
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
    WSWindowManager::the().close_menu(menu);
    m_menus.remove(it);
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
    menu.add_item(make<WSMenuItem>(menu, identifier, request.text(), request.shortcut_text(), request.is_enabled(), request.is_checkable(), request.is_checked()));
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
    strncpy(response.text, path.characters(), path.length());
    response.text_length = path.length();
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

void WSClientConnection::handle_request(const WSAPISetWindowIconRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("WSAPISetWindowIconRequest: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    if (request.icon_path().is_empty()) {
        window.set_default_icon();
    } else {
        auto icon = GraphicsBitmap::load_from_file(request.icon_path());
        if (!icon)
            return;
        window.set_icon(request.icon_path(), *icon);
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
    WSClipboard::the().set_data(*shared_buffer, request.size());
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidSetClipboardContents;
    response.clipboard.shared_buffer_id = shared_buffer->shared_buffer_id();
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPIGetClipboardContentsRequest&)
{
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidGetClipboardContents;
    response.clipboard.shared_buffer_id = -1;
    response.clipboard.contents_size = 0;
    if (WSClipboard::the().size()) {
        // FIXME: Optimize case where an app is copy/pasting within itself.
        //        We can just reuse the SharedBuffer then, since it will have the same peer PID.
        //        It would be even nicer if a SharedBuffer could have an arbitrary number of clients..
        RefPtr<SharedBuffer> shared_buffer = SharedBuffer::create(m_pid, WSClipboard::the().size());
        ASSERT(shared_buffer);
        memcpy(shared_buffer->data(), WSClipboard::the().data(), WSClipboard::the().size());
        shared_buffer->seal();
        response.clipboard.shared_buffer_id = shared_buffer->shared_buffer_id();
        response.clipboard.contents_size = WSClipboard::the().size();

        // FIXME: This is a workaround for the fact that SharedBuffers will go away if neither side is retaining them.
        //        After we respond to GetClipboardContents, we have to wait for the client to ref the buffer on his side.
        m_last_sent_clipboard_content = move(shared_buffer);
    }
    post_message(response);
}

void WSClientConnection::handle_request(const WSAPICreateWindowRequest& request)
{
    int window_id = m_next_window_id++;
    auto window = make<WSWindow>(*this, request.window_type(), window_id, request.is_modal(), request.is_resizable(), request.is_fullscreen());
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
    m_windows.remove(it);
    WSAPI_ServerMessage response;
    response.type = WSAPI_ServerMessage::Type::DidDestroyWindow;
    response.window_id = window.window_id();
    post_message(response);
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
    case WSEvent::APISetWindowIconRequest:
        return handle_request(static_cast<const WSAPISetWindowIconRequest&>(request));
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
