/*
 * Copyright (c) 2022, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/WheelEvent.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::UIEvents {

WheelEvent::WheelEvent(JS::Realm& realm, FlyString const& event_name, WheelEventInit const& event_init, double page_x, double page_y, double offset_x, double offset_y, unsigned modifiers)
    : MouseEvent(realm, event_name, event_init, page_x, page_y, offset_x, offset_y, modifiers)
    , m_delta_x(event_init.delta_x)
    , m_delta_y(event_init.delta_y)
    , m_delta_mode(event_init.delta_mode)
{
    set_event_characteristics();
}

WheelEvent::~WheelEvent() = default;

void WheelEvent::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::WheelEventPrototype>(realm, "WheelEvent"));
}

JS::NonnullGCPtr<WheelEvent> WheelEvent::create(JS::Realm& realm, FlyString const& event_name, WheelEventInit const& event_init, double page_x, double page_y, double offset_x, double offset_y, unsigned modifiers)
{
    return realm.heap().allocate<WheelEvent>(realm, realm, event_name, event_init, page_x, page_y, offset_x, offset_y, modifiers);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<WheelEvent>> WheelEvent::create_from_platform_event(JS::Realm& realm, FlyString const& event_name, CSSPixelPoint screen, CSSPixelPoint page, CSSPixelPoint client, CSSPixelPoint offset, double delta_x, double delta_y, unsigned button, unsigned buttons, unsigned modifiers)
{
    WheelEventInit event_init {};
    event_init.screen_x = screen.x().to_double();
    event_init.screen_y = screen.y().to_double();
    event_init.client_x = client.x().to_double();
    event_init.client_y = client.y().to_double();
    event_init.button = button;
    event_init.buttons = buttons;
    event_init.delta_x = delta_x;
    event_init.delta_y = delta_y;
    event_init.delta_mode = WheelDeltaMode::DOM_DELTA_PIXEL;
    return WheelEvent::create(realm, event_name, event_init, page.x().to_double(), page.y().to_double(), offset.x().to_double(), offset.y().to_double(), modifiers);
}

void WheelEvent::set_event_characteristics()
{
    set_bubbles(true);
    set_cancelable(true);
    set_composed(true);
}

}
