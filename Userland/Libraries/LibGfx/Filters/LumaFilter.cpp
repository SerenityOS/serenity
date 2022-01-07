/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LumaFilter.h"
namespace Gfx {

void LumaFilter::apply(u8 lower_bound, u8 upper_bound)
{
    if (upper_bound < lower_bound)
        return;

    int height = m_bitmap.height();
    int width = m_bitmap.width();

    auto format = m_bitmap.format();
    VERIFY(format == BitmapFormat::BGRA8888 || format == BitmapFormat::BGRx8888);

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            Color color;
            color = m_bitmap.get_pixel(x, y);

            auto luma = color.luminosity();
            if (lower_bound > luma || upper_bound < luma)
                m_bitmap.set_pixel(x, y, { 0, 0, 0, color.alpha() });
        }
    }
}

}
