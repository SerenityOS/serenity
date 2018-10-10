#pragma once

#include "String.h"
#include "Vector.h"

namespace AK {

class StringBuilder {
public:
    StringBuilder() { }
    ~StringBuilder() { }

    void append(String&&);
    void append(char);

    String build();

private:
    Vector<String> m_strings;
};

}

using AK::StringBuilder;

