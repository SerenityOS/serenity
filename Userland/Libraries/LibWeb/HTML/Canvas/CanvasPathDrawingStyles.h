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

protected:
    CanvasPathDrawingStyles() = default;

private:
    CanvasState::DrawingState& my_drawing_state() { return reinterpret_cast<IncludingClass&>(*this).drawing_state(); }
    CanvasState::DrawingState const& my_drawing_state() const { return reinterpret_cast<IncludingClass const&>(*this).drawing_state(); }
};

}
