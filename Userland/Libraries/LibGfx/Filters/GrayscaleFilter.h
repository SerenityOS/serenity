/*
 * Copyright (c) 2021, David Savary <david.savarymartinez@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <LibGfx/Filters/ColorFilter.h>

namespace Gfx {

class GrayscaleFilter : public ColorFilter {
public:
    using ColorFilter::ColorFilter;
    virtual ~GrayscaleFilter() = default;

    virtual StringView class_name() const override { return "GrayscaleFilter"sv; }

protected:
    Color convert_color(Color original) override { return original.to_grayscale(); }
};

}
