#pragma once

#include <LibWeb/DOM/Event.h>

namespace Web {

class MouseEvent final : public Event {
public:
    using WrapperType = Bindings::MouseEventWrapper;

    static NonnullRefPtr<MouseEvent> create(String event_name, i32 offset_x, i32 offset_y)
    {
        return adopt(*new MouseEvent(move(event_name), offset_x, offset_y));
    }

    virtual ~MouseEvent() override {}

    i32 offset_x() const { return m_offset_x; }
    i32 offset_y() const { return m_offset_y; }

protected:
    MouseEvent(String event_name, i32 offset_x, i32 offset_y)
        : Event(move(event_name))
        , m_offset_x(offset_x)
        , m_offset_y(offset_y)
    {
    }

private:
    virtual bool is_mouse_event() const override { return true; }

    i32 m_offset_x { 0 };
    i32 m_offset_y { 0 };
};

}
