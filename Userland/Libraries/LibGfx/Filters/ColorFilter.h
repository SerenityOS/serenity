/*
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filter.h"

namespace Gfx {

class ColorFilter : public Filter {
public:
    virtual ~ColorFilter() = default;

    virtual void apply(Bitmap& target_bitmap, IntRect const& target_rect, Bitmap const& source_bitmap, IntRect const& source_rect) override
    {
        VERIFY(source_rect.size() == target_rect.size());
        VERIFY(target_bitmap.rect().contains(target_rect));
        VERIFY(source_bitmap.rect().contains(source_rect));

        for (auto y = 0; y < source_rect.height(); ++y) {
            ssize_t source_y = y + source_rect.y();
            ssize_t target_y = y + target_rect.y();
            for (auto x = 0; x < source_rect.width(); ++x) {
                ssize_t source_x = x + source_rect.x();
                ssize_t target_x = x + target_rect.x();

                auto source_pixel = source_bitmap.get_pixel(source_x, source_y);
                auto target_color = convert_color(source_pixel);

                target_bitmap.set_pixel(target_x, target_y, target_color);
            }
        }
    }

protected:
    ColorFilter() = default;

    virtual Color convert_color(Color) = 0;
};

}
