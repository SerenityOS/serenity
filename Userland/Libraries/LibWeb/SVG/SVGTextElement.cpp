/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/SVGTextBox.h>
#include <LibWeb/SVG/SVGTextElement.h>

namespace Web::SVG {

SVGTextElement::SVGTextElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGTextPositioningElement(document, move(qualified_name))
{
}

JS::ThrowCompletionOr<void> SVGTextElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGTextElementPrototype>(realm, "SVGTextElement"));

    return {};
}

JS::GCPtr<Layout::Node> SVGTextElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::SVGTextBox>(document(), *this, move(style));
}

}
