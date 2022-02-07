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

class MouseEvent final : public UIEvent {
public:
    using WrapperType = Bindings::MouseEventWrapper;

    static NonnullRefPtr<MouseEvent> create(const FlyString& event_name, double offset_x, double offset_y, double client_x, double client_y)
    {
        return adopt_ref(*new MouseEvent(event_name, offset_x, offset_y, client_x, client_y));
    }

    virtual ~MouseEvent() override;

    double offset_x() const { return m_offset_x; }
    double offset_y() const { return m_offset_y; }

    double client_x() const { return m_client_x; }
    double client_y() const { return m_client_y; }

    double x() const { return client_x(); }
    double y() const { return client_y(); }

protected:
    MouseEvent(const FlyString& event_name, double offset_x, double offset_y, double client_x, double client_y);

private:
    void set_event_characteristics();

    double m_offset_x { 0 };
    double m_offset_y { 0 };
    double m_client_x { 0 };
    double m_client_y { 0 };
};

}
