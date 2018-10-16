#include "String.h"
#include "StdLib.h"

String::String()
{
}

String::String(const char* characters)
    : m_data(DataBuffer::copy((const BYTE*)characters, strlen(characters) + 1))
{
}

String::String(const char* characters, size_t length)
    : m_data(DataBuffer::createUninitialized(length + 1))
{
    memcpy(m_data->data(), characters, length);
    m_data->data()[length] = '\0';
}

String::String(String&& other)
    : m_data(move(other.m_data))
{
}

String::String(const String& other)
    : m_data(other.m_data)
{
}

String& String::operator=(const String& other)
{
    if (this == &other)
        return *this;
    m_data = other.m_data;
    return *this;
}

String& String::operator=(const String&& other)
{
    if (this == &other)
        return *this;
    m_data = move(other.m_data);
    return *this;
}

String::~String()
{
}

bool String::operator==(const String& other) const
{
    if (length() != other.length())
        return false;
    return strcmp(characters(), other.characters()) == 0;
}

String String::substring(size_t start, size_t length) const
{
    ASSERT(start + length <= m_data->length());
    // FIXME: This needs some input bounds checking.
    auto buffer = DataBuffer::createUninitialized(length + 1);
    memcpy(buffer->data(), characters() + start, length);
    buffer->data()[length] = '\0';
    String s;
    s.m_data = move(buffer);
    return s;
}

Vector<String> String::split(char separator) const
{
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
    size_t taillen = length() - 1 - substart;
    if (taillen != 0)
        v.append(substring(substart, taillen));
    return v;
}
