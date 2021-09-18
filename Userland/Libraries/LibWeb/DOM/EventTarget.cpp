/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/OrdinaryFunctionObject.h>
#include <LibWeb/Bindings/ScriptExecutionContext.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/HTML/EventHandler.h>

namespace Web::DOM {

EventTarget::EventTarget(Bindings::ScriptExecutionContext& script_execution_context)
    : m_script_execution_context(&script_execution_context)
{
}

EventTarget::~EventTarget()
{
}

// https://dom.spec.whatwg.org/#add-an-event-listener
void EventTarget::add_event_listener(const FlyString& event_name, RefPtr<EventListener> listener)
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
void EventTarget::remove_event_listener(const FlyString& event_name, RefPtr<EventListener> listener)
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

HTML::EventHandler EventTarget::event_handler_attribute(FlyString const& name)
{
    for (auto& listener : listeners()) {
        if (listener.event_name == name && listener.listener->is_attribute()) {
            return HTML::EventHandler { JS::make_handle(&listener.listener->function()) };
        }
    }
    return {};
}

void EventTarget::set_event_handler_attribute(FlyString const& name, HTML::EventHandler value)
{
    RefPtr<DOM::EventListener> listener;
    if (!value.callback.is_null()) {
        listener = adopt_ref(*new DOM::EventListener(move(value.callback)));
    } else {
        StringBuilder builder;
        builder.appendff("function {}(event) {{\n{}\n}}", name, value.string);
        auto parser = JS::Parser(JS::Lexer(builder.string_view()));
        auto program = parser.parse_function_node<JS::FunctionExpression>();
        if (parser.has_errors()) {
            dbgln("Failed to parse script in event handler attribute '{}'", name);
            return;
        }
        auto* function = JS::OrdinaryFunctionObject::create(script_execution_context()->interpreter().global_object(), name, program->body(), program->parameters(), program->function_length(), nullptr, JS::FunctionKind::Regular, false, false);
        VERIFY(function);
        listener = adopt_ref(*new DOM::EventListener(JS::make_handle(static_cast<JS::FunctionObject*>(function))));
    }
    if (listener) {
        for (auto& registered_listener : listeners()) {
            if (registered_listener.event_name == name && registered_listener.listener->is_attribute()) {
                remove_event_listener(name, registered_listener.listener);
                break;
            }
        }
        add_event_listener(name, listener.release_nonnull());
    }
}
}
