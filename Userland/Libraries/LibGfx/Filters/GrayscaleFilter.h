/*
 * Copyright (c) 2021, David Savary <david.savarymartinez@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Filters/ColorFilter.h>

namespace Gfx {

class GrayscaleFilter : public ColorFilter {
public:
    GrayscaleFilter() { }
    virtual ~GrayscaleFilter() { }

    virtual char const* class_name() const override { return "GrayscaleFilter"; }

protected:
    Color convert_color(Color original) override { return original.to_grayscale(); };
};

}
