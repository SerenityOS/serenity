#include <LibC/SharedBuffer.h>
#include <LibDraw/GraphicsBitmap.h>
#include <SharedBuffer.h>
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
#include <WindowServer/WindowClientEndpoint.h>
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
    : IClientConnection(*this, client_socket, client_id)
{
    if (!s_connections)
        s_connections = new HashMap<int, NonnullRefPtr<WSClientConnection>>;
    s_connections->set(client_id, *this);
}

WSClientConnection::~WSClientConnection()
{
    auto windows = move(m_windows);
}

void WSClientConnection::die()
{
    s_connections->remove(client_id());
}

void WSClientConnection::notify_about_new_screen_rect(const Rect& rect)
{
    post_message(WindowClient::ScreenRectChanged(rect));
}

void WSClientConnection::notify_about_clipboard_contents_changed()
{
    post_message(WindowClient::ClipboardContentsChanged(WSClipboard::the().data_type()));
}

OwnPtr<WindowServer::CreateMenubarResponse> WSClientConnection::handle(const WindowServer::CreateMenubar&)
{
    int menubar_id = m_next_menubar_id++;
    auto menubar = make<WSMenuBar>(*this, menubar_id);
    m_menubars.set(menubar_id, move(menubar));
    return make<WindowServer::CreateMenubarResponse>(menubar_id);
}

OwnPtr<WindowServer::DestroyMenubarResponse> WSClientConnection::handle(const WindowServer::DestroyMenubar& message)
{
    int menubar_id = message.menubar_id();
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        did_misbehave("DestroyMenubar: Bad menubar ID");
        return nullptr;
    }
    auto& menubar = *(*it).value;
    WSWindowManager::the().close_menubar(menubar);
    m_menubars.remove(it);
    return make<WindowServer::DestroyMenubarResponse>();
}

OwnPtr<WindowServer::CreateMenuResponse> WSClientConnection::handle(const WindowServer::CreateMenu& message)
{
    int menu_id = m_next_menu_id++;
    auto menu = WSMenu::construct(this, menu_id, message.menu_title());
    m_menus.set(menu_id, move(menu));
    return make<WindowServer::CreateMenuResponse>(menu_id);
}

OwnPtr<WindowServer::DestroyMenuResponse> WSClientConnection::handle(const WindowServer::DestroyMenu& message)
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
    return make<WindowServer::DestroyMenuResponse>();
}

OwnPtr<WindowServer::SetApplicationMenubarResponse> WSClientConnection::handle(const WindowServer::SetApplicationMenubar& message)
{
    int menubar_id = message.menubar_id();
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        did_misbehave("SetApplicationMenubar: Bad menubar ID");
        return nullptr;
    }
    auto& menubar = *(*it).value;
    m_app_menubar = menubar.make_weak_ptr();
    WSWindowManager::the().notify_client_changed_app_menubar(*this);
    return make<WindowServer::SetApplicationMenubarResponse>();
}

OwnPtr<WindowServer::AddMenuToMenubarResponse> WSClientConnection::handle(const WindowServer::AddMenuToMenubar& message)
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
    return make<WindowServer::AddMenuToMenubarResponse>();
}

OwnPtr<WindowServer::AddMenuItemResponse> WSClientConnection::handle(const WindowServer::AddMenuItem& message)
{
    int menu_id = message.menu_id();
    unsigned identifier = message.identifier();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        dbg() << "AddMenuItem: Bad menu ID: " << menu_id;
        return nullptr;
    }
    auto& menu = *(*it).value;
    auto menu_item = make<WSMenuItem>(menu, identifier, message.text(), message.shortcut(), message.enabled(), message.checkable(), message.checked());
    if (message.icon_buffer_id() != -1) {
        auto icon_buffer = SharedBuffer::create_from_shared_buffer_id(message.icon_buffer_id());
        if (!icon_buffer)
            return nullptr;
        // FIXME: Verify that the icon buffer can accomodate a 16x16 bitmap view.
        auto shared_icon = GraphicsBitmap::create_with_shared_buffer(GraphicsBitmap::Format::RGBA32, icon_buffer.release_nonnull(), { 16, 16 });
        menu_item->set_icon(shared_icon);
    }
    menu_item->set_submenu_id(message.submenu_id());
    menu.add_item(move(menu_item));
    return make<WindowServer::AddMenuItemResponse>();
}

OwnPtr<WindowServer::PopupMenuResponse> WSClientConnection::handle(const WindowServer::PopupMenu& message)
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
    return make<WindowServer::PopupMenuResponse>();
}

OwnPtr<WindowServer::DismissMenuResponse> WSClientConnection::handle(const WindowServer::DismissMenu& message)
{
    int menu_id = message.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("DismissMenu: Bad menu ID");
        return nullptr;
    }
    auto& menu = *(*it).value;
    menu.close();
    return make<WindowServer::DismissMenuResponse>();
}

