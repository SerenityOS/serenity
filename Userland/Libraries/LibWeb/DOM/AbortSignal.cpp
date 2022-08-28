/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/DOMExceptionWrapper.h>
#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/EventHandler.h>

namespace Web::DOM {

JS::NonnullGCPtr<AbortSignal> AbortSignal::create_with_global_object(HTML::Window& window)
{
    return *window.heap().allocate<AbortSignal>(window.realm(), window);
}

AbortSignal::AbortSignal(HTML::Window& window)
    : EventTarget(window.realm())
{
}

// https://dom.spec.whatwg.org/#abortsignal-add
void AbortSignal::add_abort_algorithm(Function<void()> abort_algorithm)
{
    // 1. If signal is aborted, then return.
    if (aborted())
        return;

    // 2. Append algorithm to signal’s abort algorithms.
    m_abort_algorithms.append(move(abort_algorithm));
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
        m_abort_reason = wrap(realm(), AbortError::create("Aborted without reason"));

    // 3. For each algorithm in signal’s abort algorithms: run algorithm.
    for (auto& algorithm : m_abort_algorithms)
        algorithm();

    // 4. Empty signal’s abort algorithms.
    m_abort_algorithms.clear();

    // 5. Fire an event named abort at signal.
    dispatch_event(*Event::create(global_object(), HTML::EventNames::abort));
}

void AbortSignal::set_onabort(Bindings::CallbackType* event_handler)
{
    set_event_handler_attribute(HTML::EventNames::abort, event_handler);
}

Bindings::CallbackType* AbortSignal::onabort()
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
}

}
