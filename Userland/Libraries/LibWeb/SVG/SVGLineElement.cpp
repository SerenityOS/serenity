/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGLineElement.h>

namespace Web::SVG {

SVGLineElement::SVGLineElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGeometryElement(document, qualified_name)
{
}

void SVGLineElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGLineElementPrototype>(realm, "SVGLineElement"));
}

void SVGLineElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    SVGGeometryElement::attribute_changed(name, value);

    if (name == SVG::AttributeNames::x1) {
        m_x1 = AttributeParser::parse_coordinate(value);
        m_path.clear();
    } else if (name == SVG::AttributeNames::y1) {
        m_y1 = AttributeParser::parse_coordinate(value);
        m_path.clear();
    } else if (name == SVG::AttributeNames::x2) {
        m_x2 = AttributeParser::parse_coordinate(value);
        m_path.clear();
    } else if (name == SVG::AttributeNames::y2) {
        m_y2 = AttributeParser::parse_coordinate(value);
        m_path.clear();
    }
}

Gfx::Path& SVGLineElement::get_path()
{
    if (m_path.has_value())
        return m_path.value();

    Gfx::Path path;
    float x1 = m_x1.value_or(0);
    float y1 = m_y1.value_or(0);
    float x2 = m_x2.value_or(0);
    float y2 = m_y2.value_or(0);

    // 1. perform an absolute moveto operation to absolute location (x1,y1)
    path.move_to({ x1, y1 });

    // 2. perform an absolute lineto operation to absolute location (x2,y2)
    path.line_to({ x2, y2 });

    m_path = move(path);
    return m_path.value();
}

// https://www.w3.org/TR/SVG11/shapes.html#LineElementX1Attribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGLineElement::x1() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_x1.value_or(0));
    auto anim_length = SVGLength::create(realm(), 0, m_x1.value_or(0));
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length));
}

// https://www.w3.org/TR/SVG11/shapes.html#LineElementY1Attribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGLineElement::y1() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_y1.value_or(0));
    auto anim_length = SVGLength::create(realm(), 0, m_y1.value_or(0));
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length));
}

// https://www.w3.org/TR/SVG11/shapes.html#LineElementX2Attribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGLineElement::x2() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_x2.value_or(0));
    auto anim_length = SVGLength::create(realm(), 0, m_x2.value_or(0));
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length));
}

// https://www.w3.org/TR/SVG11/shapes.html#LineElementY2Attribute
JS::NonnullGCPtr<SVGAnimatedLength> SVGLineElement::y2() const
{
    // FIXME: Populate the unit type when it is parsed (0 here is "unknown").
    // FIXME: Create a proper animated value when animations are supported.
    auto base_length = SVGLength::create(realm(), 0, m_y2.value_or(0));
    auto anim_length = SVGLength::create(realm(), 0, m_y2.value_or(0));
    return SVGAnimatedLength::create(realm(), move(base_length), move(anim_length));
}

}
