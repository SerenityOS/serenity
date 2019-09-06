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

const LogStream& operator<<(const LogStream& stream, const TStyle& style)
{
    stream << "\033[";

    if (style.color() != TStyle::Color::NoColor)
        stream << ((int)style.color() + 30) << (style.attributes() ? ";" : "");
    else
        stream << '0';

    if (style.attributes() & TStyle::Attribute::Bold)
        stream << '1';

    stream << 'm';

    stream.m_needs_style_reset = true;
    return stream;
}

#ifdef USERLAND
static TriState got_process_name = TriState::Unknown;
static char process_name_buffer[256];
#endif

DebugLogStream dbg()
{
    DebugLogStream stream;
#ifdef USERLAND
    if (got_process_name == TriState::Unknown) {
        if (get_process_name(process_name_buffer, sizeof(process_name_buffer)) == 0)
            got_process_name = TriState::True;
        else
            got_process_name = TriState::False;
    }
    if (got_process_name == TriState::True)
        stream << TStyle(TStyle::Color::Brown, TStyle::Attribute::Bold) << process_name_buffer << '(' << getpid() << ")" << TStyle(TStyle::None) << ": ";
#endif
    return stream;
}

}
