/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/UIEvents/FocusEvent.h>

namespace Web::UIEvents {

FocusEvent::FocusEvent(FlyString const& event_name, FocusEventInit const& event_init)
    : UIEvent(event_name)
{
    set_related_target(const_cast<DOM::EventTarget*>(event_init.related_target.ptr()));
}

FocusEvent::~FocusEvent()
{
}

}
