#include "GEventLoop.h"
#include "GEvent.h"
#include "GObject.h"
#include "GWindow.h"
#include <LibGUI/GAction.h>
#include <LibGUI/GNotifier.h>
#include <LibGUI/GMenu.h>
#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/fcntl.h>
#include <LibC/string.h>
#include <LibC/time.h>
#include <LibC/sys/select.h>
#include <LibC/sys/socket.h>
#include <LibC/errno.h>
#include <LibC/string.h>
#include <LibC/stdlib.h>

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

    m_event_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    if (m_event_fd < 0) {
        perror("socket");
        ASSERT_NOT_REACHED();
    }

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/wsportal");

    int retries = 1000;
    int rc = 0;
    while (retries) {
        rc = connect(m_event_fd, (const sockaddr*)&address, sizeof(address));
        if (rc == 0)
            break;
        dbgprintf("connect failed: %d, %s\n", errno, strerror(errno));
        sleep(1);
        --retries;
    }
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }
    dbgprintf("(%u) GEventLoop constructed :)\n", getpid());
}

GEventLoop::~GEventLoop()
{
}

GEventLoop& GEventLoop::main()
{
    ASSERT(s_mainGEventLoop);
    return *s_mainGEventLoop;
}

void GEventLoop::quit(int code)
{
    m_exit_requested = true;
    m_exit_code = code;
}

int GEventLoop::exec()
{
    m_running = true;
    for (;;) {
        if (m_exit_requested)
            return m_exit_code;
        process_unprocessed_messages();
        if (m_queued_events.is_empty()) {
            wait_for_event();
            process_unprocessed_messages();
        }
        Vector<QueuedEvent> events = move(m_queued_events);
        for (auto& queued_event : events) {
            auto* receiver = queued_event.receiver;
            auto& event = *queued_event.event;
#ifdef GEVENTLOOP_DEBUG
            dbgprintf("GEventLoop: %s{%p} event %u\n", receiver->class_name(), receiver, (unsigned)event.type());
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
    ASSERT_NOT_REACHED();
}

void GEventLoop::post_event(GObject* receiver, OwnPtr<GEvent>&& event)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("GEventLoop::post_event: {%u} << receiver=%p, event=%p\n", m_queued_events.size(), receiver, event.ptr());
#endif
    m_queued_events.append({ receiver, move(event) });
}

void GEventLoop::handle_paint_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x Paint [%d,%d %dx%d]\n", event.window_id, event.paint.rect.location.x, event.paint.rect.location.y, event.paint.rect.size.width, event.paint.rect.size.height);
#endif
    post_event(&window, make<GPaintEvent>(event.paint.rect));
}

void GEventLoop::handle_resize_event(const WSAPI_ServerMessage& event, GWindow& window)
{
    post_event(&window, make<GResizeEvent>(event.window.old_rect.size, event.window.rect.size));
}

void GEventLoop::handle_window_activation_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x WindowActivation\n", event.window_id);
#endif
    post_event(&window, make<GEvent>(event.type == WSAPI_ServerMessage::Type::WindowActivated ? GEvent::WindowBecameActive : GEvent::WindowBecameInactive));
}

void GEventLoop::handle_window_close_request_event(const WSAPI_ServerMessage&, GWindow& window)
{
    post_event(&window, make<GEvent>(GEvent::WindowCloseRequest));
}

void GEventLoop::handle_window_entered_or_left_event(const WSAPI_ServerMessage& message, GWindow& window)
{
    post_event(&window, make<GEvent>(message.type == WSAPI_ServerMessage::Type::WindowEntered ? GEvent::WindowEntered : GEvent::WindowLeft));
}

void GEventLoop::handle_key_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x KeyEvent character=0x%b\n", event.window_id, event.key.character);
#endif
    auto key_event = make<GKeyEvent>(event.type == WSAPI_ServerMessage::Type::KeyDown ? GEvent::KeyDown : GEvent::KeyUp, event.key.key);
    key_event->m_alt = event.key.alt;
    key_event->m_ctrl = event.key.ctrl;
    key_event->m_shift = event.key.shift;
    if (event.key.character != '\0')
        key_event->m_text = String(&event.key.character, 1);
    post_event(&window, move(key_event));
}

