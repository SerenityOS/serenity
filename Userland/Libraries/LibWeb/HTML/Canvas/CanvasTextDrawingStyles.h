/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/Canvas/CanvasState.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvastextdrawingstyles
template<typename IncludingClass>
class CanvasTextDrawingStyles {
public:
    ~CanvasTextDrawingStyles() = default;

    void set_text_align(Bindings::CanvasTextAlign text_align) { my_drawing_state().text_align = text_align; }
    Bindings::CanvasTextAlign text_align() const { return my_drawing_state().text_align; }

    void set_text_baseline(Bindings::CanvasTextBaseline text_baseline) { my_drawing_state().text_baseline = text_baseline; }
    Bindings::CanvasTextBaseline text_baseline() const { return my_drawing_state().text_baseline; }

protected:
    CanvasTextDrawingStyles() = default;

private:
    CanvasState::DrawingState& my_drawing_state() { return reinterpret_cast<IncludingClass&>(*this).drawing_state(); }
    CanvasState::DrawingState const& my_drawing_state() const { return reinterpret_cast<IncludingClass const&>(*this).drawing_state(); }
};

}
