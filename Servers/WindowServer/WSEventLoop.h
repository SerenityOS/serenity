#pragma once

#include <AK/ByteBuffer.h>
#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalServer.h>
#include <LibCore/CNotifier.h>

class WSClientConnection;
struct WSAPI_ClientMessage;

class WSEventLoop {
public:
    WSEventLoop();
    virtual ~WSEventLoop();

    int exec() { return m_event_loop.exec(); }

private:
    void drain_mouse();
    void drain_keyboard();

    CEventLoop m_event_loop;
    int m_keyboard_fd { -1 };
    OwnPtr<CNotifier> m_keyboard_notifier;
    int m_mouse_fd { -1 };
    OwnPtr<CNotifier> m_mouse_notifier;
    CLocalServer m_server_sock;
    OwnPtr<CNotifier> m_server_notifier;
};
