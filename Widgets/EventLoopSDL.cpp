#include "EventLoopSDL.h"
#include "Event.h"
#include <SDL.h>
#include "AbstractScreen.h"
#include "Widget.h"

EventLoopSDL::EventLoopSDL()
{
}

EventLoopSDL::~EventLoopSDL()
{
}

static inline MouseButton toMouseButton(byte sdlButton)
{
    printf("sdlbutton = %u\n", sdlButton);
    if (sdlButton == 1)
        return MouseButton::Left;
    if (sdlButton == 2)
        return MouseButton::Middle;
    if (sdlButton == 3)
        return MouseButton::Right;
    ASSERT_NOT_REACHED();
    return MouseButton::None;
}

void EventLoopSDL::waitForEvent()
{
    SDL_Event sdlEvent;
    while (SDL_WaitEvent(&sdlEvent) != 0) {
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
        }
    }
}

