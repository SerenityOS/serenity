/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>
#include <LibWeb/SVG/AttributeParser.h>

namespace Web {

class SVGContext {
public:
    SVGContext(CSSPixelRect svg_element_bounds)
        : m_svg_element_bounds(svg_element_bounds)
    {
        m_states.append(State());
    }

    SVG::FillRule fill_rule() const { return state().fill_rule; }
    Gfx::Color fill_color() const { return state().fill_color; }
    Gfx::Color stroke_color() const { return state().stroke_color; }
    float stroke_width() const { return state().stroke_width; }
    float fill_opacity() const { return state().fill_opacity; }
    float stroke_opacity() const { return state().stroke_opacity; }

    void set_fill_rule(SVG::FillRule fill_rule) { state().fill_rule = fill_rule; }
    void set_fill_color(Gfx::Color color) { state().fill_color = color; }
    void set_stroke_color(Gfx::Color color) { state().stroke_color = color; }
    void set_stroke_width(float width) { state().stroke_width = width; }
    void set_fill_opacity(float opacity) { state().fill_opacity = opacity; }
    void set_stroke_opacity(float opacity) { state().stroke_opacity = opacity; }

    CSSPixelPoint svg_element_position() const { return m_svg_element_bounds.top_left(); }
    CSSPixelSize svg_element_size() const { return m_svg_element_bounds.size(); }

    void save() { m_states.append(m_states.last()); }
    void restore() { m_states.take_last(); }

private:
    struct State {
        SVG::FillRule fill_rule { SVG::FillRule::Nonzero };
        Gfx::Color fill_color { Gfx::Color::Transparent };
        Gfx::Color stroke_color { Gfx::Color::Transparent };
        float stroke_width { 1.0f };
        float fill_opacity { 1.0f };
        float stroke_opacity { 1.0f };
    };

    State const& state() const { return m_states.last(); }
    State& state() { return m_states.last(); }

    CSSPixelRect m_svg_element_bounds;
    Vector<State> m_states;
};

}
