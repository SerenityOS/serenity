/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/PromiseRejectionEventPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/HTML/PromiseRejectionEvent.h>

namespace Web::HTML {

PromiseRejectionEvent* PromiseRejectionEvent::create(Bindings::WindowObject& window_object, FlyString const& event_name, PromiseRejectionEventInit const& event_init)
{
    return window_object.heap().allocate<PromiseRejectionEvent>(window_object.realm(), window_object, event_name, event_init);
}

PromiseRejectionEvent* PromiseRejectionEvent::create_with_global_object(Bindings::WindowObject& window_object, FlyString const& event_name, PromiseRejectionEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

PromiseRejectionEvent::PromiseRejectionEvent(Bindings::WindowObject& window_object, FlyString const& event_name, PromiseRejectionEventInit const& event_init)
    : DOM::Event(window_object, event_name, event_init)
    , m_promise(const_cast<JS::Promise*>(event_init.promise.cell()))
    , m_reason(event_init.reason)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::PromiseRejectionEventPrototype>("PromiseRejectionEvent"));
}

PromiseRejectionEvent::~PromiseRejectionEvent() = default;

void PromiseRejectionEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_promise);
    visitor.visit(m_reason);
}

}
