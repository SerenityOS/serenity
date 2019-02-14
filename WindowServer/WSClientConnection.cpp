#include <WindowServer/WSClientConnection.h>
#include <WindowServer/WSMessageLoop.h>
#include <WindowServer/WSMenuBar.h>
#include <WindowServer/WSMenu.h>
#include <WindowServer/WSMenuItem.h>
#include <WindowServer/WSWindow.h>
#include <WindowServer/WSWindowManager.h>
#include <Kernel/GUITypes.h>
#include <Kernel/MemoryManager.h>

Lockable<HashMap<int, WSClientConnection*>>* s_connections;


WSClientConnection* WSClientConnection::from_client_id(int client_id)
{
    if (!s_connections)
        return nullptr;
    LOCKER(s_connections->lock());
    auto it = s_connections->resource().find(client_id);
    if (it == s_connections->resource().end())
        return nullptr;
    return (*it).value;
}

WSClientConnection* WSClientConnection::ensure_for_client_id(int client_id)
{
    if (auto* client = from_client_id(client_id))
        return client;
    return new WSClientConnection(client_id);
}

WSClientConnection::WSClientConnection(int client_id)
    : m_client_id(client_id)
{
    if (!s_connections)
        s_connections = new Lockable<HashMap<int, WSClientConnection*>>;
    LOCKER(s_connections->lock());
    s_connections->resource().set(client_id, this);

    m_process = ((Process*)m_client_id)->make_weak_ptr();
}

WSClientConnection::~WSClientConnection()
{
    LOCKER(s_connections->lock());
    s_connections->resource().remove(m_client_id);
}

void WSClientConnection::post_error(const String& error_message)
{
    dbgprintf("WSClientConnection::post_error: client_id=%d: %s\n", m_client_id, error_message.characters());
    GUI_ServerMessage message;
    message.type = GUI_ServerMessage::Type::Error;
    ASSERT(error_message.length() < sizeof(message.text));
    strcpy(message.text, error_message.characters());
    message.text_length = error_message.length();
    WSMessageLoop::the().post_message_to_client(m_client_id, message);
}

void WSClientConnection::post_message(GUI_ServerMessage&& message)
{
    if (!m_process)
        return;
    LOCKER(m_process->gui_events_lock());
    m_process->gui_events().append(move(message));
}

RetainPtr<GraphicsBitmap> WSClientConnection::create_bitmap(const Size& size)
{
    if (!m_process)
        return nullptr;
    return GraphicsBitmap::create(*m_process, size);
}

void WSClientConnection::on_message(WSMessage& message)
{
    if (message.is_client_request()) {
        on_request(static_cast<WSAPIClientRequest&>(message));
        return;
    }

    if (message.type() == WSMessage::WM_ClientDisconnected) {
        int client_id = static_cast<WSClientDisconnectedNotification&>(message).client_id();
        dbgprintf("WSClientConnection: Client disconnected: %d\n", client_id);
        delete this;
        return;
    }
}

