/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#if defined(AK_COMPILER_GCC)
#    pragma GCC optimize("O3")
#endif

#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibGfx/Filters/FastBoxBlurFilter.h>

namespace Gfx {

ALWAYS_INLINE static constexpr u8 red_value(Color color)
{
    return (color.alpha() == 0) ? 0xFF : color.red();
}
ALWAYS_INLINE static constexpr u8 green_value(Color color)
{
    return (color.alpha() == 0) ? 0xFF : color.green();
}
ALWAYS_INLINE static constexpr u8 blue_value(Color color)
{
    return (color.alpha() == 0) ? 0xFF : color.blue();
}

FastBoxBlurFilter::FastBoxBlurFilter(Bitmap& bitmap)
    : m_bitmap(bitmap)
{
}

void FastBoxBlurFilter::apply_single_pass(size_t radius)
{
    apply_single_pass(radius, radius);
}

template<typename GetPixelFunction, typename SetPixelFunction>
static void do_single_pass(int width, int height, size_t radius_x, size_t radius_y, GetPixelFunction get_pixel_function, SetPixelFunction set_pixel_function)
{
    int div_x = 2 * radius_x + 1;
    int div_y = 2 * radius_y + 1;

    Vector<Color, 1024> intermediate;
    intermediate.resize(width * height);

    // First pass: vertical
    for (int y = 0; y < height; ++y) {
        size_t sum_red = 0;
        size_t sum_green = 0;
        size_t sum_blue = 0;
        size_t sum_alpha = 0;

        // Setup sliding window
        for (int i = -(int)radius_x; i <= (int)radius_x; ++i) {
            auto color_at_px = get_pixel_function(clamp(i, 0, width - 1), y);
            sum_red += red_value(color_at_px);
            sum_green += green_value(color_at_px);
            sum_blue += blue_value(color_at_px);
            sum_alpha += color_at_px.alpha();
        }
        // Slide horizontally
        for (int x = 0; x < width; ++x) {
            auto const index = y * width + x;
            auto& current_intermediate = intermediate[index];
            current_intermediate.set_red(sum_red / div_x);
            current_intermediate.set_green(sum_green / div_x);
            current_intermediate.set_blue(sum_blue / div_x);
            current_intermediate.set_alpha(sum_alpha / div_x);

            auto leftmost_x_coord = max(x - (int)radius_x, 0);
            auto rightmost_x_coord = min(x + (int)radius_x + 1, width - 1);

            auto leftmost_x_color = get_pixel_function(leftmost_x_coord, y);
            auto rightmost_x_color = get_pixel_function(rightmost_x_coord, y);

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
        for (int i = -(int)radius_y; i <= (int)radius_y; ++i) {
            int offset = clamp(i, 0, height - 1) * width + x;
            auto& current_intermediate = intermediate[offset];
            sum_red += current_intermediate.red();
            sum_green += current_intermediate.green();
            sum_blue += current_intermediate.blue();
            sum_alpha += current_intermediate.alpha();
        }

        for (int y = 0; y < height; ++y) {
            auto color = Color(
                sum_red / div_y,
                sum_green / div_y,
                sum_blue / div_y,
                sum_alpha / div_y);

            set_pixel_function(x, y, color);

            auto const bottommost_y_coord = min(y + (int)radius_y + 1, height - 1);
            auto const bottom_index = x + bottommost_y_coord * width;
            auto& bottom_intermediate = intermediate[bottom_index];
            sum_red += bottom_intermediate.red();
            sum_green += bottom_intermediate.green();
            sum_blue += bottom_intermediate.blue();
            sum_alpha += bottom_intermediate.alpha();

            auto const topmost_y_coord = max(y - (int)radius_y, 0);
            auto const top_index = x + topmost_y_coord * width;
            auto& top_intermediate = intermediate[top_index];
            sum_red -= top_intermediate.red();
            sum_green -= top_intermediate.green();
            sum_blue -= top_intermediate.blue();
            sum_alpha -= top_intermediate.alpha();
        }
    }
}

// Based on the super fast blur algorithm by Quasimondo, explored here: https://stackoverflow.com/questions/21418892/understanding-super-fast-blur-algorithm
FLATTEN void FastBoxBlurFilter::apply_single_pass(size_t radius_x, size_t radius_y)
{
    auto format = m_bitmap.format();
    VERIFY(format == BitmapFormat::BGRA8888 || format == BitmapFormat::BGRx8888);

    switch (format) {
    case BitmapFormat::BGRx8888:
        do_single_pass(
            m_bitmap.width(), m_bitmap.height(), radius_x, radius_y,
            [&](int x, int y) { return m_bitmap.get_pixel<StorageFormat::BGRx8888>(x, y); },
            [&](int x, int y, Color color) { return m_bitmap.set_pixel<StorageFormat::BGRx8888>(x, y, color); });
        break;
    case BitmapFormat::BGRA8888:
        do_single_pass(
            m_bitmap.width(), m_bitmap.height(), radius_x, radius_y,
            [&](int x, int y) { return m_bitmap.get_pixel<StorageFormat::BGRA8888>(x, y); },
            [&](int x, int y, Color color) { return m_bitmap.set_pixel<StorageFormat::BGRA8888>(x, y, color); });
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

// Math from here: http://blog.ivank.net/fastest-gaussian-blur.html
void FastBoxBlurFilter::apply_three_passes(size_t radius)
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

}
