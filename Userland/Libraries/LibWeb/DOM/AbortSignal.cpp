/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/AbortSignalWrapper.h>
#include <LibWeb/Bindings/DOMExceptionWrapper.h>
#include <LibWeb/Bindings/Wrapper.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventDispatcher.h>
#include <LibWeb/HTML/EventHandler.h>

namespace Web::DOM {

AbortSignal::AbortSignal(Document& document)
    : EventTarget(static_cast<Bindings::ScriptExecutionContext&>(document))
{
}

AbortSignal::~AbortSignal()
{
}

JS::Object* AbortSignal::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
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
        m_abort_reason = wrap(wrapper()->global_object(), AbortError::create("Aborted without reason"));

    // 3. For each algorithm in signal’s abort algorithms: run algorithm.
    for (auto& algorithm : m_abort_algorithms)
        algorithm();

    // 4. Empty signal’s abort algorithms.
    m_abort_algorithms.clear();

    // 5. Fire an event named abort at signal.
    dispatch_event(Event::create(HTML::EventNames::abort));
}

void AbortSignal::set_onabort(HTML::EventHandler event_handler)
{
    set_event_handler_attribute(HTML::EventNames::abort, event_handler);
}

HTML::EventHandler AbortSignal::onabort()
{
    return event_handler_attribute(HTML::EventNames::abort);
}

// https://dom.spec.whatwg.org/#dom-abortsignal-throwifaborted
JS::ThrowCompletionOr<void> AbortSignal::throw_if_aborted() const
{
    auto& global_object = wrapper()->global_object();
    auto& vm = global_object.vm();

    // The throwIfAborted() method steps are to throw this’s abort reason, if this is aborted.
    if (!aborted())
        return {};

    // FIXME: Remove this once VM::exception() has been removed.
    vm.throw_exception(global_object, m_abort_reason);

    return JS::throw_completion(m_abort_reason);
}

void AbortSignal::visit_edges(JS::Cell::Visitor& visitor)
{
    visitor.visit(m_abort_reason);
}

}
