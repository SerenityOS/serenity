/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/SubmitEventPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/HTML/SubmitEvent.h>

namespace Web::HTML {

SubmitEvent* SubmitEvent::create(Bindings::WindowObject& window_object, FlyString const& event_name, SubmitEventInit const& event_init)
{
    return window_object.heap().allocate<SubmitEvent>(window_object.realm(), window_object, event_name, event_init);
}

SubmitEvent* SubmitEvent::create_with_global_object(Bindings::WindowObject& window_object, FlyString const& event_name, SubmitEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

SubmitEvent::SubmitEvent(Bindings::WindowObject& window_object, FlyString const& event_name, SubmitEventInit const& event_init)
    : DOM::Event(window_object, event_name, event_init)
    , m_submitter(event_init.submitter)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::SubmitEventPrototype>("SubmitEvent"));
}

SubmitEvent::~SubmitEvent() = default;

}
