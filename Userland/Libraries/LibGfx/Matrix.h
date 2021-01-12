/*
 * Copyright (c) 2020, the SerenityOS developers.
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
        ASSERT(elements.size() == N * N);
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
