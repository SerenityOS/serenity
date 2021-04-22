/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <math.h>

namespace Gfx {
template<typename T>
class Vector3 {
public:
    Vector3() = default;
    Vector3(T x, T y, T z)
        : m_x(x)
        , m_y(y)
        , m_z(z)
    {
    }

    T x() const { return m_x; }
    T y() const { return m_y; }
    T z() const { return m_z; }

    void set_x(T value) { m_x = value; }
    void set_y(T value) { m_y = value; }
    void set_z(T value) { m_z = value; }

    Vector3 operator+(const Vector3& other) const
    {
        return Vector3(m_x + other.m_x, m_y + other.m_y, m_z + other.m_z);
    }

    Vector3 operator-(const Vector3& other) const
    {
        return Vector3(m_x - other.m_x, m_y - other.m_y, m_z - other.m_z);
    }

    Vector3 operator*(T f) const
    {
        return Vector3(m_x * f, m_y * f, m_z * f);
    }

    Vector3 operator/(T f) const
    {
        return Vector3(m_x / f, m_y / f, m_z / f);
    }

    T dot(const Vector3& other) const
    {
        return m_x * other.m_x + m_y * other.m_y + m_z * other.m_z;
    }

    Vector3 cross(const Vector3& other) const
    {
        return Vector3(
            m_y * other.m_z - m_z * other.m_y,
            m_z * other.m_x - m_x * other.m_z,
            m_x * other.m_y - m_y * other.m_x);
    }

    Vector3 normalized() const
    {
        T inv_length = 1 / length();
        return *this * inv_length;
    }

    Vector3 clamped(T m, T x) const
    {
        Vector3 copy { *this };
        copy.clamp(m, x);
        return copy;
    }

    void clamp(T min_value, T max_value)
    {
        m_x = max(min_value, m_x);
        m_y = max(min_value, m_y);
        m_z = max(min_value, m_z);
        m_x = min(max_value, m_x);
        m_y = min(max_value, m_y);
        m_z = min(max_value, m_z);
    }

    void normalize()
    {
        T inv_length = 1 / length();
        m_x *= inv_length;
        m_y *= inv_length;
        m_z *= inv_length;
    }

    T length() const
    {
        return sqrt(m_x * m_x + m_y * m_y + m_z * m_z);
    }

private:
    T m_x;
    T m_y;
    T m_z;
};

typedef Vector3<float> FloatVector3;
typedef Vector3<double> DoubleVector3;

}

using Gfx::DoubleVector3;
using Gfx::FloatVector3;
using Gfx::Vector3;
