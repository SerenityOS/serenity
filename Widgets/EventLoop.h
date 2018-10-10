#pragma once

#include <AK/OwnPtr.h>
#include <AK/Vector.h>

class Event;
class Object;

class EventLoop {
public:
    virtual ~EventLoop();

    int exec();

    virtual void waitForEvent() = 0;

    void postEvent(Object* receiver, OwnPtr<Event>&&);

    static EventLoop& main();

protected:
    EventLoop();

private:
    struct QueuedEvent {
        Object* receiver { nullptr };
        OwnPtr<Event> event;
    };
    Vector<QueuedEvent> m_queuedEvents;
};
