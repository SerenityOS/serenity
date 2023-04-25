/*
 * Copyright (c) 2022-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibCore/Forward.h>

namespace Core {

class ThreadEventQueue;

class EventLoopImplementation {
public:
    virtual ~EventLoopImplementation();

    enum class PumpMode {
        WaitForEvents,
        DontWaitForEvents,
    };

    void post_event(Object& receiver, NonnullOwnPtr<Event>&&);

    virtual int exec() = 0;
    virtual size_t pump(PumpMode) = 0;
    virtual void quit(int) = 0;
    virtual void wake() = 0;

    virtual void deferred_invoke(Function<void()>) = 0;

    virtual int register_timer(Object&, int milliseconds, bool should_reload, TimerShouldFireWhenNotVisible) = 0;
    virtual bool unregister_timer(int timer_id) = 0;

    virtual void register_notifier(Notifier&) = 0;
    virtual void unregister_notifier(Notifier&) = 0;

    // FIXME: These APIs only exist for obscure use-cases inside SerenityOS. Try to get rid of them.
    virtual void unquit() = 0;
    virtual bool was_exit_requested() const = 0;
    virtual void notify_forked_and_in_child() = 0;
    virtual int register_signal(int signal_number, Function<void(int)> handler) = 0;
    virtual void unregister_signal(int handler_id) = 0;

protected:
    EventLoopImplementation();

    ThreadEventQueue& m_thread_event_queue;
};

}
