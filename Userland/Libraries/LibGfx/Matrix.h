/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <initializer_list>

namespace Gfx {

template<size_t N, typename T>
class Matrix {
public:
    static constexpr size_t Size = N;

    constexpr Matrix() = default;
    constexpr Matrix(std::initializer_list<T> elements)
    {
        VERIFY(elements.size() == N * N);
        size_t i = 0;
        for (auto& element : elements) {
            m_elements[i / N][i % N] = element;
            ++i;
        }
    }

    template<typename... Args>
    constexpr Matrix(Args... args)
        : Matrix({ (T)args... })
    {
    }

    Matrix(const Matrix& other)
    {
        __builtin_memcpy(m_elements, other.elements(), sizeof(T) * N * N);
    }

    constexpr auto elements() const { return m_elements; }
    constexpr auto elements() { return m_elements; }

    constexpr Matrix operator*(const Matrix& other) const
    {
        Matrix product;
        for (int i = 0; i < N; ++i) {
            for (int j = 0; j < N; ++j) {
                auto& element = product.m_elements[i][j];

                if constexpr (N == 4) {
                    element = m_elements[i][0] * other.m_elements[0][j]
                        + m_elements[i][1] * other.m_elements[1][j]
                        + m_elements[i][2] * other.m_elements[2][j]
                        + m_elements[i][3] * other.m_elements[3][j];
                } else if constexpr (N == 3) {
                    element = m_elements[i][0] * other.m_elements[0][j]
                        + m_elements[i][1] * other.m_elements[1][j]
                        + m_elements[i][2] * other.m_elements[2][j];
                } else if constexpr (N == 2) {
                    element = m_elements[i][0] * other.m_elements[0][j]
                        + m_elements[i][1] * other.m_elements[1][j];
                } else if constexpr (N == 1) {
                    element = m_elements[i][0] * other.m_elements[0][j];
                } else {
                    T value {};
                    for (size_t k = 0; k < N; ++k)
                        value += m_elements[i][k] * other.m_elements[k][j];

                    element = value;
                }
            }
        }

        return product;
    }

private:
    T m_elements[N][N];
};

}

using Gfx::Matrix;
