#include <AK/PrintfImplementation.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <stdarg.h>

namespace AK {

inline void StringBuilder::will_append(int size)
{
    if ((m_length + size) > m_buffer.size())
        m_buffer.grow(max((int)16, m_buffer.size() * 2 + size));
}

StringBuilder::StringBuilder(int initial_capacity)
{
    m_buffer.grow(initial_capacity);
}

void StringBuilder::append(const StringView& str)
{
    if (str.is_empty())
        return;
    will_append(str.length());
    memcpy(m_buffer.pointer() + m_length, str.characters_without_null_termination(), str.length());
    m_length += str.length();
}

void StringBuilder::append(const char* characters, int length)
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
    m_length = 0;
    return move(m_buffer);
}

String StringBuilder::to_string()
{
    auto string = String((const char*)m_buffer.pointer(), m_length);
    m_buffer.clear();
    m_length = 0;
    return string;
}

}
