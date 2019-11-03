#pragma once

#include <AK/LogStream.h>
#include <LibGUI/GTextPosition.h>

class GTextRange {
public:
    GTextRange() {}
    GTextRange(const GTextPosition& start, const GTextPosition& end)
        : m_start(start)
        , m_end(end)
    {
    }

    bool is_valid() const { return m_start.is_valid() && m_end.is_valid(); }
    void clear()
    {
        m_start = {};
        m_end = {};
    }

    GTextPosition& start() { return m_start; }
    GTextPosition& end() { return m_end; }
    const GTextPosition& start() const { return m_start; }
    const GTextPosition& end() const { return m_end; }

    GTextRange normalized() const { return GTextRange(normalized_start(), normalized_end()); }

    void set_start(const GTextPosition& position) { m_start = position; }
    void set_end(const GTextPosition& position) { m_end = position; }

    void set(const GTextPosition& start, const GTextPosition& end)
    {
        m_start = start;
        m_end = end;
    }

    bool operator==(const GTextRange& other) const
    {
        return m_start == other.m_start && m_end == other.m_end;
    }

    bool contains(const GTextPosition& position) const
    {
        if (!(position.line() > m_start.line() || (position.line() == m_start.line() && position.column() >= m_start.column())))
            return false;
        if (!(position.line() < m_end.line() || (position.line() == m_end.line() && position.column() <= m_end.column())))
            return false;
        return true;
    }

private:
    GTextPosition normalized_start() const { return m_start < m_end ? m_start : m_end; }
    GTextPosition normalized_end() const { return m_start < m_end ? m_end : m_start; }

    GTextPosition m_start;
    GTextPosition m_end;
};

inline const LogStream& operator<<(const LogStream& stream, const GTextRange& value)
{
    if (!value.is_valid())
        return stream << "GTextRange(Invalid)";
    return stream << value.start() << '-' << value.end();
}
