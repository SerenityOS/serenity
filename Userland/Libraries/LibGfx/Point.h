/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/StdLibExtras.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Orientation.h>
#include <LibIPC/Forward.h>
#include <math.h>
#include <stdlib.h>

namespace Gfx {

template<typename T>
class Point {
public:
    Point() = default;

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

    [[nodiscard]] ALWAYS_INLINE T x() const { return m_x; }
    [[nodiscard]] ALWAYS_INLINE T y() const { return m_y; }

    ALWAYS_INLINE void set_x(T x) { m_x = x; }
    ALWAYS_INLINE void set_y(T y) { m_y = y; }

    [[nodiscard]] ALWAYS_INLINE bool is_null() const { return !m_x && !m_y; }
    [[nodiscard]] ALWAYS_INLINE bool is_empty() const { return m_x <= 0 && m_y <= 0; }

    void translate_by(T dx, T dy)
    {
        m_x += dx;
        m_y += dy;
    }

    ALWAYS_INLINE void translate_by(T dboth) { translate_by(dboth, dboth); }
    ALWAYS_INLINE void translate_by(const Point<T>& delta) { translate_by(delta.x(), delta.y()); }

    void scale_by(T dx, T dy)
    {
        m_x *= dx;
        m_y *= dy;
    }

    ALWAYS_INLINE void scale_by(T dboth) { scale_by(dboth, dboth); }
    ALWAYS_INLINE void scale_by(const Point<T>& delta) { scale_by(delta.x(), delta.y()); }

    void transform_by(const AffineTransform& transform) { *this = transform.map(*this); }

    Point<T> translated(const Point<T>& delta) const
    {
        Point<T> point = *this;
        point.translate_by(delta);
        return point;
    }

    Point<T> translated(T dx, T dy) const
    {
        Point<T> point = *this;
        point.translate_by(dx, dy);
        return point;
    }

    Point<T> translated(T dboth) const
    {
        Point<T> point = *this;
        point.translate_by(dboth, dboth);
        return point;
    }

    Point<T> scaled(const Point<T>& delta) const
    {
        Point<T> point = *this;
        point.scale_by(delta);
        return point;
    }

    Point<T> scaled(T sx, T sy) const
    {
        Point<T> point = *this;
        point.scale_by(sx, sy);
        return point;
    }

    Point<T> transformed(const AffineTransform& transform) const
    {
        Point<T> point = *this;
        point.transform_by(transform);
        return point;
    }

    void constrain(const Rect<T>&);
    Point<T> constrained(const Rect<T>& rect) const
    {
        Point<T> point = *this;
        point.constrain(rect);
        return point;
    }

    Point<T> moved_left(T amount) const { return { x() - amount, y() }; }
    Point<T> moved_right(T amount) const { return { x() + amount, y() }; }
    Point<T> moved_up(T amount) const { return { x(), y() - amount }; }
    Point<T> moved_down(T amount) const { return { x(), y() + amount }; }

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
