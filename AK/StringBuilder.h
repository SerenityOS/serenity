#pragma once

#include "AKString.h"
#include "Vector.h"
#include <LibC/stdarg.h>

namespace AK {

class StringBuilder {
public:
    explicit StringBuilder(size_t initial_capacity = 16);
    ~StringBuilder() { }

    void append(const String&);
    void append(char);
    void append(const char*, size_t);
    void appendf(const char*, ...);
    void appendvf(const char*, va_list);

    String build();
    ByteBuffer to_byte_buffer();

private:
    void will_append(size_t);

    ByteBuffer m_buffer;
    size_t m_length { 0 };
};

}

using AK::StringBuilder;

