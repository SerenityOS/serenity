#pragma once

#include <AK/LogStream.h>
#include <AK/String.h>

class GTextPosition {
public:
    GTextPosition() {}
    GTextPosition(int line, int column)
        : m_line(line)
        , m_column(column)
    {
    }

    bool is_valid() const { return m_line >= 0 && m_column >= 0; }

    int line() const { return m_line; }
    int column() const { return m_column; }

    void set_line(int line) { m_line = line; }
    void set_column(int column) { m_column = column; }

    bool operator==(const GTextPosition& other) const { return m_line == other.m_line && m_column == other.m_column; }
    bool operator!=(const GTextPosition& other) const { return m_line != other.m_line || m_column != other.m_column; }
    bool operator<(const GTextPosition& other) const { return m_line < other.m_line || (m_line == other.m_line && m_column < other.m_column); }

private:
    int m_line { -1 };
    int m_column { -1 };
};

inline const LogStream& operator<<(const LogStream& stream, const GTextPosition& value)
{
    if (!value.is_valid())
        return stream << "GTextPosition(Invalid)";
    return stream << String::format("(%d,%d)", value.line(), value.column());
}
