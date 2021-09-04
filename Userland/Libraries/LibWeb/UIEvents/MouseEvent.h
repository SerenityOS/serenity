/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypeCasts.h>
#include <LibWeb/UIEvents/UIEvent.h>

namespace Web::UIEvents {

class MouseEvent final : public UIEvents::UIEvent {
public:
    using WrapperType = Bindings::MouseEventWrapper;

    static NonnullRefPtr<MouseEvent> create(FlyString const& event_name, i32 offset_x, i32 offset_y, i32 client_x, i32 client_y)
    {
        return adopt_ref(*new MouseEvent(event_name, offset_x, offset_y, client_x, client_y));
    }

    virtual ~MouseEvent() override;

    i32 offset_x() const { return m_offset_x; }
    i32 offset_y() const { return m_offset_y; }
    i32 client_x() const { return m_client_x; }
    i32 client_y() const { return m_client_y; }

protected:
    MouseEvent(FlyString const& event_name, i32 offset_x, i32 offset_y, i32 client_x, i32 client_y);

private:
    void set_event_characteristics();

    i32 m_offset_x { 0 };
    i32 m_offset_y { 0 };
    i32 m_client_x { 0 };
    i32 m_client_y { 0 };
};

}
