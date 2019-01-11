#include "EventLoop.h"
#include "Event.h"
#include "Object.h"
#include "WindowManager.h"

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
    for (;;) {
        if (m_queuedEvents.is_empty())
            waitForEvent();
        auto events = move(m_queuedEvents);
        for (auto& queuedEvent : events) {
            auto* receiver = queuedEvent.receiver;
            auto& event = *queuedEvent.event;
            //printf("EventLoop: Object{%p} event %u (%s)\n", receiver, (unsigned)event.type(), event.name());
            if (!receiver) {
                switch (event.type()) {
                case Event::Quit:
                    return 0;
                default:
                    printf("event type %u with no receiver :(\n", event.type());
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
