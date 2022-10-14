/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Color.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

enum class ShadowPlacement {
    Outer,
    Inner,
};

struct ShadowData {
    Gfx::Color color;
    CSSPixels offset_x;
    CSSPixels offset_y;
    CSSPixels blur_radius;
    CSSPixels spread_distance;
    ShadowPlacement placement;
};

void paint_box_shadow(PaintContext&, CSSPixelRect const&, BorderRadiiData const&, Vector<ShadowData> const&);
void paint_text_shadow(PaintContext&, Layout::LineBoxFragment const&, Vector<ShadowData> const&);

}
