/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::UIEvents {

MouseEvent::MouseEvent(HTML::Window& window_object, FlyString const& event_name, MouseEventInit const& event_init)
    : UIEvent(window_object, event_name, event_init)
    , m_offset_x(event_init.offset_x)
    , m_offset_y(event_init.offset_y)
    , m_client_x(event_init.client_x)
    , m_client_y(event_init.client_y)
    , m_button(event_init.button)
{
    set_prototype(&window_object.cached_web_prototype("MouseEvent"));
    set_event_characteristics();
}

MouseEvent::~MouseEvent() = default;

// https://www.w3.org/TR/uievents/#dom-mouseevent-button
static i16 determine_button(unsigned mouse_button)
{
    switch (mouse_button) {
    case GUI::MouseButton::Primary:
        return 0;
    case GUI::MouseButton::Middle:
        return 1;
    case GUI::MouseButton::Secondary:
        return 2;
    case GUI::MouseButton::Backward:
        return 3;
    case GUI::MouseButton::Forward:
        return 4;
    default:
        VERIFY_NOT_REACHED();
    }
}

MouseEvent* MouseEvent::create(HTML::Window& window_object, FlyString const& event_name, MouseEventInit const& event_init)
{
    return window_object.heap().allocate<MouseEvent>(window_object.realm(), window_object, event_name, event_init);
}

MouseEvent* MouseEvent::create_from_platform_event(HTML::Window& window_object, FlyString const& event_name, double offset_x, double offset_y, double client_x, double client_y, unsigned mouse_button)
{
    MouseEventInit event_init {};
    event_init.offset_x = offset_x;
    event_init.offset_y = offset_y;
    event_init.client_x = client_x;
    event_init.client_y = client_y;
    event_init.button = determine_button(mouse_button);
    return MouseEvent::create(window_object, event_name, event_init);
}

void MouseEvent::set_event_characteristics()
{
    if (type().is_one_of(EventNames::mousedown, EventNames::mousemove, EventNames::mouseout, EventNames::mouseover, EventNames::mouseup, HTML::EventNames::click)) {
        set_bubbles(true);
        set_cancelable(true);
        set_composed(true);
    }
}

}
