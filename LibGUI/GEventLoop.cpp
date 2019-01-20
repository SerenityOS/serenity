#include "GEventLoop.h"
#include "GEvent.h"
#include "GObject.h"
#include "GWindow.h"
#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/fcntl.h>
#include <LibC/string.h>
#include <LibC/sys/select.h>
#include <LibC/gui.h>

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
    m_event_fd = open("/dev/gui_events", O_RDONLY);
    if (m_event_fd < 0) {
        perror("GEventLoop::exec(): open");
        exit(1);
    }

    m_running = true;
    for (;;) {
        if (m_queued_events.is_empty())
            wait_for_event();
        Vector<QueuedEvent> events;
        {
            events = move(m_queued_events);
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

void GEventLoop::post_event(GObject* receiver, OwnPtr<GEvent>&& event)
{
    //printf("GEventLoop::postGEvent: {%u} << receiver=%p, event=%p\n", m_queuedEvents.size(), receiver, event.ptr());
    m_queued_events.append({ receiver, move(event) });
}

void GEventLoop::handle_paint_event(const GUI_Event& event, GWindow& window)
{
    post_event(&window, make<GPaintEvent>(event.paint.rect));
}

void GEventLoop::handle_mouse_event(const GUI_Event& event, GWindow& window)
{
    GMouseEvent::Type type;
    switch (event.type) {
    case GUI_Event::Type::MouseMove: type = GEvent::MouseMove; break;
    case GUI_Event::Type::MouseUp: type = GEvent::MouseUp; break;
    case GUI_Event::Type::MouseDown: type = GEvent::MouseDown; break;
    default: ASSERT_NOT_REACHED(); break;
    }
    GMouseButton button { GMouseButton::None };
    switch (event.mouse.button) {
    case GUI_MouseButton::NoButton: button = GMouseButton::None; break;
    case GUI_MouseButton::Left: button = GMouseButton::Left; break;
    case GUI_MouseButton::Right: button = GMouseButton::Right; break;
    case GUI_MouseButton::Middle: button = GMouseButton::Middle; break;
    default: ASSERT_NOT_REACHED(); break;
    }
    auto mouse_event = make<GMouseEvent>(type, event.mouse.position, button);
}

void GEventLoop::wait_for_event()
{
    fd_set rfds;
    FD_ZERO(&rfds);
    FD_SET(m_event_fd, &rfds);
    int rc = select(m_event_fd + 1, &rfds, nullptr, nullptr, nullptr);
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    if (!FD_ISSET(m_event_fd, &rfds))
        return;

    for (;;) {
        GUI_Event event;
        ssize_t nread = read(m_event_fd, &event, sizeof(GUI_Event));
        if (nread < 0) {
            perror("read");
            exit(1); // FIXME: This should cause EventLoop::exec() to return 1.
        }
        if (nread == 0)
            break;
        assert(nread == sizeof(event));
        auto* window = GWindow::from_window_id(event.window_id);
        if (!window) {
            dbgprintf("GEventLoop received event for invalid window ID %d\n", event.window_id);
        }
        switch (event.type) {
        case GUI_Event::Type::Paint:
            dbgprintf("WID=%x Paint [%d,%d %dx%d]\n", event.window_id, event.paint.rect.location.x, event.paint.rect.location.y, event.paint.rect.size.width, event.paint.rect.size.height); break;
            handle_paint_event(event, *window);
            break;
        case GUI_Event::Type::MouseDown:
        case GUI_Event::Type::MouseUp:
        case GUI_Event::Type::MouseMove:
            dbgprintf("WID=%x MouseEvent %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y);
            handle_mouse_event(event, *window);
            break;
        case GUI_Event::Type::WindowActivated:
            dbgprintf("WID=%x WindowActivated\n", event.window_id);
            break;
        case GUI_Event::Type::WindowDeactivated:
            dbgprintf("WID=%x WindowDeactivated\n", event.window_id);
            break;
        }
    }
}
