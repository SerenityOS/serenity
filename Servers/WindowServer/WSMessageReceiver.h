#pragma once

#include <AK/Weakable.h>

class WSMessage;

class WSMessageReceiver : public Weakable<WSMessageReceiver> {
public:
    WSMessageReceiver();
    virtual ~WSMessageReceiver();

    virtual void on_message(const WSMessage&) = 0;
};
