/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/Forward.h>
#include <sys/time.h>
#include <sys/types.h>

namespace Core {

class EventLoop {
public:
    enum class MakeInspectable {
        No,
        Yes,
    };

    explicit EventLoop(MakeInspectable = MakeInspectable::No);
    ~EventLoop();

    int exec();

    enum class WaitMode {
        WaitForEvents,
        PollForEvents,
    };

    // processe events, generally called by exec() in a loop.
    // this should really only be used for integrating with other event loops
    void pump(WaitMode = WaitMode::WaitForEvents);

    void post_event(Object& receiver, NonnullOwnPtr<Event>&&);

    static EventLoop& main();
    static EventLoop& current();

    bool was_exit_requested() const { return m_exit_requested; }

    static int register_timer(Object&, int milliseconds, bool should_reload, TimerShouldFireWhenNotVisible);
    static bool unregister_timer(int timer_id);

    static void register_notifier(Badge<Notifier>, Notifier&);
    static void unregister_notifier(Badge<Notifier>, Notifier&);

    void quit(int);
    void unquit();

    void take_pending_events_from(EventLoop& other)
    {
        m_queued_events.extend(move(other.m_queued_events));
    }

    static void wake();

    static int register_signal(int signo, Function<void(int)> handler);
    static void unregister_signal(int handler_id);

    // Note: Boost uses Parent/Child/Prepare, but we don't really have anything
    //       interesting to do in the parent or before forking.
    enum class ForkEvent {
        Child,
    };
    static void notify_forked(ForkEvent);

private:
    void wait_for_event(WaitMode);
    Optional<struct timeval> get_next_timer_expiration();
    static void dispatch_signal(int);
    static void handle_signal(int);

    struct QueuedEvent {
        AK_MAKE_NONCOPYABLE(QueuedEvent);

    public:
        QueuedEvent(Object& receiver, NonnullOwnPtr<Event>);
        QueuedEvent(QueuedEvent&&);
        ~QueuedEvent();

        WeakPtr<Object> receiver;
        NonnullOwnPtr<Event> event;
    };

    Vector<QueuedEvent, 64> m_queued_events;
    static pid_t s_pid;

    bool m_exit_requested { false };
    int m_exit_code { 0 };

    static int s_wake_pipe_fds[2];

    struct Private;
    NonnullOwnPtr<Private> m_private;
};

}
