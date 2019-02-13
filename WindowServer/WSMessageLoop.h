#pragma once

#include "WSMessage.h"
#include <AK/Lock.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/Function.h>

class WSMessageReceiver;
class Process;
struct GUI_ServerMessage;

class WSMessageLoop {
public:
    WSMessageLoop();
    ~WSMessageLoop();

    int exec();

    void post_message(WSMessageReceiver* receiver, OwnPtr<WSMessage>&&);

    static WSMessageLoop& the();

    bool running() const { return m_running; }
    Process& server_process() { return *m_server_process; }

    void set_server_process(Process& process) { m_server_process = &process; }

    int start_timer(int ms, Function<void()>&&);
    int stop_timer(int timer_id);

    void post_message_to_client(int client_id, const GUI_ServerMessage&);
    ssize_t on_receive_from_client(int client_id, const byte*, size_t);

    static Process* process_from_client_id(int client_id);

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

    struct Timer {
        void reload();

        int timer_id { 0 };
        int interval { 0 };
        struct timeval next_fire_time { 0, 0 };
        Function<void()> callback;
    };

    int m_next_timer_id { 1 };
    HashMap<int, OwnPtr<Timer>> m_timers;
};
