/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SVGPolylineElement.h"
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>

namespace Web::SVG {

SVGPolylineElement::SVGPolylineElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGeometryElement(document, qualified_name)
{
}

void SVGPolylineElement::parse_attribute(FlyString const& name, String const& value)
{
    SVGGeometryElement::parse_attribute(name, value);

    if (name == SVG::AttributeNames::points) {
        m_points = AttributeParser::parse_points(value);
        m_path.clear();
    }
}

Gfx::Path& SVGPolylineElement::get_path()
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

    m_path = move(path);
    return m_path.value();
}

}
