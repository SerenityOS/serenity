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

    void set_line_width(float line_width) { my_drawing_state().line_width = line_width; }
    float line_width() const { return my_drawing_state().line_width; }

protected:
    CanvasPathDrawingStyles() = default;

private:
    CanvasState::DrawingState& my_drawing_state() { return reinterpret_cast<IncludingClass&>(*this).drawing_state(); }
    CanvasState::DrawingState const& my_drawing_state() const { return reinterpret_cast<IncludingClass const&>(*this).drawing_state(); }
};

}
