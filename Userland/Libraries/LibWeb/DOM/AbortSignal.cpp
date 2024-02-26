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
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HTML/WindowOrWorkerGlobalScope.h>

namespace Web::DOM {

JS_DEFINE_ALLOCATOR(AbortSignal);

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
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AbortSignalPrototype>(realm, "AbortSignal"_fly_string));
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

// https://dom.spec.whatwg.org/#dom-abortsignal-abort
WebIDL::ExceptionOr<JS::NonnullGCPtr<AbortSignal>> AbortSignal::abort(JS::VM& vm, JS::Value reason)
{
    // 1. Let signal be a new AbortSignal object.
    auto signal = TRY(construct_impl(*vm.current_realm()));

    // 2. Set signal’s abort reason to reason if it is given; otherwise to a new "AbortError" DOMException.
    if (reason.is_undefined())
        reason = WebIDL::AbortError::create(*vm.current_realm(), "Aborted without reason"_fly_string).ptr();

    signal->set_reason(reason);

    // 3. Return signal.
    return signal;
}

// https://dom.spec.whatwg.org/#dom-abortsignal-timeout
WebIDL::ExceptionOr<JS::NonnullGCPtr<AbortSignal>> AbortSignal::timeout(JS::VM& vm, WebIDL::UnsignedLongLong milliseconds)
{
    auto& realm = *vm.current_realm();

    // 1. Let signal be a new AbortSignal object.
    auto signal = TRY(construct_impl(realm));

    // 2. Let global be signal’s relevant global object.
    auto& global = HTML::relevant_global_object(signal);
    auto* window_or_worker = dynamic_cast<HTML::WindowOrWorkerGlobalScopeMixin*>(&global);
    VERIFY(window_or_worker);

    // 3. Run steps after a timeout given global, "AbortSignal-timeout", milliseconds, and the following step:
    window_or_worker->run_steps_after_a_timeout(milliseconds, [&realm, &global, strong_signal = JS::make_handle(signal)]() {
        // 1. Queue a global task on the timer task source given global to signal abort given signal and a new "TimeoutError" DOMException.
        HTML::queue_global_task(HTML::Task::Source::TimerTask, global, [&realm, &strong_signal]() mutable {
            auto reason = WebIDL::TimeoutError::create(realm, "Signal timed out"_fly_string);
            strong_signal->signal_abort(reason);
        });
    });

    // 4. Return signal.
    return signal;
}

}
