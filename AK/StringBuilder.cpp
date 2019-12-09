#include <AK/PrintfImplementation.h>
#include <AK/StdLibExtras.h>
#include <AK/StringBuilder.h>
#include <stdarg.h>

namespace AK {

inline void StringBuilder::will_append(size_t size)
{
    if ((m_length + size) > (size_t)m_buffer.size())
        m_buffer.grow(max((size_t)16, (size_t)m_buffer.size() * 2 + size));
}

StringBuilder::StringBuilder(size_t initial_capacity)
{
    m_buffer.grow((int)initial_capacity);
}

void StringBuilder::append(const StringView& str)
{
    if (str.is_empty())
        return;
    will_append(str.length());
    memcpy(m_buffer.data() + m_length, str.characters_without_null_termination(), str.length());
    m_length += str.length();
}

void StringBuilder::append(const char* characters, size_t length)
{
    if (!length)
        return;
    will_append(length);
    memcpy(m_buffer.data() + m_length, characters, length);
    m_length += length;
}

void StringBuilder::append(char ch)
{
    will_append(1);
    m_buffer.data()[m_length] = ch;
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
    auto string = String((const char*)m_buffer.data(), m_length);
    clear();
    return string;
}

StringView StringBuilder::string_view() const
{
    return StringView { (const char*)m_buffer.data(), m_length };
}

void StringBuilder::clear()
{
    m_buffer.clear();
    m_length = 0;
}

}
