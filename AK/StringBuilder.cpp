#include "StringBuilder.h"

namespace AK {

void StringBuilder::append(String&& str)
{
    m_strings.append(move(str));
}

void StringBuilder::append(const String& str)
{
    m_strings.append(str);
}

void StringBuilder::append(char ch)
{
    m_strings.append(StringImpl::create(&ch, 1));
}

String StringBuilder::build()
{
    auto strings = move(m_strings);
    if (strings.isEmpty())
        return String::empty();

    size_t sizeNeeded = 0;
    for (auto& string : strings)
        sizeNeeded += string.length();

    char* buffer;
    auto impl = StringImpl::createUninitialized(sizeNeeded, buffer);
    if (!impl)
        return String();

    for (auto& string : strings) {
        memcpy(buffer, string.characters(), string.length());
        buffer += string.length();
    }
    *buffer = '\0';
    return String(move(impl));
}

}

