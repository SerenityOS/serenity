#include <WindowServer/WSMessageLoop.h>
#include <WindowServer/WSMessage.h>
#include <WindowServer/WSMessageReceiver.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSClientConnection.h>
#include <WindowServer/WSAPITypes.h>
#include <Kernel/KeyCode.h>
#include <LibC/sys/socket.h>
#include <LibC/sys/select.h>
#include <LibC/unistd.h>
#include <LibC/time.h>
#include <LibC/fcntl.h>
#include <LibC/stdio.h>
#include <LibC/errno.h>

//#define WSEVENTLOOP_DEBUG

static WSMessageLoop* s_the;

WSMessageLoop::WSMessageLoop()
{
    if (!s_the)
        s_the = this;
}

WSMessageLoop::~WSMessageLoop()
{
}

WSMessageLoop& WSMessageLoop::the()
{
    ASSERT(s_the);
    return *s_the;
}

int WSMessageLoop::exec()
{
    m_keyboard_fd = open("/dev/keyboard", O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    m_mouse_fd = open("/dev/psaux", O_RDONLY | O_NONBLOCK | O_CLOEXEC);

    unlink("/wsportal");

    m_server_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    ASSERT(m_server_fd >= 0);
    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/wsportal");
    int rc = bind(m_server_fd, (const sockaddr*)&address, sizeof(address));
    ASSERT(rc == 0);
    rc = listen(m_server_fd, 5);
    ASSERT(rc == 0);

    ASSERT(m_keyboard_fd >= 0);
    ASSERT(m_mouse_fd >= 0);

    m_running = true;
    for (;;) {
        wait_for_message();
        Vector<QueuedMessage> messages = move(m_queued_messages);

        for (auto& queued_message : messages) {
            auto* receiver = queued_message.receiver;
            auto& message = *queued_message.message;
#ifdef WSEVENTLOOP_DEBUG
            dbgprintf("WSMessageLoop: receiver{%p} message %u\n", receiver, (unsigned)message.type());
#endif
            if (!receiver) {
                dbgprintf("WSMessage type %u with no receiver :(\n", message.type());
                ASSERT_NOT_REACHED();
                return 1;
            } else {
                receiver->on_message(message);
            }
        }
    }
}

void WSMessageLoop::post_message(WSMessageReceiver* receiver, OwnPtr<WSMessage>&& message)
{
#ifdef WSEVENTLOOP_DEBUG
    dbgprintf("WSMessageLoop::post_message: {%u} << receiver=%p, message=%p (type=%u)\n", m_queued_messages.size(), receiver, message.ptr(), message->type());
#endif
    m_queued_messages.append({ receiver, move(message) });
}

void WSMessageLoop::Timer::reload()
{
    struct timeval now;
    gettimeofday(&now, nullptr);
    next_fire_time = {
        now.tv_sec + (interval / 1000),
        now.tv_usec + (interval % 1000)
    };
}

int WSMessageLoop::start_timer(int interval, Function<void()>&& callback)
{
    auto timer = make<Timer>();
    int timer_id = m_next_timer_id++;
    timer->timer_id = timer_id;
    timer->callback = move(callback);
    timer->interval = interval;
    timer->reload();
    m_timers.set(timer_id, move(timer));
    return timer_id;
}

int WSMessageLoop::stop_timer(int timer_id)
{
    auto it = m_timers.find(timer_id);
    if (it == m_timers.end())
        return -1;
    m_timers.remove(it);
    return 0;
}

void WSMessageLoop::wait_for_message()
{
    fd_set rfds;
    FD_ZERO(&rfds);
    int max_fd = 0;
    auto add_fd_to_set = [&max_fd] (int fd, auto& set) {
        FD_SET(fd, &set);
        if (fd > max_fd)
            max_fd = fd;
    };

    add_fd_to_set(m_keyboard_fd, rfds);
    add_fd_to_set(m_mouse_fd, rfds);
    add_fd_to_set(m_server_fd, rfds);

    WSClientConnection::for_each_client([&] (WSClientConnection& client) {
        add_fd_to_set(client.fd(), rfds);
    });

    struct timeval timeout = { 0, 0 };
    bool had_any_timer = false;

    for (auto& it : m_timers) {
        auto& timer = *it.value;
        if (!had_any_timer) {
            timeout = timer.next_fire_time;
            had_any_timer = true;
            continue;
        }
        if (timer.next_fire_time.tv_sec > timeout.tv_sec || (timer.next_fire_time.tv_sec == timeout.tv_sec && timer.next_fire_time.tv_usec > timeout.tv_usec))
            timeout = timer.next_fire_time;
    }

    int rc = select(max_fd + 1, &rfds, nullptr, nullptr, m_timers.is_empty() && m_queued_messages.is_empty() ? nullptr : &timeout);
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    struct timeval now;
    gettimeofday(&now, nullptr);
    for (auto& it : m_timers) {
        auto& timer = *it.value;
        if (now.tv_sec > timer.next_fire_time.tv_sec || (now.tv_sec == timer.next_fire_time.tv_sec && now.tv_usec > timer.next_fire_time.tv_usec)) {
            timer.callback();
            timer.reload();
        }
    }

    if (FD_ISSET(m_keyboard_fd, &rfds))
        drain_keyboard();
    if (FD_ISSET(m_mouse_fd, &rfds))
        drain_mouse();
    if (FD_ISSET(m_server_fd, &rfds)) {
        sockaddr_un address;
        socklen_t address_size = sizeof(address);
        int client_fd = accept(m_server_fd, (sockaddr*)&address, &address_size);
        dbgprintf("accept() returned fd=%d, address=%s\n", client_fd, address.sun_path);
        ASSERT(client_fd >= 0);
        new WSClientConnection(client_fd);
    }
    WSClientConnection::for_each_client([&] (WSClientConnection& client) {
        if (!FD_ISSET(client.fd(), &rfds))
            return;
        unsigned messages_received = 0;
        for (;;) {
            WSAPI_ClientMessage message;
            // FIXME: Don't go one message at a time, that's so much context switching, oof.
            ssize_t nread = read(client.fd(), &message, sizeof(WSAPI_ClientMessage));
            if (nread == 0) {
                if (!messages_received)
                    notify_client_disconnected(client.client_id());
                break;
            }
            if (nread < 0) {
                perror("read");
                ASSERT_NOT_REACHED();
            }
            on_receive_from_client(client.client_id(), message);
            ++messages_received;
        }
    });
}

void WSMessageLoop::drain_mouse()
{
    auto& screen = WSScreen::the();
    bool prev_left_button = screen.left_mouse_button_pressed();
    bool prev_right_button = screen.right_mouse_button_pressed();
    int dx = 0;
    int dy = 0;
    bool left_button = prev_left_button;
    bool right_button = prev_right_button;
    for (;;) {
        byte data[3];
        ssize_t nread = read(m_mouse_fd, data, sizeof(data));
        if (nread == 0)
            break;
        ASSERT(nread == sizeof(data));
        bool left_button = data[0] & 1;
        bool right_button = data[0] & 2;
        bool x_overflow = data[0] & 0x40;
        bool y_overflow = data[0] & 0x80;
        bool x_sign = data[0] & 0x10;
        bool y_sign = data[0] & 0x20;

        if (x_overflow || y_overflow)
            continue;

        int x = data[1];
        int y = data[2];
        if (x && x_sign)
            x -= 0x100;
        if (y && y_sign)
            y -= 0x100;

        dx += x;
        dy += -y;
        if (left_button != prev_left_button || right_button != prev_right_button) {
            prev_left_button = left_button;
            prev_right_button = right_button;
            screen.on_receive_mouse_data(dx, dy, left_button, right_button);
            dx = 0;
            dy = 0;
        }
    }
    if (dx || dy) {
        screen.on_receive_mouse_data(dx, dy, left_button, right_button);
    }
}

void WSMessageLoop::drain_keyboard()
{
    auto& screen = WSScreen::the();
    for (;;) {
        KeyEvent event;
        ssize_t nread = read(m_keyboard_fd, (byte*)&event, sizeof(KeyEvent));
        if (nread == 0)
            break;
        ASSERT(nread == sizeof(KeyEvent));
        screen.on_receive_keyboard_data(event);
    }
}

void WSMessageLoop::notify_client_disconnected(int client_id)
{
    auto* client = WSClientConnection::from_client_id(client_id);
    if (!client)
        return;
    post_message(client, make<WSClientDisconnectedNotification>(client_id));
}

void WSMessageLoop::on_receive_from_client(int client_id, const WSAPI_ClientMessage& message)
{
#if 0
    // FIXME: This should not be necessary.. why is this necessary?
    while (!running())
        sched_yield();
#endif

    WSClientConnection* client = WSClientConnection::from_client_id(client_id);
    ASSERT(client);
    switch (message.type) {
    case WSAPI_ClientMessage::Type::CreateMenubar:
        post_message(client, make<WSAPICreateMenubarRequest>(client_id));
        break;
    case WSAPI_ClientMessage::Type::DestroyMenubar:
        post_message(client, make<WSAPIDestroyMenubarRequest>(client_id, message.menu.menubar_id));
        break;
    case WSAPI_ClientMessage::Type::SetApplicationMenubar:
        post_message(client, make<WSAPISetApplicationMenubarRequest>(client_id, message.menu.menubar_id));
        break;
    case WSAPI_ClientMessage::Type::AddMenuToMenubar:
        post_message(client, make<WSAPIAddMenuToMenubarRequest>(client_id, message.menu.menubar_id, message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::CreateMenu:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPICreateMenuRequest>(client_id, String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::DestroyMenu:
        post_message(client, make<WSAPIDestroyMenuRequest>(client_id, message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::AddMenuItem:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPIAddMenuItemRequest>(client_id, message.menu.menu_id, message.menu.identifier, String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::CreateWindow:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPICreateWindowRequest>(client_id, message.window.rect, String(message.text, message.text_length), message.window.has_alpha_channel, message.window.opacity));
        break;
    case WSAPI_ClientMessage::Type::DestroyWindow:
        post_message(client, make<WSAPIDestroyWindowRequest>(client_id, message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowTitle:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPISetWindowTitleRequest>(client_id, message.window_id, String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::GetWindowTitle:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPIGetWindowTitleRequest>(client_id, message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowRect:
        post_message(client, make<WSAPISetWindowRectRequest>(client_id, message.window_id, message.window.rect));
        break;
    case WSAPI_ClientMessage::Type::GetWindowRect:
        post_message(client, make<WSAPIGetWindowRectRequest>(client_id, message.window_id));
        break;
    case WSAPI_ClientMessage::Type::InvalidateRect:
        post_message(client, make<WSAPIInvalidateRectRequest>(client_id, message.window_id, message.window.rect));
        break;
    case WSAPI_ClientMessage::Type::DidFinishPainting:
        post_message(client, make<WSAPIDidFinishPaintingNotification>(client_id, message.window_id, message.window.rect));
        break;
    case WSAPI_ClientMessage::Type::GetWindowBackingStore:
        post_message(client, make<WSAPIGetWindowBackingStoreRequest>(client_id, message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowBackingStore:
        post_message(client, make<WSAPISetWindowBackingStoreRequest>(client_id, message.window_id, message.backing.shared_buffer_id, message.backing.size, message.backing.bpp, message.backing.pitch, message.backing.has_alpha_channel));
        break;
    case WSAPI_ClientMessage::Type::SetGlobalCursorTracking:
        post_message(client, make<WSAPISetGlobalCursorTrackingRequest>(client_id, message.window_id, message.value));
        break;
    }
}
