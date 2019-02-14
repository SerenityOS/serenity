#pragma once

#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <WindowServer/WSMessageReceiver.h>

class WSWindow;
class WSMenu;
class WSMenuBar;
class WSAPIClientRequest;

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

    void handle_client_request(WSAPIClientRequest&);

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
