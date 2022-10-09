/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filter.h"

namespace PixelPaint {

class InplaceFilter : public Filter {
public:
    using Filter::apply;
    using Filter::Filter;

    virtual void apply(Gfx::Bitmap& target_bitmap) const = 0;

    virtual void apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const override
    {
        // This filter only works in-place, so if we have different target and source, we first copy over
        // the source bitmap to the target one.
        if (&target_bitmap != &source_bitmap) {
            VERIFY(source_bitmap.size_in_bytes() == target_bitmap.size_in_bytes());
            memcpy(target_bitmap.scanline(0), source_bitmap.scanline(0), source_bitmap.size_in_bytes());
        }
        apply(target_bitmap);
    }
};

}
