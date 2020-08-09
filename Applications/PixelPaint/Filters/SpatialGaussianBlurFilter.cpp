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

#include "SpatialGaussianBlurFilter.h"

namespace PixelPaint {

template<size_t N, typename T>
SpatialGaussianBlurFilter<N, T>::SpatialGaussianBlurFilter()
{
}

template<size_t N, typename T>
SpatialGaussianBlurFilter<N, T>::~SpatialGaussianBlurFilter()
{
}

template<size_t N, typename _T>
OwnPtr<typename GenericConvolutionFilter<N>::Parameters>
SpatialGaussianBlurFilter<N, _T>::get_parameters(Gfx::Bitmap& bitmap, const Gfx::IntRect& rect)
{
    Matrix<N, float> kernel;
    auto sigma = 1.0f;
    auto s = 2.0f * sigma * sigma;

    for (auto x = -(ssize_t)N / 2; x <= (ssize_t)N / 2; x++) {
        for (auto y = -(ssize_t)N / 2; y <= (ssize_t)N / 2; y++) {
            auto r = sqrt(x * x + y * y);
            kernel.elements()[x + 2][y + 2] = (exp(-(r * r) / s)) / (M_PI * s);
        }
    }

    normalize(kernel);

    return make<typename GenericConvolutionFilter<N>::Parameters>(bitmap, rect, kernel);
}

}

template class PixelPaint::SpatialGaussianBlurFilter<3>;
template class PixelPaint::SpatialGaussianBlurFilter<5>;
