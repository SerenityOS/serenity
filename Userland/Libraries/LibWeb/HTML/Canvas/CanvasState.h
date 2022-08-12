/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/AffineTransform.h>
#include <LibGfx/Color.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/canvas.html#canvasstate
class CanvasState {
public:
    virtual ~CanvasState() = default;

    void save();
    void restore();
    void reset();
    bool is_context_lost();

    // https://html.spec.whatwg.org/multipage/canvas.html#drawing-state
    struct DrawingState {
        Gfx::AffineTransform transform;
        Gfx::Color fill_style { Gfx::Color::Black };
        Gfx::Color stroke_style { Gfx::Color::Black };
        float line_width { 1 };
    };
    DrawingState& drawing_state() { return m_drawing_state; }
    DrawingState const& drawing_state() const { return m_drawing_state; }

    void clear_drawing_state_stack() { m_drawing_state_stack.clear(); }
    void reset_drawing_state() { m_drawing_state = {}; }

    virtual void reset_to_default_state() = 0;

protected:
    CanvasState() = default;

private:
    DrawingState m_drawing_state;
    Vector<DrawingState> m_drawing_state_stack;

    // https://html.spec.whatwg.org/multipage/canvas.html#concept-canvas-context-lost
    bool m_context_lost { false };
};

}
