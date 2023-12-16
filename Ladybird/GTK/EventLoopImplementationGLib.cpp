/*
 * Copyright (c) 2023, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventLoopImplementationGLib.h"
#include <AK/HashMap.h>
#include <LibCore/Event.h>
#include <LibCore/Notifier.h>
#include <LibCore/ThreadEventQueue.h>
#include <glib-unix.h>

// A GSource for the thread's Core::ThreadEventQueue.

static gboolean thread_event_queue_source_prepare([[maybe_unused]] GSource* source, [[maybe_unused]] gint* timeout)
{
    return Core::ThreadEventQueue::current().has_pending_events();
}

static gboolean thread_event_queue_source_dispatch([[maybe_unused]] GSource* source, [[maybe_unused]] GSourceFunc callback, [[maybe_unused]] void* user_data)
{
    // Here's a fun thing: we're not actually going to call the provided callback;
    // We're going to dispatch the events from the queue directly.
    Core::ThreadEventQueue::current().process();
    return true;
}

static GSourceFuncs thread_event_queue_source_funcs = {
    thread_event_queue_source_prepare,
    nullptr,
    thread_event_queue_source_dispatch,
    nullptr,
    nullptr,
    nullptr
};

EventLoopManagerGLib::~EventLoopManagerGLib()
{
}

NonnullOwnPtr<Core::EventLoopImplementation> EventLoopManagerGLib::make_implementation()
{
    return adopt_own(*new EventLoopImplementationGLib);
}

static bool s_created_main_loop = false;

EventLoopImplementationGLib::EventLoopImplementationGLib()
{
    // This relies on the fact that the main loop is created first.
    if (!s_created_main_loop) {
        // Grab the global default GLib context.
        m_context = g_main_context_default();
        s_created_main_loop = true;
    } else {
        m_context = g_main_context_get_thread_default();
        if (m_context == nullptr) {
            m_context = g_main_context_new();
            g_main_context_push_thread_default(m_context);
            m_owns_context = true;
        }
    }

    m_thread_event_queue_source = g_source_new(&thread_event_queue_source_funcs, sizeof(GSource));
    g_source_set_name(m_thread_event_queue_source, "ThreadEventQueueSource");
    g_source_set_can_recurse(m_thread_event_queue_source, true);
    g_source_attach(m_thread_event_queue_source, m_context);
}

EventLoopImplementationGLib::~EventLoopImplementationGLib()
{
    g_source_destroy(m_thread_event_queue_source);
    g_source_unref(m_thread_event_queue_source);

    if (m_owns_context) {
        g_main_context_pop_thread_default(m_context);
        // TODO: We should be unrefing it, right? Right?
        g_main_context_unref(m_context);
    }
}

int EventLoopImplementationGLib::exec()
{
    while (!m_should_quit)
        pump(PumpMode::WaitForEvents);
    return m_exit_code;
}

void EventLoopImplementationGLib::quit(int code)
{
    m_should_quit = true;
    m_exit_code = code;
}

void EventLoopImplementationGLib::unquit()
{
    m_should_quit = false;
    m_exit_code = -1;
}

size_t EventLoopImplementationGLib::pump(PumpMode pump_mode)
{
    bool may_block = (pump_mode == PumpMode::WaitForEvents);
    g_main_context_iteration(m_context, may_block);
    return 0;
}

GMainContext* EventLoopManagerGLib::current_thread_context()
{
    GMainContext* context = g_main_context_get_thread_default();
    if (context == nullptr)
        context = g_main_context_default();
    return context;
}

void EventLoopManagerGLib::register_notifier(Core::Notifier& notifier)
{
    GIOCondition condition;
    switch (notifier.type()) {
    case Core::Notifier::Type::Read:
        condition = G_IO_IN;
        break;
    case Core::Notifier::Type::Write:
        condition = G_IO_OUT;
        break;
    case Core::Notifier::Type::Exceptional:
        condition = G_IO_ERR;
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    GSource* source = g_unix_fd_source_new(notifier.fd(), condition);
    g_source_set_can_recurse(source, true);
    g_source_set_callback(source, G_SOURCE_FUNC(+[](int fd, [[maybe_unused]] GIOCondition condition, void* user_data) -> gboolean {
        Core::Notifier& notifier = *reinterpret_cast<Core::Notifier*>(user_data);
        Core::NotifierActivationEvent event(fd);
        notifier.dispatch_event(event);
        return true;
    }),
        &notifier, nullptr);

    g_source_attach(source, current_thread_context());
}

void EventLoopManagerGLib::unregister_notifier(Core::Notifier& notifier)
{
    GSource* source = g_main_context_find_source_by_user_data(current_thread_context(), &notifier);
    if (!source) {
        // A notifier may be unregistered multiple times, or even when it was never
        // registered in the first place. Just make it succeed silently.
        return;
    }
    g_source_destroy(source);
    g_source_unref(source);
}

int EventLoopManagerGLib::register_timer(Core::EventReceiver& object, int milliseconds, bool should_reload, Core::TimerShouldFireWhenNotVisible should_fire_when_not_visible)
{
    struct Closure {
        Core::EventReceiver* object;
        int timer_id;
        bool should_reload : 1;
        Core::TimerShouldFireWhenNotVisible should_fire_when_not_visible : 1;
    };

    Closure* closure = new Closure;
    closure->object = &object;
    closure->should_reload = should_reload;
    closure->should_fire_when_not_visible = should_fire_when_not_visible;

    GSource* source = g_timeout_source_new(milliseconds);
    g_source_set_can_recurse(source, true);
    g_source_set_callback(
        source, G_SOURCE_FUNC(+[](void* user_data) -> gboolean {
            Closure& closure = *reinterpret_cast<Closure*>(user_data);
            if (closure.should_fire_when_not_visible == Core::TimerShouldFireWhenNotVisible::No) {
                if (!closure.object->is_visible_for_timer_purposes())
                    return closure.should_reload;
            }
            Core::TimerEvent event(closure.timer_id);
            closure.object->dispatch_event(event);
            return closure.should_reload;
        }),
        closure, +[](void* user_data) {
            Closure* closure = reinterpret_cast<Closure*>(user_data);
            delete closure;
        });

    int id = g_source_attach(source, current_thread_context());
    closure->timer_id = id;
    return id;
}

bool EventLoopManagerGLib::unregister_timer(int timer_id)
{
    GSource* source = g_main_context_find_source_by_id(current_thread_context(), timer_id);
    VERIFY(source);
    g_source_destroy(source);
    g_source_unref(source);
    return true;
}

void EventLoopImplementationGLib::wake()
{
    g_main_context_wakeup(m_context);
}

void EventLoopImplementationGLib::post_event(Core::EventReceiver& receiver, NonnullOwnPtr<Core::Event>&& event)
{
    Core::ThreadEventQueue::current().post_event(receiver, move(event));
    if (m_context != g_main_context_get_thread_default())
        wake();
}
