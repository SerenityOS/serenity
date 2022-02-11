/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SVGLineElement.h"
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>

namespace Web::SVG {

SVGLineElement::SVGLineElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGGeometryElement(document, qualified_name)
{
}

void SVGLineElement::parse_attribute(FlyString const& name, String const& value)
{
    SVGGeometryElement::parse_attribute(name, value);

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

}
