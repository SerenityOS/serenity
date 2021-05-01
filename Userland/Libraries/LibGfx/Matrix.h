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

    Matrix() = default;
    Matrix(std::initializer_list<T> elements)
    {
        VERIFY(elements.size() == N * N);
        size_t i = 0;
        for (auto& element : elements) {
            m_elements[i / N][i % N] = element;
            ++i;
        }
    }

    template<typename... Args>
    Matrix(Args... args)
        : Matrix({ (T)args... })
    {
    }

    Matrix(const Matrix& other)
    {
        __builtin_memcpy(m_elements, other.elements(), sizeof(T) * N * N);
    }

    auto elements() const { return m_elements; }
    auto elements() { return m_elements; }

    Matrix operator*(const Matrix& other) const
    {
        Matrix product;
        for (int i = 0; i < N; ++i) {
            for (int j = 0; j < N; ++j) {
                auto& element = product.m_elements[i][j];

                if constexpr (N == 4) {
                    element = m_elements[0][j] * other.m_elements[i][0]
                        + m_elements[1][j] * other.m_elements[i][1]
                        + m_elements[2][j] * other.m_elements[i][2]
                        + m_elements[3][j] * other.m_elements[i][3];
                } else if constexpr (N == 3) {
                    element = m_elements[0][j] * other.m_elements[i][0]
                        + m_elements[1][j] * other.m_elements[i][1]
                        + m_elements[2][j] * other.m_elements[i][2];
                } else if constexpr (N == 2) {
                    element = m_elements[0][j] * other.m_elements[i][0]
                        + m_elements[1][j] * other.m_elements[i][1];
                } else if constexpr (N == 1) {
                    element = m_elements[0][j] * other.m_elements[i][0];
                } else {
                    T value {};
                    for (size_t k = 0; k < N; ++k)
                        value += m_elements[k][j] * other.m_elements[i][k];

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
