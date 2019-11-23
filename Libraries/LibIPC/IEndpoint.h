#pragma once

#include <AK/String.h>
#include <AK/OwnPtr.h>

namespace AK {
class BufferStream;
}

class IMessage;

class IEndpoint {
public:
    virtual ~IEndpoint();

    virtual int magic() const = 0;
    virtual String name() const = 0;
    virtual OwnPtr<IMessage> handle(const IMessage&) = 0;

protected:
    IEndpoint();

private:
    String m_name;
};
