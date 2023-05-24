/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/EventLoopImplementation.h>

namespace Core {

class EventLoopManagerUnix final : public EventLoopManager {
public:
    virtual ~EventLoopManagerUnix() override;

    virtual NonnullOwnPtr<EventLoopImplementation> make_implementation() override;

    virtual int register_timer(Object&, int milliseconds, bool should_reload, TimerShouldFireWhenNotVisible) override;
    virtual bool unregister_timer(int timer_id) override;

    virtual void register_notifier(Notifier&) override;
    virtual void unregister_notifier(Notifier&) override;

    virtual void did_post_event() override;

    virtual int register_signal(int signal_number, Function<void(int)> handler) override;
    virtual void unregister_signal(int handler_id) override;

    void wait_for_events(EventLoopImplementation::PumpMode);
    static Optional<Duration> get_next_timer_expiration();

private:
    void dispatch_signal(int signal_number);
    static void handle_signal(int signal_number);
};

class EventLoopImplementationUnix final : public EventLoopImplementation {
public:
    static NonnullOwnPtr<EventLoopImplementationUnix> create() { return make<EventLoopImplementationUnix>(); }

    EventLoopImplementationUnix();
    virtual ~EventLoopImplementationUnix();

    virtual int exec() override;
    virtual size_t pump(PumpMode) override;
    virtual void quit(int) override;

    virtual void wake() override;

    virtual void unquit() override;
    virtual bool was_exit_requested() const override;
    virtual void notify_forked_and_in_child() override;
    virtual void post_event(Object& receiver, NonnullOwnPtr<Event>&&) override;

private:
    bool m_exit_requested { false };
    int m_exit_code { 0 };

    // The wake pipe of this event loop needs to be accessible from other threads.
    int (*m_wake_pipe_fds)[2];
};

}
