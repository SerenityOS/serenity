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

#include <AK/Badge.h>
#include <AK/SharedBuffer.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/SystemTheme.h>
#include <WindowServer/AppletManager.h>
#include <WindowServer/ClientConnection.h>
#include <WindowServer/Clipboard.h>
#include <WindowServer/Compositor.h>
#include <WindowServer/EventLoop.h>
#include <WindowServer/Menu.h>
#include <WindowServer/MenuBar.h>
#include <WindowServer/MenuItem.h>
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

ClientConnection::ClientConnection(Core::LocalSocket& client_socket, int client_id)
    : IPC::ClientConnection<WindowServerEndpoint>(*this, client_socket, client_id)
{
    if (!s_connections)
        s_connections = new HashMap<int, NonnullRefPtr<ClientConnection>>;
    s_connections->set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
    MenuManager::the().close_all_menus_from_client({}, *this);
    auto windows = move(m_windows);
    for (auto& window : windows)
        window.value->detach_client({});
}

void ClientConnection::die()
{
    deferred_invoke([this](auto&) {
        s_connections->remove(client_id());
    });
}

void ClientConnection::notify_about_new_screen_rect(const Gfx::Rect& rect)
{
    post_message(Messages::WindowClient::ScreenRectChanged(rect));
}

void ClientConnection::notify_about_clipboard_contents_changed()
{
    post_message(Messages::WindowClient::ClipboardContentsChanged(Clipboard::the().data_type()));
}

OwnPtr<Messages::WindowServer::CreateMenubarResponse> ClientConnection::handle(const Messages::WindowServer::CreateMenubar&)
{
    int menubar_id = m_next_menubar_id++;
    auto menubar = make<MenuBar>(*this, menubar_id);
    m_menubars.set(menubar_id, move(menubar));
    return make<Messages::WindowServer::CreateMenubarResponse>(menubar_id);
}

OwnPtr<Messages::WindowServer::DestroyMenubarResponse> ClientConnection::handle(const Messages::WindowServer::DestroyMenubar& message)
{
    int menubar_id = message.menubar_id();
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        did_misbehave("DestroyMenubar: Bad menubar ID");
        return nullptr;
    }
    auto& menubar = *(*it).value;
    MenuManager::the().close_menubar(menubar);
    m_menubars.remove(it);
    return make<Messages::WindowServer::DestroyMenubarResponse>();
}

OwnPtr<Messages::WindowServer::CreateMenuResponse> ClientConnection::handle(const Messages::WindowServer::CreateMenu& message)
{
    int menu_id = m_next_menu_id++;
    auto menu = Menu::construct(this, menu_id, message.menu_title());
    m_menus.set(menu_id, move(menu));
    return make<Messages::WindowServer::CreateMenuResponse>(menu_id);
}

OwnPtr<Messages::WindowServer::DestroyMenuResponse> ClientConnection::handle(const Messages::WindowServer::DestroyMenu& message)
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
    return make<Messages::WindowServer::DestroyMenuResponse>();
}

OwnPtr<Messages::WindowServer::SetApplicationMenubarResponse> ClientConnection::handle(const Messages::WindowServer::SetApplicationMenubar& message)
{
    int menubar_id = message.menubar_id();
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        did_misbehave("SetApplicationMenubar: Bad menubar ID");
        return nullptr;
    }
    auto& menubar = *(*it).value;
    m_app_menubar = menubar.make_weak_ptr();
    WindowManager::the().notify_client_changed_app_menubar(*this);
    return make<Messages::WindowServer::SetApplicationMenubarResponse>();
}

OwnPtr<Messages::WindowServer::AddMenuToMenubarResponse> ClientConnection::handle(const Messages::WindowServer::AddMenuToMenubar& message)
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
    return make<Messages::WindowServer::AddMenuToMenubarResponse>();
}

OwnPtr<Messages::WindowServer::AddMenuItemResponse> ClientConnection::handle(const Messages::WindowServer::AddMenuItem& message)
{
    int menu_id = message.menu_id();
    unsigned identifier = message.identifier();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        dbg() << "AddMenuItem: Bad menu ID: " << menu_id;
        return nullptr;
    }
    auto& menu = *(*it).value;
    auto menu_item = make<MenuItem>(menu, identifier, message.text(), message.shortcut(), message.enabled(), message.checkable(), message.checked());
    if (message.icon_buffer_id() != -1) {
        auto icon_buffer = SharedBuffer::create_from_shared_buffer_id(message.icon_buffer_id());
        if (!icon_buffer)
            return nullptr;
        // FIXME: Verify that the icon buffer can accomodate a 16x16 bitmap view.
        auto shared_icon = Gfx::Bitmap::create_with_shared_buffer(Gfx::BitmapFormat::RGBA32, icon_buffer.release_nonnull(), { 16, 16 });
        menu_item->set_icon(shared_icon);
    }
    menu_item->set_submenu_id(message.submenu_id());
    menu_item->set_exclusive(message.exclusive());
    menu.add_item(move(menu_item));
    return make<Messages::WindowServer::AddMenuItemResponse>();
}

