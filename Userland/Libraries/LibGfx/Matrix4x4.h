/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGfx/Matrix.h>
#include <LibGfx/Vector3.h>
#include <math.h>

namespace Gfx {

template<typename T>
class Matrix4x4 final : public Matrix<4, T> {
public:
    Matrix4x4() = default;
    Matrix4x4(T _11, T _12, T _13, T _14,
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

    auto elements() const { return m_elements; }
    auto elements() { return m_elements; }

    Matrix4x4 operator*(const Matrix4x4& other) const
    {
        Matrix4x4 product;
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                product.m_elements[i][j] = m_elements[0][j] * other.m_elements[i][0]
                    + m_elements[1][j] * other.m_elements[i][1]
                    + m_elements[2][j] * other.m_elements[i][2]
                    + m_elements[3][j] * other.m_elements[i][3];
            }
        }
        return product;
    }

    Vector3<T> transform_point(const Vector3<T>& p) const
    {
        return Vector3<T>(
            p.x() * m_elements[0][0] + p.y() * m_elements[1][0] + p.z() * m_elements[2][0] + m_elements[3][0],
            p.x() * m_elements[0][1] + p.y() * m_elements[1][1] + p.z() * m_elements[2][1] + m_elements[3][1],
            p.x() * m_elements[0][2] + p.y() * m_elements[1][2] + p.z() * m_elements[2][2] + m_elements[3][2]);
    }

    static Matrix4x4 translate(const Vector3<T>& p)
    {
        return Matrix4x4(
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            p.x(), p.y(), p.z(), 1);
    }

    static Matrix4x4 scale(const Vector3<T>& s)
    {
        return Matrix4x4(
            s.x(), 0, 0, 0,
            0, s.y(), 0, 0,
            0, 0, s.z(), 0,
            0, 0, 0, 1);
    }

    static Matrix4x4 rotate(const Vector3<T>& axis, T angle)
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

private:
    T m_elements[4][4];
};

typedef Matrix4x4<float> FloatMatrix4x4;
typedef Matrix4x4<double> DoubleMatrix4x4;

}

using Gfx::DoubleMatrix4x4;
using Gfx::FloatMatrix4x4;
using Gfx::Matrix4x4;
