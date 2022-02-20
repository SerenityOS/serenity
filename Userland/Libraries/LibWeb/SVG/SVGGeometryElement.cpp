/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::SVG {

SVGGeometryElement::SVGGeometryElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, move(qualified_name))
{
}

RefPtr<Layout::Node> SVGGeometryElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return adopt_ref(*new Layout::SVGGeometryBox(document(), *this, move(style)));
}

}
