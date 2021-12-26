/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>

namespace Gfx {

class FastBoxBlurFilter {
public:
    FastBoxBlurFilter(Bitmap& bitmap)
        : m_bitmap(bitmap)
    {
    }

    // Based on the super fast blur algorithm by Quasimondo, explored here: https://stackoverflow.com/questions/21418892/understanding-super-fast-blur-algorithm
    void apply_single_pass(int radius)
    {
        VERIFY(radius >= 0);
        VERIFY(m_bitmap.format() == BitmapFormat::BGRA8888);

        int height = m_bitmap.height();
        int width = m_bitmap.width();

        int div = 2 * radius + 1;

        Vector<u8, 1024> intermediate_red;
        Vector<u8, 1024> intermediate_green;
        Vector<u8, 1024> intermediate_blue;
        Vector<u8, 1024> intermediate_alpha;

        intermediate_red.resize(width * height);
        intermediate_green.resize(width * height);
        intermediate_blue.resize(width * height);
        intermediate_alpha.resize(width * height);

        // First pass: vertical
        for (int y = 0; y < height; ++y) {
            size_t sum_red = 0;
            size_t sum_green = 0;
            size_t sum_blue = 0;
            size_t sum_alpha = 0;

            // Setup sliding window
            for (int i = -radius; i <= radius; ++i) {
                auto color_at_px = m_bitmap.get_pixel<StorageFormat::BGRA8888>(clamp(i, 0, width - 1), y);
                sum_red += red_value(color_at_px);
                sum_green += green_value(color_at_px);
                sum_blue += blue_value(color_at_px);
                sum_alpha += color_at_px.alpha();
            }
            // Slide horizontally
            for (int x = 0; x < width; ++x) {
                intermediate_red[y * width + x] = (sum_red / div);
                intermediate_green[y * width + x] = (sum_green / div);
                intermediate_blue[y * width + x] = (sum_blue / div);
                intermediate_alpha[y * width + x] = (sum_alpha / div);

                auto leftmost_x_coord = max(x - radius, 0);
                auto rightmost_x_coord = min(x + radius + 1, width - 1);

                auto leftmost_x_color = m_bitmap.get_pixel<StorageFormat::BGRA8888>(leftmost_x_coord, y);
                auto rightmost_x_color = m_bitmap.get_pixel<StorageFormat::BGRA8888>(rightmost_x_coord, y);

                sum_red -= red_value(leftmost_x_color);
                sum_red += red_value(rightmost_x_color);
                sum_green -= green_value(leftmost_x_color);
                sum_green += green_value(rightmost_x_color);
                sum_blue -= blue_value(leftmost_x_color);
                sum_blue += blue_value(rightmost_x_color);
                sum_alpha -= leftmost_x_color.alpha();
                sum_alpha += rightmost_x_color.alpha();
            }
        }

        // Second pass: horizontal
        for (int x = 0; x < width; ++x) {
            size_t sum_red = 0;
            size_t sum_green = 0;
            size_t sum_blue = 0;
            size_t sum_alpha = 0;

            // Setup sliding window
            for (int i = -radius; i <= radius; ++i) {
                int offset = clamp(i, 0, height - 1) * width + x;
                sum_red += intermediate_red[offset];
                sum_green += intermediate_green[offset];
                sum_blue += intermediate_blue[offset];
                sum_alpha += intermediate_alpha[offset];
            }

            for (int y = 0; y < height; ++y) {
                auto color = Color(
                    sum_red / div,
                    sum_green / div,
                    sum_blue / div,
                    sum_alpha / div);

                m_bitmap.set_pixel<StorageFormat::BGRA8888>(x, y, color);

                auto topmost_y_coord = max(y - radius, 0);
                auto bottommost_y_coord = min(y + radius + 1, height - 1);

                sum_red += intermediate_red[x + bottommost_y_coord * width];
                sum_red -= intermediate_red[x + topmost_y_coord * width];
                sum_green += intermediate_green[x + bottommost_y_coord * width];
                sum_green -= intermediate_green[x + topmost_y_coord * width];
                sum_blue += intermediate_blue[x + bottommost_y_coord * width];
                sum_blue -= intermediate_blue[x + topmost_y_coord * width];
                sum_alpha += intermediate_alpha[x + bottommost_y_coord * width];
                sum_alpha -= intermediate_alpha[x + topmost_y_coord * width];
            }
        }
    }

    // Math from here: http://blog.ivank.net/fastest-gaussian-blur.html
    void apply_three_passes(size_t radius)
    {
        if (!radius)
            return;

        constexpr size_t no_of_passes = 3;
        double w_ideal = sqrt((12 * radius * radius / (double)no_of_passes) + 1);
        int wl = floor(w_ideal);
        if (wl % 2 == 0)
            wl--;
        int wu = wl - 2;
        double m_ideal = (12 * radius * radius - no_of_passes * wl * wl - 4 * no_of_passes * wl - 3 * no_of_passes) / (double)(-4 * wl - 4);
        int m = round(m_ideal);

        for (size_t i = 0; i < no_of_passes; ++i) {
            int weighted_radius = (int)i < m ? wl : wu;
            if (weighted_radius < 2)
                continue;
            apply_single_pass((weighted_radius - 1) / 2);
        }
    }

private:
    ALWAYS_INLINE static u8 red_value(Color color)
    {
        return (color.alpha() == 0) ? 0xFF : color.red();
    }
    ALWAYS_INLINE static u8 green_value(Color color)
    {
        return (color.alpha() == 0) ? 0xFF : color.green();
    }
    ALWAYS_INLINE static u8 blue_value(Color color)
    {
        return (color.alpha() == 0) ? 0xFF : color.blue();
    }

    Bitmap& m_bitmap;
};

}
