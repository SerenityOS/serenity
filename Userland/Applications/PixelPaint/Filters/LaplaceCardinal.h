/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2023, Luiz Gustavo de França Chaves
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Applications/PixelPaint/Filters/ConvolutionFilter.h>

namespace PixelPaint::Filters {

class LaplaceCardinal final : public ConvolutionFilter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const override;
    virtual StringView filter_name() const override { return "Laplacian Cardinal"sv; }

    LaplaceCardinal(ImageEditor* editor)
        : ConvolutionFilter(editor) {};
};

}
