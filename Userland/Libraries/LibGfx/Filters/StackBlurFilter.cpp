/*
 * Copyright (c) 2010, Mario Klingemann <mario@quasimondo.com>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#if defined(AK_COMPILER_GCC)
#    pragma GCC optimize("O3")
#endif

#include <AK/Array.h>
#include <AK/IntegralMath.h>
#include <AK/Math.h>
#include <AK/Vector.h>
#include <LibGfx/Filters/StackBlurFilter.h>

namespace Gfx {

using uint = unsigned;

constexpr size_t MAX_RADIUS = 256;

// Magic lookup tables!
// `(value * sum_mult[radius - 2]) >> shift_table[radius - 2]` closely approximates value/(radius*radius)
// These LUTs are the same as the original, but converted to constexpr functions rather than magic numbers.

constexpr auto shift_table = [] {
    Array<u8, MAX_RADIUS> lut {};
    for (size_t r = 2; r <= MAX_RADIUS + 1; r++)
        lut[r - 2] = static_cast<u8>(AK::ceil_log2(256 * (r * r + 1)));
    return lut;
}();

constexpr auto mult_table = [] {
    Array<u16, MAX_RADIUS> lut {};
    for (size_t r = 2; r <= MAX_RADIUS + 1; r++)
        lut[r - 2] = static_cast<u16>(AK::ceil(static_cast<double>(1 << shift_table[r - 2]) / (r * r)));
    return lut;
}();

// Note: This is named to be consistent with the algorithm, but it's actually a simple circular buffer.
struct BlurStack {
    BlurStack(size_t size)
    {
        m_data.resize(size);
    }

    struct Iterator {
        friend BlurStack;

        ALWAYS_INLINE Color& operator*()
        {
            return m_data.at(m_idx);
        }

        ALWAYS_INLINE Color* operator->()
        {
            return &m_data.at(m_idx);
        }

        ALWAYS_INLINE Iterator operator++()
        {
            // Note: This seemed to profile slightly better than %
            if (++m_idx >= m_data.size())
                m_idx = 0;
            return *this;
        }

        ALWAYS_INLINE Iterator operator++(int)
        {
            auto prev_it = *this;
            ++*(this);
            return prev_it;
        }

    private:
        Iterator(size_t idx, Span<Color> data)
            : m_idx(idx)
            , m_data(data)
        {
        }

        size_t m_idx;
        Span<Color> m_data;
    };

    Iterator iterator_from_position(size_t position)
    {
        VERIFY(position < m_data.size());
        return Iterator(position, m_data);
    }

private:
    Vector<Color, 512> m_data;
};

// This is an implementation of StackBlur by Mario Klingemann (https://observablehq.com/@jobleonard/mario-klingemans-stackblur)
// (Link is to a secondary source as the original site is now down)
FLATTEN void StackBlurFilter::process_rgba(u8 radius, Color fill_color)
{
    // TODO: Implement a plain RGB version of this (if required)

    if (radius == 0)
        return;

    fill_color = fill_color.with_alpha(0);

    uint width = m_bitmap.width();
    uint height = m_bitmap.height();

    uint div = 2 * radius + 1;
    uint radius_plus_1 = radius + 1;
    uint sum_factor = radius_plus_1 * (radius_plus_1 + 1) / 2;

    auto get_pixel = [&](int x, int y) {
        auto color = m_bitmap.get_pixel<StorageFormat::BGRA8888>(x, y);
        if (color.alpha() == 0)
            return fill_color;
        return color;
    };

    auto set_pixel = [&](int x, int y, Color color) {
        return m_bitmap.set_pixel<StorageFormat::BGRA8888>(x, y, color);
    };

    BlurStack blur_stack { div };
    auto const stack_start = blur_stack.iterator_from_position(0);
    auto const stack_end = blur_stack.iterator_from_position(radius_plus_1);
    auto stack_iterator = stack_start;

    auto const sum_mult = mult_table[radius - 1];
    auto const sum_shift = shift_table[radius - 1];

    for (uint y = 0; y < height; y++) {
        stack_iterator = stack_start;

        auto color = get_pixel(0, y);
        for (uint i = 0; i < radius_plus_1; i++)
            *(stack_iterator++) = color;

        // All the sums here work to approximate a gaussian.
        // Note: Only about 17 bits are actually used in each sum.
        uint red_in_sum = 0;
        uint green_in_sum = 0;
        uint blue_in_sum = 0;
        uint alpha_in_sum = 0;
        uint red_out_sum = radius_plus_1 * color.red();
        uint green_out_sum = radius_plus_1 * color.green();
        uint blue_out_sum = radius_plus_1 * color.blue();
        uint alpha_out_sum = radius_plus_1 * color.alpha();
        uint red_sum = sum_factor * color.red();
        uint green_sum = sum_factor * color.green();
        uint blue_sum = sum_factor * color.blue();
        uint alpha_sum = sum_factor * color.alpha();

        for (uint i = 1; i <= radius; i++) {
            auto color = get_pixel(min(i, width - 1), y);

            auto bias = radius_plus_1 - i;
            *stack_iterator = color;
            red_sum += color.red() * bias;
            green_sum += color.green() * bias;
            blue_sum += color.blue() * bias;
            alpha_sum += color.alpha() * bias;

            red_in_sum += color.red();
            green_in_sum += color.green();
            blue_in_sum += color.blue();
            alpha_in_sum += color.alpha();

            ++stack_iterator;
        }

        auto stack_in_iterator = stack_start;
        auto stack_out_iterator = stack_end;

        for (uint x = 0; x < width; x++) {
            auto alpha = (alpha_sum * sum_mult) >> sum_shift;
            if (alpha != 0)
                set_pixel(x, y, Color((red_sum * sum_mult) >> sum_shift, (green_sum * sum_mult) >> sum_shift, (blue_sum * sum_mult) >> sum_shift, alpha));
            else
                set_pixel(x, y, fill_color);

            red_sum -= red_out_sum;
            green_sum -= green_out_sum;
            blue_sum -= blue_out_sum;
            alpha_sum -= alpha_out_sum;

            red_out_sum -= stack_in_iterator->red();
            green_out_sum -= stack_in_iterator->green();
            blue_out_sum -= stack_in_iterator->blue();
            alpha_out_sum -= stack_in_iterator->alpha();

            auto color = get_pixel(min(x + radius_plus_1, width - 1), y);
            *stack_in_iterator = color;
            red_in_sum += color.red();
            green_in_sum += color.green();
            blue_in_sum += color.blue();
            alpha_in_sum += color.alpha();

            red_sum += red_in_sum;
            green_sum += green_in_sum;
            blue_sum += blue_in_sum;
            alpha_sum += alpha_in_sum;

            ++stack_in_iterator;

            color = *stack_out_iterator;
            red_out_sum += color.red();
            green_out_sum += color.green();
            blue_out_sum += color.blue();
            alpha_out_sum += color.alpha();

            red_in_sum -= color.red();
            green_in_sum -= color.green();
            blue_in_sum -= color.blue();
            alpha_in_sum -= color.alpha();

            ++stack_out_iterator;
        }
    }

    for (uint x = 0; x < width; x++) {
        stack_iterator = stack_start;

        auto color = get_pixel(x, 0);
        for (uint i = 0; i < radius_plus_1; i++)
            *(stack_iterator++) = color;

        uint red_in_sum = 0;
        uint green_in_sum = 0;
        uint blue_in_sum = 0;
        uint alpha_in_sum = 0;
        uint red_out_sum = radius_plus_1 * color.red();
        uint green_out_sum = radius_plus_1 * color.green();
        uint blue_out_sum = radius_plus_1 * color.blue();
        uint alpha_out_sum = radius_plus_1 * color.alpha();
        uint red_sum = sum_factor * color.red();
        uint green_sum = sum_factor * color.green();
        uint blue_sum = sum_factor * color.blue();
        uint alpha_sum = sum_factor * color.alpha();

        for (uint i = 1; i <= radius; i++) {
            auto color = get_pixel(x, min(i, height - 1));

            auto bias = radius_plus_1 - i;
            *stack_iterator = color;
            red_sum += color.red() * bias;
            green_sum += color.green() * bias;
            blue_sum += color.blue() * bias;
            alpha_sum += color.alpha() * bias;

            red_in_sum += color.red();
            green_in_sum += color.green();
            blue_in_sum += color.blue();
            alpha_in_sum += color.alpha();

            ++stack_iterator;
        }

        auto stack_in_iterator = stack_start;
        auto stack_out_iterator = stack_end;

        for (uint y = 0; y < height; y++) {
            auto alpha = (alpha_sum * sum_mult) >> sum_shift;
            if (alpha != 0)
                set_pixel(x, y, Color((red_sum * sum_mult) >> sum_shift, (green_sum * sum_mult) >> sum_shift, (blue_sum * sum_mult) >> sum_shift, alpha));
            else
                set_pixel(x, y, fill_color);

            red_sum -= red_out_sum;
            green_sum -= green_out_sum;
            blue_sum -= blue_out_sum;
            alpha_sum -= alpha_out_sum;

            red_out_sum -= stack_in_iterator->red();
            green_out_sum -= stack_in_iterator->green();
            blue_out_sum -= stack_in_iterator->blue();
            alpha_out_sum -= stack_in_iterator->alpha();

            auto color = get_pixel(x, min(y + radius_plus_1, height - 1));
            *stack_in_iterator = color;
            red_in_sum += color.red();
            green_in_sum += color.green();
            blue_in_sum += color.blue();
            alpha_in_sum += color.alpha();

            red_sum += red_in_sum;
            green_sum += green_in_sum;
            blue_sum += blue_in_sum;
            alpha_sum += alpha_in_sum;

            ++stack_in_iterator;

            color = *stack_out_iterator;
            red_out_sum += color.red();
            green_out_sum += color.green();
            blue_out_sum += color.blue();
            alpha_out_sum += color.alpha();

            red_in_sum -= color.red();
            green_in_sum -= color.green();
            blue_in_sum -= color.blue();
            alpha_in_sum -= color.alpha();

            ++stack_out_iterator;
        }
    }
}

}
