/*
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BitmapMixer.h"

namespace Gfx {

void BitmapMixer::mix_with(Bitmap& other_bitmap, MixingMethod mixing_method)
{
    VERIFY(m_bitmap.size() == other_bitmap.size());

    int height = m_bitmap.height();
    int width = m_bitmap.width();

    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            auto original_color = m_bitmap.get_pixel(x, y);
            auto other_color = other_bitmap.get_pixel(x, y);

            Color output_color = { 0, 0, 0, original_color.alpha() };
            switch (mixing_method) {
            case MixingMethod::Add:
                output_color.set_red(original_color.red() + other_color.red());
                output_color.set_green(original_color.green() + other_color.green());
                output_color.set_blue(original_color.blue() + other_color.blue());
                break;
            case MixingMethod::Lightest:
                auto original_lightness = original_color.red() + original_color.green() + original_color.blue();
                auto other_lightness = other_color.red() + other_color.green() + other_color.blue();
                if (original_lightness > other_lightness) {
                    output_color = original_color;
                } else {
                    output_color.set_red(other_color.red());
                    output_color.set_green(other_color.green());
                    output_color.set_blue(other_color.blue());
                }
                break;
            }
            if (original_color != output_color)
                m_bitmap.set_pixel(x, y, output_color);
        }
    }
}

}
