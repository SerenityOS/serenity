/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/UIEventPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

UIEvent* UIEvent::create(Bindings::WindowObject& window_object, FlyString const& event_name)
{
    return window_object.heap().allocate<UIEvent>(window_object.realm(), window_object, event_name);
}

UIEvent* UIEvent::create_with_global_object(Bindings::WindowObject& window_object, FlyString const& event_name, UIEventInit const& event_init)
{
    return window_object.heap().allocate<UIEvent>(window_object.realm(), window_object, event_name, event_init);
}

UIEvent::UIEvent(Bindings::WindowObject& window_object, FlyString const& event_name)
    : Event(window_object, event_name)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::UIEventPrototype>("UIEvent"));
}

UIEvent::UIEvent(Bindings::WindowObject& window_object, FlyString const& event_name, UIEventInit const& event_init)
    : Event(window_object, event_name, event_init)
    , m_view(event_init.view)
    , m_detail(event_init.detail)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::UIEventPrototype>("UIEvent"));
}

UIEvent::~UIEvent() = default;

}
