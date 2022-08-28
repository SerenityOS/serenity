/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MessageEventPrototype.h>
#include <LibWeb/HTML/MessageEvent.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

MessageEvent* MessageEvent::create(HTML::Window& window_object, FlyString const& event_name, MessageEventInit const& event_init)
{
    return window_object.heap().allocate<MessageEvent>(window_object.realm(), window_object, event_name, event_init);
}

MessageEvent* MessageEvent::create_with_global_object(HTML::Window& window_object, FlyString const& event_name, MessageEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

MessageEvent::MessageEvent(HTML::Window& window_object, FlyString const& event_name, MessageEventInit const& event_init)
    : DOM::Event(window_object, event_name, event_init)
    , m_data(event_init.data)
    , m_origin(event_init.origin)
    , m_last_event_id(event_init.last_event_id)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::MessageEventPrototype>("MessageEvent"));
}

MessageEvent::~MessageEvent() = default;

void MessageEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_data);
}

}
