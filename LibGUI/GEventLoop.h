#pragma once

#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <WindowServer/WSAPITypes.h>
#include <LibGUI/GEvent.h>

class GAction;
class CObject;
class GNotifier;
class GWindow;

class GEventLoop {
public:
    GEventLoop();
    ~GEventLoop();

    int exec();

    void post_event(CObject& receiver, OwnPtr<CEvent>&&);

    static GEventLoop& main();
    static GEventLoop& current();

    bool running() const { return m_running; }

    static int register_timer(CObject&, int milliseconds, bool should_reload);
    static bool unregister_timer(int timer_id);

    static void register_notifier(Badge<GNotifier>, GNotifier&);
    static void unregister_notifier(Badge<GNotifier>, GNotifier&);

    void quit(int);

    static bool post_message_to_server(const WSAPI_ClientMessage&);
    bool wait_for_specific_event(WSAPI_ServerMessage::Type, WSAPI_ServerMessage&);

    WSAPI_ServerMessage sync_request(const WSAPI_ClientMessage& request, WSAPI_ServerMessage::Type response_type);

    static pid_t server_pid() { return s_server_pid; }

    void take_pending_events_from(GEventLoop& other)
    {
        m_queued_events.append(move(other.m_queued_events));
        m_unprocessed_messages.append(move(other.m_unprocessed_messages));
    }

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
    void handle_wm_event(const WSAPI_ServerMessage&, GWindow&);
    void get_next_timer_expiration(timeval&);
    void connect_to_server();

    struct QueuedEvent {
        WeakPtr<CObject> receiver;
        OwnPtr<CEvent> event;
    };
    Vector<QueuedEvent> m_queued_events;

    Vector<WSAPI_ServerMessage> m_unprocessed_messages;

    bool m_running { false };
    bool m_exit_requested { false };
    int m_exit_code { 0 };

    static pid_t s_server_pid;
    static pid_t s_event_fd;

    struct EventLoopTimer {
        int timer_id { 0 };
        int interval { 0 };
        timeval fire_time;
        bool should_reload { false };
        WeakPtr<CObject> owner;

        void reload();
        bool has_expired() const;
    };

    static HashMap<int, OwnPtr<EventLoopTimer>>* s_timers;
    static int s_next_timer_id;

    static HashTable<GNotifier*>* s_notifiers;
};
