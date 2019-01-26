#pragma once

#include "WSMessage.h"
#include <AK/Lock.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>

class WSMessageReceiver;
class Process;

class WSMessageLoop {
public:
    WSMessageLoop();
    ~WSMessageLoop();

    int exec();

    void post_event(WSMessageReceiver* receiver, OwnPtr<WSMessage>&&);

    static WSMessageLoop& the();

    static void initialize();

    bool running() const { return m_running; }
    Process& server_process() { return *m_server_process; }

private:
    void wait_for_event();
    void drain_mouse();
    void drain_keyboard();

    Lock m_lock;

    struct QueuedEvent {
        WSMessageReceiver* receiver { nullptr };
        OwnPtr<WSMessage> event;
    };
    Vector<QueuedEvent> m_queued_events;

    Process* m_server_process { nullptr };
    bool m_running { false };

    int m_keyboard_fd { -1 };
    int m_mouse_fd { -1 };
};
