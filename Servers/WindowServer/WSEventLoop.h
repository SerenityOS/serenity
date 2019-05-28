#pragma once

#include <AK/ByteBuffer.h>
#include <LibCore/CEventLoop.h>

class WSClientConnection;
struct WSAPI_ClientMessage;

class WSEventLoop : public CEventLoop {
public:
    WSEventLoop();
    virtual ~WSEventLoop() override;

    static WSEventLoop& the() { return static_cast<WSEventLoop&>(CEventLoop::current()); }

private:
    virtual void add_file_descriptors_for_select(fd_set&, int& max_fd_added) override;
    virtual void process_file_descriptors_after_select(const fd_set&) override;

    void drain_server();
    void drain_mouse();
    void drain_keyboard();
    void drain_client(WSClientConnection&);
    bool on_receive_from_client(int client_id, const WSAPI_ClientMessage&, ByteBuffer&& extra_data);

    int m_keyboard_fd { -1 };
    int m_mouse_fd { -1 };
    int m_server_fd { -1 };
};
