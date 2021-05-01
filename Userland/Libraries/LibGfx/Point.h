/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Orientation.h>
#include <LibIPC/Forward.h>
#include <math.h>
#include <stdlib.h>

namespace Gfx {

template<typename T>
class Point {
public:
    Point() { }

    Point(T x, T y)
        : m_x(x)
        , m_y(y)
    {
    }

    template<typename U>
    Point(U x, U y)
        : m_x(x)
        , m_y(y)
    {
    }

    template<typename U>
    explicit Point(const Point<U>& other)
        : m_x(other.x())
        , m_y(other.y())
    {
    }

    T x() const { return m_x; }
    T y() const { return m_y; }

    void set_x(T x) { m_x = x; }
    void set_y(T y) { m_y = y; }

    void move_by(T dx, T dy)
    {
        m_x += dx;
        m_y += dy;
    }

    void move_by(const Point<T>& delta)
    {
        move_by(delta.x(), delta.y());
    }

    Point<T> translated(const Point<T>& delta) const
    {
        Point<T> point = *this;
        point.move_by(delta);
        return point;
    }

    Point<T> translated(T dx, T dy) const
    {
        Point<T> point = *this;
        point.move_by(dx, dy);
        return point;
    }

    Point<T> translated(T dboth) const
    {
        Point<T> point = *this;
        point.move_by(dboth, dboth);
        return point;
    }

    void constrain(const Rect<T>&);
    Point<T> constrained(const Rect<T>& rect) const
    {
        Point<T> point = *this;
        point.constrain(rect);
        return point;
    }

    template<class U>
    bool operator==(const Point<U>& other) const
    {
        return x() == other.x() && y() == other.y();
    }

    template<class U>
    bool operator!=(const Point<U>& other) const
    {
        return !(*this == other);
    }

    Point<T> operator+(const Point<T>& other) const { return { m_x + other.m_x, m_y + other.m_y }; }

    Point<T>& operator+=(const Point<T>& other)
    {
        m_x += other.m_x;
        m_y += other.m_y;
        return *this;
    }

    Point<T> operator-() const { return { -m_x, -m_y }; }

    Point<T> operator-(const Point<T>& other) const { return { m_x - other.m_x, m_y - other.m_y }; }

    Point<T>& operator-=(const Point<T>& other)
    {
        m_x -= other.m_x;
        m_y -= other.m_y;
        return *this;
    }

    Point<T> operator*(T factor) const { return { m_x * factor, m_y * factor }; }

    Point<T>& operator*=(T factor)
    {
        m_x *= factor;
        m_y *= factor;
        return *this;
    }

    Point<T> operator/(T factor) const { return { m_x / factor, m_y / factor }; }

    Point<T>& operator/=(T factor)
    {
        m_x /= factor;
        m_y /= factor;
        return *this;
    }

    bool is_null() const { return !m_x && !m_y; }

    T primary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? y() : x();
    }

    void set_primary_offset_for_orientation(Orientation orientation, T value)
    {
        if (orientation == Orientation::Vertical) {
            set_y(value);
        } else {
            set_x(value);
        }
    }

    T secondary_offset_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? x() : y();
    }

    void set_secondary_offset_for_orientation(Orientation orientation, T value)
    {
        if (orientation == Orientation::Vertical) {
            set_x(value);
        } else {
            set_y(value);
        }
    }

    T dx_relative_to(const Point<T>& other) const
    {
        return x() - other.x();
    }

    T dy_relative_to(const Point<T>& other) const
    {
        return y() - other.y();
    }

    // Returns pixels moved from other in either direction
    T pixels_moved(const Point<T>& other) const
    {
        return max(abs(dx_relative_to(other)), abs(dy_relative_to(other)));
    }

    float distance_from(const Point<T>& other) const
    {
        if (*this == other)
            return 0;
        return sqrtf(powf(m_x - other.m_x, 2.0f) + powf(m_y - other.m_y, 2.0f));
    }

    Point absolute_relative_distance_to(const Point& other) const
    {
        return { abs(dx_relative_to(other)), abs(dy_relative_to(other)) };
    }

    template<typename U>
    Point<U> to_type() const
    {
        return Point<U>(*this);
    }

    String to_string() const;

private:
    T m_x { 0 };
    T m_y { 0 };
};

using IntPoint = Point<int>;
using FloatPoint = Point<float>;

}

namespace AK {

template<typename T>
struct Formatter<Gfx::Point<T>> : Formatter<StringView> {
    void format(FormatBuilder& builder, const Gfx::Point<T>& value)
    {
        Formatter<StringView>::format(builder, value.to_string());
    }
};

}

namespace IPC {

bool encode(Encoder&, const Gfx::IntPoint&);
bool decode(Decoder&, Gfx::IntPoint&);

}
