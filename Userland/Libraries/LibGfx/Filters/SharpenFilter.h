/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "GenericConvolutionFilter.h"

namespace Gfx {

class SharpenFilter : public GenericConvolutionFilter<3> {
public:
    SharpenFilter() { }
    virtual ~SharpenFilter() { }

    virtual char const* class_name() const override { return "SharpenFilter"; }
};

}
