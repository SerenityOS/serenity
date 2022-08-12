/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibWeb/HTML/Canvas/CanvasState.h>
#include <LibWeb/HTML/CanvasGradient.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvasfillstrokestyles
template<typename IncludingClass>
class CanvasFillStrokeStyles {
public:
    ~CanvasFillStrokeStyles() = default;

    void set_fill_style(String style)
    {
        // FIXME: 2. If the given value is a CanvasPattern object that is marked as not origin-clean, then set this's origin-clean flag to false.
        my_drawing_state().fill_style = Gfx::Color::from_string(style).value_or(Color::Black);
    }

    String fill_style() const
    {
        return my_drawing_state().fill_style.to_string();
    }

    void set_stroke_style(String style)
    {
        // FIXME: 2. If the given value is a CanvasPattern object that is marked as not origin-clean, then set this's origin-clean flag to false.
        my_drawing_state().stroke_style = Gfx::Color::from_string(style).value_or(Color::Black);
    }

    String stroke_style() const
    {
        return my_drawing_state().stroke_style.to_string();
    }

    NonnullRefPtr<CanvasGradient> create_radial_gradient(double x0, double y0, double r0, double x1, double y1, double r1)
    {
        return CanvasGradient::create_radial(x0, y0, r0, x1, y1, r1);
    }

    NonnullRefPtr<CanvasGradient> create_linear_gradient(double x0, double y0, double x1, double y1)
    {
        return CanvasGradient::create_linear(x0, y0, x1, y1);
    }

    NonnullRefPtr<CanvasGradient> create_conic_gradient(double start_angle, double x, double y)
    {
        return CanvasGradient::create_conic(start_angle, x, y);
    }

protected:
    CanvasFillStrokeStyles() = default;

private:
    CanvasState::DrawingState& my_drawing_state() { return reinterpret_cast<IncludingClass&>(*this).drawing_state(); }
    CanvasState::DrawingState const& my_drawing_state() const { return reinterpret_cast<IncludingClass const&>(*this).drawing_state(); }
};

}
