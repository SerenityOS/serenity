/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

struct FocusEventInit : public UIEventInit {
    RefPtr<DOM::EventTarget> related_target;
};

class FocusEvent final : public UIEvent {
    JS_OBJECT(FocusEvent, UIEvent);

public:
    static FocusEvent* create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, FocusEventInit const& event_init);

    FocusEvent(Bindings::WindowObject&, FlyString const& event_name, FocusEventInit const&);
    virtual ~FocusEvent() override;

    FocusEvent& impl() { return *this; }
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::UIEvents::FocusEvent& object) { return &object; }
using FocusEventWrapper = Web::UIEvents::FocusEvent;
}
