#include <AK/AKString.h>
#include <AK/LogStream.h>
#include <AK/StringView.h>

namespace AK {

const LogStream& operator<<(const LogStream& stream, const String& value)
{
    stream.write(value.characters(), value.length());
    return stream;
}

const LogStream& operator<<(const LogStream& stream, const StringView& value)
{
    stream.write(value.characters_without_null_termination(), value.length());
    return stream;
}

const LogStream& operator<<(const LogStream& stream, int value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, unsigned value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, const void* value)
{
    return stream << String::format("%p", value);
}

}
