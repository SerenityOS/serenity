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

//#define GEVENTLOOP_DEBUG

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
    m_event_fd = open("/dev/gui_events", O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    if (m_event_fd < 0) {
        perror("GEventLoop::exec(): open");
        exit(1);
    }

    m_running = true;
    for (;;) {
        if (m_queued_events.is_empty())
            wait_for_event();
        Vector<QueuedEvent> events = move(m_queued_events);
        for (auto& queued_event : events) {
            auto* receiver = queued_event.receiver;
            auto& event = *queued_event.event;
#ifdef GEVENTLOOP_DEBUG
            dbgprintf("GEventLoop: GObject{%p} event %u (%s)\n", receiver, (unsigned)event.type(), event.name());
#endif
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
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("GEventLoop::post_event: {%u} << receiver=%p, event=%p\n", m_queued_events.size(), receiver, event.ptr());
#endif
    m_queued_events.append({ receiver, move(event) });
}

void GEventLoop::handle_paint_event(const GUI_Event& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x Paint [%d,%d %dx%d]\n", event.window_id, event.paint.rect.location.x, event.paint.rect.location.y, event.paint.rect.size.width, event.paint.rect.size.height);
#endif
    post_event(&window, make<GPaintEvent>(event.paint.rect));
}

void GEventLoop::handle_window_activation_event(const GUI_Event& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x WindowActivation\n", event.window_id);
#endif
    post_event(&window, make<GEvent>(event.type == GUI_Event::Type::WindowActivated ? GEvent::WindowBecameActive : GEvent::WindowBecameInactive));
}

void GEventLoop::handle_key_event(const GUI_Event& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x KeyEvent character=0x%b\n", event.window_id, event.key.character);
#endif
    auto key_event = make<GKeyEvent>(event.type == GUI_Event::Type::KeyDown ? GEvent::KeyDown : GEvent::KeyUp, event.key.key);
    key_event->m_alt = event.key.alt;
    key_event->m_ctrl = event.key.ctrl;
    key_event->m_shift = event.key.shift;
    if (event.key.character != '\0')
        key_event->m_text = String(&event.key.character, 1);
    post_event(&window, move(key_event));
}

void GEventLoop::handle_mouse_event(const GUI_Event& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x MouseEvent %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y);
#endif
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
    post_event(&window, make<GMouseEvent>(type, event.mouse.position, event.mouse.buttons, button));
}

void GEventLoop::wait_for_event()
{
    fd_set rfds;
    FD_ZERO(&rfds);
    FD_SET(m_event_fd, &rfds);
    struct timeval timeout = { 0, 0 };
    int rc = select(m_event_fd + 1, &rfds, nullptr, nullptr, m_queued_events.is_empty() ? nullptr : &timeout);
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
            continue;
        }
        switch (event.type) {
        case GUI_Event::Type::Paint:
            handle_paint_event(event, *window);
            break;
        case GUI_Event::Type::MouseDown:
        case GUI_Event::Type::MouseUp:
        case GUI_Event::Type::MouseMove:
            handle_mouse_event(event, *window);
            break;
        case GUI_Event::Type::WindowActivated:
        case GUI_Event::Type::WindowDeactivated:
            handle_window_activation_event(event, *window);
            break;
        case GUI_Event::Type::KeyDown:
        case GUI_Event::Type::KeyUp:
            handle_key_event(event, *window);
            break;
        }
    }
}
