#include "EventLoopSDL.h"
#include "Event.h"
#include <SDL.h>
#include "Widget.h"
#include "TerminalWidget.h"
#include "WindowManager.h"
#include <unistd.h>

int g_fd;
extern TerminalWidget* g_tw;

EventLoopSDL::EventLoopSDL()
{
}

EventLoopSDL::~EventLoopSDL()
{
}

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

void EventLoopSDL::handleKeyEvent(Event::Type type, const SDL_KeyboardEvent& sdlKey)
{
    auto keyEvent = make<KeyEvent>(type, 0);
    int key = 0;

    switch (sdlKey.keysym.sym) {
    case SDLK_LEFT: key = KeyboardKey::LeftArrow; break;
    case SDLK_RIGHT: key = KeyboardKey::RightArrow; break;
    case SDLK_UP: key = KeyboardKey::UpArrow; break;
    case SDLK_DOWN: key = KeyboardKey::DownArrow; break;
    case SDLK_BACKSPACE: key = KeyboardKey::Backspace; break;
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

    postEvent(&WindowManager::the(), std::move(keyEvent));
}

void EventLoopSDL::waitForEvent()
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

    fd_set rfds;
    FD_ZERO(&rfds);
    FD_SET(g_fd, &rfds);

    struct timeval tv = { 0, 5000 };
    int rc = select(g_fd + 1, &rfds, NULL, NULL, &tv);

    //printf("select{%d} = %d\n", g_fd, rc);

    if (rc > 0) {
        byte buf[1024];
        int nread = read(g_fd, buf, sizeof(buf));
        g_tw->onReceive(ByteBuffer::wrap(buf, nread));
    }
}

