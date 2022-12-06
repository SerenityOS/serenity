/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Forward.h>
#include <LibWeb/CSS/ComputedValues.h>

namespace Web::Painting {

struct BorderRadiusData {
    float horizontal_radius { 0 };
    float vertical_radius { 0 };

    Gfx::AntiAliasingPainter::CornerRadius as_corner() const
    {
        return Gfx::AntiAliasingPainter::CornerRadius {
            static_cast<int>(horizontal_radius),
            static_cast<int>(vertical_radius)
        };
    };

    inline operator bool() const
    {
        return static_cast<int>(horizontal_radius) > 0 && static_cast<int>(vertical_radius) > 0;
    }

    inline void shrink(float horizontal, float vertical)
    {
        horizontal_radius = max(0, horizontal_radius - horizontal);
        vertical_radius = max(0, vertical_radius - vertical);
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

    inline void shrink(float top, float right, float bottom, float left)
    {
        top_left.shrink(left, top);
        top_right.shrink(right, top);
        bottom_right.shrink(right, bottom);
        bottom_left.shrink(left, bottom);
    }
};

BorderRadiiData normalized_border_radii_data(Layout::Node const&, Gfx::FloatRect const&, CSS::BorderRadiusData top_left_radius, CSS::BorderRadiusData top_right_radius, CSS::BorderRadiusData bottom_right_radius, CSS::BorderRadiusData bottom_left_radius);

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

RefPtr<Gfx::Bitmap> get_cached_corner_bitmap(Gfx::IntSize corners_size);

void paint_border(PaintContext& context, BorderEdge edge, Gfx::IntRect const& rect, BorderRadiiData const& border_radii_data, BordersData const& borders_data);
void paint_all_borders(PaintContext& context, Gfx::FloatRect const& bordered_rect, BorderRadiiData const& border_radii_data, BordersData const&);

}
