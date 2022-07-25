/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

struct ColorStop {
    Gfx::Color color;
    float position = 0;
};

using ColorStopList = Vector<ColorStop, 4>;

struct LinearGradientData {
    float gradient_angle;
    ColorStopList color_stops;
};

LinearGradientData resolve_linear_gradient_data(Layout::Node const&, Gfx::FloatRect const&, CSS::LinearGradientStyleValue const&);

void paint_linear_gradient(PaintContext&, Gfx::IntRect const&, LinearGradientData const&);

}
