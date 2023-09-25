/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibJS/Heap/HeapFunction.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#abortsignal
class AbortSignal final : public EventTarget {
    WEB_PLATFORM_OBJECT(AbortSignal, EventTarget);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<AbortSignal>> construct_impl(JS::Realm&);

    virtual ~AbortSignal() override = default;

    void add_abort_algorithm(Function<void()>);

    // https://dom.spec.whatwg.org/#dom-abortsignal-aborted
    // An AbortSignal object is aborted when its abort reason is not undefined.
    bool aborted() const { return !m_abort_reason.is_undefined(); }

    void signal_abort(JS::Value reason);

    void set_onabort(WebIDL::CallbackType*);
    WebIDL::CallbackType* onabort();

    // https://dom.spec.whatwg.org/#dom-abortsignal-reason
    JS::Value reason() const { return m_abort_reason; }

    JS::ThrowCompletionOr<void> throw_if_aborted() const;

    void follow(JS::NonnullGCPtr<AbortSignal> parent_signal);

private:
    explicit AbortSignal(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(JS::Cell::Visitor&) override;

    // https://dom.spec.whatwg.org/#abortsignal-abort-reason
    // An AbortSignal object has an associated abort reason, which is a JavaScript value. It is undefined unless specified otherwise.
    JS::Value m_abort_reason { JS::js_undefined() };

    // https://dom.spec.whatwg.org/#abortsignal-abort-algorithms
    // FIXME: This should be a set.
    Vector<JS::NonnullGCPtr<JS::HeapFunction<void()>>> m_abort_algorithms;
};

}
