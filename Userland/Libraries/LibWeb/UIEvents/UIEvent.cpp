/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Window.h>
#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

UIEvent* UIEvent::create(HTML::Window& window_object, FlyString const& event_name)
{
    return window_object.heap().allocate<UIEvent>(window_object.realm(), window_object, event_name);
}

UIEvent* UIEvent::create_with_global_object(HTML::Window& window_object, FlyString const& event_name, UIEventInit const& event_init)
{
    return window_object.heap().allocate<UIEvent>(window_object.realm(), window_object, event_name, event_init);
}

UIEvent::UIEvent(HTML::Window& window_object, FlyString const& event_name)
    : Event(window_object, event_name)
{
    set_prototype(&window_object.cached_web_prototype("UIEvent"));
}

UIEvent::UIEvent(HTML::Window& window_object, FlyString const& event_name, UIEventInit const& event_init)
    : Event(window_object, event_name, event_init)
    , m_view(event_init.view)
    , m_detail(event_init.detail)
{
    set_prototype(&window_object.cached_web_prototype("UIEvent"));
}

UIEvent::~UIEvent() = default;

void UIEvent::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_view.ptr());
}

}
