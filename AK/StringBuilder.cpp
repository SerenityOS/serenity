#include "StringBuilder.h"
#include "printf.cpp"
#include <AK/StdLibExtras.h>
#include <LibC/stdarg.h>

namespace AK {

inline void StringBuilder::will_append(ssize_t size)
{
    if ((m_length + size) > m_buffer.size())
        m_buffer.grow(max((ssize_t)16, m_buffer.size() * 2 + size));
}

StringBuilder::StringBuilder(ssize_t initial_capacity)
{
    m_buffer.grow(initial_capacity);
}

void StringBuilder::append(const String& str)
{
    if (str.is_empty())
        return;
    will_append(str.length());
    memcpy(m_buffer.pointer() + m_length, str.characters(), str.length());
    m_length += str.length();
}

void StringBuilder::append(const char* characters, ssize_t length)
{
    if (!length)
        return;
    will_append(length);
    memcpy(m_buffer.pointer() + m_length, characters, length);
    m_length += length;
}

void StringBuilder::append(char ch)
{
    will_append(1);
    m_buffer.pointer()[m_length] = ch;
    m_length += 1;
}

void StringBuilder::appendvf(const char* fmt, va_list ap)
{
    printf_internal([this](char*&, char ch) {
        append(ch);
    },
        nullptr, fmt, ap);
}

void StringBuilder::appendf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    will_append(strlen(fmt));
    appendvf(fmt, ap);
    va_end(ap);
}

ByteBuffer StringBuilder::to_byte_buffer()
{
    m_buffer.trim(m_length);
    return move(m_buffer);
}

String StringBuilder::to_string()
{
    auto string = String((const char*)m_buffer.pointer(), m_length);
    m_buffer.clear();
    return string;
}

}
