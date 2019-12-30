#pragma once

#include <AK/String.h>

typedef Vector<u8, 1024> IMessageBuffer;

class IMessage {
public:
    virtual ~IMessage();

    virtual int endpoint_magic() const = 0;
    virtual int message_id() const = 0;
    virtual String message_name() const = 0;
    virtual IMessageBuffer encode() const = 0;

protected:
    IMessage();
};