void GEventLoop::handle_mouse_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x MouseEvent %d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y);
#endif
    GMouseEvent::Type type;
    switch (event.type) {
    case WSAPI_ServerMessage::Type::MouseMove: type = GEvent::MouseMove; break;
    case WSAPI_ServerMessage::Type::MouseUp: type = GEvent::MouseUp; break;
    case WSAPI_ServerMessage::Type::MouseDown: type = GEvent::MouseDown; break;
    default: ASSERT_NOT_REACHED(); break;
    }
    GMouseButton button { GMouseButton::None };
    switch (event.mouse.button) {
    case WSAPI_MouseButton::NoButton: button = GMouseButton::None; break;
    case WSAPI_MouseButton::Left: button = GMouseButton::Left; break;
    case WSAPI_MouseButton::Right: button = GMouseButton::Right; break;
    case WSAPI_MouseButton::Middle: button = GMouseButton::Middle; break;
    default: ASSERT_NOT_REACHED(); break;
    }
    post_event(&window, make<GMouseEvent>(type, event.mouse.position, event.mouse.buttons, button));
}

void GEventLoop::handle_menu_event(const WSAPI_ServerMessage& event)
{
    if (event.type == WSAPI_ServerMessage::Type::MenuItemActivated) {
        auto* menu = GMenu::from_menu_id(event.menu.menu_id);
        if (!menu) {
            dbgprintf("GEventLoop received event for invalid window ID %d\n", event.window_id);
            return;
        }
        if (auto* action = menu->action_at(event.menu.identifier))
            action->activate();
        return;
    }
    ASSERT_NOT_REACHED();
}

void GEventLoop::wait_for_event()
{
    fd_set rfds;
    fd_set wfds;
    FD_ZERO(&rfds);
    FD_ZERO(&wfds);

    int max_fd = 0;
    auto add_fd_to_set = [&max_fd] (int fd, fd_set& set){
        FD_SET(fd, &set);
        if (fd > max_fd)
            max_fd = fd;
    };

    add_fd_to_set(m_event_fd, rfds);
    for (auto& notifier : m_notifiers) {
        if (notifier->event_mask() & GNotifier::Read)
            add_fd_to_set(notifier->fd(), rfds);
        if (notifier->event_mask() & GNotifier::Write)
            add_fd_to_set(notifier->fd(), wfds);
        if (notifier->event_mask() & GNotifier::Exceptional)
            ASSERT_NOT_REACHED();
    }

    struct timeval timeout = { 0, 0 };
    if (!m_timers.is_empty())
        get_next_timer_expiration(timeout);
    ASSERT(m_unprocessed_messages.is_empty());
    int rc = select(max_fd + 1, &rfds, &wfds, nullptr, (m_queued_events.is_empty() && m_timers.is_empty()) ? nullptr : &timeout);
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    for (auto& it : m_timers) {
        auto& timer = *it.value;
        if (!timer.has_expired())
            continue;
#ifdef GEVENTLOOP_DEBUG
        dbgprintf("GEventLoop: Timer %d has expired, sending GTimerEvent to %p\n", timer.timer_id, timer.owner);
#endif
        post_event(timer.owner, make<GTimerEvent>(timer.timer_id));
        if (timer.should_reload) {
            timer.reload();
        } else {
            // FIXME: Support removing expired timers that don't want to reload.
            ASSERT_NOT_REACHED();
        }
    }

    for (auto& notifier : m_notifiers) {
        if (FD_ISSET(notifier->fd(), &rfds)) {
            if (notifier->on_ready_to_read)
                notifier->on_ready_to_read(*notifier);
        }
        if (FD_ISSET(notifier->fd(), &wfds)) {
            if (notifier->on_ready_to_write)
                notifier->on_ready_to_write(*notifier);
        }
    }

    if (!FD_ISSET(m_event_fd, &rfds))
        return;

    bool success = drain_messages_from_server();
    ASSERT(success);
}

void GEventLoop::process_unprocessed_messages()
{
    auto unprocessed_events = move(m_unprocessed_messages);
    for (auto& event : unprocessed_events) {
        if (event.type == WSAPI_ServerMessage::Type::Greeting) {
            m_server_pid = event.greeting.server_pid;
            continue;
        }

        if (event.type == WSAPI_ServerMessage::Error) {
            dbgprintf("GEventLoop got error message from server\n");
            dbgprintf("  - error message: %s\n", String(event.text, event.text_length).characters());
            quit(1);
            return;
        }

        switch (event.type) {
        case WSAPI_ServerMessage::MenuItemActivated:
            handle_menu_event(event);
            continue;
        default:
            break;
        }

        auto* window = GWindow::from_window_id(event.window_id);
        if (!window) {
            dbgprintf("GEventLoop received event for invalid window ID %d\n", event.window_id);
            continue;
        }
        switch (event.type) {
        case WSAPI_ServerMessage::Type::Paint:
            handle_paint_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::MouseDown:
        case WSAPI_ServerMessage::Type::MouseUp:
        case WSAPI_ServerMessage::Type::MouseMove:
            handle_mouse_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::WindowActivated:
        case WSAPI_ServerMessage::Type::WindowDeactivated:
            handle_window_activation_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::WindowCloseRequest:
            handle_window_close_request_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::KeyDown:
        case WSAPI_ServerMessage::Type::KeyUp:
            handle_key_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::WindowEntered:
        case WSAPI_ServerMessage::Type::WindowLeft:
            handle_window_entered_or_left_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::WindowResized:
            handle_resize_event(event, *window);
            break;
        default:
            break;
        }
    }

    if (!m_unprocessed_messages.is_empty())
        process_unprocessed_messages();
}

