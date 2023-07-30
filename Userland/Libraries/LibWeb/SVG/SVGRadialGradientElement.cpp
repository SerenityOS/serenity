/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/SVGRadialGradientElement.h>

namespace Web::SVG {

SVGRadialGradientElement::SVGRadialGradientElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGradientElement(document, qualified_name)
{
}

JS::ThrowCompletionOr<void> SVGRadialGradientElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGRadialGradientElementPrototype>(realm, "SVGRadialGradientElement"));

    return {};
}

void SVGRadialGradientElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGGradientElement::attribute_changed(name, value);

    // FIXME: These are <length> or <coordinate> in the spec, but all examples seem to allow percentages
    // and unitless values.
    if (name == SVG::AttributeNames::cx) {
        m_cx = AttributeParser::parse_number_percentage(value);
        m_paint_style = nullptr;
    } else if (name == SVG::AttributeNames::cy) {
        m_cy = AttributeParser::parse_number_percentage(value);
        m_paint_style = nullptr;
    } else if (name == SVG::AttributeNames::fx) {
        m_fx = AttributeParser::parse_number_percentage(value);
        m_paint_style = nullptr;
    } else if (name == SVG::AttributeNames::fy) {
        m_fy = AttributeParser::parse_number_percentage(value);
        m_paint_style = nullptr;
    } else if (name == SVG::AttributeNames::fr) {
        m_fr = AttributeParser::parse_number_percentage(value);
        m_paint_style = nullptr;
    } else if (name == SVG::AttributeNames::r) {
        m_r = AttributeParser::parse_number_percentage(value);
        m_paint_style = nullptr;
    }
}

// https://svgwg.org/svg2-draft/pservers.html#RadialGradientElementFXAttribute
NumberPercentage SVGRadialGradientElement::start_circle_x() const
{
    if (m_fx.has_value())
        return *m_fx;
    // If the element references an element that specifies a value for 'fx', then the value of 'fx' is
    // inherited from the referenced element.
    if (auto gradient = linked_radial_gradient())
        return gradient->start_circle_x();
    // If attribute ‘fx’ is not specified, ‘fx’ will coincide with the presentational value of ‘cx’ for
    // the element whether the value for 'cx' was inherited or not.
    return end_circle_x();
}

// https://svgwg.org/svg2-draft/pservers.html#RadialGradientElementFYAttribute
NumberPercentage SVGRadialGradientElement::start_circle_y() const
{
    if (m_fy.has_value())
        return *m_fy;
    // If the element references an element that specifies a value for 'fy', then the value of 'fy' is
    // inherited from the referenced element.
    if (auto gradient = linked_radial_gradient())
        return gradient->start_circle_y();
    // If attribute ‘fy’ is not specified, ‘fy’ will coincide with the presentational value of ‘cy’ for
    // the element whether the value for 'cy' was inherited or not.
    return end_circle_y();
}

// https://svgwg.org/svg2-draft/pservers.html#RadialGradientElementFRAttribute
NumberPercentage SVGRadialGradientElement::start_circle_radius() const
{
    // Note: A negative value is an error.
    if (m_fr.has_value() && m_fr->value() >= 0)
        return *m_fr;
    // if the element references an element that specifies a value for 'fr', then the value of
    // 'fr' is inherited from the referenced element.
    if (auto gradient = linked_radial_gradient())
        return gradient->start_circle_radius();
    // If the attribute is not specified, the effect is as if a value of '0%' were specified.
    return NumberPercentage::create_percentage(0);
}

// https://svgwg.org/svg2-draft/pservers.html#RadialGradientElementCXAttribute
NumberPercentage SVGRadialGradientElement::end_circle_x() const
{
    if (m_cx.has_value())
        return *m_cx;
    if (auto gradient = linked_radial_gradient())
        return gradient->end_circle_x();
    return NumberPercentage::create_percentage(50);
}

// https://svgwg.org/svg2-draft/pservers.html#RadialGradientElementCYAttribute
NumberPercentage SVGRadialGradientElement::end_circle_y() const
{
    if (m_cy.has_value())
        return *m_cy;
    if (auto gradient = linked_radial_gradient())
        return gradient->end_circle_y();
    return NumberPercentage::create_percentage(50);
}

