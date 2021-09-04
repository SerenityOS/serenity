/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GenericConvolutionFilter.h"

namespace Gfx {

class LaplacianFilter : public GenericConvolutionFilter<3> {
public:
    LaplacianFilter() { }
    virtual ~LaplacianFilter() { }

    virtual char const* class_name() const override { return "LaplacianFilter"; }
};

}
