/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filter.h"

namespace PixelPaint::Filters {

// FIXME: Make a generic gaussian blur that does not need the templated radius
class GaussBlur3 final : public Filter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const override;
    virtual StringView filter_name() const override { return "Gaussian Blur (3x3)"sv; }

    GaussBlur3(ImageEditor* editor)
        : Filter(editor) {};
};

}
