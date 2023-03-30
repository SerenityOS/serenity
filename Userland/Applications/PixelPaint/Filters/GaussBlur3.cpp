/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2023, Luiz Gustavo de França Chaves
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GaussBlur3.h"
#include <Applications/PixelPaint/FilterParams.h>

namespace PixelPaint::Filters {

void GaussBlur3::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    Gfx::SpatialGaussianBlurFilter<3> filter;
    if (auto parameters = PixelPaint::FilterParameters<Gfx::SpatialGaussianBlurFilter<3>>::get(get_filter_options()))
        filter.apply(target_bitmap, target_bitmap.rect(), source_bitmap, source_bitmap.rect(), *parameters);
}

}
