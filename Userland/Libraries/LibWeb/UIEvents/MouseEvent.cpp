/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::UIEvents {

MouseEvent::MouseEvent(JS::Realm& realm, DeprecatedFlyString const& event_name, MouseEventInit const& event_init)
    : UIEvent(realm, event_name, event_init)
    , m_offset_x(event_init.offset_x)
    , m_offset_y(event_init.offset_y)
    , m_client_x(event_init.client_x)
    , m_client_y(event_init.client_y)
    , m_page_x(event_init.page_x)
    , m_page_y(event_init.page_y)
    , m_button(event_init.button)
    , m_buttons(event_init.buttons)
{
    set_prototype(&Bindings::cached_web_prototype(realm, "MouseEvent"));
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

MouseEvent* MouseEvent::create(JS::Realm& realm, DeprecatedFlyString const& event_name, MouseEventInit const& event_init)
{
    return realm.heap().allocate<MouseEvent>(realm, realm, event_name, event_init);
}

MouseEvent* MouseEvent::create_from_platform_event(JS::Realm& realm, DeprecatedFlyString const& event_name, CSSPixelPoint offset, CSSPixelPoint client_offset, CSSPixelPoint page_offset, unsigned buttons, unsigned mouse_button)
{
    MouseEventInit event_init {};
    event_init.offset_x = static_cast<double>(offset.x().value());
    event_init.offset_y = static_cast<double>(offset.y().value());
    event_init.client_x = static_cast<double>(client_offset.x().value());
    event_init.client_y = static_cast<double>(client_offset.y().value());
    event_init.page_x = static_cast<double>(page_offset.x().value());
    event_init.page_y = static_cast<double>(page_offset.y().value());
    event_init.button = determine_button(mouse_button);
    event_init.buttons = buttons;
    return MouseEvent::create(realm, event_name, event_init);
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
