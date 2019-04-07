#include "GEventLoop.h"
#include "GEvent.h"
#include "GObject.h"
#include "GWindow.h"
#include <LibGUI/GApplication.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GNotifier.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GDesktop.h>
#include <LibC/unistd.h>
#include <LibC/stdio.h>
#include <LibC/fcntl.h>
#include <LibC/string.h>
#include <LibC/time.h>
#include <LibC/sys/select.h>
#include <LibC/sys/socket.h>
#include <LibC/sys/time.h>
#include <LibC/errno.h>
#include <LibC/string.h>
#include <LibC/stdlib.h>

//#define GEVENTLOOP_DEBUG

static HashMap<GShortcut, GAction*>* g_actions;
static GEventLoop* s_main_event_loop;
static Vector<GEventLoop*>* s_event_loop_stack;
int GEventLoop::s_event_fd = -1;
pid_t GEventLoop::s_server_pid = -1;
HashMap<int, OwnPtr<GEventLoop::EventLoopTimer>>* GEventLoop::s_timers;
HashTable<GNotifier*>* GEventLoop::s_notifiers;
int GEventLoop::s_next_timer_id = 1;

void GEventLoop::connect_to_server()
{
    ASSERT(s_event_fd == -1);
    s_event_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    if (s_event_fd < 0) {
        perror("socket");
        ASSERT_NOT_REACHED();
    }

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/wsportal");

    int retries = 1000;
    int rc = 0;
    while (retries) {
        rc = connect(s_event_fd, (const sockaddr*)&address, sizeof(address));
        if (rc == 0)
            break;
#ifdef GEVENTLOOP_DEBUG
        dbgprintf("connect failed: %d, %s\n", errno, strerror(errno));
#endif
        sleep(1);
        --retries;
    }
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::Greeting;
    request.greeting.client_pid = getpid();
    auto response = sync_request(request, WSAPI_ServerMessage::Type::Greeting);
    s_server_pid = response.greeting.server_pid;
    GDesktop::the().did_receive_screen_rect(Badge<GEventLoop>(), response.greeting.screen_rect);
}

GEventLoop::GEventLoop()
{
    if (!s_event_loop_stack) {
        s_event_loop_stack = new Vector<GEventLoop*>;
        s_timers = new HashMap<int, OwnPtr<GEventLoop::EventLoopTimer>>;
        s_notifiers = new HashTable<GNotifier*>;
    }

    if (!s_main_event_loop) {
        s_main_event_loop = this;
        s_event_loop_stack->append(this);
        connect_to_server();
    }

    if (!g_actions)
        g_actions = new HashMap<GShortcut, GAction*>;

#ifdef GEVENTLOOP_DEBUG
    dbgprintf("(%u) GEventLoop constructed :)\n", getpid());
#endif
}

GEventLoop::~GEventLoop()
{
}

GEventLoop& GEventLoop::main()
{
    ASSERT(s_main_event_loop);
    return *s_main_event_loop;
}

GEventLoop& GEventLoop::current()
{
    return *s_event_loop_stack->last();
}

void GEventLoop::quit(int code)
{
    m_exit_requested = true;
    m_exit_code = code;
}

struct GEventLoopPusher {
public:
    GEventLoopPusher(GEventLoop& event_loop) : m_event_loop(event_loop)
    {
        if (&m_event_loop != s_main_event_loop) {
            m_event_loop.take_pending_events_from(GEventLoop::current());
            s_event_loop_stack->append(&event_loop);
        }
    }
    ~GEventLoopPusher()
    {
        if (&m_event_loop != s_main_event_loop) {
            s_event_loop_stack->take_last();
            GEventLoop::current().take_pending_events_from(m_event_loop);
        }
    }
private:
    GEventLoop& m_event_loop;
};

int GEventLoop::exec()
{
    GEventLoopPusher pusher(*this);

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
            auto* receiver = queued_event.receiver.ptr();
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
                    dbgprintf("Event type %u with no receiver :(\n", event.type());
                }
            } else if (event.type() == GEvent::Type::DeferredInvoke) {
                printf("DeferredInvoke: receiver=%s{%p}\n", receiver->class_name(), receiver);
                static_cast<GDeferredInvocationEvent&>(event).m_invokee(*receiver);
            } else {
                receiver->event(event);
            }

            if (m_exit_requested) {
                auto rejigged_event_queue = move(events);
                rejigged_event_queue.append(move(m_queued_events));
                m_queued_events = move(rejigged_event_queue);
                return m_exit_code;
            }
        }
    }
    ASSERT_NOT_REACHED();
}

