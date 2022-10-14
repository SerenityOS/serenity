/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filter.h"

namespace PixelPaint::Filters {

class Grayscale final : public Filter {
public:
    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const override;
    virtual StringView filter_name() const override { return "Grayscale"sv; }

    Grayscale(ImageEditor* editor)
        : Filter(editor) {};
};

}
