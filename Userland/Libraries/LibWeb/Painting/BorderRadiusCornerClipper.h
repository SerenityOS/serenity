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
    enum class UseCachedBitmap {
        Yes,
        No
    };

    static ErrorOr<BorderRadiusCornerClipper> create(PaintContext&, DevicePixelRect const& border_rect, BorderRadiiData const& border_radii, CornerClip corner_clip = CornerClip::Outside, UseCachedBitmap use_cached_bitmap = UseCachedBitmap::Yes);

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
            DevicePixelPoint top_left;
            DevicePixelPoint top_right;
            DevicePixelPoint bottom_right;
            DevicePixelPoint bottom_left;
        };
        CornerLocations page_locations;
        CornerLocations bitmap_locations;
        DevicePixelSize corner_bitmap_size;
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

struct ScopedCornerRadiusClip {
    ScopedCornerRadiusClip(PaintContext& context, Gfx::Painter& painter, DevicePixelRect const& border_rect, BorderRadiiData const& border_radii, CornerClip corner_clip = CornerClip::Outside, BorderRadiusCornerClipper::UseCachedBitmap use_cached_bitmap = BorderRadiusCornerClipper::UseCachedBitmap::Yes)
        : m_painter(painter)
    {
        if (border_radii.has_any_radius()) {
            auto clipper = BorderRadiusCornerClipper::create(context, border_rect, border_radii, corner_clip, use_cached_bitmap);
            if (!clipper.is_error()) {
                m_corner_clipper = clipper.release_value();
                m_corner_clipper->sample_under_corners(m_painter);
            }
        }
    }

    ~ScopedCornerRadiusClip()
    {
        if (m_corner_clipper.has_value()) {
            m_corner_clipper->blit_corner_clipping(m_painter);
        }
    }

    AK_MAKE_NONMOVABLE(ScopedCornerRadiusClip);
    AK_MAKE_NONCOPYABLE(ScopedCornerRadiusClip);

private:
    Gfx::Painter& m_painter;
    Optional<BorderRadiusCornerClipper> m_corner_clipper;
};

}
