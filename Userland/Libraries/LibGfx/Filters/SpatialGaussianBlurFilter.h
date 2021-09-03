/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GenericConvolutionFilter.h"
#include <YAK/StdLibExtras.h>

namespace Gfx {

template<size_t N, typename = typename EnableIf<N % 2 == 1>::Type>
class SpatialGaussianBlurFilter : public GenericConvolutionFilter<N> {
public:
    SpatialGaussianBlurFilter() { }
    virtual ~SpatialGaussianBlurFilter() { }

    virtual const char* class_name() const override { return "SpatialGaussianBlurFilter"; }
};

}
