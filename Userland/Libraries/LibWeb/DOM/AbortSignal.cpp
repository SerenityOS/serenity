/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/AbortSignalWrapper.h>
#include <LibWeb/DOM/AbortSignal.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/EventDispatcher.h>

namespace Web::DOM {

AbortSignal::AbortSignal(Document& document)
    : EventTarget(static_cast<Bindings::ScriptExecutionContext&>(document))
{
}

AbortSignal::~AbortSignal()
{
}

bool AbortSignal::dispatch_event(NonnullRefPtr<Event> event)
{
    return EventDispatcher::dispatch(*this, event);
}

JS::Object* AbortSignal::create_wrapper(JS::GlobalObject& global_object)
{
    return wrap(global_object, *this);
}

// https://dom.spec.whatwg.org/#abortsignal-add
void AbortSignal::add_abort_algorithm(Function<void()> abort_algorithm)
{
    if (m_aborted)
        return;

    m_abort_algorithms.append(move(abort_algorithm));
}

// https://dom.spec.whatwg.org/#abortsignal-signal-abort
void AbortSignal::signal_abort()
{
    if (m_aborted)
        return;

    m_aborted = true;

    for (auto& algorithm : m_abort_algorithms)
        algorithm();

    m_abort_algorithms.clear();

    dispatch_event(Event::create(HTML::EventNames::abort));
}

}
