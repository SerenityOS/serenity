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

#include "GenericConvolutionFilter.h"
#include <AK/TemporaryChange.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

template<size_t N>
GenericConvolutionFilter<N>::GenericConvolutionFilter()
{
}

template<size_t N>
GenericConvolutionFilter<N>::~GenericConvolutionFilter()
{
}

template<size_t N>
void GenericConvolutionFilter<N>::apply(const Filter::Parameters& parameters)
{
    ASSERT(parameters.is_generic_convolution_filter());

    auto& gcf_params = static_cast<const GenericConvolutionFilter::Parameters&>(parameters);

    auto& source = gcf_params.bitmap();
    const auto& source_rect = gcf_params.rect();
    auto target = Gfx::Bitmap::create(source.format(), parameters.rect().size());

    // FIXME: Help! I am naive!
    for (auto i_ = 0; i_ < source_rect.width(); ++i_) {
        auto i = i_ + source_rect.x();
        for (auto j_ = 0; j_ < source_rect.height(); ++j_) {
            auto j = j_ + source_rect.y();
            FloatVector3 value(0, 0, 0);
            for (auto k = 0; k < 4; ++k) {
                auto ki = i + k - 2;
                if (ki < 0 || ki >= source.size().width()) {
                    if (gcf_params.should_wrap())
                        ki = (ki + source.size().width()) % source.size().width();
                    else
                        continue;
                }

                for (auto l = 0; l < 4; ++l) {
                    auto lj = j + l - 2;
                    if (lj < 0 || lj >= source.size().height()) {
                        if (gcf_params.should_wrap())
                            lj = (lj + source.size().height()) % source.size().height();
                        else
                            continue;
                    }

                    auto pixel = source.get_pixel(ki, lj);
                    FloatVector3 pixel_value(pixel.red(), pixel.green(), pixel.blue());

                    value = value + pixel_value * gcf_params.kernel().elements()[k][l];
                }
            }

            // The float->u8 overflow is intentional.
            target->set_pixel(i_, j_, Color(value.x(), value.y(), value.z(), source.get_pixel(i, j).alpha()));
        }
    }

    // FIXME: Substitute for some sort of faster "blit" method.
    for (auto i_ = 0; i_ < source_rect.width(); ++i_) {
        auto i = i_ + source_rect.x();
        for (auto j_ = 0; j_ < source_rect.height(); ++j_) {
            auto j = j_ + source_rect.y();
            source.set_pixel(i, j, target->get_pixel(i_, j_));
        }
    }
}

}

template class PixelPaint::GenericConvolutionFilter<3>;
template class PixelPaint::GenericConvolutionFilter<5>;
