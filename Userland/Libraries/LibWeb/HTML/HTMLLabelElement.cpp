/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLLabelElement.h>
#include <LibWeb/Layout/Label.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLLabelElement);

HTMLLabelElement::HTMLLabelElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLLabelElement::~HTMLLabelElement() = default;

void HTMLLabelElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(HTMLLabelElement);
}

JS::GCPtr<Layout::Node> HTMLLabelElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::Label>(document(), this, move(style));
}

}
