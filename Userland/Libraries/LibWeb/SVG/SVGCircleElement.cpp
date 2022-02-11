/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SVGCircleElement.h"
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>

namespace Web::SVG {

SVGCircleElement::SVGCircleElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGGeometryElement(document, qualified_name)
{
}

void SVGCircleElement::parse_attribute(FlyString const& name, String const& value)
{
    SVGGeometryElement::parse_attribute(name, value);

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

}
