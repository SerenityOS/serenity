/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/UIEvents/EventNames.h>
#include <LibWeb/UIEvents/MouseEvent.h>

namespace Web::UIEvents {

MouseEvent::MouseEvent(const FlyString& event_name, double offset_x, double offset_y, double client_x, double client_y)
    : UIEvent(event_name)
    , m_offset_x(offset_x)
    , m_offset_y(offset_y)
    , m_client_x(client_x)
    , m_client_y(client_y)
{
    set_event_characteristics();
}

MouseEvent::~MouseEvent()
{
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
