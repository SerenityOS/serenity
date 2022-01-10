/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GaussBlur5.h"
#include "../FilterParams.h"

namespace PixelPaint::Filters {

void GaussBlur5::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    Gfx::SpatialGaussianBlurFilter<5> filter;
    if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<5>>::get())
        filter.apply(target_bitmap, target_bitmap.rect(), source_bitmap, source_bitmap.rect(), *parameters);
}

}
