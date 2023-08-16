/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Gradients.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

using ColorStopList = Vector<Gfx::ColorStop, 4>;

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

LinearGradientData resolve_linear_gradient_data(Layout::NodeWithStyleAndBoxModelMetrics const&, CSSPixelSize, CSS::LinearGradientStyleValue const&);
ConicGradientData resolve_conic_gradient_data(Layout::NodeWithStyleAndBoxModelMetrics const&, CSS::ConicGradientStyleValue const&);
RadialGradientData resolve_radial_gradient_data(Layout::NodeWithStyleAndBoxModelMetrics const&, CSSPixelSize, CSS::RadialGradientStyleValue const&);

void paint_linear_gradient(PaintContext&, DevicePixelRect const&, LinearGradientData const&);
void paint_conic_gradient(PaintContext&, DevicePixelRect const&, ConicGradientData const&, DevicePixelPoint position);
void paint_radial_gradient(PaintContext&, DevicePixelRect const&, RadialGradientData const&, DevicePixelPoint position, DevicePixelSize size);

}
