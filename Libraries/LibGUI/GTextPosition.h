#pragma once

#include <AK/LogStream.h>
#include <AK/String.h>

class GTextPosition {
public:
    GTextPosition() {}
    GTextPosition(size_t line, size_t column)
        : m_line(line)
        , m_column(column)
    {
    }

    bool is_valid() const { return m_line != 0xffffffffu && m_column != 0xffffffffu; }

    size_t line() const { return m_line; }
    size_t column() const { return m_column; }

    void set_line(size_t line) { m_line = line; }
    void set_column(size_t column) { m_column = column; }

    bool operator==(const GTextPosition& other) const { return m_line == other.m_line && m_column == other.m_column; }
    bool operator!=(const GTextPosition& other) const { return m_line != other.m_line || m_column != other.m_column; }
    bool operator<(const GTextPosition& other) const { return m_line < other.m_line || (m_line == other.m_line && m_column < other.m_column); }

private:
    size_t m_line { 0xffffffff };
    size_t m_column { 0xffffffff };
};

inline const LogStream& operator<<(const LogStream& stream, const GTextPosition& value)
{
    if (!value.is_valid())
        return stream << "GTextPosition(Invalid)";
    return stream << String::format("(%zu,%zu)", value.line(), value.column());
}
