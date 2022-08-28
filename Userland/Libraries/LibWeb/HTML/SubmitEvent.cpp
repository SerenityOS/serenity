/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/SubmitEventPrototype.h>
#include <LibWeb/HTML/SubmitEvent.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

SubmitEvent* SubmitEvent::create(HTML::Window& window_object, FlyString const& event_name, SubmitEventInit const& event_init)
{
    return window_object.heap().allocate<SubmitEvent>(window_object.realm(), window_object, event_name, event_init);
}

SubmitEvent* SubmitEvent::create_with_global_object(HTML::Window& window_object, FlyString const& event_name, SubmitEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

SubmitEvent::SubmitEvent(HTML::Window& window_object, FlyString const& event_name, SubmitEventInit const& event_init)
    : DOM::Event(window_object, event_name, event_init)
    , m_submitter(event_init.submitter)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::SubmitEventPrototype>("SubmitEvent"));
}

SubmitEvent::~SubmitEvent() = default;

void SubmitEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_submitter.ptr());
}

}
