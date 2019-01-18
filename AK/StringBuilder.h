#pragma once

#include "AKString.h"
#include "Vector.h"

namespace AK {

class StringBuilder {
public:
    StringBuilder() { }
    ~StringBuilder() { }

    void append(const String&);
    void append(char);
    void appendf(const char*, ...);

    String build();
    ByteBuffer to_byte_buffer();

private:
    void will_append(size_t);

    ByteBuffer m_buffer;
    size_t m_length { 0 };
};

}

using AK::StringBuilder;

