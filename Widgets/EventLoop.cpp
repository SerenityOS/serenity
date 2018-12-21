#include "EventLoop.h"
#include "Event.h"
#include "Object.h"

static EventLoop* s_mainEventLoop;

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
    for (;;) {
        if (m_queuedEvents.is_empty())
            waitForEvent();
        auto events = std::move(m_queuedEvents);
        for (auto& queuedEvent : events) {
            auto* receiver = queuedEvent.receiver;
            auto& event = *queuedEvent.event;
            //printf("EventLoop: Object{%p} event %u (%s)\n", receiver, (unsigned)event.type(), event.name());
            if (!receiver) {
                switch (event.type()) {
                case Event::Quit:
                    return 0;
                default:
                    printf("event type %u with no receiver :(\n", event.type());
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
    m_queuedEvents.append({ receiver, std::move(event) });
}

