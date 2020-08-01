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

#include "Filter.h"
#include <LibGUI/Dialog.h>
#include <LibGfx/Matrix.h>
#include <LibGfx/Matrix4x4.h>

namespace PixelPaint {

template<size_t N, typename T>
inline static constexpr void normalize(Matrix<N, T>& matrix)
{
    auto sum = 0.0f;
    for (size_t i = 0; i < matrix.Size; ++i) {
        for (size_t j = 0; j < matrix.Size; ++j) {
            sum += matrix.elements()[i][j];
        }
    }
    for (size_t i = 0; i < matrix.Size; ++i) {
        for (size_t j = 0; j < matrix.Size; ++j) {
            matrix.elements()[i][j] /= sum;
        }
    }
}

template<size_t N>
class GenericConvolutionFilter : public Filter {
public:
    class Parameters : public Filter::Parameters {
    public:
        Parameters(Gfx::Bitmap& bitmap, const Gfx::IntRect& rect, Gfx::Matrix<N, float> kernel, bool should_wrap = false)
            : Filter::Parameters(bitmap, rect)
            , m_kernel(move(kernel))
            , m_should_wrap(should_wrap)

        {
        }

        const Gfx::Matrix<N, float>& kernel() const { return m_kernel; }
        Gfx::Matrix<N, float>& kernel() { return m_kernel; }
        bool should_wrap() const { return m_should_wrap; }

    private:
        virtual bool is_generic_convolution_filter() const override { return true; }
        Gfx::Matrix<N, float> m_kernel;
        bool m_should_wrap { false };
    };

    GenericConvolutionFilter();
    virtual ~GenericConvolutionFilter();

    virtual const char* class_name() const override { return "GenericConvolutionFilter"; }

    virtual void apply(const Filter::Parameters&) override;

    OwnPtr<Parameters> get_parameters(Gfx::Bitmap&, const Gfx::IntRect&, GUI::Window* parent_window);
};

template<size_t N>
class GenericConvolutionFilterInputDialog : public GUI::Dialog {
    C_OBJECT(GenericConvolutionFilterInputDialog);

public:
    const Matrix<N, float>& matrix() const { return m_matrix; }
    bool should_wrap() const { return m_should_wrap; }

private:
    explicit GenericConvolutionFilterInputDialog(GUI::Window*);
    Matrix<N, float> m_matrix {};
    bool m_should_wrap { false };
};

}
