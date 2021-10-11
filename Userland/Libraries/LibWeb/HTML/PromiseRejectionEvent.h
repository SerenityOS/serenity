/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Promise.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/DOM/Event.h>

namespace Web::HTML {

struct PromiseRejectionEventInit : public DOM::EventInit {
    JS::Handle<JS::Promise> promise;
    JS::Value reason;
};

class PromiseRejectionEvent : public DOM::Event {
public:
    using WrapperType = Bindings::PromiseRejectionEventWrapper;

    static NonnullRefPtr<PromiseRejectionEvent> create(FlyString const& event_name, PromiseRejectionEventInit const& event_init = {})
    {
        return adopt_ref(*new PromiseRejectionEvent(event_name, event_init));
    }
    static NonnullRefPtr<PromiseRejectionEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, PromiseRejectionEventInit const& event_init)
    {
        return PromiseRejectionEvent::create(event_name, event_init);
    }

    virtual ~PromiseRejectionEvent() override = default;

    // Needs to return a pointer for the generated JS bindings to work.
    JS::Promise const* promise() const { return m_promise.cell(); }
    JS::Value reason() const { return m_reason; }

protected:
    PromiseRejectionEvent(FlyString const& event_name, PromiseRejectionEventInit const& event_init)
        : DOM::Event(event_name, event_init)
        , m_promise(event_init.promise)
        , m_reason(event_init.reason)
    {
    }

    JS::Handle<JS::Promise> m_promise;
    // FIXME: Protect this from GC! Currently we have no handle for arbitrary JS::Value.
    JS::Value m_reason;
};

}