OwnPtr<WindowServer::UpdateMenuItemResponse> WSClientConnection::handle(const WindowServer::UpdateMenuItem& message)
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
    if (message.checkable())
        menu_item->set_checked(message.checked());
    return make<WindowServer::UpdateMenuItemResponse>();
}

OwnPtr<WindowServer::AddMenuSeparatorResponse> WSClientConnection::handle(const WindowServer::AddMenuSeparator& message)
{
    int menu_id = message.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("AddMenuSeparator: Bad menu ID");
        return nullptr;
    }
    auto& menu = *(*it).value;
    menu.add_item(make<WSMenuItem>(menu, WSMenuItem::Separator));
    return make<WindowServer::AddMenuSeparatorResponse>();
}

OwnPtr<WindowServer::MoveWindowToFrontResponse> WSClientConnection::handle(const WindowServer::MoveWindowToFront& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("MoveWindowToFront: Bad window ID");
        return nullptr;
    }
    WSWindowManager::the().move_to_front_and_make_active(*(*it).value);
    return make<WindowServer::MoveWindowToFrontResponse>();
}

OwnPtr<WindowServer::SetFullscreenResponse> WSClientConnection::handle(const WindowServer::SetFullscreen& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetFullscreen: Bad window ID");
        return nullptr;
    }
    it->value->set_fullscreen(message.fullscreen());
    return make<WindowServer::SetFullscreenResponse>();
}

OwnPtr<WindowServer::SetWindowOpacityResponse> WSClientConnection::handle(const WindowServer::SetWindowOpacity& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowOpacity: Bad window ID");
        return nullptr;
    }
    it->value->set_opacity(message.opacity());
    return make<WindowServer::SetWindowOpacityResponse>();
}

void WSClientConnection::handle(const WindowServer::AsyncSetWallpaper& message)
{
    WSCompositor::the().set_wallpaper(message.path(), [&](bool success) {
        post_message(WindowClient::AsyncSetWallpaperFinished(success));
    });
}

OwnPtr<WindowServer::GetWallpaperResponse> WSClientConnection::handle(const WindowServer::GetWallpaper&)
{
    return make<WindowServer::GetWallpaperResponse>(WSCompositor::the().wallpaper_path());
}

OwnPtr<WindowServer::SetResolutionResponse> WSClientConnection::handle(const WindowServer::SetResolution& message)
{
    WSWindowManager::the().set_resolution(message.resolution().width(), message.resolution().height());
    return make<WindowServer::SetResolutionResponse>();
}

OwnPtr<WindowServer::SetWindowTitleResponse> WSClientConnection::handle(const WindowServer::SetWindowTitle& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowTitle: Bad window ID");
        return nullptr;
    }
    it->value->set_title(message.title());
    return make<WindowServer::SetWindowTitleResponse>();
}

OwnPtr<WindowServer::GetWindowTitleResponse> WSClientConnection::handle(const WindowServer::GetWindowTitle& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("GetWindowTitle: Bad window ID");
        return nullptr;
    }
    return make<WindowServer::GetWindowTitleResponse>(it->value->title());
}

OwnPtr<WindowServer::SetWindowIconBitmapResponse> WSClientConnection::handle(const WindowServer::SetWindowIconBitmap& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowIconBitmap: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;

    auto icon_buffer = SharedBuffer::create_from_shared_buffer_id(message.icon_buffer_id());

    if (!icon_buffer) {
        window.set_default_icon();
    } else {
        window.set_icon(GraphicsBitmap::create_with_shared_buffer(GraphicsBitmap::Format::RGBA32, *icon_buffer, message.icon_size()));
    }

    window.frame().invalidate_title_bar();
    WSWindowManager::the().tell_wm_listeners_window_icon_changed(window);
    return make<WindowServer::SetWindowIconBitmapResponse>();
}

OwnPtr<WindowServer::SetWindowRectResponse> WSClientConnection::handle(const WindowServer::SetWindowRect& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowRect: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    if (window.is_fullscreen()) {
        dbg() << "WSClientConnection: Ignoring SetWindowRect request for fullscreen window";
        return nullptr;
    }
    window.set_rect(message.rect());
    window.request_update(message.rect());
    return make<WindowServer::SetWindowRectResponse>();
}

OwnPtr<WindowServer::GetWindowRectResponse> WSClientConnection::handle(const WindowServer::GetWindowRect& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowRect: Bad window ID");
        return nullptr;
    }
    return make<WindowServer::GetWindowRectResponse>(it->value->rect());
}

