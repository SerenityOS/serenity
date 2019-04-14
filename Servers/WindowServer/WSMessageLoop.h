#pragma once

#include "WSMessage.h"
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/Function.h>
#include <AK/WeakPtr.h>
#include <LibCore/CEventLoop.h>

class CObject;
struct WSAPI_ClientMessage;
struct WSAPI_ServerMessage;

class WSMessageLoop : public CEventLoop {
public:
    WSMessageLoop();
    virtual ~WSMessageLoop() override;

    static WSMessageLoop& the();

    void on_receive_from_client(int client_id, const WSAPI_ClientMessage&);
    void notify_client_disconnected(int client_id);

private:
    virtual void add_file_descriptors_for_select(fd_set&, int& max_fd_added) override;
    virtual void process_file_descriptors_after_select(const fd_set&) override;
    virtual void do_processing() override { }

    void drain_server();
    void drain_mouse();
    void drain_keyboard();

    int m_keyboard_fd { -1 };
    int m_mouse_fd { -1 };
    int m_server_fd { -1 };
};
