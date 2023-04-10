/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/SVG/SVGGraphicsElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::SVG {

SVGGraphicsElement::SVGGraphicsElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGElement(document, move(qualified_name))
{
}

JS::ThrowCompletionOr<void> SVGGraphicsElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGGraphicsElementPrototype>(realm, "SVGGraphicsElement"));

    return {};
}

void SVGGraphicsElement::parse_attribute(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGElement::parse_attribute(name, value);
    if (name == "fill-opacity"sv) {
        m_fill_opacity = AttributeParser::parse_length(value);
    }
}

void SVGGraphicsElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    CSS::Parser::ParsingContext parsing_context { document() };
    for_each_attribute([&](auto& name, auto& value) {
        if (name.equals_ignoring_ascii_case("fill"sv)) {
            // FIXME: The `fill` attribute and CSS `fill` property are not the same! But our support is limited enough that they are equivalent for now.
            if (auto fill_value = parse_css_value(parsing_context, value, CSS::PropertyID::Fill))
                style.set_property(CSS::PropertyID::Fill, fill_value.release_nonnull());
        } else if (name.equals_ignoring_ascii_case("stroke"sv)) {
            // FIXME: The `stroke` attribute and CSS `stroke` property are not the same! But our support is limited enough that they are equivalent for now.
            if (auto stroke_value = parse_css_value(parsing_context, value, CSS::PropertyID::Stroke))
                style.set_property(CSS::PropertyID::Stroke, stroke_value.release_nonnull());
        } else if (name.equals_ignoring_ascii_case("stroke-width"sv)) {
            if (auto stroke_width_value = parse_css_value(parsing_context, value, CSS::PropertyID::StrokeWidth))
                style.set_property(CSS::PropertyID::StrokeWidth, stroke_width_value.release_nonnull());
        } else if (name.equals_ignoring_ascii_case("transform"sv)) {
            if (auto transform = parse_css_value(parsing_context, value, CSS::PropertyID::Transform))
                style.set_property(CSS::PropertyID::Transform, transform.release_nonnull());
        }
    });
}

Optional<Gfx::Color> SVGGraphicsElement::fill_color() const
{
    if (!layout_node())
        return {};
    // FIXME: In the working-draft spec, `fill` is intended to be a shorthand, with `fill-color`
    //        being what we actually want to use. But that's not final or widely supported yet.
    return layout_node()->computed_values().fill().map([&](Gfx::Color color) {
        return color.with_alpha(m_fill_opacity.value_or(1) * 255);
    });
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
        CSSPixels viewport_width = 0;
        CSSPixels viewport_height = 0;
        if (auto* svg_svg_element = first_ancestor_of_type<SVGSVGElement>()) {
            if (auto* svg_svg_layout_node = svg_svg_element->layout_node()) {
                viewport_width = svg_svg_layout_node->computed_values().width().resolved(*svg_svg_layout_node, CSS::Length::make_px(0)).to_px(*svg_svg_layout_node);
                viewport_height = svg_svg_layout_node->computed_values().height().resolved(*svg_svg_layout_node, CSS::Length::make_px(0)).to_px(*svg_svg_layout_node);
            }
        }
        auto scaled_viewport_size = CSS::Length::make_px((viewport_width + viewport_height) * 0.5f);
        return width->resolved(*layout_node(), scaled_viewport_size).to_px(*layout_node()).value();
    }
    return {};
}

}
