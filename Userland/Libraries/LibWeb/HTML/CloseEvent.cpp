/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CloseEventPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/HTML/CloseEvent.h>

namespace Web::HTML {

CloseEvent* CloseEvent::create(Bindings::WindowObject& window_object, FlyString const& event_name, CloseEventInit const& event_init)
{
    return window_object.heap().allocate<CloseEvent>(window_object.realm(), window_object, event_name, event_init);
}

CloseEvent* CloseEvent::create_with_global_object(Bindings::WindowObject& window_object, FlyString const& event_name, CloseEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

CloseEvent::CloseEvent(Bindings::WindowObject& window_object, FlyString const& event_name, CloseEventInit const& event_init)
    : DOM::Event(window_object, event_name, event_init)
    , m_was_clean(event_init.was_clean)
    , m_code(event_init.code)
    , m_reason(event_init.reason)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::CloseEventPrototype>("CloseEvent"));
}

CloseEvent::~CloseEvent() = default;

}
