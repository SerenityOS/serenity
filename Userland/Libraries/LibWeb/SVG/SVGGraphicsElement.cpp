/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

SVGGraphicsElement::SVGGraphicsElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGElement(document, move(qualified_name))
{
}

void SVGGraphicsElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    CSS::ParsingContext parsing_context { document() };
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_case("fill")) {
            // FIXME: The `fill` attribute and CSS `fill` property are not the same! But our support is limited enough that they are equivalent for now.
            if (auto fill_value = parse_css_value(parsing_context, value, CSS::PropertyID::Fill))
                style.set_property(CSS::PropertyID::Fill, fill_value.release_nonnull());
        } else if (name.equals_ignoring_case("stroke")) {
            // FIXME: The `stroke` attribute and CSS `stroke` property are not the same! But our support is limited enough that they are equivalent for now.
            if (auto stroke_value = parse_css_value(parsing_context, value, CSS::PropertyID::Stroke))
                style.set_property(CSS::PropertyID::Stroke, stroke_value.release_nonnull());
        } else if (name.equals_ignoring_case("stroke-width")) {
            if (auto stroke_width_value = parse_css_value(parsing_context, value, CSS::PropertyID::StrokeWidth))
                style.set_property(CSS::PropertyID::StrokeWidth, stroke_width_value.release_nonnull());
        }
    });
}

Optional<Gfx::Color> SVGGraphicsElement::fill_color() const
{
    if (!layout_node())
        return {};
    // FIXME: In the working-draft spec, `fill` is intended to be a shorthand, with `fill-color`
    //        being what we actually want to use. But that's not final or widely supported yet.
    return layout_node()->computed_values().fill();
}

Optional<Gfx::Color> SVGGraphicsElement::stroke_color() const
{
    if (!layout_node())
        return {};
    // FIXME: In the working-draft spec, `stroke` is intended to be a shorthand, with `stroke-color`
    //        being what we actually want to use. But that's not final or widely supported yet.
    return layout_node()->computed_values().stroke();
}

Optional<float> SVGGraphicsElement::stroke_width() const
{
    if (!layout_node())
        return {};
    // FIXME: Converting to pixels isn't really correct - values should be in "user units"
    //        https://svgwg.org/svg2-draft/coords.html#TermUserUnits
    if (auto width = layout_node()->computed_values().stroke_width(); width.has_value()) {
        // Resolved relative to the "Scaled viewport size": https://www.w3.org/TR/2017/WD-fill-stroke-3-20170413/#scaled-viewport-size
        // FIXME: This isn't right, but it's something.
        auto scaled_viewport_size = CSS::Length::make_px((client_width() + client_height()) * 0.5f);
        return width->resolved(*layout_node(), scaled_viewport_size).to_px(*layout_node());
    }
    return {};
}

}
