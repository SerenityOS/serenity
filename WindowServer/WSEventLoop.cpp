#include "WSEventLoop.h"
#include "WSEvent.h"
#include "WSEventReceiver.h"
#include "WSWindowManager.h"
#include "WSScreen.h"
#include "PS2MouseDevice.h"
#include <Kernel/Keyboard.h>
#include <AK/Bitmap.h>
#include "Process.h"

//#define WSEVENTLOOP_DEBUG

static WSEventLoop* s_the;

void WSEventLoop::initialize()
{
    s_the = nullptr;
}

WSEventLoop::WSEventLoop()
{
    if (!s_the)
        s_the = this;
}

WSEventLoop::~WSEventLoop()
{
}

WSEventLoop& WSEventLoop::the()
{
    ASSERT(s_the);
    return *s_the;
}

int WSEventLoop::exec()
{
    m_server_process = current;

    m_keyboard_fd = m_server_process->sys$open("/dev/keyboard", O_RDONLY);
    m_mouse_fd = m_server_process->sys$open("/dev/psaux", O_RDONLY);

    ASSERT(m_keyboard_fd >= 0);
    ASSERT(m_mouse_fd >= 0);

    m_running = true;
    for (;;) {

        if (m_queued_events.is_empty())
            wait_for_event();

        Vector<QueuedEvent> events;
        {
            ASSERT_INTERRUPTS_ENABLED();
            LOCKER(m_lock);
            events = move(m_queued_events);
        }

        for (auto& queued_event : events) {
            auto* receiver = queued_event.receiver;
            auto& event = *queued_event.event;
#ifdef WSEVENTLOOP_DEBUG
            dbgprintf("WSEventLoop: receiver{%p} event %u (%s)\n", receiver, (unsigned)event.type(), event.name());
#endif
            if (!receiver) {
                dbgprintf("WSEvent type %u with no receiver :(\n", event.type());
                ASSERT_NOT_REACHED();
                return 1;
            } else {
                receiver->event(event);
            }
        }
    }
}

void WSEventLoop::post_event(WSEventReceiver* receiver, OwnPtr<WSEvent>&& event)
{
    ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
#ifdef WSEVENTLOOP_DEBUG
    dbgprintf("WSEventLoop::post_event: {%u} << receiver=%p, event=%p\n", m_queued_events.size(), receiver, event.ptr());
#endif

    if (event->type() == WSEvent::WM_Invalidate) {
        for (auto& queued_event : m_queued_events) {
            if (receiver == queued_event.receiver
                && queued_event.event->type() == WSEvent::WM_Invalidate
                && (queued_event.event->rect().is_empty() || queued_event.event->rect().contains(event->rect()))) {
#ifdef WSEVENTLOOP_DEBUG
                dbgprintf("Swallow WM_Invalidate\n");
#endif
                return;
            }
        }
    }

    m_queued_events.append({ receiver, move(event) });

    if (current != m_server_process)
        m_server_process->request_wakeup();
}

void WSEventLoop::wait_for_event()
{
    fd_set rfds;
    memset(&rfds, 0, sizeof(rfds));
    auto bitmap = Bitmap::wrap((byte*)&rfds, FD_SETSIZE);
    bitmap.set(m_keyboard_fd, true);
    bitmap.set(m_mouse_fd, true);
    Syscall::SC_select_params params;
    params.nfds = max(m_keyboard_fd, m_mouse_fd) + 1;
    params.readfds = &rfds;
    params.writefds = nullptr;
    params.exceptfds = nullptr;
    params.timeout = nullptr;
    int rc = m_server_process->sys$select(&params);
    memory_barrier();
    if (rc < 0) {
        ASSERT_NOT_REACHED();
    }

    if (bitmap.get(m_keyboard_fd))
        drain_keyboard();
    if (bitmap.get(m_mouse_fd))
        drain_mouse();
}

void WSEventLoop::drain_mouse()
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
        dx += data[1] ? (int)data[1] - (int)((data[0] << 4) & 0x100) : 0;
        dy += data[2] ? (int)((data[0] << 3) & 0x100) - (int)data[2] : 0;
        if (left_button != prev_left_button || right_button != prev_right_button || !mouse.can_read(*m_server_process)) {
            prev_left_button = left_button;
            prev_right_button = right_button;
            screen.on_receive_mouse_data(dx, dy, left_button, right_button);
            dx = 0;
            dy = 0;
        }
    }
}

void WSEventLoop::drain_keyboard()
{
    auto& screen = WSScreen::the();
    auto& keyboard = Keyboard::the();
    while (keyboard.can_read(*m_server_process)) {
        byte data[2];
        ssize_t nread = keyboard.read(*m_server_process, (byte*)data, sizeof(data));
        ASSERT(nread == sizeof(data));
        Keyboard::Key key;
        key.character = data[0];
        key.modifiers = data[1];
        screen.on_receive_keyboard_data(key);
    }
}
