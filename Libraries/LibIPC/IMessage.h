#pragma once

#include <AK/AKString.h>
#include <AK/ByteBuffer.h>

class IMessage {
public:
    virtual ~IMessage();

    const String& name() const { return m_name; }
    virtual ByteBuffer encode() = 0;

protected:
    IMessage();

private:
    String m_name;
};
