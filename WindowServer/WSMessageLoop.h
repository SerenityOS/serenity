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

    void post_message(WSMessageReceiver* receiver, OwnPtr<WSMessage>&&, bool unsafe = false);

    static WSMessageLoop& the();

    bool running() const { return m_running; }
    Process& server_process() { return *m_server_process; }

private:
    void wait_for_message();
    void drain_mouse();
    void drain_keyboard();

    Lock m_lock;

    struct QueuedMessage {
        WSMessageReceiver* receiver { nullptr };
        OwnPtr<WSMessage> message;
    };
    Vector<QueuedMessage> m_queued_messages;

    Process* m_server_process { nullptr };
    bool m_running { false };

    int m_keyboard_fd { -1 };
    int m_mouse_fd { -1 };
};
