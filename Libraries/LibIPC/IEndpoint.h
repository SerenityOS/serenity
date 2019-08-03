#pragma once

#include <AK/AKString.h>

class IEndpoint {
public:
    virtual ~IEndpoint();

    const String& name() const { return m_name; }

protected:
    IEndpoint();

private:
    String m_name;
};
