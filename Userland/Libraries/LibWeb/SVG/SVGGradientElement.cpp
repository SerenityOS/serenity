/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/SVGGradientElement.h>
#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

SVGGradientElement::SVGGradientElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGElement(document, move(qualified_name))
{
}

void SVGGradientElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGElement::attribute_changed(name, value);
    if (name == AttributeNames::gradientUnits) {
        m_gradient_units = AttributeParser::parse_gradient_units(value);
    } else if (name == AttributeNames::gradientTransform) {
        if (auto transform_list = AttributeParser::parse_transform(value); transform_list.has_value()) {
            m_gradient_transform = transform_from_transform_list(*transform_list);
        } else {
            m_gradient_transform = {};
        }
    }
}

GradientUnits SVGGradientElement::gradient_units() const
{
    if (m_gradient_units.has_value())
        return *m_gradient_units;
    if (auto gradient = linked_gradient())
        return gradient->gradient_units();
    return GradientUnits::ObjectBoundingBox;
}

Optional<Gfx::AffineTransform> SVGGradientElement::gradient_transform() const
{
    if (m_gradient_transform.has_value())
        return m_gradient_transform;
    if (auto gradient = linked_gradient())
        return gradient->gradient_transform();
    return {};
}

// The gradient transform, appropriately scaled and combined with the paint transform.
Gfx::AffineTransform SVGGradientElement::gradient_paint_transform(SVGPaintContext const& paint_context) const
{
    auto transform = gradient_transform().value_or(Gfx::AffineTransform {});
    if (gradient_units() == GradientUnits::ObjectBoundingBox) {
        // Adjust transform to take place in the coordinate system defined by the bounding box:
        return Gfx::AffineTransform { paint_context.transform }
            .translate(paint_context.path_bounding_box.location())
            .scale(paint_context.path_bounding_box.width(), paint_context.path_bounding_box.height())
            .multiply(transform);
    }
    return Gfx::AffineTransform { paint_context.transform }.multiply(transform);
}

void SVGGradientElement::add_color_stops(Gfx::SVGGradientPaintStyle& paint_style) const
{
    for_each_color_stop([&](auto& stop) {
        // https://svgwg.org/svg2-draft/pservers.html#StopNotes
        // Gradient offset values less than 0 (or less than 0%) are rounded up to 0%.
        // Gradient offset values greater than 1 (or greater than 100%) are rounded down to 100%.
        float stop_offset = AK::clamp(stop.stop_offset().value(), 0.0f, 1.0f);
        // FIXME: Each gradient offset value is required to be equal to or greater than the previous gradient
        // stop's offset value. If a given gradient stop's offset value is not equal to or greater than all
        // previous offset values, then the offset value is adjusted to be equal to the largest of all previous
        // offset values.
        paint_style.add_color_stop(stop_offset, stop.stop_color().with_opacity(stop.stop_opacity())).release_value_but_fixme_should_propagate_errors();
    });
}

JS::GCPtr<SVGGradientElement const> SVGGradientElement::linked_gradient() const
{
    // FIXME: This entire function is an ad-hoc hack!
    // It can only resolve #<ids> in the same document.

    auto link = has_attribute("href") ? get_attribute("href") : get_attribute("xlink:href");
    if (auto href = link; !href.is_empty()) {
        auto url = document().parse_url(href);
        auto id = url.fragment();
        if (id.is_empty())
            return {};
        auto element = document().get_element_by_id(id);
        if (!element)
            return {};
        if (!is<SVGGradientElement>(*element))
            return {};
        return &verify_cast<SVGGradientElement>(*element);
    }
    return {};
}

void SVGGradientElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGGradientElementPrototype>(realm, "SVGGradientElement"));
}

}
