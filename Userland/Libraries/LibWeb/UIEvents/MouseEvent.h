/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypeCasts.h>
#include <LibWeb/UIEvents/EventModifier.h>
#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

struct MouseEventInit : public EventModifierInit {
    double offset_x = 0;
    double offset_y = 0;
    double client_x = 0;
    double client_y = 0;

    i16 button = 0;
};

class MouseEvent final : public UIEvent {
public:
    using WrapperType = Bindings::MouseEventWrapper;

    static NonnullRefPtr<MouseEvent> create(FlyString const& event_name, MouseEventInit const& event_init = {})
    {
        return adopt_ref(*new MouseEvent(event_name, event_init));
    }

    static NonnullRefPtr<MouseEvent> create_from_platform_event(FlyString const& event_name, double offset_x, double offset_y, double client_x, double client_y, unsigned mouse_button = 1);

    virtual ~MouseEvent() override = default;

    double offset_x() const { return m_offset_x; }
    double offset_y() const { return m_offset_y; }

    double client_x() const { return m_client_x; }
    double client_y() const { return m_client_y; }

    double x() const { return client_x(); }
    double y() const { return client_y(); }

    i16 button() const { return m_button; }

    virtual u32 which() const override { return m_button + 1; }

private:
    MouseEvent(FlyString const& event_name, MouseEventInit const& event_init);

    void set_event_characteristics();

    double m_offset_x { 0 };
    double m_offset_y { 0 };
    double m_client_x { 0 };
    double m_client_y { 0 };
    i16 m_button { 0 };
};

}
