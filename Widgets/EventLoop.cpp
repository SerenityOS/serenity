#include "EventLoop.h"
#include "Event.h"
#include "Object.h"
#include "WindowManager.h"
#include "AbstractScreen.h"

#ifdef SERENITY
#include "PS2MouseDevice.h"
#include "Scheduler.h"
#endif

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
#ifdef SERENITY
    m_server_process = current;
#endif
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

#ifdef SERENITY
void EventLoop::waitForEvent()
{
    auto& mouse = PS2MouseDevice::the();
    auto& screen = AbstractScreen::the();
    bool prev_left_button = screen.left_mouse_button_pressed();
    bool prev_right_button = screen.right_mouse_button_pressed();
    int dx = 0;
    int dy = 0;
    while (mouse.has_data_available_for_reading()) {
        signed_byte data[3];
        ssize_t nread = mouse.read((byte*)data, 3);
        ASSERT(nread == 3);
        bool left_button = data[0] & 1;
        bool right_button = data[0] & 2;
        dx += data[1];
        dy += -data[2];
        if (left_button != prev_left_button || right_button != prev_right_button || !mouse.has_data_available_for_reading()) {
            prev_left_button = left_button;
            prev_right_button = right_button;
            screen.on_receive_mouse_data(dx, dy, left_button, right_button);
            dx = 0;
            dy = 0;
        }
    }
}
#endif

#ifdef USE_SDL
static inline MouseButton toMouseButton(byte sdlButton)
{
    if (sdlButton == 1)
        return MouseButton::Left;
    if (sdlButton == 2)
        return MouseButton::Middle;
    if (sdlButton == 3)
        return MouseButton::Right;
    ASSERT_NOT_REACHED();
    return MouseButton::None;
}

void EventLoop::handleKeyEvent(Event::Type type, const SDL_KeyboardEvent& sdlKey)
{
    auto keyEvent = make<KeyEvent>(type, 0);
    int key = 0;

    switch (sdlKey.keysym.sym) {
    case SDLK_LEFT: key = KeyboardKey::LeftArrow; break;
    case SDLK_RIGHT: key = KeyboardKey::RightArrow; break;
    case SDLK_UP: key = KeyboardKey::UpArrow; break;
    case SDLK_DOWN: key = KeyboardKey::DownArrow; break;
    case SDLK_BACKSPACE: key = KeyboardKey::Backspace; break;
    case SDLK_RETURN: key = KeyboardKey::Return; break;
    }
    keyEvent->m_key = key;

    if (sdlKey.keysym.sym > SDLK_UNKNOWN && sdlKey.keysym.sym <= 'z') {
        char buf[] = { 0, 0 };
        char& ch = buf[0];
        ch = (char)sdlKey.keysym.sym;
        if (sdlKey.keysym.mod & KMOD_SHIFT) {
            if (ch >= 'a' && ch <= 'z') {
                ch &= ~0x20;
            } else {
                switch (ch) {
                case '1': ch = '!'; break;
                case '2': ch = '@'; break;
                case '3': ch = '#'; break;
                case '4': ch = '$'; break;
                case '5': ch = '%'; break;
                case '6': ch = '^'; break;
                case '7': ch = '&'; break;
                case '8': ch = '*'; break;
                case '9': ch = '('; break;
                case '0': ch = ')'; break;
                case '-': ch = '_'; break;
                case '=': ch = '+'; break;
                case '`': ch = '~'; break;
                case ',': ch = '<'; break;
                case '.': ch = '>'; break;
                case '/': ch = '?'; break;
                case '[': ch = '{'; break;
                case ']': ch = '}'; break;
                case '\\': ch = '|'; break;
                case '\'': ch = '"'; break;
                case ';': ch = ':'; break;
                }
            }
        }
        keyEvent->m_text = buf;
    }

    keyEvent->m_shift = sdlKey.keysym.mod & KMOD_SHIFT;
    keyEvent->m_ctrl = sdlKey.keysym.mod & KMOD_CTRL;
    keyEvent->m_alt = sdlKey.keysym.mod & KMOD_ALT;

    postEvent(&WindowManager::the(), move(keyEvent));
}

void EventLoop::waitForEvent()
{
    SDL_Event sdlEvent;
    while (SDL_PollEvent(&sdlEvent) != 0) {
        switch (sdlEvent.type) {
        case SDL_QUIT:
            postEvent(nullptr, make<QuitEvent>());
            return;
        case SDL_WINDOWEVENT:
            if (sdlEvent.window.event == SDL_WINDOWEVENT_EXPOSED) {
                // Spam paint events whenever we get exposed.
                // This is obviously not ideal, but the SDL backend here is just a prototype anyway.
                postEvent(&WindowManager::the(), make<PaintEvent>());
            }
            return;
        case SDL_MOUSEMOTION:
            postEvent(&WindowManager::the(), make<MouseEvent>(Event::MouseMove, sdlEvent.motion.x, sdlEvent.motion.y));
            return;
        case SDL_MOUSEBUTTONDOWN:
            postEvent(&WindowManager::the(), make<MouseEvent>(Event::MouseDown, sdlEvent.button.x, sdlEvent.button.y, toMouseButton(sdlEvent.button.button)));
            return;
        case SDL_MOUSEBUTTONUP:
            postEvent(&WindowManager::the(), make<MouseEvent>(Event::MouseUp, sdlEvent.button.x, sdlEvent.button.y, toMouseButton(sdlEvent.button.button)));
            return;
        case SDL_KEYDOWN:
            handleKeyEvent(Event::KeyDown, sdlEvent.key);
            return;
        case SDL_KEYUP:
            handleKeyEvent(Event::KeyUp, sdlEvent.key);
            return;
        }
    }
}
#endif
