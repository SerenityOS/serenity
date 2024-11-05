/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/Canvas/CanvasState.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvaspathdrawingstyles
template<typename IncludingClass>
class CanvasPathDrawingStyles {
public:
    ~CanvasPathDrawingStyles() = default;

    // https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-linewidth
    void set_line_width(float line_width)
    {
        // On setting, zero, negative, infinite, and NaN values must be ignored, leaving the value unchanged;
        if (line_width <= 0 || !isfinite(line_width))
            return;
        // other values must change the current value to the new value.
        my_drawing_state().line_width = line_width;
    }
    float line_width() const
    {
        // On getting, it must return the current value.
        return my_drawing_state().line_width;
    }

    // https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-linecap
    void set_line_cap(Bindings::CanvasLineCap line_cap)
    {
        // On setting, the current value must be changed to the new value.
        my_drawing_state().line_cap = line_cap;
    }
    Bindings::CanvasLineCap line_cap() const
    {
        // On getting, it must return the current value.
        return my_drawing_state().line_cap;
    }

    // https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-linejoin
    void set_line_join(Bindings::CanvasLineJoin line_join)
    {
        // On setting, the current value must be changed to the new value.
        my_drawing_state().line_join = line_join;
    }
    Bindings::CanvasLineJoin line_join() const
    {
        // On getting, it must return the current value.
        return my_drawing_state().line_join;
    }

    // https://html.spec.whatwg.org/multipage/canvas.html#dom-context-2d-miterlimit
    void set_miter_limit(float miter_limit)
    {
        // On setting, zero, negative, infinite, and NaN values must be ignored, leaving the value unchanged;
        if (miter_limit <= 0 || !isfinite(miter_limit))
            return;
        // other values must change the current value to the new value.
        my_drawing_state().miter_limit = miter_limit;
    }
    float miter_limit() const
    {
        // On getting, it must return the current value.
        return my_drawing_state().miter_limit;
    }

protected:
    CanvasPathDrawingStyles() = default;

private:
    CanvasState::DrawingState& my_drawing_state() { return reinterpret_cast<IncludingClass&>(*this).drawing_state(); }
    CanvasState::DrawingState const& my_drawing_state() const { return reinterpret_cast<IncludingClass const&>(*this).drawing_state(); }
};

}
