#include "EventLoop.h"
#include "Event.h"
#include "Object.h"

static EventLoop* s_mainEventLoop;

void EventLoop::initialize()
{
    s_mainEventLoop = nullptr;
}

EventLoop::EventLoop()
{
    if (!s_mainEventLoop)
        s_mainEventLoop = this;
}

EventLoop::~EventLoop()
{
}

EventLoop& EventLoop::main()
{
    ASSERT(s_mainEventLoop);
    return *s_mainEventLoop;
}

int EventLoop::exec()
{
    m_running = true;
    for (;;) {
        if (m_queuedEvents.is_empty())
            waitForEvent();
        Vector<QueuedEvent> events;
        {
            events = move(m_queuedEvents);
        }
        for (auto& queuedEvent : events) {
            auto* receiver = queuedEvent.receiver;
            auto& event = *queuedEvent.event;
            //printf("EventLoop: Object{%p} event %u (%s)\n", receiver, (unsigned)event.type(), event.name());
            if (!receiver) {
                switch (event.type()) {
                case Event::Quit:
                    ASSERT_NOT_REACHED();
                    return 0;
                default:
                    dbgprintf("event type %u with no receiver :(\n", event.type());
                    ASSERT_NOT_REACHED();
                    return 1;
                }
            } else {
                receiver->event(event);
            }
        }
    }
}

void EventLoop::postEvent(Object* receiver, OwnPtr<Event>&& event)
{
    //printf("EventLoop::postEvent: {%u} << receiver=%p, event=%p\n", m_queuedEvents.size(), receiver, event.ptr());
    m_queuedEvents.append({ receiver, move(event) });
}

void EventLoop::waitForEvent()
{
}
