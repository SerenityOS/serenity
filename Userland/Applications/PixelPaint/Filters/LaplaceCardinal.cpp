/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LaplaceCardinal.h"
#include "../FilterParams.h"

namespace PixelPaint::Filters {

void LaplaceCardinal::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    Gfx::LaplacianFilter filter;
    if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(false))
        filter.apply(target_bitmap, target_bitmap.rect(), source_bitmap, source_bitmap.rect(), *parameters);
}

}
