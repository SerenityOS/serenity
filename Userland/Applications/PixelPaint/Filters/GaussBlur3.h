/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2023, Luiz Gustavo de França Chaves
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Applications/PixelPaint/Filters/ConvolutionFilter.h>

namespace PixelPaint::Filters {

// FIXME: Make a generic gaussian blur that does not need the templated radius
class GaussBlur3 final : public ConvolutionFilter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const override;
    virtual StringView filter_name() const override { return "Gaussian Blur (3x3)"sv; }

    GaussBlur3(ImageEditor* editor)
        : ConvolutionFilter(editor) {};
};

}
