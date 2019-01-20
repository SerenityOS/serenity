#pragma once

#include "GEvent.h"
#include <AK/OwnPtr.h>
#include <AK/Vector.h>

class GObject;

class GEventLoop {
public:
    GEventLoop();
    ~GEventLoop();

    int exec();

    void postEvent(GObject* receiver, OwnPtr<GEvent>&&);

    static GEventLoop& main();

    static void initialize();

    bool running() const { return m_running; }

private:
    void waitForEvent();

    struct QueuedEvent {
        GObject* receiver { nullptr };
        OwnPtr<GEvent> event;
    };
    Vector<QueuedEvent> m_queuedEvents;

    bool m_running { false };
};
