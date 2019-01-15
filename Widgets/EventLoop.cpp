#include "EventLoop.h"
#include "Event.h"
#include "Object.h"
#include "WindowManager.h"
#include "AbstractScreen.h"
#include "PS2MouseDevice.h"
#include "Scheduler.h"

static EventLoop* s_mainEventLoop;

void EventLoop::initialize()
{
    s_mainEventLoop = nullptr;
}

EventLoop::EventLoop()
{
    if (!s_mainEventLoop)
        s_mainEventLoop = this;
}

EventLoop::~EventLoop()
{
}

EventLoop& EventLoop::main()
{
    ASSERT(s_mainEventLoop);
    return *s_mainEventLoop;
}

int EventLoop::exec()
{
    m_server_process = current;
    m_running = true;
    for (;;) {
        if (m_queuedEvents.is_empty())
            waitForEvent();
        Vector<QueuedEvent> events;
        {
            InterruptDisabler disabler;
            events = move(m_queuedEvents);
        }
        for (auto& queuedEvent : events) {
            auto* receiver = queuedEvent.receiver;
            auto& event = *queuedEvent.event;
            //printf("EventLoop: Object{%p} event %u (%s)\n", receiver, (unsigned)event.type(), event.name());
            if (!receiver) {
                switch (event.type()) {
                case Event::Quit:
                    ASSERT_NOT_REACHED();
                    return 0;
                default:
                    printf("event type %u with no receiver :(\n", event.type());
                    ASSERT_NOT_REACHED();
                    return 1;
                }
            } else {
                receiver->event(event);
            }
        }
    }
}

void EventLoop::postEvent(Object* receiver, OwnPtr<Event>&& event)
{
    //printf("EventLoop::postEvent: {%u} << receiver=%p, event=%p\n", m_queuedEvents.size(), receiver, event.ptr());
    m_queuedEvents.append({ receiver, move(event) });
}

void EventLoop::waitForEvent()
{
    auto& mouse = PS2MouseDevice::the();
    auto& screen = AbstractScreen::the();
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
