/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/PromiseRejectionEvent.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

PromiseRejectionEvent* PromiseRejectionEvent::create(HTML::Window& window_object, FlyString const& event_name, PromiseRejectionEventInit const& event_init)
{
    return window_object.heap().allocate<PromiseRejectionEvent>(window_object.realm(), window_object, event_name, event_init);
}

PromiseRejectionEvent* PromiseRejectionEvent::create_with_global_object(HTML::Window& window_object, FlyString const& event_name, PromiseRejectionEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

PromiseRejectionEvent::PromiseRejectionEvent(HTML::Window& window_object, FlyString const& event_name, PromiseRejectionEventInit const& event_init)
    : DOM::Event(window_object, event_name, event_init)
    , m_promise(const_cast<JS::Promise*>(event_init.promise.cell()))
    , m_reason(event_init.reason)
{
    set_prototype(&window_object.cached_web_prototype("PromiseRejectionEvent"));
}

PromiseRejectionEvent::~PromiseRejectionEvent() = default;

void PromiseRejectionEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_promise);
    visitor.visit(m_reason);
}

}