OwnPtr<Messages::WindowServer::PopupMenuResponse> ClientConnection::handle(const Messages::WindowServer::PopupMenu& message)
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
    return make<Messages::WindowServer::PopupMenuResponse>();
}

OwnPtr<Messages::WindowServer::DismissMenuResponse> ClientConnection::handle(const Messages::WindowServer::DismissMenu& message)
{
    int menu_id = message.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("DismissMenu: Bad menu ID");
        return nullptr;
    }
    auto& menu = *(*it).value;
    menu.close();
    return make<Messages::WindowServer::DismissMenuResponse>();
}

OwnPtr<Messages::WindowServer::UpdateMenuItemResponse> ClientConnection::handle(const Messages::WindowServer::UpdateMenuItem& message)
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
    return make<Messages::WindowServer::UpdateMenuItemResponse>();
}

OwnPtr<Messages::WindowServer::AddMenuSeparatorResponse> ClientConnection::handle(const Messages::WindowServer::AddMenuSeparator& message)
{
    int menu_id = message.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        did_misbehave("AddMenuSeparator: Bad menu ID");
        return nullptr;
    }
    auto& menu = *(*it).value;
    menu.add_item(make<MenuItem>(menu, MenuItem::Separator));
    return make<Messages::WindowServer::AddMenuSeparatorResponse>();
}

OwnPtr<Messages::WindowServer::MoveWindowToFrontResponse> ClientConnection::handle(const Messages::WindowServer::MoveWindowToFront& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("MoveWindowToFront: Bad window ID");
        return nullptr;
    }
    WindowManager::the().move_to_front_and_make_active(*(*it).value);
    return make<Messages::WindowServer::MoveWindowToFrontResponse>();
}

OwnPtr<Messages::WindowServer::SetFullscreenResponse> ClientConnection::handle(const Messages::WindowServer::SetFullscreen& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetFullscreen: Bad window ID");
        return nullptr;
    }
    it->value->set_fullscreen(message.fullscreen());
    return make<Messages::WindowServer::SetFullscreenResponse>();
}

OwnPtr<Messages::WindowServer::SetWindowOpacityResponse> ClientConnection::handle(const Messages::WindowServer::SetWindowOpacity& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowOpacity: Bad window ID");
        return nullptr;
    }
    it->value->set_opacity(message.opacity());
    return make<Messages::WindowServer::SetWindowOpacityResponse>();
}

void ClientConnection::handle(const Messages::WindowServer::AsyncSetWallpaper& message)
{
    Compositor::the().set_wallpaper(message.path(), [&](bool success) {
        post_message(Messages::WindowClient::AsyncSetWallpaperFinished(success));
    });
}

OwnPtr<Messages::WindowServer::GetWallpaperResponse> ClientConnection::handle(const Messages::WindowServer::GetWallpaper&)
{
    return make<Messages::WindowServer::GetWallpaperResponse>(Compositor::the().wallpaper_path());
}

OwnPtr<Messages::WindowServer::SetResolutionResponse> ClientConnection::handle(const Messages::WindowServer::SetResolution& message)
{
    WindowManager::the().set_resolution(message.resolution().width(), message.resolution().height());
    return make<Messages::WindowServer::SetResolutionResponse>();
}

OwnPtr<Messages::WindowServer::SetWindowTitleResponse> ClientConnection::handle(const Messages::WindowServer::SetWindowTitle& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowTitle: Bad window ID");
        return nullptr;
    }
    it->value->set_title(message.title());
    return make<Messages::WindowServer::SetWindowTitleResponse>();
}

OwnPtr<Messages::WindowServer::GetWindowTitleResponse> ClientConnection::handle(const Messages::WindowServer::GetWindowTitle& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("GetWindowTitle: Bad window ID");
        return nullptr;
    }
    return make<Messages::WindowServer::GetWindowTitleResponse>(it->value->title());
}

OwnPtr<Messages::WindowServer::SetWindowIconBitmapResponse> ClientConnection::handle(const Messages::WindowServer::SetWindowIconBitmap& message)
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
        window.set_icon(Gfx::Bitmap::create_with_shared_buffer(Gfx::BitmapFormat::RGBA32, *icon_buffer, message.icon_size()));
    }

    window.frame().invalidate_title_bar();
    WindowManager::the().tell_wm_listeners_window_icon_changed(window);
    return make<Messages::WindowServer::SetWindowIconBitmapResponse>();
}

