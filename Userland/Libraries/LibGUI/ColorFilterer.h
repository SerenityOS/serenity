/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Filters/ColorBlindnessFilter.h>

namespace GUI {

class ColorFilterer {
public:
    virtual ~ColorFilterer() = default;
    virtual void set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter>) = 0;

protected:
    ColorFilterer() = default;
};

}
