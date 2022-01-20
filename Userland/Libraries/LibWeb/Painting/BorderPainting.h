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
    float top_left { 0 };
    float top_right { 0 };
    float bottom_right { 0 };
    float bottom_left { 0 };
};
BorderRadiusData normalized_border_radius_data(Layout::Node const&, Gfx::FloatRect const&, CSS::LengthPercentage top_left_radius, CSS::LengthPercentage top_right_radius, CSS::LengthPercentage bottom_right_radius, CSS::LengthPercentage bottom_left_radius);

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
void paint_border(PaintContext& context, BorderEdge edge, Gfx::FloatRect const& rect, BorderRadiusData const& border_radius_data, BordersData const& borders_data);
void paint_all_borders(PaintContext& context, Gfx::FloatRect const& bordered_rect, BorderRadiusData const& border_radius_data, BordersData const&);

}
