/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URL.h>
#include <LibWeb/Layout/SVGTextPathBox.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/SVGTextPathElement.h>

namespace Web::SVG {

JS_DEFINE_ALLOCATOR(SVGTextPathElement);

SVGTextPathElement::SVGTextPathElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGTextContentElement(document, move(qualified_name))
{
}

JS::GCPtr<SVGGeometryElement const> SVGTextPathElement::path_or_shape() const
{
    auto href = get_attribute(AttributeNames::href);
    if (!href.has_value())
        return {};
    auto url = document().url().complete_url(*href);
    return try_resolve_url_to<SVGGeometryElement const>(url);
}

void SVGTextPathElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGTextPathElementPrototype>(realm, "SVGTextPathElement"_fly_string));
}

JS::GCPtr<Layout::Node> SVGTextPathElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::SVGTextPathBox>(document(), *this, move(style));
}

};
