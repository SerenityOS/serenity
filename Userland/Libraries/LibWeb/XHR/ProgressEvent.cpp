/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/ProgressEventPrototype.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/XHR/ProgressEvent.h>

namespace Web::XHR {

ProgressEvent* ProgressEvent::create(HTML::Window& window_object, FlyString const& event_name, ProgressEventInit const& event_init)
{
    return window_object.heap().allocate<ProgressEvent>(window_object.realm(), window_object, event_name, event_init);
}

ProgressEvent* ProgressEvent::create_with_global_object(HTML::Window& window_object, FlyString const& event_name, ProgressEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

ProgressEvent::ProgressEvent(HTML::Window& window_object, FlyString const& event_name, ProgressEventInit const& event_init)
    : Event(window_object, event_name, event_init)
    , m_length_computable(event_init.length_computable)
    , m_loaded(event_init.loaded)
    , m_total(event_init.total)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::ProgressEventPrototype>("ProgressEvent"));
}

ProgressEvent::~ProgressEvent() = default;

}
