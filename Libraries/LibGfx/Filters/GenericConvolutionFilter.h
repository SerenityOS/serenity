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
#include <LibGfx/Matrix.h>
#include <LibGfx/Matrix4x4.h>

namespace Gfx {

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

    class ApplyCache {
        template<size_t>
        friend class GenericConvolutionFilter;
    private:
        RefPtr<Gfx::Bitmap> m_target;
    };

    GenericConvolutionFilter() { }
    virtual ~GenericConvolutionFilter() { }

    virtual const char* class_name() const override { return "GenericConvolutionFilter"; }

    virtual void apply(const Filter::Parameters& parameters) override
    {
        ASSERT(parameters.is_generic_convolution_filter());
        auto& gcf_params = static_cast<const GenericConvolutionFilter::Parameters&>(parameters);

        ApplyCache apply_cache;
        apply(gcf_params, apply_cache);
    }

    void apply(const GenericConvolutionFilter::Parameters& parameters, ApplyCache& apply_cache)
    {
        auto& source = parameters.bitmap();
        const auto& source_rect = parameters.rect();
        if (!apply_cache.m_target || apply_cache.m_target->size().contains(parameters.rect().size()))
            apply_cache.m_target = Gfx::Bitmap::create(source.format(), parameters.rect().size());

        // FIXME: Help! I am naive!
        for (auto i_ = 0; i_ < source_rect.width(); ++i_) {
            auto i = i_ + source_rect.x();
            for (auto j_ = 0; j_ < source_rect.height(); ++j_) {
                auto j = j_ + source_rect.y();
                FloatVector3 value(0, 0, 0);
                for (auto k = 0; k < 4; ++k) {
                    auto ki = i + k - 2;
                    if (ki < 0 || ki >= source.size().width()) {
                        if (parameters.should_wrap())
                            ki = (ki + source.size().width()) % source.size().width();
                        else
                            continue;
                    }

                    for (auto l = 0; l < 4; ++l) {
                        auto lj = j + l - 2;
                        if (lj < 0 || lj >= source.size().height()) {
                            if (parameters.should_wrap())
                                lj = (lj + source.size().height()) % source.size().height();
                            else
                                continue;
                        }

                        auto pixel = source.get_pixel(ki, lj);
                        FloatVector3 pixel_value(pixel.red(), pixel.green(), pixel.blue());

                        value = value + pixel_value * parameters.kernel().elements()[k][l];
                    }
                }

                // The float->u8 overflow is intentional.
                apply_cache.m_target->set_pixel(i_, j_, Color(value.x(), value.y(), value.z(), source.get_pixel(i, j).alpha()));
            }
        }

        // FIXME: Substitute for some sort of faster "blit" method.
        for (auto i_ = 0; i_ < source_rect.width(); ++i_) {
            auto i = i_ + source_rect.x();
            for (auto j_ = 0; j_ < source_rect.height(); ++j_) {
                auto j = j_ + source_rect.y();
                source.set_pixel(i, j, apply_cache.m_target->get_pixel(i_, j_));
            }
        }
    }
};

}
