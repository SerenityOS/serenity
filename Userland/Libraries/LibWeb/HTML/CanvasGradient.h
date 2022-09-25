/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Color.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::HTML {

class CanvasGradient final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(CanvasGradient, Bindings::PlatformObject);

public:
    enum class Type {
        Linear,
        Radial,
        Conic,
    };

    static JS::NonnullGCPtr<CanvasGradient> create_radial(HTML::Window&, double x0, double y0, double r0, double x1, double y1, double r1);
    static JS::NonnullGCPtr<CanvasGradient> create_linear(HTML::Window&, double x0, double y0, double x1, double y1);
    static JS::NonnullGCPtr<CanvasGradient> create_conic(HTML::Window&, double start_angle, double x, double y);

    DOM::ExceptionOr<void> add_color_stop(double offset, String const& color);

    ~CanvasGradient();

private:
    CanvasGradient(HTML::Window&, Type);

    Type m_type {};

    struct ColorStop {
        double offset { 0 };
        Gfx::Color color;
    };

    Vector<ColorStop> m_color_stops;
};

}
