#pragma once

#include "AKString.h"
#include "Vector.h"

namespace AK {

class StringBuilder {
public:
    StringBuilder() { }
    ~StringBuilder() { }

    void append(const String&);
    void append(String&&);
    void append(char);
    void appendf(const char*, ...);

    String build();

private:
    Vector<String> m_strings;
};

}

using AK::StringBuilder;

