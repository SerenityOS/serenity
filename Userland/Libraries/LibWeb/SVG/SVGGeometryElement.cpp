/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Window.h>
#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

SVGGeometryElement::SVGGeometryElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, move(qualified_name))
{
    set_prototype(&window().cached_web_prototype("SVGGeometryElement"));
}

RefPtr<Layout::Node> SVGGeometryElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return adopt_ref(*new Layout::SVGGeometryBox(document(), *this, move(style)));
}

float SVGGeometryElement::get_total_length()
{
    return 0;
}

NonnullRefPtr<Geometry::DOMPoint> SVGGeometryElement::get_point_at_length(float distance)
{
    (void)distance;
    return Geometry::DOMPoint::create(0, 0, 0, 0);
}

}
