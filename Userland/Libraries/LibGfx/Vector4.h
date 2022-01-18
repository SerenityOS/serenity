/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <AK/String.h>

namespace Gfx {
template<typename T>
class Vector3;

template<typename T>
class Vector4 final {
public:
    constexpr Vector4() = default;
    constexpr Vector4(T x, T y, T z, T w)
        : m_x(x)
        , m_y(y)
        , m_z(z)
        , m_w(w)
    {
    }

    constexpr T x() const { return m_x; }
    constexpr T y() const { return m_y; }
    constexpr T z() const { return m_z; }
    constexpr T w() const { return m_w; }

    constexpr void set_x(T value) { m_x = value; }
    constexpr void set_y(T value) { m_y = value; }
    constexpr void set_z(T value) { m_z = value; }
    constexpr void set_w(T value) { m_w = value; }

    constexpr Vector4& operator+=(const Vector4& other)
    {
        m_x += other.m_x;
        m_y += other.m_y;
        m_z += other.m_z;
        m_w += other.m_w;
        return *this;
    }

    constexpr Vector4& operator-=(const Vector4& other)
    {
        m_x -= other.m_x;
        m_y -= other.m_y;
        m_z -= other.m_z;
        m_w -= other.m_w;
        return *this;
    }

    constexpr Vector4 operator+(const Vector4& other) const
    {
        return Vector4(m_x + other.m_x, m_y + other.m_y, m_z + other.m_z, m_w + other.m_w);
    }

    constexpr Vector4 operator-(const Vector4& other) const
    {
        return Vector4(m_x - other.m_x, m_y - other.m_y, m_z - other.m_z, m_w - other.m_w);
    }

    constexpr Vector4 operator-() const
    {
        return Vector4(-m_x, -m_y, -m_z, -m_w);
    }

    constexpr Vector4 operator*(const Vector4& other) const
    {
        return Vector4(m_x * other.m_x, m_y * other.m_y, m_z * other.m_z, m_w * other.m_w);
    }

    constexpr Vector4 operator/(const Vector4& other) const
    {
        return Vector4(m_x / other.m_x, m_y / other.m_y, m_z / other.m_z, m_w / other.m_w);
    }

    template<typename U>
    constexpr Vector4 operator*(U f) const
    {
        return Vector4(m_x * f, m_y * f, m_z * f, m_w * f);
    }

    template<typename U>
    constexpr Vector4 operator/(U f) const
    {
        return Vector4(m_x / f, m_y / f, m_z / f, m_w / f);
    }

    constexpr T dot(const Vector4& other) const
    {
        return m_x * other.m_x + m_y * other.m_y + m_z * other.m_z + m_w * other.m_w;
    }

    constexpr Vector4 normalized() const
    {
        T inv_length = 1 / length();
        return *this * inv_length;
    }

    constexpr Vector4 clamped(T m, T x) const
    {
        Vector4 copy { *this };
        copy.clamp(m, x);
        return copy;
    }

    constexpr void clamp(T min_value, T max_value)
    {
        m_x = max(min_value, m_x);
        m_y = max(min_value, m_y);
        m_z = max(min_value, m_z);
        m_w = max(min_value, m_w);
        m_x = min(max_value, m_x);
        m_y = min(max_value, m_y);
        m_z = min(max_value, m_z);
        m_w = min(max_value, m_w);
    }

    constexpr void normalize()
    {
        T inv_length = 1 / length();
        m_x *= inv_length;
        m_y *= inv_length;
        m_z *= inv_length;
        m_w *= inv_length;
    }

    constexpr T length() const
    {
        return AK::sqrt(m_x * m_x + m_y * m_y + m_z * m_z + m_w * m_w);
    }

    constexpr Vector3<T> xyz() const
    {
        return Vector3<T>(m_x, m_y, m_z);
    }

    String to_string() const
    {
        return String::formatted("[{},{},{},{}]", x(), y(), z(), w());
    }

private:
    T m_x;
    T m_y;
    T m_z;
    T m_w;
};

typedef Vector4<float> FloatVector4;
typedef Vector4<double> DoubleVector4;

}

namespace AK {

template<typename T>
struct Formatter<Gfx::Vector4<T>> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::Vector4<T> const& value)
    {
        return Formatter<StringView>::format(builder, value.to_string());
    }
};

}

using Gfx::DoubleVector4;
using Gfx::FloatVector4;
using Gfx::Vector4;
