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

void WSClientConnection::on_message(WSMessage& message)
{
    if (message.is_client_request()) {
        handle_client_request(static_cast<WSAPIClientRequest&>(message));
        return;
    }

    if (message.type() == WSMessage::WM_ClientDisconnected) {
        int client_id = static_cast<WSClientDisconnectedNotification&>(message).client_id();
        dbgprintf("WSClientConnection: Client disconnected: %d\n", client_id);
        delete this;
        return;
    }
}

void WSClientConnection::handle_client_request(WSAPIClientRequest& request)
{
    switch (request.type()) {
    case WSMessage::APICreateMenubarRequest: {
        int menubar_id = m_next_menubar_id++;
        auto menubar = make<WSMenuBar>(menubar_id, *WSMessageLoop::process_from_client_id(request.client_id()));
        m_menubars.set(menubar_id, move(menubar));
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidCreateMenubar;
        response.menu.menubar_id = menubar_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIDestroyMenubarRequest: {
        int menubar_id = static_cast<WSAPIDestroyMenubarRequest&>(request).menubar_id();
        auto it = m_menubars.find(menubar_id);
        if (it == m_menubars.end()) {
            ASSERT_NOT_REACHED();
            // FIXME: Send an error.
            return;
        }
        auto& menubar = *(*it).value;
        WSWindowManager::the().close_menubar(menubar);
        m_menubars.remove(it);
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidDestroyMenubar;
        response.menu.menubar_id = menubar_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APICreateMenuRequest: {
        int menu_id = m_next_menu_id++;
        auto menu = make<WSMenu>(*WSMessageLoop::process_from_client_id(request.client_id()), menu_id, static_cast<WSAPICreateMenuRequest&>(request).text());
        m_menus.set(menu_id, move(menu));
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidCreateMenu;
        response.menu.menu_id = menu_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIDestroyMenuRequest: {
        int menu_id = static_cast<WSAPIDestroyMenuRequest&>(request).menu_id();
        auto it = m_menus.find(menu_id);
        if (it == m_menus.end()) {
            ASSERT_NOT_REACHED();
            // FIXME: Send an error.
            return;
        }
        auto& menu = *(*it).value;
        WSWindowManager::the().close_menu(menu);
        m_menus.remove(it);
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidDestroyMenu;
        response.menu.menu_id = menu_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APISetApplicationMenubarRequest: {
        int menubar_id = static_cast<WSAPISetApplicationMenubarRequest&>(request).menubar_id();
        auto it = m_menubars.find(menubar_id);
        if (it == m_menubars.end()) {
            ASSERT_NOT_REACHED();
            // FIXME: Send an error.
            return;
        }
        auto& menubar = *(*it).value;
        m_app_menubar = menubar.make_weak_ptr();
        WSWindowManager::the().notify_client_changed_app_menubar(*this);
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidSetApplicationMenubar;
        response.menu.menubar_id = menubar_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIAddMenuToMenubarRequest: {
        int menubar_id = static_cast<WSAPIAddMenuToMenubarRequest&>(request).menubar_id();
        int menu_id = static_cast<WSAPIAddMenuToMenubarRequest&>(request).menu_id();
        auto it = m_menubars.find(menubar_id);
        auto jt = m_menus.find(menu_id);
        if (it == m_menubars.end() || jt == m_menus.end()) {
            ASSERT_NOT_REACHED();
        }
        auto& menubar = *(*it).value;
        auto& menu = *(*jt).value;
        menubar.add_menu(&menu);
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidAddMenuToMenubar;
        response.menu.menubar_id = menubar_id;
        response.menu.menu_id = menu_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIAddMenuItemRequest: {
        int menu_id = static_cast<WSAPIAddMenuItemRequest&>(request).menu_id();
        unsigned identifier = static_cast<WSAPIAddMenuItemRequest&>(request).identifier();
        String text = static_cast<WSAPIAddMenuItemRequest&>(request).text();
        auto it = m_menus.find(menu_id);
        if (it == m_menus.end()) {
            ASSERT_NOT_REACHED();
        }
        auto& menu = *(*it).value;
        menu.add_item(make<WSMenuItem>(identifier, move(text)));
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidAddMenuItem;
        response.menu.menu_id = menu_id;
        response.menu.identifier = identifier;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIAddMenuSeparatorRequest: {
        int menu_id = static_cast<WSAPIAddMenuSeparatorRequest&>(request).menu_id();
        auto it = m_menus.find(menu_id);
        if (it == m_menus.end()) {
            ASSERT_NOT_REACHED();
        }
        auto& menu = *(*it).value;
        menu.add_item(make<WSMenuItem>(WSMenuItem::Separator));
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidAddMenuSeparator;
        response.menu.menu_id = menu_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APISetWindowTitleRequest: {
        int window_id = static_cast<WSAPISetWindowTitleRequest&>(request).window_id();
        auto it = m_windows.find(window_id);
        if (it == m_windows.end()) {
            ASSERT_NOT_REACHED();
        }
        auto& window = *(*it).value;
        window.set_title(static_cast<WSAPISetWindowTitleRequest&>(request).title());
        break;
    }
    case WSMessage::APIGetWindowTitleRequest: {
        int window_id = static_cast<WSAPIGetWindowTitleRequest&>(request).window_id();
        auto it = m_windows.find(window_id);
        if (it == m_windows.end()) {
            ASSERT_NOT_REACHED();
        }
        auto& window = *(*it).value;
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidGetWindowTitle;
        response.window_id = window.window_id();
        ASSERT(window.title().length() < sizeof(response.text));
        strcpy(response.text, window.title().characters());
        response.text_length = window.title().length();
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APISetWindowRectRequest: {
        int window_id = static_cast<WSAPISetWindowRectRequest&>(request).window_id();
        auto it = m_windows.find(window_id);
        if (it == m_windows.end()) {
            ASSERT_NOT_REACHED();
        }
        auto& window = *(*it).value;
        window.set_rect(static_cast<WSAPISetWindowRectRequest&>(request).rect());
        break;
    }
    case WSMessage::APIGetWindowRectRequest: {
        int window_id = static_cast<WSAPIGetWindowRectRequest&>(request).window_id();
        auto it = m_windows.find(window_id);
        if (it == m_windows.end()) {
            ASSERT_NOT_REACHED();
        }
        auto& window = *(*it).value;
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidGetWindowRect;
        response.window_id = window.window_id();
        response.window.rect = window.rect();
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APICreateWindowRequest: {
        int window_id = m_next_window_id++;
        auto window = make<WSWindow>(request.client_id(), window_id);
        window->set_title(static_cast<WSAPICreateWindowRequest&>(request).title());
        window->set_rect(static_cast<WSAPICreateWindowRequest&>(request).rect());
        m_windows.set(window_id, move(window));
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::DidCreateWindow;
        response.window_id = window_id;
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIDestroyWindowRequest: {
        int window_id = static_cast<WSAPIGetWindowRectRequest&>(request).window_id();
        auto it = m_windows.find(window_id);
        if (it == m_windows.end()) {
            ASSERT_NOT_REACHED();
        }
        auto& window = *(*it).value;
        WSWindowManager::the().invalidate(window);
        m_windows.remove(it);
        break;
    }
    case WSMessage::APIInvalidateRectRequest: {
        int window_id = static_cast<WSAPIInvalidateRectRequest&>(request).window_id();
        auto it = m_windows.find(window_id);
        if (it == m_windows.end()) {
            ASSERT_NOT_REACHED();
        }
        GUI_ServerMessage response;
        response.type = GUI_ServerMessage::Type::Paint;
        response.window_id = window_id;
        response.paint.rect = static_cast<WSAPIInvalidateRectRequest&>(request).rect();
        WSMessageLoop::the().post_message_to_client(request.client_id(), response);
        break;
    }
    case WSMessage::APIDidFinishPaintingNotification: {
        int window_id = static_cast<WSAPIDidFinishPaintingNotification&>(request).window_id();
        auto it = m_windows.find(window_id);
        if (it == m_windows.end()) {
            ASSERT_NOT_REACHED();
        }
        auto& window = *(*it).value;
        WSWindowManager::the().invalidate(window, static_cast<WSAPIDidFinishPaintingNotification&>(request).rect());
        break;
    }
    case WSMessage::APIGetWindowBackingStoreRequest: {
        int window_id = static_cast<WSAPIGetWindowBackingStoreRequest&>(request).window_id();
        auto it = m_windows.find(window_id);
        if (it == m_windows.end()) {
            ASSERT_NOT_REACHED();
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
        break;
    }
    case WSMessage::APIReleaseWindowBackingStoreRequest: {
        int backing_store_id = static_cast<WSAPIReleaseWindowBackingStoreRequest&>(request).backing_store_id();
        // FIXME: It shouldn't work this way!
        auto* backing_store = (GraphicsBitmap*)backing_store_id;
        backing_store->release();
        break;
    }
    default:
        break;
    }
}
