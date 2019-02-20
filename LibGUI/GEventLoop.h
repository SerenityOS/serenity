#pragma once

#include "GEvent.h"
#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <WindowServer/WSAPITypes.h>

class GObject;
class GNotifier;
class GWindow;

class GEventLoop {
public:
    GEventLoop();
    ~GEventLoop();

    int exec();

    void post_event(GObject* receiver, OwnPtr<GEvent>&&);

    static GEventLoop& main();

    static void initialize();

    bool running() const { return m_running; }

    int register_timer(GObject&, int milliseconds, bool should_reload);
    bool unregister_timer(int timer_id);

    void register_notifier(Badge<GNotifier>, GNotifier&);
    void unregister_notifier(Badge<GNotifier>, GNotifier&);

    void quit(int);

    bool post_message_to_server(const WSAPI_ClientMessage&);
    bool wait_for_specific_event(WSAPI_ServerMessage::Type, WSAPI_ServerMessage&);

    WSAPI_ServerMessage sync_request(const WSAPI_ClientMessage& request, WSAPI_ServerMessage::Type response_type);

    pid_t server_pid() const { return m_server_pid; }

private:
    void wait_for_event();
    bool drain_messages_from_server();
    void process_unprocessed_messages();
    void handle_paint_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_resize_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_mouse_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_key_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_window_activation_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_window_close_request_event(const WSAPI_ServerMessage&, GWindow&);
    void handle_menu_event(const WSAPI_ServerMessage&);
    void handle_window_entered_or_left_event(const WSAPI_ServerMessage&, GWindow&);
    void get_next_timer_expiration(timeval&);

    struct QueuedEvent {
        GObject* receiver { nullptr };
        OwnPtr<GEvent> event;
    };
    Vector<QueuedEvent> m_queued_events;

    Vector<WSAPI_ServerMessage> m_unprocessed_messages;

    int m_event_fd { -1 };
    bool m_running { false };
    bool m_exit_requested { false };
    int m_exit_code { 0 };
    int m_next_timer_id { 1 };
    pid_t m_server_pid { 0 };

    struct EventLoopTimer {
        int timer_id { 0 };
        int interval { 0 };
        timeval fire_time;
        bool should_reload { false };
        GObject* owner { nullptr };

        void reload();
        bool has_expired() const;
    };

    HashMap<int, OwnPtr<EventLoopTimer>> m_timers;
    HashTable<GNotifier*> m_notifiers;
};