void WSClientConnection::handle_request(WSAPICreateMenubarRequest& request)
{
    int menubar_id = m_next_menubar_id++;
    auto menubar = make<WSMenuBar>(menubar_id, *WSMessageLoop::process_from_client_id(request.client_id()));
    m_menubars.set(menubar_id, move(menubar));
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidCreateMenubar;
    response.menu.menubar_id = menubar_id;
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPIDestroyMenubarRequest& request)
{
    int menubar_id = request.menubar_id();
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        post_error("Bad menubar ID");
        return;
    }
    auto& menubar = *(*it).value;
    WSWindowManager::the().close_menubar(menubar);
    m_menubars.remove(it);
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidDestroyMenubar;
    response.menu.menubar_id = menubar_id;
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPICreateMenuRequest& request)
{
    int menu_id = m_next_menu_id++;
    auto menu = make<WSMenu>(*WSMessageLoop::process_from_client_id(request.client_id()), menu_id, request.text());
    m_menus.set(menu_id, move(menu));
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidCreateMenu;
    response.menu.menu_id = menu_id;
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPIDestroyMenuRequest& request)
{
    int menu_id = static_cast<WSAPIDestroyMenuRequest&>(request).menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        post_error("Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    WSWindowManager::the().close_menu(menu);
    m_menus.remove(it);
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidDestroyMenu;
    response.menu.menu_id = menu_id;
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPISetApplicationMenubarRequest& request)
{
    int menubar_id = request.menubar_id();
    auto it = m_menubars.find(menubar_id);
    if (it == m_menubars.end()) {
        post_error("Bad menubar ID");
        return;
    }
    auto& menubar = *(*it).value;
    m_app_menubar = menubar.make_weak_ptr();
    WSWindowManager::the().notify_client_changed_app_menubar(*this);
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidSetApplicationMenubar;
    response.menu.menubar_id = menubar_id;
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPIAddMenuToMenubarRequest& request)
{
    int menubar_id = request.menubar_id();
    int menu_id = request.menu_id();
    auto it = m_menubars.find(menubar_id);
    auto jt = m_menus.find(menu_id);
    if (it == m_menubars.end()) {
        post_error("Bad menubar ID");
        return;
    }
    if (jt == m_menus.end()) {
        post_error("Bad menu ID");
        return;
    }
    auto& menubar = *(*it).value;
    auto& menu = *(*jt).value;
    menubar.add_menu(&menu);
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidAddMenuToMenubar;
    response.menu.menubar_id = menubar_id;
    response.menu.menu_id = menu_id;
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPIAddMenuItemRequest& request)
{
    int menu_id = request.menu_id();
    unsigned identifier = request.identifier();
    String text = request.text();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        post_error("Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.add_item(make<WSMenuItem>(identifier, move(text)));
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidAddMenuItem;
    response.menu.menu_id = menu_id;
    response.menu.identifier = identifier;
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPIAddMenuSeparatorRequest& request)
{
    int menu_id = request.menu_id();
    auto it = m_menus.find(menu_id);
    if (it == m_menus.end()) {
        post_error("Bad menu ID");
        return;
    }
    auto& menu = *(*it).value;
    menu.add_item(make<WSMenuItem>(WSMenuItem::Separator));
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidAddMenuSeparator;
    response.menu.menu_id = menu_id;
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPISetWindowTitleRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_title(request.title());
}

void WSClientConnection::handle_request(WSAPIGetWindowTitleRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidGetWindowTitle;
    response.window_id = window.window_id();
    ASSERT(window.title().length() < sizeof(response.text));
    strcpy(response.text, window.title().characters());
    response.text_length = window.title().length();
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPISetWindowRectRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_rect(request.rect());
}

void WSClientConnection::handle_request(WSAPIGetWindowRectRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidGetWindowRect;
    response.window_id = window.window_id();
    response.window.rect = window.rect();
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPICreateWindowRequest& request)
{
    int window_id = m_next_window_id++;
    auto window = make<WSWindow>(request.client_id(), window_id);
    window->set_title(request.title());
    window->set_rect(request.rect());
    m_windows.set(window_id, move(window));
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidCreateWindow;
    response.window_id = window_id;
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPIDestroyWindowRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WSWindowManager::the().invalidate(window);
    m_windows.remove(it);
}

void WSClientConnection::handle_request(WSAPIInvalidateRectRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("Bad window ID");
        return;
    }
    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::Paint;
    response.window_id = window_id;
    response.paint.rect = request.rect();
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPIDidFinishPaintingNotification& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    WSWindowManager::the().invalidate(window, request.rect());
}

void WSClientConnection::handle_request(WSAPIGetWindowBackingStoreRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    auto* backing_store = window.backing();

    // FIXME: It shouldn't work this way!
    backing_store->retain();

    GUI_ServerMessage response;
    response.type = GUI_ServerMessage::Type::DidGetWindowBackingStore;
    response.window_id = window_id;
    response.backing.backing_store_id = backing_store;
    response.backing.bpp = sizeof(RGBA32);
    response.backing.pitch = backing_store->pitch();
    response.backing.size = backing_store->size();
    response.backing.pixels = reinterpret_cast<RGBA32*>(backing_store->client_region()->laddr().as_ptr());
    WSMessageLoop::the().post_message_to_client(request.client_id(), response);
}

void WSClientConnection::handle_request(WSAPIReleaseWindowBackingStoreRequest& request)
{
    int backing_store_id = request.backing_store_id();
    // FIXME: It shouldn't work this way!
    auto* backing_store = (GraphicsBitmap*)backing_store_id;
    backing_store->release();
}

void WSClientConnection::handle_request(WSAPISetGlobalCursorTrackingRequest& request)
{
    int window_id = request.window_id();
    auto it = m_windows.find(window_id);
    if (it == m_windows.end()) {
        post_error("Bad window ID");
        return;
    }
    auto& window = *(*it).value;
    window.set_global_cursor_tracking_enabled(request.value());
}

void WSClientConnection::on_request(WSAPIClientRequest& request)
{
    switch (request.type()) {
    case WSMessage::APICreateMenubarRequest:
        return handle_request(static_cast<WSAPICreateMenubarRequest&>(request));
    case WSMessage::APIDestroyMenubarRequest:
        return handle_request(static_cast<WSAPIDestroyMenubarRequest&>(request));
    case WSMessage::APICreateMenuRequest:
        return handle_request(static_cast<WSAPICreateMenuRequest&>(request));
    case WSMessage::APIDestroyMenuRequest:
        return handle_request(static_cast<WSAPIDestroyMenuRequest&>(request));
    case WSMessage::APISetApplicationMenubarRequest:
        return handle_request(static_cast<WSAPISetApplicationMenubarRequest&>(request));
    case WSMessage::APIAddMenuToMenubarRequest:
        return handle_request(static_cast<WSAPIAddMenuToMenubarRequest&>(request));
    case WSMessage::APIAddMenuItemRequest:
        return handle_request(static_cast<WSAPIAddMenuItemRequest&>(request));
    case WSMessage::APIAddMenuSeparatorRequest:
        return handle_request(static_cast<WSAPIAddMenuSeparatorRequest&>(request));
    case WSMessage::APISetWindowTitleRequest:
        return handle_request(static_cast<WSAPISetWindowTitleRequest&>(request));
    case WSMessage::APIGetWindowTitleRequest:
        return handle_request(static_cast<WSAPIGetWindowTitleRequest&>(request));
    case WSMessage::APISetWindowRectRequest:
        return handle_request(static_cast<WSAPISetWindowRectRequest&>(request));
    case WSMessage::APIGetWindowRectRequest:
        return handle_request(static_cast<WSAPIGetWindowRectRequest&>(request));
    case WSMessage::APICreateWindowRequest:
        return handle_request(static_cast<WSAPICreateWindowRequest&>(request));
    case WSMessage::APIDestroyWindowRequest:
        return handle_request(static_cast<WSAPIDestroyWindowRequest&>(request));
    case WSMessage::APIInvalidateRectRequest:
        return handle_request(static_cast<WSAPIInvalidateRectRequest&>(request));
    case WSMessage::APIDidFinishPaintingNotification:
        return handle_request(static_cast<WSAPIDidFinishPaintingNotification&>(request));
    case WSMessage::APIGetWindowBackingStoreRequest:
        return handle_request(static_cast<WSAPIGetWindowBackingStoreRequest&>(request));
    case WSMessage::APIReleaseWindowBackingStoreRequest:
        return handle_request(static_cast<WSAPIReleaseWindowBackingStoreRequest&>(request));
    case WSMessage::APISetGlobalCursorTrackingRequest:
        return handle_request(static_cast<WSAPISetGlobalCursorTrackingRequest&>(request));
    default:
        break;
    }
}
