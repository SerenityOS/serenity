/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

struct FocusEventInit : public UIEventInit {
    JS::GCPtr<DOM::EventTarget> related_target;
};

class FocusEvent final : public UIEvent {
    WEB_PLATFORM_OBJECT(FocusEvent, UIEvent);

public:
    static FocusEvent* create_with_global_object(HTML::Window&, FlyString const& event_name, FocusEventInit const& event_init);

    FocusEvent(HTML::Window&, FlyString const& event_name, FocusEventInit const&);
    virtual ~FocusEvent() override;
};

}
