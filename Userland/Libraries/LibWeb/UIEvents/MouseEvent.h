/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypeCasts.h>
#include <LibWeb/PixelUnits.h>
#include <LibWeb/UIEvents/EventModifier.h>
#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

struct MouseEventInit : public EventModifierInit {
    double screen_x = 0;
    double screen_y = 0;
    double client_x = 0;
    double client_y = 0;
    double movement_x = 0;
    double movement_y = 0;
    i16 button = 0;
    u16 buttons = 0;
};

class MouseEvent : public UIEvent {
    WEB_PLATFORM_OBJECT(MouseEvent, UIEvent);

public:
    [[nodiscard]] static JS::NonnullGCPtr<MouseEvent> create(JS::Realm&, FlyString const& event_name, MouseEventInit const& = {}, double page_x = 0, double page_y = 0, double offset_x = 0, double offset_y = 0, unsigned modifiers = 0);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<MouseEvent>> create_from_platform_event(JS::Realm&, FlyString const& event_name, CSSPixelPoint screen, CSSPixelPoint page, CSSPixelPoint client, CSSPixelPoint offset, Optional<CSSPixelPoint> movement, unsigned button, unsigned buttons, unsigned modifiers);

    virtual ~MouseEvent() override;

    double screen_x() const { return m_screen_x; }
    double screen_y() const { return m_screen_y; }

    double page_x() const { return m_page_x; }
    double page_y() const { return m_page_y; }

    double client_x() const { return m_client_x; }
    double client_y() const { return m_client_y; }

    double x() const { return client_x(); }
    double y() const { return client_y(); }

    double offset_x() const { return m_offset_x; }
    double offset_y() const { return m_offset_y; }

    bool ctrl_key() const { return m_ctrl_key; }
    bool shift_key() const { return m_shift_key; }
    bool alt_key() const { return m_alt_key; }
    bool meta_key() const { return m_meta_key; }

    double movement_x() const { return m_movement_x; }
    double movement_y() const { return m_movement_y; }

    i16 button() const { return m_button; }
    u16 buttons() const { return m_buttons; }

    virtual u32 which() const override { return m_button + 1; }

protected:
    MouseEvent(JS::Realm&, FlyString const& event_name, MouseEventInit const& event_init, double page_x, double page_y, double offset_x, double offset_y, unsigned modifiers);

    virtual void initialize(JS::Realm&) override;

private:
    void set_event_characteristics();

    double m_screen_x { 0 };
    double m_screen_y { 0 };
    double m_page_x { 0 };
    double m_page_y { 0 };
    double m_client_x { 0 };
    double m_client_y { 0 };
    double m_offset_x { 0 };
    double m_offset_y { 0 };
    bool m_ctrl_key { false };
    bool m_shift_key { false };
    bool m_alt_key { false };
    bool m_meta_key { false };
    double m_movement_x { 0 };
    double m_movement_y { 0 };
    i16 m_button { 0 };
    u16 m_buttons { 0 };
};

}
