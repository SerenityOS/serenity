/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

struct InputEventInit : public UIEventInit {
    Optional<String> data;
    bool is_composing { false };
    String input_type {};
};

class InputEvent final : public UIEvent {
    WEB_PLATFORM_OBJECT(InputEvent, UIEvent);
    JS_DECLARE_ALLOCATOR(InputEvent);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<InputEvent>> construct_impl(JS::Realm&, FlyString const& event_name, InputEventInit const& event_init);

    virtual ~InputEvent() override;

    // https://w3c.github.io/uievents/#dom-inputevent-data
    Optional<String> data() const { return m_data; }

    // https://w3c.github.io/uievents/#dom-inputevent-iscomposing
    bool is_composing() const { return m_is_composing; }

    // https://w3c.github.io/uievents/#dom-inputevent-inputtype
    String input_type() const { return m_input_type; }

private:
    InputEvent(JS::Realm&, FlyString const& event_name, InputEventInit const&);

    virtual void initialize(JS::Realm&) override;

    Optional<String> m_data;
    bool m_is_composing;
    String m_input_type;
};

}
