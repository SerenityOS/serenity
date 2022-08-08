/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/FocusEventPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/UIEvents/FocusEvent.h>

namespace Web::UIEvents {

FocusEvent* FocusEvent::create_with_global_object(Bindings::WindowObject& window_object, FlyString const& event_name, FocusEventInit const& event_init)
{
    return window_object.heap().allocate<FocusEvent>(window_object.realm(), window_object, event_name, event_init);
}

FocusEvent::FocusEvent(Bindings::WindowObject& window_object, FlyString const& event_name, FocusEventInit const& event_init)
    : UIEvent(window_object, event_name)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::FocusEventPrototype>("FocusEvent"));
    set_related_target(const_cast<DOM::EventTarget*>(event_init.related_target.ptr()));
}

FocusEvent::~FocusEvent() = default;

}
