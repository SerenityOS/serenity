/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Color.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

struct BoxShadowData {
    int offset_x;
    int offset_y;
    int blur_radius;
    Gfx::Color color;
};

void paint_box_shadow(PaintContext&, Gfx::IntRect const&, BoxShadowData const&);

}
