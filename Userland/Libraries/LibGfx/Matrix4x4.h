/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Matrix.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>
#include <math.h>

namespace Gfx {

template<typename T>
class Matrix4x4 final {
public:
    constexpr Matrix4x4() = default;
    constexpr Matrix4x4(T _11, T _12, T _13, T _14,
        T _21, T _22, T _23, T _24,
        T _31, T _32, T _33, T _34,
        T _41, T _42, T _43, T _44)
        : m_elements {
            _11, _12, _13, _14,
            _21, _22, _23, _24,
            _31, _32, _33, _34,
            _41, _42, _43, _44
        }
    {
    }

    constexpr auto elements() const { return m_elements; }
    constexpr auto elements() { return m_elements; }

    constexpr Matrix4x4 operator*(const Matrix4x4& other) const
    {
        Matrix4x4 product;
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                product.m_elements[i][j] = m_elements[i][0] * other.m_elements[0][j]
                    + m_elements[i][1] * other.m_elements[1][j]
                    + m_elements[i][2] * other.m_elements[2][j]
                    + m_elements[i][3] * other.m_elements[3][j];
            }
        }
        return product;
    }

    constexpr Vector4<T> operator*(const Vector4<T>& v) const
    {
        return Vector4<T>(
            v.x() * m_elements[0][0] + v.y() * m_elements[0][1] + v.z() * m_elements[0][2] + v.w() * m_elements[0][3],
            v.x() * m_elements[1][0] + v.y() * m_elements[1][1] + v.z() * m_elements[1][2] + v.w() * m_elements[1][3],
            v.x() * m_elements[2][0] + v.y() * m_elements[2][1] + v.z() * m_elements[2][2] + v.w() * m_elements[2][3],
            v.x() * m_elements[3][0] + v.y() * m_elements[3][1] + v.z() * m_elements[3][2] + v.w() * m_elements[3][3]);
    }

    constexpr Vector3<T> transform_point(const Vector3<T>& p) const
    {
        return Vector3<T>(
            p.x() * m_elements[0][0] + p.y() * m_elements[0][1] + p.z() * m_elements[0][2] + m_elements[0][3],
            p.x() * m_elements[1][0] + p.y() * m_elements[1][1] + p.z() * m_elements[1][2] + m_elements[1][3],
            p.x() * m_elements[2][0] + p.y() * m_elements[2][1] + p.z() * m_elements[2][2] + m_elements[2][3]);
    }

    constexpr static Matrix4x4 identity()
    {
        return Matrix4x4(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1);
    }

    constexpr static Matrix4x4 translate(const Vector3<T>& p)
    {
        return Matrix4x4(
            1, 0, 0, p.x(),
            0, 1, 0, p.y(),
            0, 0, 1, p.z(),
            0, 0, 0, 1);
    }

    constexpr static Matrix4x4 scale(const Vector3<T>& s)
    {
        return Matrix4x4(
            s.x(), 0, 0, 0,
            0, s.y(), 0, 0,
            0, 0, s.z(), 0,
            0, 0, 0, 1);
    }

    constexpr static Matrix4x4 rotate(const Vector3<T>& axis, T angle)
    {
        T c = cos(angle);
        T s = sin(angle);
        T t = 1 - c;
        T x = axis.x();
        T y = axis.y();
        T z = axis.z();

        return Matrix4x4(
            t * x * x + c, t * x * y - z * s, t * x * z + y * s, 0,
            t * x * y + z * s, t * y * y + c, t * y * z - x * s, 0,
            t * x * z - y * s, t * y * z + x * s, t * z * z + c, 0,
            0, 0, 0, 1);
    }

    constexpr Matrix4x4 transpose() const
    {
        Matrix4x4 result;
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                result.m_elements[i][j] = m_elements[j][i];
            }
        }
        return result;
    }

private:
    T m_elements[4][4];
};

typedef Matrix4x4<float> FloatMatrix4x4;
typedef Matrix4x4<double> DoubleMatrix4x4;

}

using Gfx::DoubleMatrix4x4;
using Gfx::FloatMatrix4x4;
using Gfx::Matrix4x4;
