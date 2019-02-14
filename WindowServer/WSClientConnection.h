#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <WindowServer/WSMessageReceiver.h>
#include <WindowServer/WSMessage.h>

class WSWindow;
class WSMenu;
class WSMenuBar;

// FIXME: Remove.
class Process;

class WSClientConnection final : public WSMessageReceiver {
public:
    explicit WSClientConnection(int client_id);
    virtual ~WSClientConnection() override;

    static WSClientConnection* from_client_id(int client_id);
    static WSClientConnection* ensure_for_client_id(int client_id);

    // FIXME: Remove.
    Process* process() { return m_process.ptr(); }

    int client_id() const { return m_client_id; }
    WSMenuBar* app_menubar() { return m_app_menubar.ptr(); }

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
    void handle_request(WSAPIReleaseWindowBackingStoreRequest&);
    void handle_request(WSAPISetGlobalCursorTrackingRequest&);

    int m_client_id { 0 };

    HashMap<int, OwnPtr<WSWindow>> m_windows;
    HashMap<int, OwnPtr<WSMenuBar>> m_menubars;
    HashMap<int, OwnPtr<WSMenu>> m_menus;
    WeakPtr<WSMenuBar> m_app_menubar;

    int m_next_menubar_id { 10000 };
    int m_next_menu_id { 20000 };
    int m_next_window_id { 1982 };

    // FIXME: Remove.
    WeakPtr<Process> m_process;
};
