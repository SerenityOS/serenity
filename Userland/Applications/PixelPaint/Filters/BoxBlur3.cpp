/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoxBlur3.h"
#include "../FilterParams.h"

namespace PixelPaint::Filters {

void BoxBlur3::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    Gfx::BoxBlurFilter<3> filter;
    if (auto parameters = PixelPaint::FilterParameters<Gfx::BoxBlurFilter<3>>::get())
        filter.apply(target_bitmap, target_bitmap.rect(), source_bitmap, source_bitmap.rect(), *parameters);
}

}
