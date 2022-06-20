/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/AntiAliasingPainter.h>
#include <LibWeb/Painting/BorderPainting.h>

namespace Web::Painting {

enum class CornerClip {
    Outside,
    Inside
};

class BorderRadiusCornerClipper {
public:
    static ErrorOr<BorderRadiusCornerClipper> create(Gfx::IntRect const& border_rect, BorderRadiiData const& border_radii, CornerClip corner_clip = CornerClip::Outside);

    void sample_under_corners(Gfx::Painter& page_painter);
    void blit_corner_clipping(Gfx::Painter& page_painter);

private:
    using CornerRadius = Gfx::AntiAliasingPainter::CornerRadius;
    struct CornerData {
        struct CornerRadii {
            CornerRadius top_left;
            CornerRadius top_right;
            CornerRadius bottom_right;
            CornerRadius bottom_left;
        } corner_radii;
        struct CornerLocations {
            Gfx::IntPoint top_left;
            Gfx::IntPoint top_right;
            Gfx::IntPoint bottom_right;
            Gfx::IntPoint bottom_left;
        };
        CornerLocations page_locations;
        CornerLocations bitmap_locations;
        Gfx::IntSize corner_bitmap_size;
    } m_data;

    NonnullRefPtr<Gfx::Bitmap> m_corner_bitmap;
    bool m_has_sampled { false };
    CornerClip m_corner_clip { false };

    BorderRadiusCornerClipper(CornerData corner_data, NonnullRefPtr<Gfx::Bitmap> corner_bitmap, CornerClip corner_clip)
        : m_data(move(corner_data))
        , m_corner_bitmap(corner_bitmap)
        , m_corner_clip(corner_clip)
    {
    }
};

}
