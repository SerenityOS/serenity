#pragma once

#include "AKString.h"
#include "Vector.h"
#include <stdarg.h>

namespace AK {

class StringBuilder {
public:
    explicit StringBuilder(int initial_capacity = 16);
    ~StringBuilder() {}

    void append(const StringView&);
    void append(char);
    void append(const char*, int);
    void appendf(const char*, ...);
    void appendvf(const char*, va_list);

    String to_string();
    ByteBuffer to_byte_buffer();

private:
    void will_append(int);

    ByteBuffer m_buffer;
    int m_length { 0 };
};

}

using AK::StringBuilder;
