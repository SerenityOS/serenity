/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibWeb/CSS/ComputedValues.h>

namespace Web::Painting {

struct BorderRadiusData {
    float horizontal_radius { 0 };
    float vertical_radius { 0 };
};

struct BorderRadiiData {
    BorderRadiusData top_left;
    BorderRadiusData top_right;
    BorderRadiusData bottom_right;
    BorderRadiusData bottom_left;
};

enum class RelativeToWidthOnly {
    Yes,
    No
};

BorderRadiiData normalized_border_radii_data(Layout::Node const&, Gfx::FloatRect const&, CSS::BorderRadiusData top_left_radius, CSS::BorderRadiusData top_right_radius, CSS::BorderRadiusData bottom_right_radius, CSS::BorderRadiusData bottom_left_radius, RelativeToWidthOnly relative_to_width_only = RelativeToWidthOnly::No);

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
void paint_border(PaintContext& context, BorderEdge edge, Gfx::FloatRect const& rect, BorderRadiiData const& border_radii_data, BordersData const& borders_data);
void paint_all_borders(PaintContext& context, Gfx::FloatRect const& bordered_rect, BorderRadiiData const& border_radii_data, BordersData const&);

}
