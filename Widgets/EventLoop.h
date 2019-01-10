#pragma once

#include "Event.h"
#include <AK/OwnPtr.h>
#include <AK/Vector.h>

#ifdef USE_SDL
#include <SDL.h>
#endif

class Object;

class EventLoop {
public:
    EventLoop();
    ~EventLoop();

    int exec();

    void postEvent(Object* receiver, OwnPtr<Event>&&);

    static EventLoop& main();

    static void initialize();

private:
    void waitForEvent();

#ifdef USE_SDL
    void handleKeyEvent(Event::Type, const SDL_KeyboardEvent&);
#endif

    struct QueuedEvent {
        Object* receiver { nullptr };
        OwnPtr<Event> event;
    };
    Vector<QueuedEvent> m_queuedEvents;
};
