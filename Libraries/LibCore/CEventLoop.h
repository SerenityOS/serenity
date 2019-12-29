#pragma once

#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/CEvent.h>
#include <LibCore/CLocalServer.h>
#include <LibThread/Lock.h>
#include <sys/select.h>
#include <sys/time.h>
#include <time.h>

class CObject;
class CNotifier;

class CEventLoop {
public:
    CEventLoop();
    ~CEventLoop();

    int exec();

    enum class WaitMode {
        WaitForEvents,
        PollForEvents,
    };

    // processe events, generally called by exec() in a loop.
    // this should really only be used for integrating with other event loops
    void pump(WaitMode = WaitMode::WaitForEvents);

    void post_event(CObject& receiver, NonnullOwnPtr<CEvent>&&);

    static CEventLoop& main();
    static CEventLoop& current();

    bool was_exit_requested() const { return m_exit_requested; }

    static int register_timer(CObject&, int milliseconds, bool should_reload, TimerShouldFireWhenNotVisible);
    static bool unregister_timer(int timer_id);

    static void register_notifier(Badge<CNotifier>, CNotifier&);
    static void unregister_notifier(Badge<CNotifier>, CNotifier&);

    void quit(int);
    void unquit();

    void take_pending_events_from(CEventLoop& other)
    {
        m_queued_events.append(move(other.m_queued_events));
    }

    static void wake();

private:
    void wait_for_event(WaitMode);
    void get_next_timer_expiration(timeval&);

    struct QueuedEvent {
        WeakPtr<CObject> receiver;
        NonnullOwnPtr<CEvent> event;
    };

    Vector<QueuedEvent, 64> m_queued_events;

    bool m_exit_requested { false };
    int m_exit_code { 0 };

    static int s_wake_pipe_fds[2];

    LibThread::Lock m_lock;

    struct EventLoopTimer {
        int timer_id { 0 };
        int interval { 0 };
        timeval fire_time { 0, 0 };
        bool should_reload { false };
        TimerShouldFireWhenNotVisible fire_when_not_visible { TimerShouldFireWhenNotVisible::No };
        WeakPtr<CObject> owner;

        void reload(const timeval& now);
        bool has_expired(const timeval& now) const;
    };

    static HashMap<int, NonnullOwnPtr<EventLoopTimer>>* s_timers;
    static int s_next_timer_id;

    static HashTable<CNotifier*>* s_notifiers;

    static RefPtr<CLocalServer> s_rpc_server;
};
