#pragma once

#include <AK/String.h>
#include <AK/ByteBuffer.h>

class IMessage {
public:
    virtual ~IMessage();

    virtual int endpoint_magic() const = 0;
    virtual int id() const = 0;
    virtual String name() const = 0;
    virtual ByteBuffer encode() const = 0;

protected:
    IMessage();
};
