/*
 * Copyright (c) 2021, Davipb <daviparca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Mask.h"

namespace PixelPaint {

Mask::Mask(Gfx::IntRect bounding_rect, u8 default_value)
    : m_bounding_rect(bounding_rect)
{
    auto data_size = bounding_rect.size().area();
    m_data.resize(data_size);

    for (auto& x : m_data) {
        x = default_value;
    }
}

Mask Mask::with_bounding_rect(Gfx::IntRect inner_rect) const
{
    auto result = Mask::empty(inner_rect);

    result.for_each_pixel([&](int x, int y) {
        result.set(x, y, get(x, y));
    });

    return result;
}

void Mask::shrink_to_fit()
{
    int topmost = NumericLimits<int>::max();
    int bottommost = NumericLimits<int>::min();
    int leftmost = NumericLimits<int>::max();
    int rightmost = NumericLimits<int>::min();

    bool empty = true;
    for_each_pixel([&](auto x, auto y) {
        if (get(x, y) == 0) {
            return;
        }

        empty = false;

        topmost = min(topmost, y);
        bottommost = max(bottommost, y);

        leftmost = min(leftmost, x);
        rightmost = max(rightmost, x);
    });

    if (empty) {
        m_bounding_rect = {};
        m_data.clear();
        return;
    }

    Gfx::IntRect new_bounding_rect(
        leftmost,
        topmost,
        rightmost - leftmost + 1,
        bottommost - topmost + 1);

    *this = with_bounding_rect(new_bounding_rect);
}

void Mask::invert()
{
    for_each_pixel([&](int x, int y) {
        set(x, y, 0xFF - get(x, y));
    });
}

void Mask::add(Mask const& other)
{
    combine(other, [](int a, int b) { return a + b; });
}

void Mask::subtract(Mask const& other)
{
    combine(other, [](int a, int b) { return a - b; });
}

void Mask::intersect(Mask const& other)
{
    combinef(other, [](float a, float b) { return a * b; });
}

}