// https://svgwg.org/svg2-draft/pservers.html#RadialGradientElementRAttribute
NumberPercentage SVGRadialGradientElement::end_circle_radius() const
{
    // Note: A negative value is an error.
    if (m_r.has_value() && m_r->value() >= 0)
        return *m_r;
    if (auto gradient = linked_radial_gradient())
        return gradient->end_circle_radius();
    return NumberPercentage::create_percentage(50);
}

Optional<Gfx::PaintStyle const&> SVGRadialGradientElement::to_gfx_paint_style(SVGPaintContext const& paint_context) const
{
    auto units = gradient_units();
    Gfx::FloatPoint start_center;
    float start_radius = 0.0f;
    Gfx::FloatPoint end_center;
    float end_radius = 0.0f;

    if (units == GradientUnits::ObjectBoundingBox) {
        // If gradientUnits="objectBoundingBox", the user coordinate system for attributes ‘cx’, ‘cy’, ‘r’, ‘fx’, ‘fy’, and ‘fr’
        // is established using the bounding box of the element to which the gradient is applied (see Object bounding box units)
        // and then applying the transform specified by attribute ‘gradientTransform’. Percentages represent values relative
        // to the bounding box for the object.
        start_center = Gfx::FloatPoint { start_circle_x().value(), start_circle_y().value() };
        start_radius = start_circle_radius().value();
        end_center = Gfx::FloatPoint { end_circle_x().value(), end_circle_y().value() };
        end_radius = end_circle_radius().value();
    } else {
        // GradientUnits::UserSpaceOnUse
        // If gradientUnits="userSpaceOnUse", ‘cx’, ‘cy’, ‘r’, ‘fx’, ‘fy’, and ‘fr’ represent values in the coordinate system
        // that results from taking the current user coordinate system in place at the time when the gradient element is
        // referenced (i.e., the user coordinate system for the element referencing the gradient element via a fill or stroke property)
        // and then applying the transform specified by attribute ‘gradientTransform’.
        // Percentages represent values relative to the current SVG viewport.
        // Note: The start/end centers will be in relative units here.
        // They will be resolved at paint time using the gradient paint transform.
        start_center = Gfx::FloatPoint {
            start_circle_x().resolve_relative_to(paint_context.viewport.width()),
            start_circle_y().resolve_relative_to(paint_context.viewport.height()),
        };
        // FIXME: Where in the spec does it say what axis the radius is relative to?
        start_radius = start_circle_radius().resolve_relative_to(paint_context.viewport.width());
        end_center = Gfx::FloatPoint {
            end_circle_x().resolve_relative_to(paint_context.viewport.width()),
            end_circle_y().resolve_relative_to(paint_context.viewport.height()),
        };
        end_radius = end_circle_radius().resolve_relative_to(paint_context.viewport.width());
    }

    if (!m_paint_style) {
        m_paint_style = Gfx::SVGRadialGradientPaintStyle::create(start_center, start_radius, end_center, end_radius)
                            .release_value_but_fixme_should_propagate_errors();
        // FIXME: Update stops in DOM changes:
        add_color_stops(*m_paint_style);
    } else {
        m_paint_style->set_start_center(start_center);
        m_paint_style->set_start_radius(start_radius);
        m_paint_style->set_end_center(end_center);
        m_paint_style->set_end_radius(end_radius);
    }
    m_paint_style->set_gradient_transform(gradient_paint_transform(paint_context));
    return *m_paint_style;
}

JS::NonnullGCPtr<SVGAnimatedLength> SVGRadialGradientElement::cx() const
{
    TODO();
}

JS::NonnullGCPtr<SVGAnimatedLength> SVGRadialGradientElement::cy() const
{
    TODO();
}

JS::NonnullGCPtr<SVGAnimatedLength> SVGRadialGradientElement::fx() const
{
    TODO();
}

JS::NonnullGCPtr<SVGAnimatedLength> SVGRadialGradientElement::fy() const
{
    TODO();
}

JS::NonnullGCPtr<SVGAnimatedLength> SVGRadialGradientElement::fr() const
{
    TODO();
}

JS::NonnullGCPtr<SVGAnimatedLength> SVGRadialGradientElement::r() const
{
    TODO();
}

}
