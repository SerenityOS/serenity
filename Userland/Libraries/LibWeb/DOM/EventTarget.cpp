/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibJS/Interpreter.h>
#include <LibJS/Parser.h>
#include <LibJS/Runtime/ECMAScriptFunctionObject.h>
#include <LibWeb/Bindings/ScriptExecutionContext.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/HTML/EventHandler.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLFrameSetElement.h>
#include <LibWeb/UIEvents/EventNames.h>

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

// https://html.spec.whatwg.org/multipage/webappapis.html#window-reflecting-body-element-event-handler-set
static bool is_window_reflecting_body_element_event_handler(FlyString const& name)
{
    return name.is_one_of(
        HTML::EventNames::blur,
        HTML::EventNames::error,
        HTML::EventNames::focus,
        HTML::EventNames::load,
        UIEvents::EventNames::resize,
        "scroll");
}

// https://html.spec.whatwg.org/multipage/webappapis.html#windoweventhandlers
static bool is_window_event_handler(FlyString const& name)
{
    return name.is_one_of(
        HTML::EventNames::afterprint,
        HTML::EventNames::beforeprint,
        HTML::EventNames::beforeunload,
        HTML::EventNames::hashchange,
        HTML::EventNames::languagechange,
        HTML::EventNames::message,
        HTML::EventNames::messageerror,
        HTML::EventNames::offline,
        HTML::EventNames::online,
        HTML::EventNames::pagehide,
        HTML::EventNames::pageshow,
        HTML::EventNames::popstate,
        HTML::EventNames::rejectionhandled,
        HTML::EventNames::storage,
        HTML::EventNames::unhandledrejection,
        HTML::EventNames::unload);
}

// https://html.spec.whatwg.org/multipage/webappapis.html#determining-the-target-of-an-event-handler
static EventTarget* determine_target_of_event_handler(EventTarget& event_target, FlyString const& name)
{
    // To determine the target of an event handler, given an EventTarget object eventTarget on which the event handler is exposed,
    // and an event handler name name, the following steps are taken:

    // 1. If eventTarget is not a body element or a frameset element, then return eventTarget.
    if (!is<HTML::HTMLBodyElement>(event_target) && !is<HTML::HTMLFrameSetElement>(event_target))
        return &event_target;

    auto& event_target_element = static_cast<HTML::HTMLElement&>(event_target);

    // 2. If name is not the name of an attribute member of the WindowEventHandlers interface mixin and the Window-reflecting
    //    body element event handler set does not contain name, then return eventTarget.
    if (!is_window_event_handler(name) && !is_window_reflecting_body_element_event_handler(name))
        return &event_target;

    // 3. If eventTarget's node document is not an active document, then return null.
    if (!event_target_element.document().is_active())
        return nullptr;

    // Return eventTarget's node document's relevant global object.
    return &event_target_element.document().window();
}

HTML::EventHandler EventTarget::event_handler_attribute(FlyString const& name)
{
    auto target = determine_target_of_event_handler(*this, name);
    if (!target)
        return {};

    for (auto& listener : target->listeners()) {
        if (listener.event_name == name && listener.listener->is_attribute()) {
            return HTML::EventHandler { JS::make_handle(&listener.listener->function()) };
        }
    }
    return {};
}

void EventTarget::set_event_handler_attribute(FlyString const& name, HTML::EventHandler value)
{
    auto target = determine_target_of_event_handler(*this, name);
    if (!target)
        return;

    RefPtr<DOM::EventListener> listener;
    if (!value.callback.is_null()) {
        listener = adopt_ref(*new DOM::EventListener(move(value.callback), true));
    } else {
        StringBuilder builder;
        builder.appendff("function {}(event) {{\n{}\n}}", name, value.string);
        auto parser = JS::Parser(JS::Lexer(builder.string_view()));
        auto program = parser.parse_function_node<JS::FunctionExpression>();
        if (parser.has_errors()) {
            dbgln("Failed to parse script in event handler attribute '{}'", name);
            return;
        }
        auto* function = JS::ECMAScriptFunctionObject::create(target->script_execution_context()->realm().global_object(), name, program->source_text(), program->body(), program->parameters(), program->function_length(), nullptr, nullptr, JS::FunctionKind::Normal, false, false);
        VERIFY(function);
        listener = adopt_ref(*new DOM::EventListener(JS::make_handle(static_cast<JS::FunctionObject*>(function)), true));
    }
    if (listener) {
        for (auto& registered_listener : target->listeners()) {
            if (registered_listener.event_name == name && registered_listener.listener->is_attribute()) {
                target->remove_event_listener(name, registered_listener.listener);
                break;
            }
        }
        target->add_event_listener(name, listener.release_nonnull());
    }
}

bool EventTarget::dispatch_event(NonnullRefPtr<Event> event)
{
    return EventDispatcher::dispatch(*this, move(event));
}

}
