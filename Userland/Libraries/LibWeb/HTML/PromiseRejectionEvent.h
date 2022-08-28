/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
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

class PromiseRejectionEvent final : public DOM::Event {
    WEB_PLATFORM_OBJECT(PromiseRejectionEvent, DOM::Event);

public:
    static PromiseRejectionEvent* create(HTML::Window&, FlyString const& event_name, PromiseRejectionEventInit const& event_init = {});
    static PromiseRejectionEvent* create_with_global_object(HTML::Window&, FlyString const& event_name, PromiseRejectionEventInit const& event_init);

    PromiseRejectionEvent(HTML::Window&, FlyString const& event_name, PromiseRejectionEventInit const& event_init);

    virtual ~PromiseRejectionEvent() override;

    // Needs to return a pointer for the generated JS bindings to work.
    JS::Promise const* promise() const { return m_promise; }
    JS::Value reason() const { return m_reason; }

private:
    virtual void visit_edges(Cell::Visitor&) override;

    JS::Promise* m_promise { nullptr };
    JS::Value m_reason;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::HTML::PromiseRejectionEvent& object) { return &object; }
using PromiseRejectionEventWrapper = Web::HTML::PromiseRejectionEvent;
}
