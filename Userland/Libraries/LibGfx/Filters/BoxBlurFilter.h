/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GenericConvolutionFilter.h"

namespace Gfx {

template<size_t N>
class BoxBlurFilter : public GenericConvolutionFilter<N> {
public:
    BoxBlurFilter() { }
    virtual ~BoxBlurFilter() { }

    virtual char const* class_name() const override { return "BoxBlurFilter"; }
};

}
