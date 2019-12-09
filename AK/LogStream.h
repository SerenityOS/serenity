#pragma once

#include <AK/Types.h>
#include <AK/kstdio.h>

#ifdef USERLAND
#    include <AK/ScopedValueRollback.h>
#    include <AK/StringView.h>
#    include <errno.h>
#    include <unistd.h>
#endif

namespace AK {

class String;
class StringView;

class LogStream {
public:
    LogStream()
#ifdef USERLAND
        : m_errno_restorer(errno)
#endif
    {
    }
    virtual ~LogStream() {}

    virtual void write(const char*, int) const = 0;

private:
#ifdef USERLAND
    ScopedValueRollback<int> m_errno_restorer;
#endif
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
        dbgputstr(characters, length);
    }
};

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
const LogStream& operator<<(const LogStream&, i32);
const LogStream& operator<<(const LogStream&, u32);
const LogStream& operator<<(const LogStream&, u64);

#ifdef __serenity__
inline const LogStream& operator<<(const LogStream& stream, size_t value)
{
    if constexpr (sizeof(size_t) == 4)
        return stream << (u32)value;
    else
        return stream << (u64)value;
}
#endif

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

DebugLogStream dbg();

}

using AK::dbg;
using AK::LogStream;
