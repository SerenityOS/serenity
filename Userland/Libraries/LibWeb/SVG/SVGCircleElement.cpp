/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGCircleElement.h>

namespace Web::SVG {

SVGCircleElement::SVGCircleElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGeometryElement(document, qualified_name)
{
}

void SVGCircleElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGCircleElementPrototype>(realm, "SVGCircleElement"));
}

void SVGCircleElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGGeometryElement::attribute_changed(name, value);

    if (name == SVG::AttributeNames::cx) {
        m_center_x = AttributeParser::parse_coordinate(value);
        m_path.clear();
    } else if (name == SVG::AttributeNames::cy) {
        m_center_y = AttributeParser::parse_coordinate(value);
        m_path.clear();
    } else if (name == SVG::AttributeNames::r) {
        m_radius = AttributeParser::parse_positive_length(value);
        m_path.clear();
    }
}

Gfx::Path& SVGCircleElement::get_path()
{
    if (m_path.has_value())
        return m_path.value();

    float cx = m_center_x.value_or(0);
    float cy = m_center_y.value_or(0);
    float r = m_radius.value_or(0);

    Gfx::Path path;

    // A zero radius disables rendering.
    if (r == 0) {
        m_path = move(path);
        return m_path.value();
    }

    bool large_arc = false;
    bool sweep = true;

    // 1. A move-to command to the point cx+r,cy;
    path.move_to({ cx + r, cy });

    // 2. arc to cx,cy+r;
    path.arc_to({ cx, cy + r }, r, large_arc, sweep);

    // 3. arc to cx-r,cy;
    path.arc_to({ cx - r, cy }, r, large_arc, sweep);

    // 4. arc to cx,cy-r;
    path.arc_to({ cx, cy - r }, r, large_arc, sweep);

    // 5. arc with a segment-completing close path operation.
    path.arc_to({ cx + r, cy }, r, large_arc, sweep);

    m_path = move(path);
    return m_path.value();
}

// https://www.w3.org/TR/SVG11/shapes.html#CircleElementCXAttribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGCircleElement::cx() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_center_x.value_or(0)).release_value_but_fixme_should_propagate_errors();
    auto anim_length = SVGLength::create(realm(), 0, m_center_x.value_or(0)).release_value_but_fixme_should_propagate_errors();
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length)).release_value_but_fixme_should_propagate_errors();
}

// https://www.w3.org/TR/SVG11/shapes.html#CircleElementCYAttribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGCircleElement::cy() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_center_y.value_or(0)).release_value_but_fixme_should_propagate_errors();
    auto anim_length = SVGLength::create(realm(), 0, m_center_y.value_or(0)).release_value_but_fixme_should_propagate_errors();
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length)).release_value_but_fixme_should_propagate_errors();
}

// https://www.w3.org/TR/SVG11/shapes.html#CircleElementRAttribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGCircleElement::r() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_radius.value_or(0)).release_value_but_fixme_should_propagate_errors();
    auto anim_length = SVGLength::create(realm(), 0, m_radius.value_or(0)).release_value_but_fixme_should_propagate_errors();
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length)).release_value_but_fixme_should_propagate_errors();
}

}
