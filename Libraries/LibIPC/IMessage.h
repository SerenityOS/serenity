#pragma once

#include <AK/AKString.h>
#include <AK/ByteBuffer.h>

class IMessage {
public:
    virtual ~IMessage();

    virtual int id() const = 0;
    virtual String name() const = 0;
    virtual ByteBuffer encode() = 0;

protected:
    IMessage();
};
