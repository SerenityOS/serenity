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
BorderRadiusData normalized_border_radius_data(Layout::Node const&, Gfx::FloatRect const&, CSS::Length top_left_radius, CSS::Length top_right_radius, CSS::Length bottom_right_radius, CSS::Length bottom_left_radius);

enum class BorderEdge {
    Top,
    Right,
    Bottom,
    Left,
};
void paint_border(PaintContext&, BorderEdge, const Gfx::FloatRect&, const CSS::ComputedValues&);

}
