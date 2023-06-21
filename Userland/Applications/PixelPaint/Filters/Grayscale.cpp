/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Grayscale.h"
#include "../FilterParams.h"

namespace PixelPaint::Filters {

void Grayscale::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    Gfx::GrayscaleFilter filter;
    filter.apply(target_bitmap, target_bitmap.rect(), source_bitmap, source_bitmap.rect());
}

}
