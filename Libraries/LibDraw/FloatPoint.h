#pragma once

#include <AK/String.h>
#include <AK/LogStream.h>
#include <LibDraw/Orientation.h>

class FloatRect;

class FloatPoint {
public:
    FloatPoint() {}
    FloatPoint(float x, float y)
        : m_x(x)
        , m_y(y)
    {
    }
    float x() const { return m_x; }
    float y() const { return m_y; }

    void set_x(float x) { m_x = x; }
    void set_y(float y) { m_y = y; }

    void move_by(float dx, float dy)
    {
        m_x += dx;
        m_y += dy;
    }

    void move_by(const FloatPoint& delta)
    {
        move_by(delta.x(), delta.y());
    }

    FloatPoint translated(const FloatPoint& delta) const
    {
        FloatPoint point = *this;
        point.move_by(delta);
        return point;
    }

    FloatPoint translated(float dx, float dy) const
    {
        FloatPoint point = *this;
        point.move_by(dx, dy);
        return point;
    }

    void constrain(const FloatRect&);

    bool operator==(const FloatPoint& other) const
    {
        return m_x == other.m_x
            && m_y == other.m_y;
    }

    bool operator!=(const FloatPoint& other) const
    {
        return !(*this == other);
    }

    FloatPoint operator-() const { return { -m_x, -m_y }; }

    FloatPoint operator-(const FloatPoint& other) const { return { m_x - other.m_x, m_y - other.m_y }; }
    FloatPoint& operator-=(const FloatPoint& other)
    {
        m_x -= other.m_x;
        m_y -= other.m_y;
        return *this;
    }

    FloatPoint& operator+=(const FloatPoint& other)
    {
        m_x += other.m_x;
        m_y += other.m_y;
        return *this;
    }
    FloatPoint operator+(const FloatPoint& other) const { return { m_x + other.m_x, m_y + other.m_y }; }

    String to_string() const { return String::format("[%g,%g]", x(), y()); }

    bool is_null() const { return !m_x && !m_y; }

    float primary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? y() : x();
    }

    void set_primary_offset_for_orientation(Orientation orientation, float value)
    {
        if (orientation == Orientation::Vertical)
            set_y(value);
        else
            set_x(value);
    }

    float secondary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? x() : y();
    }

    void set_secondary_offset_for_orientation(Orientation orientation, float value)
    {
        if (orientation == Orientation::Vertical)
            set_x(value);
        else
            set_y(value);
    }

private:
    float m_x { 0 };
    float m_y { 0 };
};

inline const LogStream& operator<<(const LogStream& stream, const FloatPoint& value)
{
    return stream << value.to_string();
}
