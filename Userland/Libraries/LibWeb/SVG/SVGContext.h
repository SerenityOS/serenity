/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>

namespace Web {

class SVGContext {
public:
    SVGContext(CSSPixelRect svg_element_bounds)
        : m_svg_element_bounds(svg_element_bounds)
    {
        m_states.append(State());
    }

    Gfx::Color fill_color() const { return state().fill_color; }
    Gfx::Color stroke_color() const { return state().stroke_color; }
    float stroke_width() const { return state().stroke_width; }

    void set_fill_color(Gfx::Color color) { state().fill_color = color; }
    void set_stroke_color(Gfx::Color color) { state().stroke_color = color; }
    void set_stroke_width(float width) { state().stroke_width = width; }

    CSSPixelPoint svg_element_position() const { return m_svg_element_bounds.top_left(); }

    void save() { m_states.append(m_states.last()); }
    void restore() { m_states.take_last(); }

private:
    struct State {
        Gfx::Color fill_color { Gfx::Color::Transparent };
        Gfx::Color stroke_color { Gfx::Color::Transparent };
        float stroke_width { 1.0 };
    };

    State const& state() const { return m_states.last(); }
    State& state() { return m_states.last(); }

    CSSPixelRect m_svg_element_bounds;
    Vector<State> m_states;
};

}
