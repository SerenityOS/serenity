/*
 * Copyright (c) 2022, Simon Danner <danner.simon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Window.h>
#include <LibWeb/SVG/SVGDefsElement.h>

namespace Web::SVG {

SVGDefsElement::SVGDefsElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, move(qualified_name))
{
    set_prototype(&window().cached_web_prototype("SVGDefsElement"));
}

SVGDefsElement::~SVGDefsElement()
{
}

RefPtr<Layout::Node> SVGDefsElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties>)
{
    return nullptr;
}

}
