/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Forward.h>
#include <LibWeb/CSS/ComputedValues.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

struct BorderRadiusData {
    CSSPixels horizontal_radius { 0 };
    CSSPixels vertical_radius { 0 };

    Gfx::AntiAliasingPainter::CornerRadius as_corner(PaintContext& context) const
    {
        return Gfx::AntiAliasingPainter::CornerRadius {
            context.floored_device_pixels(horizontal_radius).value(),
            context.floored_device_pixels(vertical_radius).value()
        };
    }

    inline operator bool() const
    {
        return horizontal_radius > 0 && vertical_radius > 0;
    }

    inline void shrink(CSSPixels horizontal, CSSPixels vertical)
    {
        horizontal_radius = max(CSSPixels(0), horizontal_radius - horizontal);
        vertical_radius = max(CSSPixels(0), vertical_radius - vertical);
    }
};

struct BorderRadiiData {
    BorderRadiusData top_left;
    BorderRadiusData top_right;
    BorderRadiusData bottom_right;
    BorderRadiusData bottom_left;

    inline bool has_any_radius() const
    {
        return top_left || top_right || bottom_right || bottom_left;
    }

    inline void shrink(CSSPixels top, CSSPixels right, CSSPixels bottom, CSSPixels left)
    {
        top_left.shrink(left, top);
        top_right.shrink(right, top);
        bottom_right.shrink(right, bottom);
        bottom_left.shrink(left, bottom);
    }

    inline void inflate(CSSPixels top, CSSPixels right, CSSPixels bottom, CSSPixels left)
    {
        shrink(-top, -right, -bottom, -left);
    }
};

BorderRadiiData normalized_border_radii_data(Layout::Node const&, CSSPixelRect const&, CSS::BorderRadiusData top_left_radius, CSS::BorderRadiusData top_right_radius, CSS::BorderRadiusData bottom_right_radius, CSS::BorderRadiusData bottom_left_radius);

enum class BorderEdge {
    Top,
    Right,
    Bottom,
    Left,
};
struct BordersData {
    CSS::BorderData top;
    CSS::BorderData right;
    CSS::BorderData bottom;
    CSS::BorderData left;
};

// Returns OptionalNone if there is no outline to paint.
Optional<BordersData> borders_data_for_outline(Layout::Node const&, Color outline_color, CSS::OutlineStyle outline_style, CSSPixels outline_width);

RefPtr<Gfx::Bitmap> get_cached_corner_bitmap(DevicePixelSize corners_size);

void paint_border(PaintContext& context, BorderEdge edge, DevicePixelRect const& rect, Gfx::AntiAliasingPainter::CornerRadius const& radius, Gfx::AntiAliasingPainter::CornerRadius const& opposite_radius, BordersData const& borders_data, Gfx::Path& path, bool last);
void paint_all_borders(PaintContext& context, CSSPixelRect const& bordered_rect, BorderRadiiData const& border_radii_data, BordersData const&);

Gfx::Color border_color(BorderEdge edge, BordersData const& borders_data);

}
