/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/EventHandler.h>

namespace Web::DOM {

WebIDL::ExceptionOr<JS::NonnullGCPtr<AbortSignal>> AbortSignal::construct_impl(JS::Realm& realm)
{
    return realm.heap().allocate<AbortSignal>(realm, realm);
}

AbortSignal::AbortSignal(JS::Realm& realm)
    : EventTarget(realm)
{
}

void AbortSignal::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AbortSignalPrototype>(realm, "AbortSignal"));
}

// https://dom.spec.whatwg.org/#abortsignal-add
void AbortSignal::add_abort_algorithm(Function<void()> abort_algorithm)
{
    // 1. If signal is aborted, then return.
    if (aborted())
        return;

    // 2. Append algorithm to signal’s abort algorithms.
    m_abort_algorithms.append(JS::create_heap_function(vm().heap(), move(abort_algorithm)));
}

// https://dom.spec.whatwg.org/#abortsignal-signal-abort
void AbortSignal::signal_abort(JS::Value reason)
{
    // 1. If signal is aborted, then return.
    if (aborted())
        return;

    // 2. Set signal’s abort reason to reason if it is given; otherwise to a new "AbortError" DOMException.
    if (!reason.is_undefined())
        m_abort_reason = reason;
    else
        m_abort_reason = WebIDL::AbortError::create(realm(), "Aborted without reason"_fly_string).ptr();

    // 3. For each algorithm in signal’s abort algorithms: run algorithm.
    for (auto& algorithm : m_abort_algorithms)
        algorithm->function()();

    // 4. Empty signal’s abort algorithms.
    m_abort_algorithms.clear();

    // 5. Fire an event named abort at signal.
    dispatch_event(Event::create(realm(), HTML::EventNames::abort));
}

void AbortSignal::set_onabort(WebIDL::CallbackType* event_handler)
{
    set_event_handler_attribute(HTML::EventNames::abort, event_handler);
}

WebIDL::CallbackType* AbortSignal::onabort()
{
    return event_handler_attribute(HTML::EventNames::abort);
}

// https://dom.spec.whatwg.org/#dom-abortsignal-throwifaborted
JS::ThrowCompletionOr<void> AbortSignal::throw_if_aborted() const
{
    // The throwIfAborted() method steps are to throw this’s abort reason, if this is aborted.
    if (!aborted())
        return {};

    return JS::throw_completion(m_abort_reason);
}

void AbortSignal::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_abort_reason);
    for (auto& algorithm : m_abort_algorithms)
        visitor.visit(algorithm);
}

// https://dom.spec.whatwg.org/#abortsignal-follow
void AbortSignal::follow(JS::NonnullGCPtr<AbortSignal> parent_signal)
{
    // A followingSignal (an AbortSignal) is made to follow a parentSignal (an AbortSignal) by running these steps:

    // 1. If followingSignal is aborted, then return.
    if (aborted())
        return;

    // 2. If parentSignal is aborted, then signal abort on followingSignal with parentSignal’s abort reason.
    if (parent_signal->aborted()) {
        signal_abort(parent_signal->reason());
        return;
    }

    // 3. Otherwise, add the following abort steps to parentSignal:
    // NOTE: `this` and `parent_signal` are protected by AbortSignal using JS::SafeFunction.
    parent_signal->add_abort_algorithm([this, parent_signal] {
        // 1. Signal abort on followingSignal with parentSignal’s abort reason.
        signal_abort(parent_signal->reason());
    });
}

}
