#pragma once

#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <sys/select.h>
#include <time.h>

class CEvent;
class CObject;
class CNotifier;

class CEventLoop {
public:
    CEventLoop();
    virtual ~CEventLoop();

    int exec();

    void post_event(CObject& receiver, OwnPtr<CEvent>&&);

    static CEventLoop& main();
    static CEventLoop& current();

    bool running() const { return m_running; }

    static int register_timer(CObject&, int milliseconds, bool should_reload);
    static bool unregister_timer(int timer_id);

    static void register_notifier(Badge<CNotifier>, CNotifier&);
    static void unregister_notifier(Badge<CNotifier>, CNotifier&);

    void quit(int);

    virtual void take_pending_events_from(CEventLoop& other)
    {
        m_queued_events.append(move(other.m_queued_events));
    }

protected:
    virtual void add_file_descriptors_for_select(fd_set&, int& max_fd) { UNUSED_PARAM(max_fd); }
    virtual void process_file_descriptors_after_select(const fd_set&) { }
    virtual void do_processing() { }

private:
    void wait_for_event();
    void get_next_timer_expiration(timeval&);

    struct QueuedEvent {
        WeakPtr<CObject> receiver;
        OwnPtr<CEvent> event;
    };
    Vector<QueuedEvent> m_queued_events;

    bool m_running { false };
    bool m_exit_requested { false };
    int m_exit_code { 0 };

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

    static HashTable<CNotifier*>* s_notifiers;
};
