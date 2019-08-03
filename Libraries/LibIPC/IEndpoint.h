#pragma once

#include <AK/AKString.h>

class IEndpoint {
public:
    virtual ~IEndpoint();

    virtual String name() const = 0;

protected:
    IEndpoint();

private:
    String m_name;
};
