#pragma once

#include <LibCore/CEventLoop.h>
#include <LibCore/CoreIPCClient.h>
#include <LibGUI/GEvent.h>
#include <WindowServer/WSAPITypes.h>

class GAction;
class CObject;
class CNotifier;
class GWindow;

class GWindowServerConnection : public IPC::Client::Connection<WSAPI_ServerMessage, WSAPI_ClientMessage> {
    C_OBJECT(GWindowServerConnection)
public:
    GWindowServerConnection()
        : Connection("/tmp/wsportal")
    {}

    void handshake() override;
    static GWindowServerConnection& the();

private:
    void postprocess_bundles(Vector<IncomingMessageBundle>& m_unprocessed_bundles) override;
    void handle_paint_event(const WSAPI_ServerMessage&, GWindow&, const ByteBuffer& extra_data);
    void handle_resize_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_mouse_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_key_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_window_activation_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_window_close_request_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_menu_event(const WSAPI_ServerMessage&);
    void handle_window_entered_or_left_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_wm_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_greeting(WSAPI_ServerMessage&);
};
