/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MediaQueryListEventPrototype.h>
#include <LibWeb/CSS/MediaQueryListEvent.h>
#include <LibWeb/HTML/Window.h>

namespace Web::CSS {

MediaQueryListEvent* MediaQueryListEvent::create(HTML::Window& window_object, FlyString const& event_name, MediaQueryListEventInit const& event_init)
{
    return window_object.heap().allocate<MediaQueryListEvent>(window_object.realm(), window_object, event_name, event_init);
}

MediaQueryListEvent* MediaQueryListEvent::create_with_global_object(HTML::Window& window_object, FlyString const& event_name, MediaQueryListEventInit const& event_init)
{
    return create(window_object, event_name, event_init);
}

MediaQueryListEvent::MediaQueryListEvent(HTML::Window& window_object, FlyString const& event_name, MediaQueryListEventInit const& event_init)
    : DOM::Event(window_object, event_name, event_init)
    , m_media(event_init.media)
    , m_matches(event_init.matches)
{
    set_prototype(&window_object.cached_web_prototype("MediaQueryListEvent"));
}

MediaQueryListEvent::~MediaQueryListEvent() = default;

}
