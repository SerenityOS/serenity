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

WheelEvent::WheelEvent(JS::Realm& realm, FlyString const& event_name, WheelEventInit const& event_init)
    : MouseEvent(realm, event_name, event_init)
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

WebIDL::ExceptionOr<JS::NonnullGCPtr<WheelEvent>> WheelEvent::create(JS::Realm& realm, FlyString const& event_name, WheelEventInit const& event_init)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<WheelEvent>(realm, realm, event_name, event_init));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<WheelEvent>> WheelEvent::create_from_platform_event(JS::Realm& realm, FlyString const& event_name, CSSPixels offset_x, CSSPixels offset_y, CSSPixels client_x, CSSPixels client_y, double delta_x, double delta_y, unsigned buttons, unsigned button)
{
    WheelEventInit event_init {};
    event_init.offset_x = offset_x.to_double();
    event_init.offset_y = offset_y.to_double();
    event_init.client_x = client_x.to_double();
    event_init.client_y = client_y.to_double();
    event_init.button = button;
    event_init.buttons = buttons;
    event_init.delta_x = delta_x;
    event_init.delta_y = delta_y;
    event_init.delta_mode = WheelDeltaMode::DOM_DELTA_PIXEL;
    return WheelEvent::create(realm, event_name, event_init);
}

void WheelEvent::set_event_characteristics()
{
    set_bubbles(true);
    set_cancelable(true);
    set_composed(true);
}

}
