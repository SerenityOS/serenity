/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/SVGGraphicsBox.h>
#include <LibWeb/SVG/SVGGElement.h>

namespace Web::SVG {

JS_DEFINE_ALLOCATOR(SVGGElement);

SVGGElement::SVGGElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, move(qualified_name))
{
}

JS::GCPtr<Layout::Node> SVGGElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::SVGGraphicsBox>(document(), *this, move(style));
}

}
