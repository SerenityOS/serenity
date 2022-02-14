/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Time.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/DeferredInvocationContext.h>
#include <LibCore/Event.h>
#include <LibCore/Forward.h>
#include <LibThreading/MutexProtected.h>
#include <sys/time.h>
#include <sys/types.h>

namespace Core {

extern Threading::MutexProtected<EventLoop*> s_main_event_loop;

class EventLoop {
public:
    enum class MakeInspectable {
        No,
        Yes,
    };

    enum class ShouldWake {
        No,
        Yes
    };

    explicit EventLoop(MakeInspectable = MakeInspectable::No);
    ~EventLoop();
    static void initialize_wake_pipes();

    int exec();

    enum class WaitMode {
        WaitForEvents,
        PollForEvents,
    };

    // processe events, generally called by exec() in a loop.
    // this should really only be used for integrating with other event loops
    size_t pump(WaitMode = WaitMode::WaitForEvents);

    void spin_until(Function<bool()>);

    void post_event(Object& receiver, NonnullOwnPtr<Event>&&, ShouldWake = ShouldWake::No);

    template<typename Callback>
    static decltype(auto) with_main_locked(Callback callback)
    {
        return s_main_event_loop.with_locked([&callback](auto*& event_loop) {
            VERIFY(event_loop != nullptr);
            return callback(event_loop);
        });
    }
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

    static void wake_current();
    void wake();

    static int register_signal(int signo, Function<void(int)> handler);
    static void unregister_signal(int handler_id);

    // Note: Boost uses Parent/Child/Prepare, but we don't really have anything
    //       interesting to do in the parent or before forking.
    enum class ForkEvent {
        Child,
    };
    static void notify_forked(ForkEvent);

    static bool has_been_instantiated();

    void deferred_invoke(Function<void()> invokee)
    {
        auto context = DeferredInvocationContext::construct();
        post_event(context, make<Core::DeferredInvocationEvent>(context, move(invokee)));
    }

private:
    void wait_for_event(WaitMode);
    Optional<Time> get_next_timer_expiration();
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

    static thread_local int s_wake_pipe_fds[2];
    static thread_local bool s_wake_pipe_initialized;

    // The wake pipe of this event loop needs to be accessible from other threads.
    int (*m_wake_pipe_fds)[2];

    struct Private;
    NonnullOwnPtr<Private> m_private;
};

inline void deferred_invoke(Function<void()> invokee)
{
    EventLoop::current().deferred_invoke(move(invokee));
}

}