OwnPtr<WindowServer::SetClipboardContentsResponse> WSClientConnection::handle(const WindowServer::SetClipboardContents& message)
{
    auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(message.shared_buffer_id());
    if (!shared_buffer) {
        did_misbehave("SetClipboardContents: Bad shared buffer ID");
        return nullptr;
    }
    WSClipboard::the().set_data(*shared_buffer, message.content_size(), message.content_type());
    return make<WindowServer::SetClipboardContentsResponse>();
}

OwnPtr<WindowServer::GetClipboardContentsResponse> WSClientConnection::handle(const WindowServer::GetClipboardContents&)
{
    auto& clipboard = WSClipboard::the();

    i32 shared_buffer_id = -1;
    if (clipboard.size()) {
        // FIXME: Optimize case where an app is copy/pasting within itself.
        //        We can just reuse the SharedBuffer then, since it will have the same peer PID.
        //        It would be even nicer if a SharedBuffer could have an arbitrary number of clients..
        RefPtr<SharedBuffer> shared_buffer = SharedBuffer::create_with_size(clipboard.size());
        ASSERT(shared_buffer);
        memcpy(shared_buffer->data(), clipboard.data(), clipboard.size());
        shared_buffer->seal();
        shared_buffer->share_with(client_pid());
        shared_buffer_id = shared_buffer->shared_buffer_id();

        // FIXME: This is a workaround for the fact that SharedBuffers will go away if neither side is retaining them.
        //        After we respond to GetClipboardContents, we have to wait for the client to ref the buffer on his side.
        m_last_sent_clipboard_content = move(shared_buffer);
    }
    return make<WindowServer::GetClipboardContentsResponse>(shared_buffer_id, clipboard.size(), clipboard.data_type());
}

OwnPtr<WindowServer::CreateWindowResponse> WSClientConnection::handle(const WindowServer::CreateWindow& message)
{
    int window_id = m_next_window_id++;
    auto window = WSWindow::construct(*this, (WSWindowType)message.type(), window_id, message.modal(), message.resizable(), message.fullscreen());
    window->set_background_color(message.background_color());
    window->set_has_alpha_channel(message.has_alpha_channel());
    window->set_title(message.title());
    if (!message.fullscreen())
        window->set_rect(message.rect());
    window->set_show_titlebar(message.show_titlebar());
    window->set_opacity(message.opacity());
    window->set_size_increment(message.size_increment());
    window->set_base_size(message.base_size());
    window->invalidate();
    if (window->type() == WSWindowType::MenuApplet)
        WSWindowManager::the().menu_manager().add_applet(*window);
    m_windows.set(window_id, move(window));
    return make<WindowServer::CreateWindowResponse>(window_id);
}

