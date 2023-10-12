/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGPolygonElement.h>

namespace Web::SVG {

SVGPolygonElement::SVGPolygonElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGeometryElement(document, qualified_name)
{
}

void SVGPolygonElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGPolygonElementPrototype>(realm, "SVGPolygonElement"));
}

void SVGPolygonElement::attribute_changed(FlyString const& name, Optional<DeprecatedString> const& value)
{
    SVGGeometryElement::attribute_changed(name, value);

    if (name == SVG::AttributeNames::points) {
        m_points = AttributeParser::parse_points(value.value_or(""));
        m_path.clear();
    }
}

Gfx::Path& SVGPolygonElement::get_path()
{
    if (m_path.has_value())
        return m_path.value();

    Gfx::Path path;

    if (m_points.is_empty()) {
        m_path = move(path);
        return m_path.value();
    }

    // 1. perform an absolute moveto operation to the first coordinate pair in the list of points
    path.move_to(m_points.first());

    // 2. for each subsequent coordinate pair, perform an absolute lineto operation to that coordinate pair.
    for (size_t point_index = 1; point_index < m_points.size(); ++point_index)
        path.line_to(m_points[point_index]);

    // 3. perform a closepath command
    path.close();

    m_path = move(path);
    return m_path.value();
}

}
