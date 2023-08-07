/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/SVG/SVGClipPathElement.h>

namespace Web::SVG {

SVGClipPathElement::SVGClipPathElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGElement(document, move(qualified_name))
{
}

SVGClipPathElement::~SVGClipPathElement()
{
}

void SVGClipPathElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGClipPathElementPrototype>(realm, "SVGClipPathElement"));
}

JS::GCPtr<Layout::Node> SVGClipPathElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties>)
{
    return nullptr;
}

}
