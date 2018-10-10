#include "EventLoopSDL.h"
#include "Event.h"
#include <SDL.h>

EventLoopSDL::EventLoopSDL()
{
}

EventLoopSDL::~EventLoopSDL()
{
}

void EventLoopSDL::waitForEvent()
{
    SDL_Event sdlEvent;
    while (SDL_WaitEvent(&sdlEvent) != 0) {
        switch (sdlEvent.type) {
        case SDL_QUIT:
            postEvent(nullptr, make<QuitEvent>());
            return;
        }
    }
}

