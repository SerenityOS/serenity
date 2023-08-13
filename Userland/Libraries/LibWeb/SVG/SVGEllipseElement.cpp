/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Window.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGEllipseElement.h>

namespace Web::SVG {

SVGEllipseElement::SVGEllipseElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGeometryElement(document, qualified_name)
{
}

void SVGEllipseElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGEllipseElementPrototype>(realm, "SVGEllipseElement"));
}

void SVGEllipseElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGGeometryElement::attribute_changed(name, value);

    if (name == SVG::AttributeNames::cx) {
        m_center_x = AttributeParser::parse_coordinate(value);
        m_path.clear();
    } else if (name == SVG::AttributeNames::cy) {
        m_center_y = AttributeParser::parse_coordinate(value);
        m_path.clear();
    } else if (name == SVG::AttributeNames::rx) {
        m_radius_x = AttributeParser::parse_positive_length(value);
        m_path.clear();
    } else if (name == SVG::AttributeNames::ry) {
        m_radius_y = AttributeParser::parse_positive_length(value);
        m_path.clear();
    }
}

Gfx::Path& SVGEllipseElement::get_path()
{
    if (m_path.has_value())
        return m_path.value();

    float rx = m_radius_x.value_or(0);
    float ry = m_radius_y.value_or(0);
    float cx = m_center_x.value_or(0);
    float cy = m_center_y.value_or(0);
    Gfx::Path path;

    // A computed value of zero for either dimension, or a computed value of auto for both dimensions, disables rendering of the element.
    if (rx == 0 || ry == 0) {
        m_path = move(path);
        return m_path.value();
    }

    Gfx::FloatSize radii = { rx, ry };
    double x_axis_rotation = 0;
    bool large_arc = false;
    bool sweep = true; // Note: Spec says it should be false, but it's wrong. https://github.com/w3c/svgwg/issues/765

    // 1. A move-to command to the point cx+rx,cy;
    path.move_to({ cx + rx, cy });

    // 2. arc to cx,cy+ry;
    path.elliptical_arc_to({ cx, cy + ry }, radii, x_axis_rotation, large_arc, sweep);

    // 3. arc to cx-rx,cy;
    path.elliptical_arc_to({ cx - rx, cy }, radii, x_axis_rotation, large_arc, sweep);

    // 4. arc to cx,cy-ry;
    path.elliptical_arc_to({ cx, cy - ry }, radii, x_axis_rotation, large_arc, sweep);

    // 5. arc with a segment-completing close path operation.
    path.elliptical_arc_to({ cx + rx, cy }, radii, x_axis_rotation, large_arc, sweep);

    m_path = move(path);
    return m_path.value();
}

// https://www.w3.org/TR/SVG11/shapes.html#EllipseElementCXAttribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGEllipseElement::cx() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_center_x.value_or(0));
    auto anim_length = SVGLength::create(realm(), 0, m_center_x.value_or(0));
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length));
}

// https://www.w3.org/TR/SVG11/shapes.html#EllipseElementCYAttribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGEllipseElement::cy() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_center_y.value_or(0));
    auto anim_length = SVGLength::create(realm(), 0, m_center_y.value_or(0));
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length));
}

// https://www.w3.org/TR/SVG11/shapes.html#EllipseElementRXAttribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGEllipseElement::rx() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_radius_x.value_or(0));
    auto anim_length = SVGLength::create(realm(), 0, m_radius_x.value_or(0));
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length));
}

// https://www.w3.org/TR/SVG11/shapes.html#EllipseElementRYAttribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGEllipseElement::ry() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_radius_y.value_or(0));
    auto anim_length = SVGLength::create(realm(), 0, m_radius_y.value_or(0));
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length));
}

}
