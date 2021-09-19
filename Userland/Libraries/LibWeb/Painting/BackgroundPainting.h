/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/Painting/BorderPainting.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

struct BackgroundData {
    Color color;
    Gfx::Bitmap const* image;
    CSS::Repeat repeat_x;
    CSS::Repeat repeat_y;
};

void paint_background(PaintContext&, Gfx::IntRect const&, BackgroundData const&, BorderRadiusData const&);

}
