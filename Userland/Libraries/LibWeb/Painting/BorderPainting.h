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
#include <LibWeb/Forward.h>
#include <LibWeb/Painting/BorderRadiiData.h>
#include <LibWeb/Painting/BordersData.h>

namespace Web::Painting {

BorderRadiiData normalized_border_radii_data(Layout::Node const&, CSSPixelRect const&, CSS::BorderRadiusData top_left_radius, CSS::BorderRadiusData top_right_radius, CSS::BorderRadiusData bottom_right_radius, CSS::BorderRadiusData bottom_left_radius);

enum class BorderEdge {
    Top,
    Right,
    Bottom,
    Left,
};

// Returns OptionalNone if there is no outline to paint.
Optional<BordersData> borders_data_for_outline(Layout::Node const&, Color outline_color, CSS::OutlineStyle outline_style, CSSPixels outline_width);

RefPtr<Gfx::Bitmap> get_cached_corner_bitmap(DevicePixelSize corners_size);

void paint_border(PaintContext& context, BorderEdge edge, DevicePixelRect const& rect, Gfx::AntiAliasingPainter::CornerRadius const& radius, Gfx::AntiAliasingPainter::CornerRadius const& opposite_radius, BordersData const& borders_data, Gfx::Path& path, bool last);
void paint_all_borders(PaintContext& context, DevicePixelRect const& border_rect, BorderRadiiData const& border_radii_data, BordersData const&);

Gfx::Color border_color(BorderEdge edge, BordersData const& borders_data);

}