void GEventLoop::post_event(GObject& receiver, OwnPtr<GEvent>&& event)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("GEventLoop::post_event: {%u} << receiver=%p, event=%p\n", m_queued_events.size(), &receiver, event.ptr());
#endif
    m_queued_events.append({ receiver.make_weak_ptr(), move(event) });
}

void GEventLoop::handle_paint_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x Paint [%d,%d %dx%d]\n", event.window_id, event.paint.rect.location.x, event.paint.rect.location.y, event.paint.rect.size.width, event.paint.rect.size.height);
#endif
    post_event(window, make<GPaintEvent>(event.paint.rect, event.paint.window_size));
}

void GEventLoop::handle_resize_event(const WSAPI_ServerMessage& event, GWindow& window)
{
    post_event(window, make<GResizeEvent>(event.window.old_rect.size, event.window.rect.size));
}

void GEventLoop::handle_window_activation_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x WindowActivation\n", event.window_id);
#endif
    post_event(window, make<GEvent>(event.type == WSAPI_ServerMessage::Type::WindowActivated ? GEvent::WindowBecameActive : GEvent::WindowBecameInactive));
}

void GEventLoop::handle_window_close_request_event(const WSAPI_ServerMessage&, GWindow& window)
{
    post_event(window, make<GEvent>(GEvent::WindowCloseRequest));
}

void GEventLoop::handle_window_entered_or_left_event(const WSAPI_ServerMessage& message, GWindow& window)
{
    post_event(window, make<GEvent>(message.type == WSAPI_ServerMessage::Type::WindowEntered ? GEvent::WindowEntered : GEvent::WindowLeft));
}

void GEventLoop::handle_key_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x KeyEvent character=0x%b\n", event.window_id, event.key.character);
#endif
    auto key_event = make<GKeyEvent>(event.type == WSAPI_ServerMessage::Type::KeyDown ? GEvent::KeyDown : GEvent::KeyUp, event.key.key, event.key.modifiers);
    if (event.key.character != '\0')
        key_event->m_text = String(&event.key.character, 1);

    if (event.type == WSAPI_ServerMessage::Type::KeyDown) {
        if (auto* action = GApplication::the().action_for_key_event(*key_event)) {
            action->activate();
            return;
        }
    }
    post_event(window, move(key_event));
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
    post_event(window, make<GMouseEvent>(type, event.mouse.position, event.mouse.buttons, button, event.mouse.modifiers));
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

void GEventLoop::handle_wm_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("GEventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (event.type == WSAPI_ServerMessage::WM_WindowStateChanged)
        return post_event(window, make<GWMWindowStateChangedEvent>(event.wm.client_id, event.wm.window_id, String(event.text, event.text_length), event.wm.rect, event.wm.is_active, (GWindowType)event.wm.window_type, event.wm.is_minimized));
    if (event.type == WSAPI_ServerMessage::WM_WindowRemoved)
        return post_event(window, make<GWMWindowRemovedEvent>(event.wm.client_id, event.wm.window_id));
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

    add_fd_to_set(s_event_fd, rfds);
    for (auto& notifier : *s_notifiers) {
        if (notifier->event_mask() & GNotifier::Read)
            add_fd_to_set(notifier->fd(), rfds);
        if (notifier->event_mask() & GNotifier::Write)
            add_fd_to_set(notifier->fd(), wfds);
        if (notifier->event_mask() & GNotifier::Exceptional)
            ASSERT_NOT_REACHED();
    }

    struct timeval timeout = { 0, 0 };
    if (!s_timers->is_empty() && m_queued_events.is_empty())
        get_next_timer_expiration(timeout);
    ASSERT(m_unprocessed_messages.is_empty());
    int rc = select(max_fd + 1, &rfds, &wfds, nullptr, (m_queued_events.is_empty() && s_timers->is_empty()) ? nullptr : &timeout);
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    for (auto& it : *s_timers) {
        auto& timer = *it.value;
        if (!timer.has_expired())
            continue;
#ifdef GEVENTLOOP_DEBUG
        dbgprintf("GEventLoop: Timer %d has expired, sending GTimerEvent to %p\n", timer.timer_id, timer.owner);
#endif
        post_event(*timer.owner, make<GTimerEvent>(timer.timer_id));
        if (timer.should_reload) {
            timer.reload();
        } else {
            // FIXME: Support removing expired timers that don't want to reload.
            ASSERT_NOT_REACHED();
        }
    }

    for (auto& notifier : *s_notifiers) {
        if (FD_ISSET(notifier->fd(), &rfds)) {
            if (notifier->on_ready_to_read)
                notifier->on_ready_to_read(*notifier);
        }
        if (FD_ISSET(notifier->fd(), &wfds)) {
            if (notifier->on_ready_to_write)
                notifier->on_ready_to_write(*notifier);
        }
    }

    if (!FD_ISSET(s_event_fd, &rfds))
        return;

    bool success = drain_messages_from_server();
    ASSERT(success);
}

