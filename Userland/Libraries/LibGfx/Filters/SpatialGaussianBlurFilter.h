/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GenericConvolutionFilter.h"
#include <AK/StdLibExtras.h>

namespace Gfx {

template<size_t N>
requires(N % 2 == 1) class SpatialGaussianBlurFilter : public GenericConvolutionFilter<N> {
public:
    SpatialGaussianBlurFilter() = default;
    virtual ~SpatialGaussianBlurFilter() = default;

    virtual const char* class_name() const override { return "SpatialGaussianBlurFilter"; }
};
}
