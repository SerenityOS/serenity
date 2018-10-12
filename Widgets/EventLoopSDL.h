#pragma once

#include "Event.h"
#include "EventLoop.h"
#include <SDL.h>

class EventLoopSDL final : public EventLoop {
public:
    EventLoopSDL();
    virtual ~EventLoopSDL() override;

private:
    virtual void waitForEvent() override;

    void handleKeyEvent(Event::Type, const SDL_KeyboardEvent&);
};

