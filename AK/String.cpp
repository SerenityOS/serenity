#include "String.h"
#include "StdLib.h"

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

String String::empty()
{
    return StringImpl::theEmptyStringImpl();
}

String String::substring(size_t start, size_t length) const
{
    ASSERT(m_impl);
    ASSERT(start + length <= m_impl->length());
    // FIXME: This needs some input bounds checking.
    char* buffer;
    auto newImpl = StringImpl::createUninitialized(length, buffer);
    memcpy(buffer, characters() + start, length);
    buffer[length] = '\0';
    return newImpl;
}

Vector<String> String::split(const char separator) const
{
    if (isEmpty())
        return { };

    Vector<String> v;
    size_t substart = 0;
    for (size_t i = 0; i < length(); ++i) {
        char ch = characters()[i];
        if (ch == separator) {
            size_t sublen = i - substart;
            if (sublen != 0)
                v.append(substring(substart, sublen));
            substart = i + 1;
        }
    }
    size_t taillen = length() - substart;
    if (taillen != 0)
        v.append(substring(substart, taillen));
    return v;
}

ByteBuffer String::toByteBuffer() const
{
    if (!m_impl)
        return nullptr;
    return ByteBuffer::copy(reinterpret_cast<const byte*>(characters()), length());
}

}
