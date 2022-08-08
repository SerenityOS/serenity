/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MediaQueryListEventPrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/CSS/MediaQueryListEvent.h>

namespace Web::CSS {

MediaQueryListEvent* MediaQueryListEvent::create(Bindings::WindowObject& window_object, FlyString const& event_name, MediaQueryListEventInit const& event_init)
{
    return window_object.heap().allocate<MediaQueryListEvent>(window_object.realm(), window_object, event_name, event_init);
}

MediaQueryListEvent* MediaQueryListEvent::create_with_global_object(Bindings::WindowObject& window_object, FlyString const& event_name, MediaQueryListEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

MediaQueryListEvent::MediaQueryListEvent(Bindings::WindowObject& window_object, FlyString const& event_name, MediaQueryListEventInit const& event_init)
    : DOM::Event(window_object, event_name, event_init)
    , m_media(event_init.media)
    , m_matches(event_init.matches)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::MediaQueryListEventPrototype>("MediaQueryListEvent"));
}

MediaQueryListEvent::~MediaQueryListEvent() = default;

}
