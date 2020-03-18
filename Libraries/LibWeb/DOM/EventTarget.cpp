#include <LibWeb/DOM/EventListener.h>
#include <LibWeb/DOM/EventTarget.h>

namespace Web {

EventTarget::EventTarget()
{
}

EventTarget::~EventTarget()
{
}

void EventTarget::add_event_listener(String event_name, NonnullRefPtr<EventListener> listener)
{
    m_listeners.append({ move(event_name), move(listener) });
}

}
