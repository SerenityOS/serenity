/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGGradientElement.h>
#include <LibWeb/SVG/SVGGraphicsElement.h>
#include <LibWeb/SVG/SVGMaskElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>
#include <LibWeb/SVG/SVGSymbolElement.h>

namespace Web::SVG {

SVGGraphicsElement::SVGGraphicsElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGElement(document, move(qualified_name))
{
}

void SVGGraphicsElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGGraphicsElementPrototype>(realm, "SVGGraphicsElement"));
}

void SVGGraphicsElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGElement::attribute_changed(name, value);
    if (name == "transform"sv) {
        auto transform_list = AttributeParser::parse_transform(value);
        if (transform_list.has_value())
            m_transform = transform_from_transform_list(*transform_list);
    }
}

Optional<Gfx::PaintStyle const&> SVGGraphicsElement::svg_paint_computed_value_to_gfx_paint_style(SVGPaintContext const& paint_context, Optional<CSS::SVGPaint> const& paint_value) const
{
    // FIXME: This entire function is an ad-hoc hack:
    if (!paint_value.has_value() || !paint_value->is_url())
        return {};
    if (auto gradient = try_resolve_url_to<SVG::SVGGradientElement const>(paint_value->as_url()))
        return gradient->to_gfx_paint_style(paint_context);
    return {};
}

Optional<Gfx::PaintStyle const&> SVGGraphicsElement::fill_paint_style(SVGPaintContext const& paint_context) const
{
    if (!layout_node())
        return {};
    return svg_paint_computed_value_to_gfx_paint_style(paint_context, layout_node()->computed_values().fill());
}

Optional<Gfx::PaintStyle const&> SVGGraphicsElement::stroke_paint_style(SVGPaintContext const& paint_context) const
{
    if (!layout_node())
        return {};
    return svg_paint_computed_value_to_gfx_paint_style(paint_context, layout_node()->computed_values().stroke());
}

JS::GCPtr<SVG::SVGMaskElement const> SVGGraphicsElement::mask() const
{
    auto const& mask_reference = layout_node()->computed_values().mask();
    if (!mask_reference.has_value())
        return {};
    return try_resolve_url_to<SVG::SVGMaskElement const>(mask_reference->url());
}

Gfx::AffineTransform transform_from_transform_list(ReadonlySpan<Transform> transform_list)
{
    Gfx::AffineTransform affine_transform;
    for (auto& transform : transform_list) {
        transform.operation.visit(
            [&](Transform::Translate const& translate) {
                affine_transform.multiply(Gfx::AffineTransform {}.translate({ translate.x, translate.y }));
            },
            [&](Transform::Scale const& scale) {
                affine_transform.multiply(Gfx::AffineTransform {}.scale({ scale.x, scale.y }));
            },
            [&](Transform::Rotate const& rotate) {
                Gfx::AffineTransform translate_transform;
                affine_transform.multiply(
                    Gfx::AffineTransform {}
                        .translate({ rotate.x, rotate.y })
                        .rotate_radians(AK::to_radians(rotate.a))
                        .translate({ -rotate.x, -rotate.y }));
            },
            [&](Transform::SkewX const& skew_x) {
                affine_transform.multiply(Gfx::AffineTransform {}.skew_radians(AK::to_radians(skew_x.a), 0));
            },
            [&](Transform::SkewY const& skew_y) {
                affine_transform.multiply(Gfx::AffineTransform {}.skew_radians(0, AK::to_radians(skew_y.a)));
            },
            [&](Transform::Matrix const& matrix) {
                affine_transform.multiply(Gfx::AffineTransform {
                    matrix.a, matrix.b, matrix.c, matrix.d, matrix.e, matrix.f });
            });
    }
    return affine_transform;
}

Gfx::AffineTransform SVGGraphicsElement::get_transform() const
{
    Gfx::AffineTransform transform = m_transform;
    for (auto* svg_ancestor = shadow_including_first_ancestor_of_type<SVGGraphicsElement>(); svg_ancestor; svg_ancestor = svg_ancestor->shadow_including_first_ancestor_of_type<SVGGraphicsElement>()) {
        transform = Gfx::AffineTransform { svg_ancestor->element_transform() }.multiply(transform);
    }
    return transform;
}

void SVGGraphicsElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    CSS::Parser::ParsingContext parsing_context { document(), CSS::Parser::ParsingContext::Mode::SVGPresentationAttribute };
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
        } else if (name.equals_ignoring_ascii_case("fill-rule"sv)) {
            if (auto fill_rule_value = parse_css_value(parsing_context, value, CSS::PropertyID::FillRule))
                style.set_property(CSS::PropertyID::FillRule, fill_rule_value.release_nonnull());
        } else if (name.equals_ignoring_ascii_case("fill-opacity"sv)) {
            if (auto fill_opacity_value = parse_css_value(parsing_context, value, CSS::PropertyID::FillOpacity))
                style.set_property(CSS::PropertyID::FillOpacity, fill_opacity_value.release_nonnull());
        } else if (name.equals_ignoring_ascii_case("stroke-opacity"sv)) {
            if (auto stroke_opacity_value = parse_css_value(parsing_context, value, CSS::PropertyID::StrokeOpacity))
                style.set_property(CSS::PropertyID::StrokeOpacity, stroke_opacity_value.release_nonnull());
        } else if (name.equals_ignoring_ascii_case(SVG::AttributeNames::opacity)) {
            if (auto opacity_value = parse_css_value(parsing_context, value, CSS::PropertyID::Opacity))
                style.set_property(CSS::PropertyID::Opacity, opacity_value.release_nonnull());
        } else if (name.equals_ignoring_ascii_case("text-anchor"sv)) {
            if (auto text_anchor_value = parse_css_value(parsing_context, value, CSS::PropertyID::TextAnchor))
                style.set_property(CSS::PropertyID::TextAnchor, text_anchor_value.release_nonnull());
        } else if (name.equals_ignoring_ascii_case("font-size"sv)) {
            if (auto font_size_value = parse_css_value(parsing_context, value, CSS::PropertyID::FontSize))
                style.set_property(CSS::PropertyID::FontSize, font_size_value.release_nonnull());
        } else if (name.equals_ignoring_ascii_case("mask"sv)) {
            if (auto mask_value = parse_css_value(parsing_context, value, CSS::PropertyID::Mask))
                style.set_property(CSS::PropertyID::Mask, mask_value.release_nonnull());
        }
    });
}

Optional<FillRule> SVGGraphicsElement::fill_rule() const
{
    if (!layout_node())
        return {};
    switch (layout_node()->computed_values().fill_rule()) {
    case CSS::FillRule::Nonzero:
        return FillRule::Nonzero;
    case CSS::FillRule::Evenodd:
        return FillRule::Evenodd;
    default:
        VERIFY_NOT_REACHED();
    }
}

Optional<Gfx::Color> SVGGraphicsElement::fill_color() const
{
    if (!layout_node())
        return {};
    // FIXME: In the working-draft spec, `fill` is intended to be a shorthand, with `fill-color`
    //        being what we actually want to use. But that's not final or widely supported yet.
    return layout_node()->computed_values().fill().map([&](auto& paint) -> Gfx::Color {
        if (!paint.is_color())
            return Color::Black;
        return paint.as_color();
    });
}

Optional<Gfx::Color> SVGGraphicsElement::stroke_color() const
{
    if (!layout_node())
        return {};
    // FIXME: In the working-draft spec, `stroke` is intended to be a shorthand, with `stroke-color`
    //        being what we actually want to use. But that's not final or widely supported yet.
    return layout_node()->computed_values().stroke().map([](auto& paint) -> Gfx::Color {
        if (!paint.is_color())
            return Color::Black;
        return paint.as_color();
    });
}

Optional<float> SVGGraphicsElement::fill_opacity() const
{
    if (!layout_node())
        return {};
    return layout_node()->computed_values().fill_opacity();
}

Optional<float> SVGGraphicsElement::stroke_opacity() const
{
    if (!layout_node())
        return {};
    return layout_node()->computed_values().stroke_opacity();
}

Optional<float> SVGGraphicsElement::stroke_width() const
{
    if (!layout_node())
        return {};
    // FIXME: Converting to pixels isn't really correct - values should be in "user units"
    //        https://svgwg.org/svg2-draft/coords.html#TermUserUnits
    auto width = layout_node()->computed_values().stroke_width();
    // Resolved relative to the "Scaled viewport size": https://www.w3.org/TR/2017/WD-fill-stroke-3-20170413/#scaled-viewport-size
    // FIXME: This isn't right, but it's something.
    CSSPixels viewport_width = 0;
    CSSPixels viewport_height = 0;
    if (auto* svg_svg_element = shadow_including_first_ancestor_of_type<SVGSVGElement>()) {
        if (auto* svg_svg_layout_node = svg_svg_element->layout_node()) {
            viewport_width = svg_svg_layout_node->computed_values().width().to_px(*svg_svg_layout_node, 0);
            viewport_height = svg_svg_layout_node->computed_values().height().to_px(*svg_svg_layout_node, 0);
        }
    }
    auto scaled_viewport_size = (viewport_width + viewport_height) * CSSPixels(0.5);
    return width.to_px(*layout_node(), scaled_viewport_size).to_double();
}

Optional<ViewBox> SVGGraphicsElement::view_box() const
{
    if (auto* svg_svg_element = shadow_including_first_ancestor_of_type<SVGSVGElement>()) {
        if (svg_svg_element->view_box().has_value())
            return svg_svg_element->view_box();
    }

    if (auto* svg_symbol_element = shadow_including_first_ancestor_of_type<SVGSymbolElement>()) {
        if (svg_symbol_element->view_box().has_value())
            return svg_symbol_element->view_box();
    }

    return {};
}

}
