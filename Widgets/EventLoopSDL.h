#pragma once

#include "EventLoop.h"

class EventLoopSDL final : public EventLoop {
public:
    EventLoopSDL();
    virtual ~EventLoopSDL() override;

private:
    virtual void waitForEvent() override;
};

