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
public:
    using WrapperType = Bindings::FocusEventWrapper;

    virtual ~FocusEvent() override;

    static NonnullRefPtr<FocusEvent> create_with_global_object(Bindings::WindowObject&, FlyString const& event_name, FocusEventInit const& event_init)
    {
        return adopt_ref(*new FocusEvent(event_name, event_init));
    }

private:
    FocusEvent(FlyString const& event_name, FocusEventInit const&);
};

}
