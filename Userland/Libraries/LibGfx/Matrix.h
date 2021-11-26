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

    Matrix& operator=(const Matrix& other)
    {
        __builtin_memcpy(m_elements, other.elements(), sizeof(T) * N * N);
        return *this;
    }

    constexpr auto elements() const { return m_elements; }
    constexpr auto elements() { return m_elements; }

    constexpr Matrix operator*(const Matrix& other) const
    {
        Matrix product;
        for (size_t i = 0; i < N; ++i) {
            for (size_t j = 0; j < N; ++j) {
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

    constexpr Matrix operator/(T divisor) const
    {
        Matrix division;
        for (size_t i = 0; i < N; ++i) {
            for (size_t j = 0; j < N; ++j) {
                division.m_elements[i][j] = m_elements[i][j] / divisor;
            }
        }
        return division;
    }

    constexpr Matrix adjugate() const
    {
        if constexpr (N == 1)
            return Matrix(1);

        Matrix adjugate;
        for (size_t i = 0; i < N; ++i) {
            for (size_t j = 0; j < N; ++j) {
                int sign = (i + j) % 2 == 0 ? 1 : -1;
                adjugate.m_elements[j][i] = sign * first_minor(i, j);
            }
        }
        return adjugate;
    }

    constexpr T determinant() const
    {
        if constexpr (N == 1) {
            return m_elements[0][0];
        } else {
            T result = {};
            int sign = 1;
            for (size_t j = 0; j < N; ++j) {
                result += sign * m_elements[0][j] * first_minor(0, j);
                sign *= -1;
            }
            return result;
        }
    }

    constexpr T first_minor(size_t skip_row, size_t skip_column) const
    {
        static_assert(N > 1);
        VERIFY(skip_row < N);
        VERIFY(skip_column < N);

        Matrix<N - 1, T> first_minor;
        constexpr auto new_size = N - 1;
        size_t k = 0;

        for (size_t i = 0; i < N; ++i) {
            for (size_t j = 0; j < N; ++j) {
                if (i == skip_row || j == skip_column)
                    continue;

                first_minor.elements()[k / new_size][k % new_size] = m_elements[i][j];
                ++k;
            }
        }

        return first_minor.determinant();
    }

    constexpr static Matrix identity()
    {
        Matrix result;
        for (size_t i = 0; i < N; ++i) {
            for (size_t j = 0; j < N; ++j) {
                if (i == j)
                    result.m_elements[i][j] = 1;
                else
                    result.m_elements[i][j] = 0;
            }
        }
        return result;
    }

    constexpr Matrix inverse() const
    {
        auto det = determinant();
        VERIFY(det != 0);
        return adjugate() / det;
    }

    constexpr Matrix transpose() const
    {
        Matrix result;
        for (size_t i = 0; i < N; ++i) {
            for (size_t j = 0; j < N; ++j) {
                result.m_elements[i][j] = m_elements[j][i];
            }
        }
        return result;
    }

private:
    T m_elements[N][N];
};

}

using Gfx::Matrix;
