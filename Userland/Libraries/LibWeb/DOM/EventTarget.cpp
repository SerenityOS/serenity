/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/ScriptExecutionContext.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web::DOM {

EventTarget::EventTarget(Bindings::ScriptExecutionContext& script_execution_context)
    : m_script_execution_context(&script_execution_context)
{
}

EventTarget::~EventTarget()
{
}

// https://dom.spec.whatwg.org/#add-an-event-listener
void EventTarget::add_event_listener(FlyString const& event_name, RefPtr<EventListener> listener)
{
    if (listener.is_null())
        return;
    auto existing_listener = m_listeners.first_matching([&](auto& entry) {
        return entry.listener->type() == event_name && &entry.listener->function() == &listener->function() && entry.listener->capture() == listener->capture();
    });
    if (existing_listener.has_value())
        return;
    listener->set_type(event_name);
    m_listeners.append({ event_name, listener.release_nonnull() });
}

// https://dom.spec.whatwg.org/#remove-an-event-listener
void EventTarget::remove_event_listener(FlyString const& event_name, RefPtr<EventListener> listener)
{
    if (listener.is_null())
        return;
    m_listeners.remove_first_matching([&](auto& entry) {
        auto matches = entry.event_name == event_name && &entry.listener->function() == &listener->function() && entry.listener->capture() == listener->capture();
        if (matches)
            entry.listener->set_removed(true);
        return matches;
    });
}

void EventTarget::remove_from_event_listener_list(NonnullRefPtr<EventListener> listener)
{
    m_listeners.remove_first_matching([&](auto& entry) {
        return entry.listener->type() == listener->type() && &entry.listener->function() == &listener->function() && entry.listener->capture() == listener->capture();
    });
}

// https://dom.spec.whatwg.org/#dom-eventtarget-dispatchevent
ExceptionOr<bool> EventTarget::dispatch_event_binding(NonnullRefPtr<Event> event)
{
    if (event->dispatched())
        return DOM::InvalidStateError::create("The event is already being dispatched.");

    if (!event->initialized())
        return DOM::InvalidStateError::create("Cannot dispatch an uninitialized event.");

    event->set_is_trusted(false);

    return dispatch_event(event);
}

}
