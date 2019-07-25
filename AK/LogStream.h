#pragma once

#include <AK/kstdio.h>

#ifdef USERLAND
#include <AK/ScopedValueRollback.h>
#include <errno.h>
#endif

namespace AK {

class String;
class StringView;

class TStyle {
public:
    enum NoneTag { DummyValue };
    static NoneTag None;

    enum Color {
        Black = 0,
        Red,
        Green,
        Brown,
        Blue,
        Magenta,
        Cyan,
        LightGray,
        DarkGray,
        BrightRed,
        BrightGreen,
        Yellow,
        BrightBlue,
        BrightMagenta,
        BrightCyan,
        White,
        NoColor = 255,
    };
    enum Attribute {
        NoAttribute = 0,
        Bold = 1,
    };

    TStyle() {}
    TStyle(NoneTag) {}
    TStyle(Color color, unsigned attributes = NoAttribute)
        : m_color(color)
        , m_attributes(attributes)
    {
    }

    ~TStyle() {}

    Color color() const { return m_color; }
    unsigned attributes() const { return m_attributes; }

private:
    Color m_color { NoColor };
    unsigned m_attributes { NoAttribute };
};

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

protected:
    friend const LogStream& operator<<(const LogStream&, const TStyle&);
    mutable bool m_needs_style_reset { false };

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
        if (m_needs_style_reset)
            write("\033[0m", 4);
        char newline = '\n';
        write(&newline, 1);
    }

    virtual void write(const char* characters, int length) const override
    {
        dbgputstr(characters, length);
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
const LogStream& operator<<(const LogStream& stream, const TStyle&);

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

using AK::dbg;
using AK::LogStream;
using AK::TStyle;
