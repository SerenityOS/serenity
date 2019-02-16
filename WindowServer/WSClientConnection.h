#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <AK/Function.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <WindowServer/WSMessageReceiver.h>
#include <WindowServer/WSMessage.h>

class WSWindow;
class WSMenu;
class WSMenuBar;
struct WSAPI_ServerMessage;

class WSClientConnection final : public WSMessageReceiver {
public:
    explicit WSClientConnection(int fd);
    virtual ~WSClientConnection() override;

    static WSClientConnection* from_client_id(int client_id);
    static void for_each_client(Function<void(WSClientConnection&)>);

    void post_message(const WSAPI_ServerMessage&);
    RetainPtr<GraphicsBitmap> create_shared_bitmap(const Size&);

    int client_id() const { return m_client_id; }
    WSMenuBar* app_menubar() { return m_app_menubar.ptr(); }

    int fd() const { return m_fd; }
    pid_t pid() const { return m_pid; }

private:
    virtual void on_message(WSMessage&) override;

    void on_request(WSAPIClientRequest&);
    void handle_request(WSAPICreateMenubarRequest&);
    void handle_request(WSAPIDestroyMenubarRequest&);
    void handle_request(WSAPICreateMenuRequest&);
    void handle_request(WSAPIDestroyMenuRequest&);
    void handle_request(WSAPISetApplicationMenubarRequest&);
    void handle_request(WSAPIAddMenuToMenubarRequest&);
    void handle_request(WSAPIAddMenuItemRequest&);
    void handle_request(WSAPIAddMenuSeparatorRequest&);
    void handle_request(WSAPISetWindowTitleRequest&);
    void handle_request(WSAPIGetWindowTitleRequest&);
    void handle_request(WSAPISetWindowRectRequest&);
    void handle_request(WSAPIGetWindowRectRequest&);
    void handle_request(WSAPICreateWindowRequest&);
    void handle_request(WSAPIDestroyWindowRequest&);
    void handle_request(WSAPIInvalidateRectRequest&);
    void handle_request(WSAPIDidFinishPaintingNotification&);
    void handle_request(WSAPIGetWindowBackingStoreRequest&);
    void handle_request(WSAPISetGlobalCursorTrackingRequest&);

    void post_error(const String&);

    int m_client_id { 0 };
    int m_fd { -1 };
    pid_t m_pid { 0 };

    HashMap<int, OwnPtr<WSWindow>> m_windows;
    HashMap<int, OwnPtr<WSMenuBar>> m_menubars;
    HashMap<int, OwnPtr<WSMenu>> m_menus;
    WeakPtr<WSMenuBar> m_app_menubar;

    int m_next_menubar_id { 10000 };
    int m_next_menu_id { 20000 };
    int m_next_window_id { 1982 };
};
