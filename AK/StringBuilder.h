#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <stdarg.h>

namespace AK {

class StringBuilder {
public:
    using OutputType = String;

    explicit StringBuilder(int initial_capacity = 16);
    ~StringBuilder() {}

    void append(const StringView&);
    void append(char);
    void append(const char*, int);
    void appendf(const char*, ...);
    void appendvf(const char*, va_list);

    String build() { return to_string(); }

    String to_string();
    ByteBuffer to_byte_buffer();

    StringView string_view() const;
    void clear();

private:
    void will_append(int);

    ByteBuffer m_buffer;
    int m_length { 0 };
};

}

using AK::StringBuilder;
