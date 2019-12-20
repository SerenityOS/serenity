#include <AK/String.h>
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

const LogStream& operator<<(const LogStream& stream, i32 value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, u32 value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, u64 value)
{
    return stream << String::number(value);
}

const LogStream& operator<<(const LogStream& stream, const void* value)
{
    return stream << String::format("%p", value);
}

#if defined (__serenity__) && !defined(KERNEL)
static TriState got_process_name = TriState::Unknown;
static char process_name_buffer[256];
#endif

DebugLogStream dbg()
{
    DebugLogStream stream;
#if defined (__serenity__) && !defined(KERNEL)
    if (got_process_name == TriState::Unknown) {
        if (get_process_name(process_name_buffer, sizeof(process_name_buffer)) == 0)
            got_process_name = TriState::True;
        else
            got_process_name = TriState::False;
    }
    if (got_process_name == TriState::True)
        stream << "\033[33;1m" << process_name_buffer << '(' << getpid() << ")\033[0m: ";
#endif
    return stream;
}

}
