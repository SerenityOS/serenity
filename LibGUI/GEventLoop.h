#pragma once

#include "GEvent.h"
#include <AK/OwnPtr.h>
#include <AK/Vector.h>

class GObject;
class GWindow;
struct GUI_Event;

class GEventLoop {
public:
    GEventLoop();
    ~GEventLoop();

    int exec();

    void post_event(GObject* receiver, OwnPtr<GEvent>&&);

    static GEventLoop& main();

    static void initialize();

    bool running() const { return m_running; }

private:
    void wait_for_event();
    void handle_paint_event(const GUI_Event&, GWindow&);
    void handle_mouse_event(const GUI_Event&, GWindow&);
    void handle_key_event(const GUI_Event&, GWindow&);

    struct QueuedEvent {
        GObject* receiver { nullptr };
        OwnPtr<GEvent> event;
    };
    Vector<QueuedEvent> m_queued_events;

    int m_event_fd { -1 };
    bool m_running { false };
};
