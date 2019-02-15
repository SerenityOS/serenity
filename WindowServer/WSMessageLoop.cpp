#include "WSMessageLoop.h"
#include "WSMessage.h"
#include "WSMessageReceiver.h"
#include "WSWindowManager.h"
#include "WSScreen.h"
#include <WindowServer/WSClientConnection.h>
#include "PS2MouseDevice.h"
#include <Kernel/Keyboard.h>
#include <WindowServer/WSAPITypes.h>
#include <AK/Bitmap.h>
#include "Process.h"

//#define WSEVENTLOOP_DEBUG

static WSMessageLoop* s_the;

WSMessageLoop::WSMessageLoop()
    : m_lock("WSMessageLoop")
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
    ASSERT(m_server_process == current);

    m_keyboard_fd = m_server_process->sys$open("/dev/keyboard", O_RDONLY);
    m_mouse_fd = m_server_process->sys$open("/dev/psaux", O_RDONLY);

    m_server_process->sys$unlink("/wsportal");

    m_server_fd = m_server_process->sys$socket(AF_LOCAL, SOCK_STREAM, 0);
    ASSERT(m_server_fd >= 0);
    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/wsportal");
    int rc = m_server_process->sys$bind(m_server_fd, (const sockaddr*)&address, sizeof(address));
    ASSERT(rc == 0);
    rc = m_server_process->sys$listen(m_server_fd, 5);
    ASSERT(rc == 0);

    ASSERT(m_keyboard_fd >= 0);
    ASSERT(m_mouse_fd >= 0);

    m_running = true;
    for (;;) {
        wait_for_message();

        Vector<QueuedMessage> messages;
        {
            ASSERT_INTERRUPTS_ENABLED();
            LOCKER(m_lock);
            messages = move(m_queued_messages);
        }

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

Process* WSMessageLoop::process_from_client_id(int client_id)
{
    // FIXME: This shouldn't work this way lol.
    return (Process*)client_id;
}

void WSMessageLoop::post_message_to_client(int client_id, const GUI_ServerMessage& message)
{
    auto* client = WSClientConnection::from_client_id(client_id);
    if (!client)
        return;
    client->post_message(message);
}

void WSMessageLoop::post_message(WSMessageReceiver* receiver, OwnPtr<WSMessage>&& message)
{
    LOCKER(m_lock);
#ifdef WSEVENTLOOP_DEBUG
    dbgprintf("WSMessageLoop::post_message: {%u} << receiver=%p, message=%p (type=%u)\n", m_queued_messages.size(), receiver, message.ptr(), message->type());
#endif

#if 0
    if (message->type() == WSMessage::WM_ClientFinishedPaint) {
        auto& invalidation_message = static_cast<WSClientFinishedPaintMessage&>(*message);
        for (auto& queued_message : m_queued_messages) {
            if (receiver == queued_message.receiver && queued_message.message->type() == WSMessage::WM_ClientFinishedPaint) {
                auto& queued_invalidation_message = static_cast<WSClientFinishedPaintMessage&>(*queued_message.message);
                if (queued_invalidation_message.rect().is_empty() || queued_invalidation_message.rect().contains(invalidation_message.rect())) {
#ifdef WSEVENTLOOP_DEBUG
                    dbgprintf("Swallow WM_ClientFinishedPaint\n");
#endif
                    return;
                }
            }
        }
    }

    if (message->type() == WSMessage::WM_ClientWantsToPaint) {
        auto& invalidation_message = static_cast<WSClientWantsToPaintMessage&>(*message);
        for (auto& queued_message : m_queued_messages) {
            if (receiver == queued_message.receiver && queued_message.message->type() == WSMessage::WM_ClientWantsToPaint) {
                auto& queued_invalidation_message = static_cast<WSClientWantsToPaintMessage&>(*queued_message.message);
                if (queued_invalidation_message.rect().is_empty() || queued_invalidation_message.rect().contains(invalidation_message.rect())) {
#ifdef WSEVENTLOOP_DEBUG
                    dbgprintf("Swallow WM_ClientWantsToPaint\n");
#endif
                    return;
                }
            }
        }
    }
#endif

    m_queued_messages.append({ receiver, move(message) });

    if (current != m_server_process)
        m_server_process->request_wakeup();
}

void WSMessageLoop::Timer::reload()
{
    struct timeval now;
    current->sys$gettimeofday(&now);
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
    memset(&rfds, 0, sizeof(rfds));
    auto bitmap = Bitmap::wrap((byte*)&rfds, FD_SETSIZE);
    int max_fd = 0;
    auto add_fd_to_set = [&max_fd] (int fd, auto& bitmap) {
        bitmap.set(fd, true);
        if (fd > max_fd)
            max_fd = fd;
    };

    add_fd_to_set(m_keyboard_fd, bitmap);
    add_fd_to_set(m_mouse_fd, bitmap);
    add_fd_to_set(m_server_fd, bitmap);

    WSClientConnection::for_each_client([&] (WSClientConnection& client) {
        add_fd_to_set(client.fd(), bitmap);
    });

    Syscall::SC_select_params params;
    params.nfds = max_fd + 1;
    params.readfds = &rfds;
    params.writefds = nullptr;
    params.exceptfds = nullptr;
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

    if (m_timers.is_empty() && m_queued_messages.is_empty())
        params.timeout = nullptr;
    else
        params.timeout = &timeout;

    int rc = m_server_process->sys$select(&params);
    memory_barrier();
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    struct timeval now;
    current->sys$gettimeofday(&now);
    for (auto& it : m_timers) {
        auto& timer = *it.value;
        if (now.tv_sec > timer.next_fire_time.tv_sec || (now.tv_sec == timer.next_fire_time.tv_sec && now.tv_usec > timer.next_fire_time.tv_usec)) {
            timer.callback();
            timer.reload();
        }
    }

    if (bitmap.get(m_keyboard_fd))
        drain_keyboard();
    if (bitmap.get(m_mouse_fd))
        drain_mouse();
    if (bitmap.get(m_server_fd)) {
        sockaddr_un address;
        socklen_t address_size = sizeof(address);
        int client_fd = m_server_process->sys$accept(m_server_fd, (sockaddr*)&address, &address_size);
        kprintf("accept() returned fd=%d, address=%s\n", client_fd, address.sun_path);
        new WSClientConnection(client_fd);
    }
    WSClientConnection::for_each_client([&] (WSClientConnection& client) {
        if (bitmap.get(client.fd())) {
            byte buffer[4096];
            ssize_t nread = m_server_process->sys$read(client.fd(), buffer, sizeof(GUI_ClientMessage));
            on_receive_from_client(client.client_id(), buffer, nread);
        }
    });
}

void WSMessageLoop::drain_mouse()
{
    auto& screen = WSScreen::the();
    auto& mouse = PS2MouseDevice::the();
    bool prev_left_button = screen.left_mouse_button_pressed();
    bool prev_right_button = screen.right_mouse_button_pressed();
    int dx = 0;
    int dy = 0;
    while (mouse.can_read(*m_server_process)) {
        byte data[3];
        ssize_t nread = mouse.read(*m_server_process, (byte*)data, sizeof(data));
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
        if (left_button != prev_left_button || right_button != prev_right_button || !mouse.can_read(*m_server_process)) {
            prev_left_button = left_button;
            prev_right_button = right_button;
            screen.on_receive_mouse_data(dx, dy, left_button, right_button);
            dx = 0;
            dy = 0;
        }
    }
}

void WSMessageLoop::drain_keyboard()
{
    auto& screen = WSScreen::the();
    auto& keyboard = Keyboard::the();
    while (keyboard.can_read(*m_server_process)) {
        Keyboard::Event event;
        ssize_t nread = keyboard.read(*m_server_process, (byte*)&event, sizeof(Keyboard::Event));
        ASSERT(nread == sizeof(Keyboard::Event));
        screen.on_receive_keyboard_data(event);
    }
}

void WSMessageLoop::notify_client_died(int client_id)
{
    LOCKER(m_lock);
    auto* client = WSClientConnection::from_client_id(client_id);
    if (!client)
        return;
    post_message(client, make<WSClientDisconnectedNotification>(client_id));
}

ssize_t WSMessageLoop::on_receive_from_client(int client_id, const byte* data, size_t size)
{
    // FIXME: This should not be necessary.. why is this necessary?
    while (!running())
        Scheduler::yield();

    LOCKER(m_lock);

    WSClientConnection* client = WSClientConnection::ensure_for_client_id(client_id);

    ASSERT(size == sizeof(GUI_ClientMessage));
    auto& message = *reinterpret_cast<const GUI_ClientMessage*>(data);
    switch (message.type) {
    case GUI_ClientMessage::Type::CreateMenubar:
        post_message(client, make<WSAPICreateMenubarRequest>(client_id));
        break;
    case GUI_ClientMessage::Type::DestroyMenubar:
        post_message(client, make<WSAPIDestroyMenubarRequest>(client_id, message.menu.menubar_id));
        break;
    case GUI_ClientMessage::Type::SetApplicationMenubar:
        post_message(client, make<WSAPISetApplicationMenubarRequest>(client_id, message.menu.menubar_id));
        break;
    case GUI_ClientMessage::Type::AddMenuToMenubar:
        post_message(client, make<WSAPIAddMenuToMenubarRequest>(client_id, message.menu.menubar_id, message.menu.menu_id));
        break;
    case GUI_ClientMessage::Type::CreateMenu:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPICreateMenuRequest>(client_id, String(message.text, message.text_length)));
        break;
    case GUI_ClientMessage::Type::DestroyMenu:
        post_message(client, make<WSAPIDestroyMenuRequest>(client_id, message.menu.menu_id));
        break;
    case GUI_ClientMessage::Type::AddMenuItem:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPIAddMenuItemRequest>(client_id, message.menu.menu_id, message.menu.identifier, String(message.text, message.text_length)));
        break;
    case GUI_ClientMessage::Type::CreateWindow:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPICreateWindowRequest>(client_id, message.window.rect, String(message.text, message.text_length)));
        break;
    case GUI_ClientMessage::Type::DestroyWindow:
        post_message(client, make<WSAPIDestroyWindowRequest>(client_id, message.window_id));
        break;
    case GUI_ClientMessage::Type::SetWindowTitle:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPISetWindowTitleRequest>(client_id, message.window_id, String(message.text, message.text_length)));
        break;
    case GUI_ClientMessage::Type::GetWindowTitle:
        ASSERT(message.text_length < sizeof(message.text));
        post_message(client, make<WSAPIGetWindowTitleRequest>(client_id, message.window_id));
        break;
    case GUI_ClientMessage::Type::SetWindowRect:
        post_message(client, make<WSAPISetWindowRectRequest>(client_id, message.window_id, message.window.rect));
        break;
    case GUI_ClientMessage::Type::GetWindowRect:
        post_message(client, make<WSAPIGetWindowRectRequest>(client_id, message.window_id));
        break;
    case GUI_ClientMessage::Type::InvalidateRect:
        post_message(client, make<WSAPIInvalidateRectRequest>(client_id, message.window_id, message.window.rect));
        break;
    case GUI_ClientMessage::Type::DidFinishPainting:
        post_message(client, make<WSAPIDidFinishPaintingNotification>(client_id, message.window_id, message.window.rect));
        break;
    case GUI_ClientMessage::Type::GetWindowBackingStore:
        post_message(client, make<WSAPIGetWindowBackingStoreRequest>(client_id, message.window_id));
        break;
    case GUI_ClientMessage::Type::ReleaseWindowBackingStore:
        post_message(client, make<WSAPIReleaseWindowBackingStoreRequest>(client_id, (int)message.backing.backing_store_id));
        break;
    case GUI_ClientMessage::Type::SetGlobalCursorTracking:
        post_message(client, make<WSAPISetGlobalCursorTrackingRequest>(client_id, message.window_id, message.value));
        break;
    }
    server_process().request_wakeup();
    return size;
}
