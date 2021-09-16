/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

SVGGraphicsElement::SVGGraphicsElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGElement(document, move(qualified_name))
{
}

void SVGGraphicsElement::parse_attribute(FlyString const& name, String const& value)
{
    SVGElement::parse_attribute(name, value);

    if (name == "fill") {
        m_fill_color = Gfx::Color::from_string(value).value_or(Color::Transparent);
    } else if (name == "stroke") {
        m_stroke_color = Gfx::Color::from_string(value).value_or(Color::Transparent);
    } else if (name == "stroke-width") {
        auto result = value.to_int();
        if (result.has_value())
            m_stroke_width = result.value();
    }
}

Optional<Gfx::Color> SVGGraphicsElement::fill_color() const
{
    if (m_fill_color.has_value())
        return m_fill_color;
    if (!layout_node())
        return {};
    // FIXME: In the working-draft spec, `fill` is intended to be a shorthand, with `fill-color`
    //        being what we actually want to use. But that's not final or widely supported yet.
    return layout_node()->computed_values().fill();
}

Optional<Gfx::Color> SVGGraphicsElement::stroke_color() const
{
    if (m_stroke_color.has_value())
        return m_stroke_color;
    if (!layout_node())
        return {};
    // FIXME: In the working-draft spec, `stroke` is intended to be a shorthand, with `stroke-color`
    //        being what we actually want to use. But that's not final or widely supported yet.
    return layout_node()->computed_values().stroke();
}

Optional<float> SVGGraphicsElement::stroke_width() const
{
    if (m_stroke_width.has_value())
        return m_stroke_width;
    if (!layout_node())
        return {};
    // FIXME: Converting to pixels isn't really correct - values should be in "user units"
    //        https://svgwg.org/svg2-draft/coords.html#TermUserUnits
    if (auto width = layout_node()->computed_values().stroke_width(); width.has_value())
        return width->to_px(*layout_node());
    return {};
}

}
