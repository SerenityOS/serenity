#pragma once

#include <AK/AKString.h>
#include <AK/StringView.h>
#include <AK/kstdio.h>

class LogStream {
public:
    LogStream() {}
    virtual ~LogStream() {}

    virtual void write(const char*, int) const = 0;
};

class DebugLogStream final : public LogStream {
public:
    DebugLogStream() {}
    virtual ~DebugLogStream() override
    {
        char newline = '\n';
        write(&newline, 1);
    }

    virtual void write(const char* characters, int length) const override
    {
        for (int i = 0; i < length; ++i)
            dbgprintf("%c", characters[i]);
    }
};

inline DebugLogStream dbg()
{
    return {};
}

inline const LogStream& operator<<(const LogStream& stream, const char* value)
{
    stream.write(value, strlen(value));
    return stream;
}

inline const LogStream& operator<<(const LogStream& stream, const String& value)
{
    stream.write(value.characters(), value.length());
    return stream;
}

inline const LogStream& operator<<(const LogStream& stream, const StringView& value)
{
    stream.write(value.characters(), value.length());
    return stream;
}

inline const LogStream& operator<<(const LogStream& stream, char value)
{
    stream.write(&value, 1);
    return stream;
}

inline const LogStream& operator<<(const LogStream& stream, bool value)
{
    return stream << (value ? "true" : "false");
}

inline const LogStream& operator<<(const LogStream& stream, int value)
{
    return stream << String::number(value);
}

inline const LogStream& operator<<(const LogStream& stream, unsigned value)
{
    return stream << String::number(value);
}

inline const LogStream& operator<<(const LogStream& stream, const void* value)
{
    return stream << String::format("%p", value);
}
