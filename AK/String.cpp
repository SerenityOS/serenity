#include "String.h"
#include <cstring>

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

Vector<String> String::split(const char separator) const
{
    if (isEmpty())
        return { };

    Vector<String> parts;

    auto* characters = this->characters();
    unsigned startOfPart = 0;
    unsigned i = 0;
    for (i = 0; i < length(); ++i) {
        char ch = characters[i];
        if (ch == separator) {
            if (i != startOfPart) {
                parts.append(String(characters + startOfPart, i - startOfPart));
            }
            startOfPart = i + 1;
        }
    }
    if (startOfPart != length())
        parts.append(String(characters + startOfPart, i - startOfPart));
    return parts;
}

ByteBuffer String::toByteBuffer() const
{
    if (!m_impl)
        return nullptr;
    return ByteBuffer::copy(reinterpret_cast<const byte*>(characters()), length());
}

}
