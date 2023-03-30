/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2023, Luiz Gustavo de França Chaves
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LaplaceDiagonal.h"
#include <Applications/PixelPaint/FilterParams.h>

namespace PixelPaint::Filters {

void LaplaceDiagonal::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    Gfx::LaplacianFilter filter;
    if (auto parameters = PixelPaint::FilterParameters<Gfx::LaplacianFilter>::get(true, get_filter_options()))
        filter.apply(target_bitmap, target_bitmap.rect(), source_bitmap, source_bitmap.rect(), *parameters);
}

}
