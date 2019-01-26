#pragma once

#include <AK/Weakable.h>

class WSMessage;

class WSMessageReceiver : public Weakable<WSMessageReceiver> {
public:
    WSMessageReceiver();
    virtual ~WSMessageReceiver();

    virtual void event(WSMessage&) = 0;
};
