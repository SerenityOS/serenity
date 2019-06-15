#include "GEventLoop.h"
#include "GEvent.h"
#include "GWindow.h"
#include <LibC/errno.h>
#include <LibC/fcntl.h>
#include <LibC/stdio.h>
#include <LibC/stdlib.h>
#include <LibC/string.h>
#include <LibC/sys/select.h>
#include <LibC/sys/socket.h>
#include <LibC/sys/time.h>
#include <LibC/time.h>
#include <LibC/unistd.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GWidget.h>
#include <sys/uio.h>

//#define GEVENTLOOP_DEBUG
//#define COALESCING_DEBUG

int GEventLoop::s_windowserver_fd = -1;
int GEventLoop::s_my_client_id = -1;
pid_t GEventLoop::s_server_pid = -1;

void GEventLoop::connect_to_server()
{
    ASSERT(s_windowserver_fd == -1);
    s_windowserver_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (s_windowserver_fd < 0) {
        perror("socket");
        ASSERT_NOT_REACHED();
    }

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/wsportal");

    int retries = 1000;
    int rc = 0;
    while (retries) {
        rc = connect(s_windowserver_fd, (const sockaddr*)&address, sizeof(address));
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
    handle_greeting(response);
}

GEventLoop::GEventLoop()
{
    static bool connected = false;
    if (!connected) {
        connect_to_server();
        connected = true;
    }

#ifdef GEVENTLOOP_DEBUG
    dbgprintf("(%u) GEventLoop constructed :)\n", getpid());
#endif
}

GEventLoop::~GEventLoop()
{
}

void GEventLoop::handle_paint_event(const WSAPI_ServerMessage& event, GWindow& window, const ByteBuffer& extra_data)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x Paint [%d,%d %dx%d]\n", event.window_id, event.paint.rect.location.x, event.paint.rect.location.y, event.paint.rect.size.width, event.paint.rect.size.height);
#endif
    Vector<Rect, 32> rects;
    for (int i = 0; i < min(WSAPI_ServerMessage::max_inline_rect_count, event.rect_count); ++i)
        rects.append(event.rects[i]);
    if (event.extra_size) {
        auto* extra_rects = reinterpret_cast<const WSAPI_Rect*>(extra_data.data());
        for (int i = 0; i < event.rect_count - WSAPI_ServerMessage::max_inline_rect_count; ++i)
            rects.append(extra_rects[i]);
    }
    post_event(window, make<GMultiPaintEvent>(rects, event.paint.window_size));
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
        if (auto* focused_widget = window.focused_widget()) {
            if (auto* action = focused_widget->action_for_key_event(*key_event)) {
                if (action->is_enabled()) {
                    action->activate();
                    return;
                }
            }
        }

        if (auto* action = GApplication::the().action_for_key_event(*key_event)) {
            if (action->is_enabled()) {
                action->activate();
                return;
            }
        }
    }
    post_event(window, move(key_event));
}

void GEventLoop::handle_mouse_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x MouseEvent %d,%d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y, event.mouse.wheel_delta);
#endif
    GMouseEvent::Type type;
    switch (event.type) {
    case WSAPI_ServerMessage::Type::MouseMove:
        type = GEvent::MouseMove;
        break;
    case WSAPI_ServerMessage::Type::MouseUp:
        type = GEvent::MouseUp;
        break;
    case WSAPI_ServerMessage::Type::MouseDown:
        type = GEvent::MouseDown;
        break;
    case WSAPI_ServerMessage::Type::MouseDoubleClick:
        type = GEvent::MouseDoubleClick;
        break;
    case WSAPI_ServerMessage::Type::MouseWheel:
        type = GEvent::MouseWheel;
        break;
    default:
        ASSERT_NOT_REACHED();
        break;
    }
    GMouseButton button { GMouseButton::None };
    switch (event.mouse.button) {
    case WSAPI_MouseButton::NoButton:
        button = GMouseButton::None;
        break;
    case WSAPI_MouseButton::Left:
        button = GMouseButton::Left;
        break;
    case WSAPI_MouseButton::Right:
        button = GMouseButton::Right;
        break;
    case WSAPI_MouseButton::Middle:
        button = GMouseButton::Middle;
        break;
    default:
        ASSERT_NOT_REACHED();
        break;
    }
    post_event(window, make<GMouseEvent>(type, event.mouse.position, event.mouse.buttons, button, event.mouse.modifiers, event.mouse.wheel_delta));
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
    if (event.type == WSAPI_ServerMessage::WM_WindowRectChanged)
        return post_event(window, make<GWMWindowRectChangedEvent>(event.wm.client_id, event.wm.window_id, event.wm.rect));
    if (event.type == WSAPI_ServerMessage::WM_WindowIconChanged)
        return post_event(window, make<GWMWindowIconChangedEvent>(event.wm.client_id, event.wm.window_id, String(event.text, event.text_length)));
    if (event.type == WSAPI_ServerMessage::WM_WindowRemoved)
        return post_event(window, make<GWMWindowRemovedEvent>(event.wm.client_id, event.wm.window_id));
    ASSERT_NOT_REACHED();
}

