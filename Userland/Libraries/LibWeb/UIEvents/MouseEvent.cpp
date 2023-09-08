/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/KeyCode.h>
#include <LibGUI/Event.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::UIEvents {

MouseEvent::MouseEvent(JS::Realm& realm, FlyString const& event_name, MouseEventInit const& event_init, double page_x, double page_y, double offset_x, double offset_y, unsigned modifiers)
    : UIEvent(realm, event_name, event_init)
    , m_screen_x(event_init.screen_x)
    , m_screen_y(event_init.screen_y)
    , m_page_x(page_x)
    , m_page_y(page_y)
    , m_client_x(event_init.client_x)
    , m_client_y(event_init.client_y)
    , m_offset_x(offset_x)
    , m_offset_y(offset_y)
    , m_ctrl_key(modifiers & Mod_Ctrl)
    , m_shift_key(modifiers & Mod_Shift)
    , m_alt_key(modifiers & Mod_Alt)
    , m_meta_key(false) // FIXME: Implement meta key
    , m_movement_x(event_init.movement_x)
    , m_movement_y(event_init.movement_y)
    , m_button(event_init.button)
    , m_buttons(event_init.buttons)
{
    set_event_characteristics();
}

MouseEvent::~MouseEvent() = default;

void MouseEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::MouseEventPrototype>(realm, "MouseEvent"));
}

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

JS::NonnullGCPtr<MouseEvent> MouseEvent::create(JS::Realm& realm, FlyString const& event_name, MouseEventInit const& event_init, double page_x, double page_y, double offset_x, double offset_y, unsigned modifiers)
{
    return realm.heap().allocate<MouseEvent>(realm, realm, event_name, event_init, page_x, page_y, offset_x, offset_y, modifiers);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<MouseEvent>> MouseEvent::create_from_platform_event(JS::Realm& realm, FlyString const& event_name, CSSPixelPoint screen, CSSPixelPoint page, CSSPixelPoint client, CSSPixelPoint offset, Optional<CSSPixelPoint> movement, unsigned button, unsigned buttons, unsigned modifiers)
{
    MouseEventInit event_init {};
    event_init.screen_x = screen.x().to_double();
    event_init.screen_y = screen.y().to_double();
    event_init.client_x = client.x().to_double();
    event_init.client_y = client.y().to_double();
    if (movement.has_value()) {
        event_init.movement_x = movement.value().x().to_double();
        event_init.movement_y = movement.value().y().to_double();
    }
    event_init.button = determine_button(button);
    event_init.buttons = buttons;
    return MouseEvent::create(realm, event_name, event_init, page.x().to_double(), page.y().to_double(), offset.x().to_double(), offset.y().to_double(), modifiers);
}

void MouseEvent::set_event_characteristics()
{
    if (type().is_one_of(EventNames::mousedown, EventNames::mousemove, EventNames::mouseout, EventNames::mouseover, EventNames::mouseup, HTML::EventNames::click, EventNames::dblclick, EventNames::contextmenu)) {
        set_bubbles(true);
        set_cancelable(true);
        set_composed(true);
    }
}

}
