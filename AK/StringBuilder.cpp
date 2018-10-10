#include "StringBuilder.h"

namespace AK {

void StringBuilder::append(String&& str)
{
    m_strings.append(std::move(str));
}

void StringBuilder::append(char ch)
{
    m_strings.append(StringImpl::create(&ch, 1));
}

String StringBuilder::build()
{
    auto strings = std::move(m_strings);
    if (strings.isEmpty())
        return String::empty();

    return String();
}

}

