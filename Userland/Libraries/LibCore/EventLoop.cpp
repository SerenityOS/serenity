/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <malu.bertsch@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/EventLoopImplementationUnix.h>
#include <LibCore/Object.h>
#include <LibCore/Promise.h>
#include <LibCore/ThreadEventQueue.h>

namespace Core {

namespace {
Vector<EventLoop&>& event_loop_stack()
{
    thread_local OwnPtr<Vector<EventLoop&>> s_event_loop_stack = nullptr;
    if (s_event_loop_stack == nullptr)
        s_event_loop_stack = make<Vector<EventLoop&>>();
    return *s_event_loop_stack;
}
}

EventLoop::EventLoop()
    : m_impl(EventLoopManager::the().make_implementation())
{
    if (event_loop_stack().is_empty()) {
        event_loop_stack().append(*this);
    }
}

EventLoop::~EventLoop()
{
    if (!event_loop_stack().is_empty() && &event_loop_stack().last() == this) {
        event_loop_stack().take_last();
    }
}

EventLoop& EventLoop::current()
{
    return event_loop_stack().last();
}

void EventLoop::quit(int code)
{
    ThreadEventQueue::current().cancel_all_pending_jobs();
    m_impl->quit(code);
}

void EventLoop::unquit()
{
    m_impl->unquit();
}

struct EventLoopPusher {
public:
    EventLoopPusher(EventLoop& event_loop)
    {
        event_loop_stack().append(event_loop);
    }
    ~EventLoopPusher()
    {
        event_loop_stack().take_last();
    }
};

int EventLoop::exec()
{
    EventLoopPusher pusher(*this);
    return m_impl->exec();
}

void EventLoop::spin_until(Function<bool()> goal_condition)
{
    EventLoopPusher pusher(*this);
    while (!m_impl->was_exit_requested() && !goal_condition())
        pump();
}

size_t EventLoop::pump(WaitMode mode)
{
    return m_impl->pump(mode == WaitMode::WaitForEvents ? EventLoopImplementation::PumpMode::WaitForEvents : EventLoopImplementation::PumpMode::DontWaitForEvents);
}

void EventLoop::post_event(Object& receiver, NonnullOwnPtr<Event>&& event)
{
    m_impl->post_event(receiver, move(event));
}

void EventLoop::add_job(NonnullRefPtr<Promise<NonnullRefPtr<Object>>> job_promise)
{
    ThreadEventQueue::current().add_job(move(job_promise));
}

int EventLoop::register_signal(int signal_number, Function<void(int)> handler)
{
    return EventLoopManager::the().register_signal(signal_number, move(handler));
}

void EventLoop::unregister_signal(int handler_id)
{
    EventLoopManager::the().unregister_signal(handler_id);
}

void EventLoop::notify_forked(ForkEvent)
{
    current().m_impl->notify_forked_and_in_child();
}

int EventLoop::register_timer(Object& object, int milliseconds, bool should_reload, TimerShouldFireWhenNotVisible fire_when_not_visible)
{
    return EventLoopManager::the().register_timer(object, milliseconds, should_reload, fire_when_not_visible);
}

bool EventLoop::unregister_timer(int timer_id)
{
    return EventLoopManager::the().unregister_timer(timer_id);
}

void EventLoop::register_notifier(Badge<Notifier>, Notifier& notifier)
{
    EventLoopManager::the().register_notifier(notifier);
}

void EventLoop::unregister_notifier(Badge<Notifier>, Notifier& notifier)
{
    EventLoopManager::the().unregister_notifier(notifier);
}

void EventLoop::wake()
{
    m_impl->wake();
}

void EventLoop::deferred_invoke(Function<void()> invokee)
{
    auto context = DeferredInvocationContext::construct();
    post_event(context, make<Core::DeferredInvocationEvent>(context, move(invokee)));
}

void deferred_invoke(Function<void()> invokee)
{
    EventLoop::current().deferred_invoke(move(invokee));
}

bool EventLoop::was_exit_requested() const
{
    return m_impl->was_exit_requested();
}

}
