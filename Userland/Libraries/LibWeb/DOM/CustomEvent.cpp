/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/CustomEvent.h>
#include <LibWeb/HTML/Window.h>

namespace Web::DOM {

CustomEvent* CustomEvent::create(HTML::Window& window_object, FlyString const& event_name, CustomEventInit const& event_init)
{
    return window_object.heap().allocate<CustomEvent>(window_object.realm(), window_object, event_name, event_init);
}

CustomEvent* CustomEvent::create_with_global_object(HTML::Window& window_object, FlyString const& event_name, CustomEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

CustomEvent::CustomEvent(HTML::Window& window_object, FlyString const& event_name)
    : Event(window_object, event_name)
{
    set_prototype(&window_object.cached_web_prototype("CustomEvent"));
}

CustomEvent::CustomEvent(HTML::Window& window_object, FlyString const& event_name, CustomEventInit const& event_init)
    : Event(window_object, event_name, event_init)
    , m_detail(event_init.detail)
{
    set_prototype(&window_object.cached_web_prototype("CustomEvent"));
}

CustomEvent::~CustomEvent() = default;

void CustomEvent::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_detail);
}

// https://dom.spec.whatwg.org/#dom-customevent-initcustomevent
void CustomEvent::init_custom_event(String const& type, bool bubbles, bool cancelable, JS::Value detail)
{
    // 1. If this’s dispatch flag is set, then return.
    if (dispatched())
        return;

    // 2. Initialize this with type, bubbles, and cancelable.
    initialize_event(type, bubbles, cancelable);

    // 3. Set this’s detail attribute to detail.
    m_detail = detail;
}

}
