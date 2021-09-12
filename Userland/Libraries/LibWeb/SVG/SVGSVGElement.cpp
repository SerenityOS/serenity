/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::SVG {

SVGSVGElement::SVGSVGElement(DOM::Document& document, QualifiedName qualified_name)
    : SVGGraphicsElement(document, qualified_name)
{
}

RefPtr<Layout::Node> SVGSVGElement::create_layout_node()
{
    auto style = document().style_resolver().resolve_style(*this);
    if (style->display() == CSS::Display::None)
        return nullptr;
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

}