bool GEventLoop::drain_messages_from_server()
{
    for (;;) {
        WSAPI_ServerMessage message;
        ssize_t nread = read(m_event_fd, &message, sizeof(WSAPI_ServerMessage));
        if (nread < 0) {
            perror("read");
            quit(1);
            return false;
        }
        if (nread == 0)
            return true;
        assert(nread == sizeof(message));
        m_unprocessed_messages.append(move(message));
    }
}

bool GEventLoop::EventLoopTimer::has_expired() const
{
    timeval now;
    gettimeofday(&now, nullptr);
    return now.tv_sec > fire_time.tv_sec || (now.tv_sec == fire_time.tv_sec && now.tv_usec >= fire_time.tv_usec);
}

void GEventLoop::EventLoopTimer::reload()
{
    gettimeofday(&fire_time, nullptr);
    fire_time.tv_sec += interval / 1000;
    fire_time.tv_usec += interval % 1000;
}

void GEventLoop::get_next_timer_expiration(timeval& soonest)
{
    ASSERT(!m_timers.is_empty());
    bool has_checked_any = false;
    for (auto& it : m_timers) {
        auto& fire_time = it.value->fire_time;
        if (!has_checked_any || fire_time.tv_sec < soonest.tv_sec || (fire_time.tv_sec == soonest.tv_sec && fire_time.tv_usec < soonest.tv_usec))
            soonest = fire_time;
        has_checked_any = true;
    }
}

int GEventLoop::register_timer(GObject& object, int milliseconds, bool should_reload)
{
    ASSERT(milliseconds >= 0);
    auto timer = make<EventLoopTimer>();
    timer->owner = &object;
    timer->interval = milliseconds;
    timer->reload();
    timer->should_reload = should_reload;
    int timer_id = ++m_next_timer_id;  // FIXME: This will eventually wrap around.
    ASSERT(timer_id); // FIXME: Aforementioned wraparound.
    timer->timer_id = timer_id;
    m_timers.set(timer->timer_id, move(timer));
    return timer_id;
}

bool GEventLoop::unregister_timer(int timer_id)
{
    auto it = m_timers.find(timer_id);
    if (it == m_timers.end())
        return false;
    m_timers.remove(it);
    return true;
}

void GEventLoop::register_notifier(Badge<GNotifier>, GNotifier& notifier)
{
    m_notifiers.set(&notifier);
}

void GEventLoop::unregister_notifier(Badge<GNotifier>, GNotifier& notifier)
{
    m_notifiers.remove(&notifier);
}

bool GEventLoop::post_message_to_server(const WSAPI_ClientMessage& message)
{
    int nwritten = write(m_event_fd, &message, sizeof(WSAPI_ClientMessage));
    return nwritten == sizeof(WSAPI_ClientMessage);
}

bool GEventLoop::wait_for_specific_event(WSAPI_ServerMessage::Type type, WSAPI_ServerMessage& event)
{
    for (;;) {
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(m_event_fd, &rfds);
        int rc = select(m_event_fd + 1, &rfds, nullptr, nullptr, nullptr);
        ASSERT(rc > 0);
        ASSERT(FD_ISSET(m_event_fd, &rfds));
        bool success = drain_messages_from_server();
        if (!success)
            return false;
        for (size_t i = 0; i < m_unprocessed_messages.size(); ++i) {
            if (m_unprocessed_messages[i].type == type) {
                event = move(m_unprocessed_messages[i]);
                m_unprocessed_messages.remove(i);
                return true;
            }
        }
    }
}

WSAPI_ServerMessage GEventLoop::sync_request(const WSAPI_ClientMessage& request, WSAPI_ServerMessage::Type response_type)
{
    bool success = post_message_to_server(request);
    ASSERT(success);

    WSAPI_ServerMessage response;
    success = GEventLoop::main().wait_for_specific_event(response_type, response);
    ASSERT(success);
    return response;
}
