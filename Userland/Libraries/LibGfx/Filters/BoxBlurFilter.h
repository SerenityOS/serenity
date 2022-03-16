/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GenericConvolutionFilter.h"
#include <AK/StringView.h>

namespace Gfx {

template<size_t N>
class BoxBlurFilter : public GenericConvolutionFilter<N> {
public:
    BoxBlurFilter() = default;
    virtual ~BoxBlurFilter() = default;

    virtual StringView class_name() const override { return "BoxBlurFilter"sv; }
};

}
