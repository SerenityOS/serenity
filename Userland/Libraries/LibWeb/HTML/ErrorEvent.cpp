/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/ErrorEventPrototype.h>
#include <LibWeb/HTML/ErrorEvent.h>
#include <LibWeb/HTML/Window.h>

namespace Web::HTML {

ErrorEvent* ErrorEvent::create(HTML::Window& window_object, FlyString const& event_name, ErrorEventInit const& event_init)
{
    return window_object.heap().allocate<ErrorEvent>(window_object.realm(), window_object, event_name, event_init);
}

ErrorEvent* ErrorEvent::create_with_global_object(HTML::Window& window_object, FlyString const& event_name, ErrorEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

ErrorEvent::ErrorEvent(HTML::Window& window_object, FlyString const& event_name, ErrorEventInit const& event_init)
    : DOM::Event(window_object, event_name)
    , m_message(event_init.message)
    , m_filename(event_init.filename)
    , m_lineno(event_init.lineno)
    , m_colno(event_init.colno)
    , m_error(event_init.error)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::ErrorEventPrototype>("ErrorEvent"));
}

ErrorEvent::~ErrorEvent() = default;

void ErrorEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_error);
}

}
