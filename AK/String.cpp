#include "AKString.h"
#include "StdLibExtras.h"
#include "StringBuilder.h"
#include <LibC/stdarg.h>

namespace AK {

bool String::operator==(const String& other) const
{
    if (!m_impl)
        return !other.m_impl;

    if (!other.m_impl)
        return false;

    if (length() != other.length())
        return false;
    
    return !memcmp(characters(), other.characters(), length());
}

bool String::operator<(const String& other) const
{
    if (!m_impl)
        return other.m_impl;

    if (!other.m_impl)
        return false;

    return strcmp(characters(), other.characters()) < 0;
}

String String::empty()
{
    return StringImpl::the_empty_stringimpl();
}

String String::isolated_copy() const
{
    if (!m_impl)
        return { };
    if (!m_impl->length())
        return empty();
    char* buffer;
    auto impl = StringImpl::create_uninitialized(length(), buffer);
    memcpy(buffer, m_impl->characters(), m_impl->length());
    return String(move(*impl));
}

String String::substring(ssize_t start, ssize_t length) const
{
    if (!length)
        return empty();
    ASSERT(m_impl);
    ASSERT(start + length <= m_impl->length());
    // FIXME: This needs some input bounds checking.
    char* buffer;
    auto new_impl = StringImpl::create_uninitialized(length, buffer);
    memcpy(buffer, characters() + start, length);
    buffer[length] = '\0';
    return new_impl;
}

Vector<String> String::split(const char separator) const
{
    if (is_empty())
        return { };

    Vector<String> v;
    ssize_t substart = 0;
    for (ssize_t i = 0; i < length(); ++i) {
        char ch = characters()[i];
        if (ch == separator) {
            ssize_t sublen = i - substart;
            if (sublen != 0)
                v.append(substring(substart, sublen));
            substart = i + 1;
        }
    }
    ssize_t taillen = length() - substart;
    if (taillen != 0)
        v.append(substring(substart, taillen));
    if (characters()[length() - 1] == separator)
        v.append(empty());
    return v;
}

ByteBuffer String::to_byte_buffer() const
{
    if (!m_impl)
        return nullptr;
    return ByteBuffer::copy(reinterpret_cast<const byte*>(characters()), length());
}

String String::from_byte_buffer(const ByteBuffer& buffer, ShouldChomp should_chomp)
{
    if (buffer.is_null())
        return nullptr;
    if (buffer.is_empty())
        return empty();
    return String((const char*)buffer.pointer(), buffer.size(), should_chomp);
}

unsigned String::to_uint(bool& ok) const
{
    unsigned value = 0;
    for (ssize_t i = 0; i < length(); ++i) {
        if (characters()[i] < '0' || characters()[i] > '9') {
            ok = false;
            return 0;
        }
        value = value * 10;
        value += characters()[i] - '0';
    }
    ok = true;
    return value;
}

String String::format(const char* fmt, ...)
{
    StringBuilder builder;
    va_list ap;
    va_start(ap, fmt);
    builder.appendvf(fmt, ap);
    va_end(ap);
    return builder.to_string();
}

bool String::ends_with(const String& str) const
{
    if (str.is_empty())
        return true;
    if (is_empty())
        return false;
    if (str.length() > length())
        return false;
    return !memcmp(characters() + (length() - str.length()), str.characters(), str.length());
}

}
