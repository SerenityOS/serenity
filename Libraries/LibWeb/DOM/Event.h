#pragma once

#include <AK/RefCounted.h>
#include <AK/String.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web {

class Event
    : public RefCounted<Event>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::EventWrapper;

    static NonnullRefPtr<Event> create(String event_name)
    {
        return adopt(*new Event(move(event_name)));
    }

    virtual ~Event() {}

    const String& name() const { return m_event_name; }

    virtual bool is_mouse_event() const { return false; }

protected:
    Event(String event_name)
        : m_event_name(move(event_name))
    {
    }

private:
    String m_event_name;
};

}
