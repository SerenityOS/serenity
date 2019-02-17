#pragma once

#include "WSMessage.h"
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/Function.h>

class WSMessageReceiver;
struct WSAPI_ClientMessage;
struct WSAPI_ServerMessage;

class WSMessageLoop {
public:
    WSMessageLoop();
    ~WSMessageLoop();

    int exec();

    void post_message(WSMessageReceiver* receiver, OwnPtr<WSMessage>&&);

    static WSMessageLoop& the();

    bool running() const { return m_running; }

    int start_timer(int ms, Function<void()>&&);
    int stop_timer(int timer_id);

    void on_receive_from_client(int client_id, const WSAPI_ClientMessage&);

    void notify_client_disconnected(int client_id);

private:
    void wait_for_message();
    void drain_mouse();
    void drain_keyboard();

    struct QueuedMessage {
        WSMessageReceiver* receiver { nullptr };
        OwnPtr<WSMessage> message;
    };
    Vector<QueuedMessage> m_queued_messages;

    bool m_running { false };

    int m_keyboard_fd { -1 };
    int m_mouse_fd { -1 };
    int m_server_fd { -1 };

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
