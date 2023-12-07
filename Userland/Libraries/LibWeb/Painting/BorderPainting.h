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

enum class BorderEdge {
    Top,
    Right,
    Bottom,
    Left,
};

// Returns OptionalNone if there is no outline to paint.
Optional<BordersData> borders_data_for_outline(Layout::Node const&, Color outline_color, CSS::OutlineStyle outline_style, CSSPixels outline_width);

void paint_border(Gfx::Painter& painter, BorderEdge edge, DevicePixelRect const& rect, Gfx::AntiAliasingPainter::CornerRadius const& radius, Gfx::AntiAliasingPainter::CornerRadius const& opposite_radius, BordersDataDevicePixels const& borders_data, Gfx::Path& path, bool last);
void paint_all_borders(Gfx::Painter& painter, DevicePixelRect const& border_rect, CornerRadii const& corner_radii, BordersDataDevicePixels const&);

Gfx::Color border_color(BorderEdge edge, BordersDataDevicePixels const& borders_data);

}
