/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GenericConvolutionFilter.h"
#include <AK/StringView.h>

namespace Gfx {

class LaplacianFilter : public GenericConvolutionFilter<3> {
public:
    LaplacianFilter() = default;
    virtual ~LaplacianFilter() = default;

    virtual StringView class_name() const override { return "LaplacianFilter"sv; }
};

}
