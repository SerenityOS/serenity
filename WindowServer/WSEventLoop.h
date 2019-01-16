#pragma once

#include "WSEvent.h"
#include <AK/Lock.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>

class WSEventReceiver;
class Process;

class WSEventLoop {
public:
    WSEventLoop();
    ~WSEventLoop();

    int exec();

    void post_event(WSEventReceiver* receiver, OwnPtr<WSEvent>&&);

    static WSEventLoop& the();

    static void initialize();

    bool running() const { return m_running; }
    Process& server_process() { return *m_server_process; }

private:
    void wait_for_event();
    void drain_mouse();
    void drain_keyboard();

    SpinLock m_lock;

    struct QueuedEvent {
        WSEventReceiver* receiver { nullptr };
        OwnPtr<WSEvent> event;
    };
    Vector<QueuedEvent> m_queued_events;

    Process* m_server_process { nullptr };
    bool m_running { false };

    int m_keyboard_fd { -1 };
    int m_mouse_fd { -1 };
};
