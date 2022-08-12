/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <LibWeb/HTML/Canvas/CanvasState.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvastransform
template<typename IncludingClass>
class CanvasTransform {
public:
    ~CanvasTransform() = default;

    void scale(float sx, float sy)
    {
        dbgln_if(CANVAS_RENDERING_CONTEXT_2D_DEBUG, "CanvasTransform::scale({}, {})", sx, sy);

        my_drawing_state().transform.scale(sx, sy);
    }

    void translate(float tx, float ty)
    {
        dbgln_if(CANVAS_RENDERING_CONTEXT_2D_DEBUG, "CanvasTransform::translate({}, {})", tx, ty);
        my_drawing_state().transform.translate(tx, ty);
    }

    void rotate(float radians)
    {
        dbgln_if(CANVAS_RENDERING_CONTEXT_2D_DEBUG, "CanvasTransform::rotate({})", radians);
        my_drawing_state().transform.rotate_radians(radians);
    }

    // https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-transform
    void transform(double a, double b, double c, double d, double e, double f)
    {
        // 1. If any of the arguments are infinite or NaN, then return.
        if (!isfinite(a) || !isfinite(b) || !isfinite(c) || !isfinite(d) || !isfinite(e) || !isfinite(f))
            return;

        // 2. Replace the current transformation matrix with the result of multiplying the current transformation matrix with the matrix described by:
        //    a c e
        //    b d f
        //    0 0 1
        my_drawing_state().transform.multiply({ static_cast<float>(a), static_cast<float>(b), static_cast<float>(c), static_cast<float>(d), static_cast<float>(e), static_cast<float>(f) });
    }

    // https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-settransform
    void set_transform(double a, double b, double c, double d, double e, double f)
    {
        // 1. If any of the arguments are infinite or NaN, then return.
        if (!isfinite(a) || !isfinite(b) || !isfinite(c) || !isfinite(d) || !isfinite(e) || !isfinite(f))
            return;

        // 2. Reset the current transformation matrix to the identity matrix.
        my_drawing_state().transform = {};

        // 3. Invoke the transform(a, b, c, d, e, f) method with the same arguments.
        transform(a, b, c, d, e, f);
    }

    // https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-resettransform
    void reset_transform()
    {
        // The resetTransform() method, when invoked, must reset the current transformation matrix to the identity matrix.
        my_drawing_state().transform = {};
    }

protected:
    CanvasTransform() = default;

private:
    CanvasState::DrawingState& my_drawing_state() { return reinterpret_cast<IncludingClass&>(*this).drawing_state(); }
    CanvasState::DrawingState const& my_drawing_state() const { return reinterpret_cast<IncludingClass const&>(*this).drawing_state(); }
};

}
