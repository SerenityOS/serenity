/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <AK/String.h>

namespace Gfx {
template<typename T>
class Vector2 final {
public:
    constexpr Vector2() = default;
    constexpr Vector2(T x, T y)
        : m_x(x)
        , m_y(y)
    {
    }

    constexpr T x() const { return m_x; }
    constexpr T y() const { return m_y; }

    constexpr void set_x(T value) { m_x = value; }
    constexpr void set_y(T value) { m_y = value; }

    constexpr Vector2& operator+=(const Vector2& other)
    {
        m_x += other.m_x;
        m_y += other.m_y;
        return *this;
    }

    constexpr Vector2& operator-=(const Vector2& other)
    {
        m_x -= other.m_x;
        m_y -= other.m_y;
        return *this;
    }

    constexpr Vector2 operator+(const Vector2& other) const
    {
        return Vector2(m_x + other.m_x, m_y + other.m_y);
    }

    constexpr Vector2 operator-(const Vector2& other) const
    {
        return Vector2(m_x - other.m_x, m_y - other.m_y);
    }

    constexpr Vector2 operator-() const
    {
        return Vector2(-m_x, -m_y);
    }

    constexpr Vector2 operator*(const Vector2& other) const
    {
        return Vector2(m_x * other.m_x, m_y * other.m_y);
    }

    constexpr Vector2 operator/(const Vector2& other) const
    {
        return Vector2(m_x / other.m_x, m_y / other.m_y);
    }

    template<typename U>
    constexpr Vector2 operator*(U f) const
    {
        return Vector2(m_x * f, m_y * f);
    }

    template<typename U>
    constexpr Vector2 operator/(U f) const
    {
        return Vector2(m_x / f, m_y / f);
    }

    constexpr T dot(const Vector2& other) const
    {
        return m_x * other.m_x + m_y * other.m_y;
    }

    constexpr Vector2 normalized() const
    {
        T inv_length = 1 / length();
        return *this * inv_length;
    }

    constexpr Vector2 clamped(T m, T x) const
    {
        Vector2 copy { *this };
        copy.clamp(m, x);
        return copy;
    }

    constexpr void clamp(T min_value, T max_value)
    {
        m_x = max(min_value, m_x);
        m_y = max(min_value, m_y);
        m_x = min(max_value, m_x);
        m_y = min(max_value, m_y);
    }

    constexpr void normalize()
    {
        T inv_length = 1 / length();
        m_x *= inv_length;
        m_y *= inv_length;
    }

    constexpr T length() const
    {
        return AK::hypot(m_x, m_y);
    }

    String to_string() const
    {
        return String::formatted("[{},{}]", x(), y());
    }

private:
    T m_x;
    T m_y;
};

typedef Vector2<float> FloatVector2;
typedef Vector2<double> DoubleVector2;

}

namespace AK {

template<typename T>
struct Formatter<Gfx::Vector2<T>> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::Vector2<T> const& value)
    {
        return Formatter<StringView>::format(builder, value.to_string());
    }
};

}

using Gfx::DoubleVector2;
using Gfx::FloatVector2;
using Gfx::Vector2;
