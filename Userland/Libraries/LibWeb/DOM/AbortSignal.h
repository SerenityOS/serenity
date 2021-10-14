/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/EventTarget.h>
#include <LibWeb/DOM/Window.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#abortsignal
class AbortSignal final
    : public RefCounted<AbortSignal>
    , public Weakable<AbortSignal>
    , public EventTarget
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::AbortSignalWrapper;

    using RefCounted::ref;
    using RefCounted::unref;

    static NonnullRefPtr<AbortSignal> create()
    {
        return adopt_ref(*new AbortSignal());
    }

    static NonnullRefPtr<AbortSignal> create_with_global_object(Bindings::WindowObject&)
    {
        return AbortSignal::create();
    }

    virtual ~AbortSignal() override;

    void add_abort_algorithm(Function<void()>);

    // https://dom.spec.whatwg.org/#dom-abortsignal-aborted
    // An AbortSignal object is aborted when its abort reason is not undefined.
    bool aborted() const { return !m_abort_reason.is_undefined(); }

    void signal_abort(JS::Value reason);

    void set_onabort(Optional<Bindings::CallbackType>);
    Bindings::CallbackType* onabort();

    // https://dom.spec.whatwg.org/#dom-abortsignal-reason
    JS::Value reason() const { return m_abort_reason; }

    JS::ThrowCompletionOr<void> throw_if_aborted() const;

    void visit_edges(JS::Cell::Visitor&);

    // ^EventTarget
    virtual void ref_event_target() override { ref(); }
    virtual void unref_event_target() override { unref(); }
    virtual JS::Object* create_wrapper(JS::GlobalObject&) override;

private:
    AbortSignal();

    // https://dom.spec.whatwg.org/#abortsignal-abort-reason
    // An AbortSignal object has an associated abort reason, which is a JavaScript value. It is undefined unless specified otherwise.
    JS::Value m_abort_reason { JS::js_undefined() };

    // https://dom.spec.whatwg.org/#abortsignal-abort-algorithms
    // FIXME: This should be a set.
    Vector<Function<void()>> m_abort_algorithms;
};

}
