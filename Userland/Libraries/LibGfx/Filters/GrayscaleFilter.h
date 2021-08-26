/*
 * Copyright (c) 2021, David Savary <david.savarymartinez@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Filters/Filter.h>

namespace Gfx {

class GrayscaleFilter : public Filter {
public:
    GrayscaleFilter() { }
    virtual ~GrayscaleFilter() { }

    virtual char const* class_name() const override { return "GrayscaleFilter"; }

    virtual void apply(Bitmap& target_bitmap, IntRect const& target_rect, Bitmap const& source_bitmap, IntRect const& source_rect) override
    {
        // source_rect should be describing the pixels that can be accessed
        // to apply this filter, while target_rect should describe the area
        // where to apply the filter on.
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
                auto target_color = source_pixel.to_grayscale();

                target_bitmap.set_pixel(target_x, target_y, target_color);
            }
        }
    }
};

}
