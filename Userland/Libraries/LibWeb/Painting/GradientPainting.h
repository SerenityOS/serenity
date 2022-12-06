/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

struct ColorStop {
    Gfx::Color color;
    float position = AK::NaN<float>;
    Optional<float> transition_hint = {};
};

using ColorStopList = Vector<ColorStop, 4>;

struct ColorStopData {
    ColorStopList list;
    Optional<float> repeat_length;
};

struct LinearGradientData {
    float gradient_angle;
    ColorStopData color_stops;
};

struct ConicGradientData {
    float start_angle;
    ColorStopData color_stops;
};

struct RadialGradientData {
    ColorStopData color_stops;
};

LinearGradientData resolve_linear_gradient_data(Layout::Node const&, Gfx::FloatSize, CSS::LinearGradientStyleValue const&);
ConicGradientData resolve_conic_gradient_data(Layout::Node const&, CSS::ConicGradientStyleValue const&);
RadialGradientData resolve_radial_gradient_data(Layout::Node const&, Gfx::FloatSize, CSS::RadialGradientStyleValue const&);

void paint_linear_gradient(PaintContext&, Gfx::IntRect const&, LinearGradientData const&);
void paint_conic_gradient(PaintContext&, Gfx::IntRect const&, ConicGradientData const&, Gfx::IntPoint position);
void paint_radial_gradient(PaintContext&, Gfx::IntRect const&, RadialGradientData const&, Gfx::IntPoint position, Gfx::FloatSize size);

}
