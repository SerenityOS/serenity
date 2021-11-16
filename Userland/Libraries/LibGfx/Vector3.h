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
class Vector3 final {
public:
    constexpr Vector3() = default;
    constexpr Vector3(T x, T y, T z)
        : m_x(x)
        , m_y(y)
        , m_z(z)
    {
    }

    constexpr T x() const { return m_x; }
    constexpr T y() const { return m_y; }
    constexpr T z() const { return m_z; }

    constexpr void set_x(T value) { m_x = value; }
    constexpr void set_y(T value) { m_y = value; }
    constexpr void set_z(T value) { m_z = value; }

    constexpr Vector3& operator+=(const Vector3& other)
    {
        m_x += other.m_x;
        m_y += other.m_y;
        m_z += other.m_z;
        return *this;
    }

    constexpr Vector3& operator-=(const Vector3& other)
    {
        m_x -= other.m_x;
        m_y -= other.m_y;
        m_z -= other.m_z;
        return *this;
    }

    constexpr Vector3 operator+(const Vector3& other) const
    {
        return Vector3(m_x + other.m_x, m_y + other.m_y, m_z + other.m_z);
    }

    constexpr Vector3 operator-(const Vector3& other) const
    {
        return Vector3(m_x - other.m_x, m_y - other.m_y, m_z - other.m_z);
    }

    constexpr Vector3 operator*(const Vector3& other) const
    {
        return Vector3(m_x * other.m_x, m_y * other.m_y, m_z * other.m_z);
    }

    constexpr Vector3 operator/(const Vector3& other) const
    {
        return Vector3(m_x / other.m_x, m_y / other.m_y, m_z / other.m_z);
    }

    constexpr Vector3 operator*(T f) const
    {
        return Vector3(m_x * f, m_y * f, m_z * f);
    }

    constexpr Vector3 operator/(T f) const
    {
        return Vector3(m_x / f, m_y / f, m_z / f);
    }

    constexpr T dot(const Vector3& other) const
    {
        return m_x * other.m_x + m_y * other.m_y + m_z * other.m_z;
    }

    constexpr Vector3 cross(const Vector3& other) const
    {
        return Vector3(
            m_y * other.m_z - m_z * other.m_y,
            m_z * other.m_x - m_x * other.m_z,
            m_x * other.m_y - m_y * other.m_x);
    }

    constexpr Vector3 normalized() const
    {
        T inv_length = 1 / length();
        return *this * inv_length;
    }

    constexpr Vector3 clamped(T m, T x) const
    {
        Vector3 copy { *this };
        copy.clamp(m, x);
        return copy;
    }

    constexpr void clamp(T min_value, T max_value)
    {
        m_x = max(min_value, m_x);
        m_y = max(min_value, m_y);
        m_z = max(min_value, m_z);
        m_x = min(max_value, m_x);
        m_y = min(max_value, m_y);
        m_z = min(max_value, m_z);
    }

    constexpr void normalize()
    {
        T inv_length = 1 / length();
        m_x *= inv_length;
        m_y *= inv_length;
        m_z *= inv_length;
    }

    constexpr T length() const
    {
        return AK::sqrt(m_x * m_x + m_y * m_y + m_z * m_z);
    }

    String to_string() const
    {
        return String::formatted("[{},{},{}]", x(), y(), z());
    }

private:
    T m_x;
    T m_y;
    T m_z;
};

typedef Vector3<float> FloatVector3;
typedef Vector3<double> DoubleVector3;

}

namespace AK {

template<typename T>
struct Formatter<Gfx::Vector3<T>> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::Vector3<T> const& value)
    {
        return Formatter<StringView>::format(builder, value.to_string());
    }
};

}

using Gfx::DoubleVector3;
using Gfx::FloatVector3;
using Gfx::Vector3;
