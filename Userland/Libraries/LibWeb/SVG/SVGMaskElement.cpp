/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/SVGMaskElementPrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/SVGGraphicsBox.h>
#include <LibWeb/SVG/SVGMaskElement.h>

namespace Web::SVG {

SVGMaskElement::SVGMaskElement(DOM::Document& document, DOM::QualifiedName tag_name)
    : SVGGraphicsElement(document, move(tag_name))
{
}

SVGMaskElement::~SVGMaskElement() = default;

void SVGMaskElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGMaskElementPrototype>(realm, "SVGMaskElement"));
}

JS::GCPtr<Layout::Node> SVGMaskElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::SVGGraphicsBox>(document(), *this, move(style));
}

}
