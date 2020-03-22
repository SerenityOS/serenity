#pragma once

#include <AK/FlyString.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web {

class Event
    : public RefCounted<Event>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::EventWrapper;

    static NonnullRefPtr<Event> create(const FlyString& event_name)
    {
        return adopt(*new Event(event_name));
    }

    virtual ~Event() {}

    const FlyString& name() const { return m_event_name; }

    virtual bool is_mouse_event() const { return false; }

protected:
    Event(const FlyString& event_name)
        : m_event_name(event_name)
    {
    }

private:
    FlyString m_event_name;
};

}
