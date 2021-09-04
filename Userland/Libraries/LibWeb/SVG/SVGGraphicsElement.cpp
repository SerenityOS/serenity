/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/SVG/SVGGraphicsElement.h>

namespace Web::SVG {

SVGGraphicsElement::SVGGraphicsElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGElement(document, move(qualified_name))
{
}

void SVGGraphicsElement::parse_attribute(FlyString const& name, String const& value)
{
    SVGElement::parse_attribute(name, value);

    if (name == "fill") {
        m_fill_color = Gfx::Color::from_string(value).value_or(Color::Transparent);
    } else if (name == "stroke") {
        m_stroke_color = Gfx::Color::from_string(value).value_or(Color::Transparent);
    } else if (name == "stroke-width") {
        auto result = value.to_int();
        if (result.has_value())
            m_stroke_width = result.value();
    }
}

}