OwnPtr<WindowServer::DestroyWindowResponse> WSClientConnection::handle(const WindowServer::DestroyWindow& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("DestroyWindow: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;

    if (window.type() == WSWindowType::MenuApplet)
        WSWindowManager::the().menu_manager().remove_applet(window);

    WSWindowManager::the().invalidate(window);
    remove_child(window);
    ASSERT(it->value.ptr() == &window);
    m_windows.remove(message.window_id());

    return make<WindowServer::DestroyWindowResponse>();
}

void WSClientConnection::post_paint_message(WSWindow& window)
{
    auto rect_set = window.take_pending_paint_rects();
    if (window.is_minimized())
        return;

    Vector<Rect> rects;
    rects.ensure_capacity(rect_set.size());
    for (auto& r : rect_set.rects()) {
        rects.append(r);
    }
    post_message(WindowClient::Paint(window.window_id(), window.size(), rects));
}

void WSClientConnection::handle(const WindowServer::InvalidateRect& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("InvalidateRect: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    for (int i = 0; i < message.rects().size(); ++i)
        window.request_update(message.rects()[i].intersected({ {}, window.size() }));
}

void WSClientConnection::handle(const WindowServer::DidFinishPainting& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("DidFinishPainting: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    for (auto& rect : message.rects())
        WSWindowManager::the().invalidate(window, rect);

    WSWindowSwitcher::the().refresh_if_needed();
}

OwnPtr<WindowServer::SetWindowBackingStoreResponse> WSClientConnection::handle(const WindowServer::SetWindowBackingStore& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowBackingStore: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    if (window.last_backing_store() && window.last_backing_store()->shared_buffer_id() == message.shared_buffer_id()) {
        window.swap_backing_stores();
    } else {
        auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(message.shared_buffer_id());
        if (!shared_buffer)
            return make<WindowServer::SetWindowBackingStoreResponse>();
        auto backing_store = GraphicsBitmap::create_with_shared_buffer(
            message.has_alpha_channel() ? GraphicsBitmap::Format::RGBA32 : GraphicsBitmap::Format::RGB32,
            *shared_buffer,
            message.size());
        window.set_backing_store(move(backing_store));
    }

    if (message.flush_immediately())
        window.invalidate();

    return make<WindowServer::SetWindowBackingStoreResponse>();
}

OwnPtr<WindowServer::SetGlobalCursorTrackingResponse> WSClientConnection::handle(const WindowServer::SetGlobalCursorTracking& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetGlobalCursorTracking: Bad window ID");
        return nullptr;
    }
    it->value->set_global_cursor_tracking_enabled(message.enabled());
    return make<WindowServer::SetGlobalCursorTrackingResponse>();
}

OwnPtr<WindowServer::SetWindowOverrideCursorResponse> WSClientConnection::handle(const WindowServer::SetWindowOverrideCursor& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowOverrideCursor: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    window.set_override_cursor(WSCursor::create((WSStandardCursor)message.cursor_type()));
    return make<WindowServer::SetWindowOverrideCursorResponse>();
}

OwnPtr<WindowServer::SetWindowHasAlphaChannelResponse> WSClientConnection::handle(const WindowServer::SetWindowHasAlphaChannel& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowHasAlphaChannel: Bad window ID");
        return nullptr;
    }
    it->value->set_has_alpha_channel(message.has_alpha_channel());
    return make<WindowServer::SetWindowHasAlphaChannelResponse>();
}

void WSClientConnection::handle(const WindowServer::WM_SetActiveWindow& message)
{
    auto* client = WSClientConnection::from_client_id(message.client_id());
    if (!client) {
        did_misbehave("WM_SetActiveWindow: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(message.window_id());
    if (it == client->m_windows.end()) {
        did_misbehave("WM_SetActiveWindow: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_minimized(false);
    WSWindowManager::the().move_to_front_and_make_active(window);
}

void WSClientConnection::handle(const WindowServer::WM_PopupWindowMenu& message)
{
    auto* client = WSClientConnection::from_client_id(message.client_id());
    if (!client) {
        did_misbehave("WM_PopupWindowMenu: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(message.window_id());
    if (it == client->m_windows.end()) {
        did_misbehave("WM_PopupWindowMenu: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.popup_window_menu(message.screen_position());
}

void WSClientConnection::handle(const WindowServer::WM_StartWindowResize& request)
{
    auto* client = WSClientConnection::from_client_id(request.client_id());
    if (!client) {
        did_misbehave("WM_StartWindowResize: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(request.window_id());
    if (it == client->m_windows.end()) {
        did_misbehave("WM_StartWindowResize: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    // FIXME: We are cheating a bit here by using the current cursor location and hard-coding the left button.
    //        Maybe the client should be allowed to specify what initiated this request?
    WSWindowManager::the().start_window_resize(window, WSScreen::the().cursor_location(), MouseButton::Left);
}

void WSClientConnection::handle(const WindowServer::WM_SetWindowMinimized& message)
{
    auto* client = WSClientConnection::from_client_id(message.client_id());
    if (!client) {
        did_misbehave("WM_SetWindowMinimized: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(message.window_id());
    if (it == client->m_windows.end()) {
        did_misbehave("WM_SetWindowMinimized: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_minimized(message.minimized());
}

OwnPtr<WindowServer::GreetResponse> WSClientConnection::handle(const WindowServer::Greet&)
{
    return make<WindowServer::GreetResponse>(client_id(), WSScreen::the().rect());
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

void WSClientConnection::handle(const WindowServer::WM_SetWindowTaskbarRect& message)
{
    auto* client = WSClientConnection::from_client_id(message.client_id());
    if (!client) {
        did_misbehave("WM_SetWindowTaskbarRect: Bad client ID");
        return;
    }
    auto it = client->m_windows.find(message.window_id());
    if (it == client->m_windows.end()) {
        did_misbehave("WM_SetWindowTaskbarRect: Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_taskbar_rect(message.rect());
}

OwnPtr<WindowServer::StartDragResponse> WSClientConnection::handle(const WindowServer::StartDrag& message)
{
    auto& wm = WSWindowManager::the();
    if (wm.dnd_client())
        return make<WindowServer::StartDragResponse>(false);

    RefPtr<GraphicsBitmap> bitmap;
    if (message.bitmap_id() != -1) {
        auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(message.bitmap_id());
        ssize_t size_in_bytes = message.bitmap_size().area() * sizeof(RGBA32);
        if (size_in_bytes > shared_buffer->size()) {
            did_misbehave("SetAppletBackingStore: Shared buffer is too small for applet size");
            return nullptr;
        }
        bitmap = GraphicsBitmap::create_with_shared_buffer(GraphicsBitmap::Format::RGBA32, *shared_buffer, message.bitmap_size());
    }

    wm.start_dnd_drag(*this, message.text(), bitmap, message.data_type(), message.data());
    return make<WindowServer::StartDragResponse>(true);
}
