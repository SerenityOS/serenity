#pragma once

#include <AK/kstdio.h>

namespace AK {

class String;
class StringView;

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
    int length = 0;
    const char* p = value;
    while (*(p++))
        ++length;
    stream.write(value, length);
    return stream;
}

const LogStream& operator<<(const LogStream&, const String&);
const LogStream& operator<<(const LogStream&, const StringView&);
const LogStream& operator<<(const LogStream&, int);
const LogStream& operator<<(const LogStream&, unsigned);
const LogStream& operator<<(const LogStream&, const void*);

inline const LogStream& operator<<(const LogStream& stream, char value)
{
    stream.write(&value, 1);
    return stream;
}

inline const LogStream& operator<<(const LogStream& stream, bool value)
{
    return stream << (value ? "true" : "false");
}

}

using AK::LogStream;
using AK::dbg;
