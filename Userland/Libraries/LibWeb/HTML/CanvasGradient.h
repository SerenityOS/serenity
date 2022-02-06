/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibGfx/Color.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::HTML {

class CanvasGradient final
    : public RefCounted<CanvasGradient>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::CanvasGradientWrapper;

    enum class Type {
        Linear,
        Radial,
        Conic,
    };

    static NonnullRefPtr<CanvasGradient> create_radial(double x0, double y0, double r0, double x1, double y1, double r1);
    static NonnullRefPtr<CanvasGradient> create_linear(double x0, double y0, double x1, double y1);
    static NonnullRefPtr<CanvasGradient> create_conic(double start_angle, double x, double y);

    DOM::ExceptionOr<void> add_color_stop(double offset, String const& color);

    ~CanvasGradient();

private:
    explicit CanvasGradient(Type);

    Type m_type {};

    struct ColorStop {
        double offset { 0 };
        Gfx::Color color;
    };

    Vector<ColorStop> m_color_stops;
};

}
