/*
 * Copyright (c) 2022, Simon Danner <danner.simon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Layout/SVGBox.h>
#include <LibWeb/SVG/SVGDefsElement.h>

namespace Web::SVG {

JS_DEFINE_ALLOCATOR(SVGDefsElement);

SVGDefsElement::SVGDefsElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, move(qualified_name))
{
}

SVGDefsElement::~SVGDefsElement()
{
}

void SVGDefsElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGDefsElementPrototype>(realm, "SVGDefsElement"_fly_string));
}

JS::GCPtr<Layout::Node> SVGDefsElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    // FIXME: We need this layout node so any <mask>s inside this element get layout computed.
    return heap().allocate_without_realm<Layout::SVGBox>(document(), *this, move(style));
}

}
