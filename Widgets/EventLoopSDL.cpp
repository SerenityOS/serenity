#include "EventLoopSDL.h"
#include "Event.h"
#include <SDL.h>
#include "AbstractScreen.h"
#include "Widget.h"
#include "TerminalWidget.h"
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

static inline int toKey(const SDL_Keysym& sym)
{
    return sym.sym;
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
                postEvent(AbstractScreen::the().rootWidget(), make<PaintEvent>());
            }
            return;
        case SDL_MOUSEMOTION:
            postEvent(&AbstractScreen::the(), make<MouseEvent>(Event::MouseMove, sdlEvent.motion.x, sdlEvent.motion.y));
            return;
        case SDL_MOUSEBUTTONDOWN:
            postEvent(&AbstractScreen::the(), make<MouseEvent>(Event::MouseDown, sdlEvent.button.x, sdlEvent.button.y, toMouseButton(sdlEvent.button.button)));
            return;
        case SDL_MOUSEBUTTONUP:
            postEvent(&AbstractScreen::the(), make<MouseEvent>(Event::MouseUp, sdlEvent.button.x, sdlEvent.button.y, toMouseButton(sdlEvent.button.button)));
            return;
        case SDL_KEYDOWN:
            postEvent(&AbstractScreen::the(), make<KeyEvent>(Event::KeyDown, toKey(sdlEvent.key.keysym)));
            return;
        case SDL_KEYUP:
            postEvent(&AbstractScreen::the(), make<KeyEvent>(Event::KeyUp, toKey(sdlEvent.key.keysym)));
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

