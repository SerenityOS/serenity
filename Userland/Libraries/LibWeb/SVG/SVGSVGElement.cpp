/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::SVG {

SVGSVGElement::SVGSVGElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGGraphicsElement(document, qualified_name)
{
}

RefPtr<Layout::Node> SVGSVGElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return adopt_ref(*new Layout::SVGSVGBox(document(), *this, move(style)));
}

unsigned SVGSVGElement::width() const
{
    return attribute(HTML::AttributeNames::width).to_uint().value_or(300);
}

unsigned SVGSVGElement::height() const
{
    return attribute(HTML::AttributeNames::height).to_uint().value_or(150);
}

void SVGSVGElement::parse_attribute(FlyString const& name, String const& value)
{
    SVGGraphicsElement::parse_attribute(name, value);

    if (name.equals_ignoring_case(SVG::AttributeNames::viewBox))
        m_view_box = try_parse_view_box(value);
}

}
