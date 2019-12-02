#pragma once

#include <AK/String.h>
#include <AK/LogStream.h>
#include <LibDraw/Orientation.h>

class Rect;

class Point {
public:
    Point() {}
    Point(int x, int y)
        : m_x(x)
        , m_y(y)
    {
    }

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
    Point& operator-=(const Point& other)
    {
        m_x -= other.m_x;
        m_y -= other.m_y;
        return *this;
    }

    Point& operator+=(const Point& other)
    {
        m_x += other.m_x;
        m_y += other.m_y;
        return *this;
    }
    Point operator+(const Point& other) const { return { m_x + other.m_x, m_y + other.m_y }; }

    String to_string() const { return String::format("[%d,%d]", x(), y()); }

    bool is_null() const { return !m_x && !m_y; }

    int primary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? y() : x();
    }

    void set_primary_offset_for_orientation(Orientation orientation, int value)
    {
        if (orientation == Orientation::Vertical)
            set_y(value);
        else
            set_x(value);
    }

    int secondary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? x() : y();
    }

    void set_secondary_offset_for_orientation(Orientation orientation, int value)
    {
        if (orientation == Orientation::Vertical)
            set_x(value);
        else
            set_y(value);
    }

    // Returns pixels moved from other in either direction
    int pixels_moved(const Point &other) const
    {
        auto pixels_moved = max(
            abs(other.x() - x()),
            abs(other.y() - y())
        );
        return pixels_moved;
    }

private:
    int m_x { 0 };
    int m_y { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, const Point& value)
{
    return stream << value.to_string();
}
