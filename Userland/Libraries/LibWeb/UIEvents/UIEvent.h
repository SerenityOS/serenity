/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web::UIEvents {

class UIEvent : public DOM::Event {
public:
    using WrapperType = Bindings::UIEventWrapper;

    virtual ~UIEvent() override { }

protected:
    explicit UIEvent(FlyString const& event_name)
        : Event(event_name)
    {
    }
};

}
