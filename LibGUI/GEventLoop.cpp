#include "GEventLoop.h"
#include "GEvent.h"
#include "GObject.h"

static GEventLoop* s_mainGEventLoop;

void GEventLoop::initialize()
{
    s_mainGEventLoop = nullptr;
}

GEventLoop::GEventLoop()
{
    if (!s_mainGEventLoop)
        s_mainGEventLoop = this;
}

GEventLoop::~GEventLoop()
{
}

GEventLoop& GEventLoop::main()
{
    ASSERT(s_mainGEventLoop);
    return *s_mainGEventLoop;
}

int GEventLoop::exec()
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
            //printf("GEventLoop: GObject{%p} event %u (%s)\n", receiver, (unsigned)event.type(), event.name());
            if (!receiver) {
                switch (event.type()) {
                case GEvent::Quit:
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

void GEventLoop::postEvent(GObject* receiver, OwnPtr<GEvent>&& event)
{
    //printf("GEventLoop::postGEvent: {%u} << receiver=%p, event=%p\n", m_queuedEvents.size(), receiver, event.ptr());
    m_queuedEvents.append({ receiver, move(event) });
}

void GEventLoop::waitForEvent()
{
}
