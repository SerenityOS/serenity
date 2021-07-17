/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Math.h>
#include <LibGfx/Matrix.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>

namespace Gfx {

template<typename T>
using Matrix4x4 = Matrix<4, T>;

template<typename T>
constexpr static Vector4<T> operator*(const Matrix4x4<T>& m, const Vector4<T>& v)
{
    auto const& elements = m.elements();
    return Vector4<T>(
        v.x() * elements[0][0] + v.y() * elements[0][1] + v.z() * elements[0][2] + v.w() * elements[0][3],
        v.x() * elements[1][0] + v.y() * elements[1][1] + v.z() * elements[1][2] + v.w() * elements[1][3],
        v.x() * elements[2][0] + v.y() * elements[2][1] + v.z() * elements[2][2] + v.w() * elements[2][3],
        v.x() * elements[3][0] + v.y() * elements[3][1] + v.z() * elements[3][2] + v.w() * elements[3][3]);
}

template<typename T>
constexpr static Vector3<T> transform_point(const Matrix4x4<T>& m, const Vector3<T>& p)
{
    auto const& elements = m.elements();
    return Vector3<T>(
        p.x() * elements[0][0] + p.y() * elements[0][1] + p.z() * elements[0][2] + elements[0][3],
        p.x() * elements[1][0] + p.y() * elements[1][1] + p.z() * elements[1][2] + elements[1][3],
        p.x() * elements[2][0] + p.y() * elements[2][1] + p.z() * elements[2][2] + elements[2][3]);
}

template<typename T>
constexpr static Vector3<T> transform_direction(const Matrix4x4<T>& m, const Vector3<T>& d)
{
    auto const& elements = m.elements();
    return Vector3<T>(
        d.x() * elements[0][0] + d.y() * elements[0][1] + d.z() * elements[0][2],
        d.x() * elements[1][0] + d.y() * elements[1][1] + d.z() * elements[1][2],
        d.x() * elements[2][0] + d.y() * elements[2][1] + d.z() * elements[2][2]);
}

template<typename T>
constexpr static Matrix4x4<T> translation_matrix(const Vector3<T>& p)
{
    return Matrix4x4<T>(
        1, 0, 0, p.x(),
        0, 1, 0, p.y(),
        0, 0, 1, p.z(),
        0, 0, 0, 1);
}

template<typename T>
constexpr static Matrix4x4<T> scale_matrix(const Vector3<T>& s)
{
    return Matrix4x4<T>(
        s.x(), 0, 0, 0,
        0, s.y(), 0, 0,
        0, 0, s.z(), 0,
        0, 0, 0, 1);
}

template<typename T>
constexpr static Matrix4x4<T> rotation_matrix(const Vector3<T>& axis, T angle)
{
    T c = AK::cos(angle);
    T s = AK::sin(angle);
    T t = 1 - c;
    T x = axis.x();
    T y = axis.y();
    T z = axis.z();

    return Matrix4x4<T>(
        t * x * x + c, t * x * y - z * s, t * x * z + y * s, 0,
        t * x * y + z * s, t * y * y + c, t * y * z - x * s, 0,
        t * x * z - y * s, t * y * z + x * s, t * z * z + c, 0,
        0, 0, 0, 1);
}

typedef Matrix4x4<float> FloatMatrix4x4;
typedef Matrix4x4<double> DoubleMatrix4x4;
}

using Gfx::DoubleMatrix4x4;
using Gfx::FloatMatrix4x4;
using Gfx::Matrix4x4;
