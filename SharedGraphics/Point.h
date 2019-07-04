#pragma once

#include <AK/AKString.h>
#include <AK/LogStream.h>

class Rect;
struct WSAPI_Point;

class Point {
public:
    Point() {}
    Point(int x, int y)
        : m_x(x)
        , m_y(y)
    {
    }
    Point(const WSAPI_Point&);

    int x() const { return m_x; }
    int y() const { return m_y; }

    void set_x(int x) { m_x = x; }
    void set_y(int y) { m_y = y; }

    void move_by(int dx, int dy)
    {
        m_x += dx;
        m_y += dy;
    }

    void move_by(const Point& delta)
    {
        move_by(delta.x(), delta.y());
    }

    Point translated(const Point& delta) const
    {
        Point point = *this;
        point.move_by(delta);
        return point;
    }

    Point translated(int dx, int dy) const
    {
        Point point = *this;
        point.move_by(dx, dy);
        return point;
    }

    void constrain(const Rect&);

    bool operator==(const Point& other) const
    {
        return m_x == other.m_x
            && m_y == other.m_y;
    }

    bool operator!=(const Point& other) const
    {
        return !(*this == other);
    }

    Point operator-() const { return { -m_x, -m_y }; }
    Point operator-(const Point& other) const { return { m_x - other.m_x, m_y - other.m_y }; }

    operator WSAPI_Point() const;
    String to_string() const { return String::format("[%d,%d]", x(), y()); }

    bool is_null() const { return !m_x && !m_y; }

private:
    int m_x { 0 };
    int m_y { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, const Point& value)
{
    return stream << value.to_string();
}