void GEventLoop::process_unprocessed_messages()
{
    auto unprocessed_events = move(m_unprocessed_messages);
    for (auto& event : unprocessed_events) {
        if (event.type == WSAPI_ServerMessage::Type::Greeting) {
            s_server_pid = event.greeting.server_pid;
            GDesktop::the().did_receive_screen_rect(Badge<GEventLoop>(), event.greeting.screen_rect);
            continue;
        }

        if (event.type == WSAPI_ServerMessage::Type::ScreenRectChanged) {
            GDesktop::the().did_receive_screen_rect(Badge<GEventLoop>(), event.screen.rect);
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
        case WSAPI_ServerMessage::Type::WM_WindowRemoved:
        case WSAPI_ServerMessage::Type::WM_WindowStateChanged:
            handle_wm_event(event, *window);
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
    bool is_first_pass = true;
    for (;;) {
        WSAPI_ServerMessage message;
        ssize_t nread = read(s_event_fd, &message, sizeof(WSAPI_ServerMessage));
        if (nread < 0) {
            perror("read");
            quit(1);
            return false;
        }
        if (nread == 0) {
            if (is_first_pass) {
                fprintf(stderr, "EOF on WindowServer fd\n");
                quit(1);
                return false;
            }
            return true;
        }
        assert(nread == sizeof(message));
        m_unprocessed_messages.append(move(message));
        is_first_pass = false;
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
    fire_time.tv_usec += (interval % 1000) * 1000;
}

void GEventLoop::get_next_timer_expiration(timeval& soonest)
{
    ASSERT(!s_timers->is_empty());
    bool has_checked_any = false;
    for (auto& it : *s_timers) {
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
    timer->owner = object.make_weak_ptr();
    timer->interval = milliseconds;
    timer->reload();
    timer->should_reload = should_reload;
    int timer_id = ++s_next_timer_id;  // FIXME: This will eventually wrap around.
    ASSERT(timer_id); // FIXME: Aforementioned wraparound.
    timer->timer_id = timer_id;
    s_timers->set(timer->timer_id, move(timer));
    return timer_id;
}

bool GEventLoop::unregister_timer(int timer_id)
{
    auto it = s_timers->find(timer_id);
    if (it == s_timers->end())
        return false;
    s_timers->remove(it);
    return true;
}

void GEventLoop::register_notifier(Badge<GNotifier>, GNotifier& notifier)
{
    s_notifiers->set(&notifier);
}

void GEventLoop::unregister_notifier(Badge<GNotifier>, GNotifier& notifier)
{
    s_notifiers->remove(&notifier);
}

bool GEventLoop::post_message_to_server(const WSAPI_ClientMessage& message)
{
    int nwritten = write(s_event_fd, &message, sizeof(WSAPI_ClientMessage));
    return nwritten == sizeof(WSAPI_ClientMessage);
}

bool GEventLoop::wait_for_specific_event(WSAPI_ServerMessage::Type type, WSAPI_ServerMessage& event)
{

    for (;;) {
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(s_event_fd, &rfds);
        int rc = select(s_event_fd + 1, &rfds, nullptr, nullptr, nullptr);
        ASSERT(rc > 0);
        ASSERT(FD_ISSET(s_event_fd, &rfds));
        bool success = drain_messages_from_server();
        if (!success)
            return false;
        for (ssize_t i = 0; i < m_unprocessed_messages.size(); ++i) {
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
    success = wait_for_specific_event(response_type, response);
    ASSERT(success);
    return response;
}