OwnPtr<Messages::WindowServer::SetWindowRectResponse> ClientConnection::handle(const Messages::WindowServer::SetWindowRect& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetWindowRect: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    if (window.is_fullscreen()) {
        dbg() << "ClientConnection: Ignoring SetWindowRect request for fullscreen window";
        return nullptr;
    }
    window.set_rect(message.rect());
    window.request_update(message.rect());
    return make<Messages::WindowServer::SetWindowRectResponse>();
}

OwnPtr<Messages::WindowServer::GetWindowRectResponse> ClientConnection::handle(const Messages::WindowServer::GetWindowRect& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("GetWindowRect: Bad window ID");
        return nullptr;
    }
    return make<Messages::WindowServer::GetWindowRectResponse>(it->value->rect());
}

OwnPtr<Messages::WindowServer::SetClipboardContentsResponse> ClientConnection::handle(const Messages::WindowServer::SetClipboardContents& message)
{
    auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(message.shared_buffer_id());
    if (!shared_buffer) {
        did_misbehave("SetClipboardContents: Bad shared buffer ID");
        return nullptr;
    }
    Clipboard::the().set_data(*shared_buffer, message.content_size(), message.content_type());
    return make<Messages::WindowServer::SetClipboardContentsResponse>();
}

OwnPtr<Messages::WindowServer::GetClipboardContentsResponse> ClientConnection::handle(const Messages::WindowServer::GetClipboardContents&)
{
    auto& clipboard = Clipboard::the();

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
    return make<Messages::WindowServer::GetClipboardContentsResponse>(shared_buffer_id, clipboard.size(), clipboard.data_type());
}

OwnPtr<Messages::WindowServer::CreateWindowResponse> ClientConnection::handle(const Messages::WindowServer::CreateWindow& message)
{
    int window_id = m_next_window_id++;
    auto window = Window::construct(*this, (WindowType)message.type(), window_id, message.modal(), message.minimizable(), message.resizable(), message.fullscreen());
    window->set_has_alpha_channel(message.has_alpha_channel());
    window->set_title(message.title());
    if (!message.fullscreen())
        window->set_rect(message.rect());
    window->set_show_titlebar(message.show_titlebar());
    window->set_opacity(message.opacity());
    window->set_size_increment(message.size_increment());
    window->set_base_size(message.base_size());
    window->invalidate();
    if (window->type() == WindowType::MenuApplet)
        AppletManager::the().add_applet(*window);
    m_windows.set(window_id, move(window));
    return make<Messages::WindowServer::CreateWindowResponse>(window_id);
}

OwnPtr<Messages::WindowServer::DestroyWindowResponse> ClientConnection::handle(const Messages::WindowServer::DestroyWindow& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("DestroyWindow: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;

    if (window.type() == WindowType::MenuApplet)
        AppletManager::the().remove_applet(window);

    WindowManager::the().invalidate(window);
    remove_child(window);
    ASSERT(it->value.ptr() == &window);
    m_windows.remove(message.window_id());

    return make<Messages::WindowServer::DestroyWindowResponse>();
}

void ClientConnection::post_paint_message(Window& window)
{
    auto rect_set = window.take_pending_paint_rects();
    if (window.is_minimized() || window.is_occluded())
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
    for (int i = 0; i < message.rects().size(); ++i)
        window.request_update(message.rects()[i].intersected({ {}, window.size() }));
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
        WindowManager::the().invalidate(window, rect);

    WindowSwitcher::the().refresh_if_needed();
}

OwnPtr<Messages::WindowServer::SetWindowBackingStoreResponse> ClientConnection::handle(const Messages::WindowServer::SetWindowBackingStore& message)
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
            return make<Messages::WindowServer::SetWindowBackingStoreResponse>();
        auto backing_store = Gfx::Bitmap::create_with_shared_buffer(
            message.has_alpha_channel() ? Gfx::BitmapFormat::RGBA32 : Gfx::BitmapFormat::RGB32,
            *shared_buffer,
            message.size());
        window.set_backing_store(move(backing_store));
    }

    if (message.flush_immediately())
        window.invalidate();

    return make<Messages::WindowServer::SetWindowBackingStoreResponse>();
}

OwnPtr<Messages::WindowServer::SetGlobalCursorTrackingResponse> ClientConnection::handle(const Messages::WindowServer::SetGlobalCursorTracking& message)
{
    int window_id = message.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        did_misbehave("SetGlobalCursorTracking: Bad window ID");
        return nullptr;
    }
    it->value->set_global_cursor_tracking_enabled(message.enabled());
    return make<Messages::WindowServer::SetGlobalCursorTrackingResponse>();
}