void GEventLoop::process_unprocessed_bundles()
{
    int coalesced_paints = 0;
    int coalesced_resizes = 0;
    auto unprocessed_bundles = move(m_unprocessed_bundles);

    HashMap<int, Size> latest_size_for_window_id;
    for (auto& bundle : unprocessed_bundles) {
        auto& event = bundle.message;
        if (event.type == WSAPI_ServerMessage::Type::WindowResized) {
            latest_size_for_window_id.set(event.window_id, event.window.rect.size);
        }
    }

    int paint_count = 0;
    HashMap<int, Size> latest_paint_size_for_window_id;
    for (auto& bundle : unprocessed_bundles) {
        auto& event = bundle.message;
        if (event.type == WSAPI_ServerMessage::Type::Paint) {
            ++paint_count;
#ifdef COALESCING_DEBUG
            dbgprintf("    %s (window: %s)\n", Rect(event.paint.rect).to_string().characters(), Size(event.paint.window_size).to_string().characters());
#endif
            latest_paint_size_for_window_id.set(event.window_id, event.paint.window_size);
        }
    }
#ifdef COALESCING_DEBUG
    dbgprintf("paint_count: %d\n", paint_count);
#endif

    for (auto& bundle : unprocessed_bundles) {
        auto& event = bundle.message;
        if (event.type == WSAPI_ServerMessage::Type::Greeting) {
            handle_greeting(event);
            continue;
        }

        if (event.type == WSAPI_ServerMessage::Type::ScreenRectChanged) {
            GDesktop::the().did_receive_screen_rect({}, event.screen.rect);
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
            if (Size(event.paint.window_size) != latest_paint_size_for_window_id.get(event.window_id)) {
                ++coalesced_paints;
                break;
            }
            handle_paint_event(event, *window, bundle.extra_data);
            break;
        case WSAPI_ServerMessage::Type::MouseDown:
        case WSAPI_ServerMessage::Type::MouseDoubleClick:
        case WSAPI_ServerMessage::Type::MouseUp:
        case WSAPI_ServerMessage::Type::MouseMove:
        case WSAPI_ServerMessage::Type::MouseWheel:
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
            if (Size(event.window.rect.size) != latest_size_for_window_id.get(event.window_id)) {
                ++coalesced_resizes;
                break;
            }
            handle_resize_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::WM_WindowRemoved:
        case WSAPI_ServerMessage::Type::WM_WindowStateChanged:
        case WSAPI_ServerMessage::Type::WM_WindowIconChanged:
            handle_wm_event(event, *window);
            break;
        default:
            break;
        }
    }

#ifdef COALESCING_DEBUG
    if (coalesced_paints)
        dbgprintf("Coalesced %d paints\n", coalesced_paints);
    if (coalesced_resizes)
        dbgprintf("Coalesced %d resizes\n", coalesced_resizes);
#endif
}

bool GEventLoop::drain_messages_from_server()
{
    for (;;) {
        WSAPI_ServerMessage message;
        ssize_t nread = recv(s_windowserver_fd, &message, sizeof(WSAPI_ServerMessage), MSG_DONTWAIT);
        if (nread < 0) {
            if (errno == EAGAIN) {
                return true;
            }
            perror("read");
            quit(1);
            return false;
        }
        if (nread == 0) {
            fprintf(stderr, "EOF on WindowServer fd\n");
            quit(1);
            exit(-1);
            return false;
        }
        ASSERT(nread == sizeof(message));
        ByteBuffer extra_data;
        if (message.extra_size) {
            extra_data = ByteBuffer::create_uninitialized(message.extra_size);
            int extra_nread = read(s_windowserver_fd, extra_data.data(), extra_data.size());
            ASSERT(extra_nread == message.extra_size);
        }
        m_unprocessed_bundles.append({ move(message), move(extra_data) });
    }
}

bool GEventLoop::post_message_to_server(const WSAPI_ClientMessage& message, const ByteBuffer& extra_data)
{
    if (!extra_data.is_empty())
        const_cast<WSAPI_ClientMessage&>(message).extra_size = extra_data.size();

    struct iovec iov[2];
    int iov_count = 1;
    iov[0].iov_base = (void*)&message;
    iov[0].iov_len = sizeof(message);

    if (!extra_data.is_empty()) {
        iov[1].iov_base = (void*)extra_data.data();
        iov[1].iov_len = extra_data.size();
        ++iov_count;
    }

    int nwritten = writev(s_windowserver_fd, iov, iov_count);
    ASSERT(nwritten == sizeof(message) + extra_data.size());

    return true;
}

bool GEventLoop::wait_for_specific_event(WSAPI_ServerMessage::Type type, WSAPI_ServerMessage& event)
{
    for (;;) {
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(s_windowserver_fd, &rfds);
        int rc = select(s_windowserver_fd + 1, &rfds, nullptr, nullptr, nullptr);
        if (rc < 0) {
            perror("select");
        }
        ASSERT(rc > 0);
        ASSERT(FD_ISSET(s_windowserver_fd, &rfds));
        bool success = drain_messages_from_server();
        if (!success)
            return false;
        for (ssize_t i = 0; i < m_unprocessed_bundles.size(); ++i) {
            if (m_unprocessed_bundles[i].message.type == type) {
                event = move(m_unprocessed_bundles[i].message);
                m_unprocessed_bundles.remove(i);
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

void GEventLoop::handle_greeting(WSAPI_ServerMessage& message)
{
    s_server_pid = message.greeting.server_pid;
    s_my_client_id = message.greeting.your_client_id;
    GDesktop::the().did_receive_screen_rect({}, message.greeting.screen_rect);
}
