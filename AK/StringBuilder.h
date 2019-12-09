#pragma once

#include <AK/String.h>
#include <AK/Vector.h>
#include <stdarg.h>

namespace AK {

class StringBuilder {
public:
    using OutputType = String;

    explicit StringBuilder(size_t initial_capacity = 16);
    ~StringBuilder() {}

    void append(const StringView&);
    void append(char);
    void append(const char*, size_t);
    void appendf(const char*, ...);
    void appendvf(const char*, va_list);

    String build() { return to_string(); }

    String to_string();
    ByteBuffer to_byte_buffer();

    StringView string_view() const;
    void clear();

    size_t length() const { return m_length; }
    void trim(size_t count) { m_length -= count; }

private:
    void will_append(size_t);

    ByteBuffer m_buffer;
    size_t m_length { 0 };
};

}

using AK::StringBuilder;