OwnPtr<Messages::WindowServer::SetWindowOverrideCursorResponse> ClientConnection::handle(const Messages::WindowServer::SetWindowOverrideCursor& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowOverrideCursor: Bad window ID");
        return nullptr;
    }
    auto& window = *(*it).value;
    window.set_override_cursor(Cursor::create((StandardCursor)message.cursor_type()));
    return make<Messages::WindowServer::SetWindowOverrideCursorResponse>();
}

OwnPtr<Messages::WindowServer::SetWindowHasAlphaChannelResponse> ClientConnection::handle(const Messages::WindowServer::SetWindowHasAlphaChannel& message)
{
    auto it = m_windows.find(message.window_id());
    if (it == m_windows.end()) {
        did_misbehave("SetWindowHasAlphaChannel: Bad window ID");
        return nullptr;
    }
    it->value->set_has_alpha_channel(message.has_alpha_channel());
    return make<Messages::WindowServer::SetWindowHasAlphaChannelResponse>();
}

void ClientConnection::handle(const Messages::WindowServer::WM_SetActiveWindow& message)
{
    auto* client = ClientConnection::from_client_id(message.client_id());
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
    WindowManager::the().move_to_front_and_make_active(window);
}

void ClientConnection::handle(const Messages::WindowServer::WM_PopupWindowMenu& message)
{
    auto* client = ClientConnection::from_client_id(message.client_id());
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

void ClientConnection::handle(const Messages::WindowServer::WM_StartWindowResize& request)
{
    auto* client = ClientConnection::from_client_id(request.client_id());
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
    WindowManager::the().start_window_resize(window, Screen::the().cursor_location(), MouseButton::Left);
}

void ClientConnection::handle(const Messages::WindowServer::WM_SetWindowMinimized& message)
{
    auto* client = ClientConnection::from_client_id(message.client_id());
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

OwnPtr<Messages::WindowServer::GreetResponse> ClientConnection::handle(const Messages::WindowServer::Greet&)
{
    return make<Messages::WindowServer::GreetResponse>(client_id(), Screen::the().rect(), Gfx::current_system_theme_buffer_id());
}

bool ClientConnection::is_showing_modal_window() const
{
    for (auto& it : m_windows) {
        auto& window = *it.value;
        if (window.is_visible() && window.is_modal())
            return true;
    }
    return false;
}

void ClientConnection::handle(const Messages::WindowServer::WM_SetWindowTaskbarRect& message)
{
    auto* client = ClientConnection::from_client_id(message.client_id());
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

OwnPtr<Messages::WindowServer::StartDragResponse> ClientConnection::handle(const Messages::WindowServer::StartDrag& message)
{
    auto& wm = WindowManager::the();
    if (wm.dnd_client())
        return make<Messages::WindowServer::StartDragResponse>(false);

    RefPtr<Gfx::Bitmap> bitmap;
    if (message.bitmap_id() != -1) {
        auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(message.bitmap_id());
        ssize_t size_in_bytes = message.bitmap_size().area() * sizeof(Gfx::RGBA32);
        if (size_in_bytes > shared_buffer->size()) {
            did_misbehave("SetAppletBackingStore: Shared buffer is too small for applet size");
            return nullptr;
        }
        bitmap = Gfx::Bitmap::create_with_shared_buffer(Gfx::BitmapFormat::RGBA32, *shared_buffer, message.bitmap_size());
    }

    wm.start_dnd_drag(*this, message.text(), bitmap, message.data_type(), message.data());
    return make<Messages::WindowServer::StartDragResponse>(true);
}

OwnPtr<Messages::WindowServer::SetSystemMenuResponse> ClientConnection::handle(const Messages::WindowServer::SetSystemMenu& message)
{
    auto it = m_menus.find(message.menu_id());
    if (it == m_menus.end()) {
        did_misbehave("SetSystemMenu called with invalid menu ID");
        return nullptr;
    }

    auto& menu = it->value;
    MenuManager::the().set_system_menu(menu);
    return make<Messages::WindowServer::SetSystemMenuResponse>();
}

OwnPtr<Messages::WindowServer::SetSystemThemeResponse> ClientConnection::handle(const Messages::WindowServer::SetSystemTheme& message)
{
    bool success = WindowManager::the().update_theme(message.theme_path(), message.theme_name());
    return make<Messages::WindowServer::SetSystemThemeResponse>(success);
}

void ClientConnection::boost()
{
    if (set_process_boost(client_pid(), 10) < 0)
        perror("boost: set_process_boost");
}

void ClientConnection::deboost()
{
    if (set_process_boost(client_pid(), 0) < 0)
        perror("deboost: set_process_boost");
}

}
