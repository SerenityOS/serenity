/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGTextBox.h>
#include <LibWeb/SVG/SVGTSpanElement.h>
#include <LibWeb/SVG/SVGTextElement.h>

namespace Web::SVG {

SVGTSpanElement::SVGTSpanElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGTextPositioningElement(document, move(qualified_name))
{
}

void SVGTSpanElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGTSpanElementPrototype>(realm, "SVGTSpanElement"));
}

JS::GCPtr<Layout::Node> SVGTSpanElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    // Text must be within an SVG <text> element.
    if (shadow_including_first_ancestor_of_type<SVGTextElement>())
        return heap().allocate_without_realm<Layout::SVGTextBox>(document(), *this, move(style));
    return {};
}

}
