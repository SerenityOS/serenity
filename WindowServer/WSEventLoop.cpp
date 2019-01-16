#include "WSEventLoop.h"
#include "WSEvent.h"
#include "WSEventReceiver.h"
#include "WSWindowManager.h"
#include "WSScreen.h"
#include "PS2MouseDevice.h"
#include "Scheduler.h"

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
    m_running = true;
    for (;;) {
        if (m_queued_events.is_empty())
            waitForEvent();
        Vector<QueuedEvent> events;
        {
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
    //ASSERT_INTERRUPTS_ENABLED();
    LOCKER(m_lock);
#ifdef WSEVENTLOOP_DEBUG
    dbgprintf("WSEventLoop::post_event: {%u} << receiver=%p, event=%p\n", m_queued_events.size(), receiver, event.ptr());
#endif
    m_queued_events.append({ receiver, move(event) });
}

void WSEventLoop::waitForEvent()
{
    auto& mouse = PS2MouseDevice::the();
    auto& screen = WSScreen::the();
    bool prev_left_button = screen.left_mouse_button_pressed();
    bool prev_right_button = screen.right_mouse_button_pressed();
    int dx = 0;
    int dy = 0;
    while (mouse.can_read(*m_server_process)) {
        signed_byte data[3];
        ssize_t nread = mouse.read(*m_server_process, (byte*)data, 3);
        ASSERT(nread == 3);
        bool left_button = data[0] & 1;
        bool right_button = data[0] & 2;
        dx += data[1];
        dy += -data[2];
        if (left_button != prev_left_button || right_button != prev_right_button || !mouse.can_read(*m_server_process)) {
            prev_left_button = left_button;
            prev_right_button = right_button;
            screen.on_receive_mouse_data(dx, dy, left_button, right_button);
            dx = 0;
            dy = 0;
        }
    }
}
