/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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

namespace Core {

class Object;
class Notifier;

class EventLoop {
public:
    EventLoop();
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
        m_queued_events.append(move(other.m_queued_events));
    }

    static void wake();

private:
    void wait_for_event(WaitMode);
    void get_next_timer_expiration(timeval&);

    struct QueuedEvent {
        WeakPtr<Object> receiver;
        NonnullOwnPtr<Event> event;
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
        WeakPtr<Object> owner;

        void reload(const timeval& now);
        bool has_expired(const timeval& now) const;
    };

    static HashMap<int, NonnullOwnPtr<EventLoopTimer>>* s_timers;

    static HashTable<Notifier*>* s_notifiers;

    static RefPtr<LocalServer> s_rpc_server;
};

}
