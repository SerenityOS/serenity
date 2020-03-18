#pragma once

#include <AK/Noncopyable.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/Forward.h>

namespace Web {

class EventTarget {
    AK_MAKE_NONCOPYABLE(EventTarget);
    AK_MAKE_NONMOVABLE(EventTarget);

public:
    virtual ~EventTarget();

    void ref() { ref_event_target(); }
    void unref() { unref_event_target(); }

    void add_event_listener(String event_name, NonnullRefPtr<EventListener>);

    virtual void dispatch_event(String event_name) = 0;

    struct EventListenerRegistration {
        String event_name;
        NonnullRefPtr<EventListener> listener;
    };

    const Vector<EventListenerRegistration>& listeners() const { return m_listeners; }

protected:
    EventTarget();

    virtual void ref_event_target() = 0;
    virtual void unref_event_target() = 0;

private:
    Vector<EventListenerRegistration> m_listeners;
};

}
