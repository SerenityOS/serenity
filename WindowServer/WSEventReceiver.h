#pragma once

#include <AK/Weakable.h>

class WSEvent;

class WSEventReceiver : public Weakable<WSEventReceiver> {
public:
    WSEventReceiver();
    virtual ~WSEventReceiver();

    virtual void event(WSEvent&) = 0;
};
