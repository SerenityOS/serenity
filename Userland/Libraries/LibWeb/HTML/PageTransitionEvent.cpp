/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/PageTransitionEventPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/HTML/PageTransitionEvent.h>

namespace Web::HTML {

PageTransitionEvent* PageTransitionEvent::create(Bindings::WindowObject& window_object, FlyString const& event_name, PageTransitionEventInit const& event_init)
{
    return window_object.heap().allocate<PageTransitionEvent>(window_object.realm(), window_object, event_name, event_init);
}

PageTransitionEvent* PageTransitionEvent::create_with_global_object(Bindings::WindowObject& window_object, FlyString const& event_name, PageTransitionEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

PageTransitionEvent::PageTransitionEvent(Bindings::WindowObject& window_object, FlyString const& event_name, PageTransitionEventInit const& event_init)
    : DOM::Event(window_object, event_name, event_init)
    , m_persisted(event_init.persisted)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::PageTransitionEventPrototype>("PageTransitionEvent"));
}

PageTransitionEvent::~PageTransitionEvent() = default;

}
